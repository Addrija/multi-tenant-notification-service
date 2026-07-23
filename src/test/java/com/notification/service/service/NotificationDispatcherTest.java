package com.notification.service.service;

import com.notification.service.channel.ChannelSender;
import com.notification.service.channel.ChannelSenderRegistry;
import com.notification.service.channel.SendResult;
import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.Notification;
import com.notification.service.entity.NotificationStatusHistory;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import com.notification.service.repository.ChannelConfigRepository;
import com.notification.service.repository.DeliveryAttemptRepository;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.NotificationStatusHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.orm.ObjectOptimisticLockingFailureException;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationDispatcherTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationStatusHistoryRepository statusHistoryRepository;
    @Mock private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private ChannelConfigRepository channelConfigRepository;
    @Mock private RateLimiterService rateLimiterService;
    @Mock private RetryPolicy retryPolicy;
    @Mock private ChannelSenderRegistry channelSenderRegistry;
    @Mock private ChannelSender channelSender;

    private NotificationDispatcher dispatcher;
    private Tenant tenant;
    private UUID notificationId;

    @BeforeEach
    void setUp() {
        dispatcher = new NotificationDispatcher(
                notificationRepository, statusHistoryRepository, deliveryAttemptRepository,
                channelConfigRepository, rateLimiterService, retryPolicy, channelSenderRegistry);

        tenant = Tenant.builder().id(2L).name("Acme").build();
        notificationId = UUID.randomUUID();
    }

    private Notification pendingNotification() {
        return Notification.builder()
                .id(notificationId).tenant(tenant).channelType(ChannelType.EMAIL)
                .recipient("jane@example.com").renderedContent("hi").status(NotificationStatus.PENDING)
                .attemptCount(0).build();
    }

    @Test
    void doesNothingWhenNotificationNoLongerExists() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.empty());

        dispatcher.dispatchOne(notificationId);

        verify(rateLimiterService, never()).tryAcquire(any());
    }

    @Test
    void skipsWhenNotInDispatchableStatus() {
        Notification alreadySent = pendingNotification();
        alreadySent.setStatus(NotificationStatus.SENT);
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(alreadySent));

        dispatcher.dispatchOne(notificationId);

        verify(rateLimiterService, never()).tryAcquire(any());
    }

    @Test
    void leavesUntouchedWhenRateLimited() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(false);

        dispatcher.dispatchOne(notificationId);

        verify(notificationRepository, never()).saveAndFlush(any());
    }

    @Test
    void skipsWhenClaimLosesOptimisticLockRace() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(true);
        when(notificationRepository.saveAndFlush(any())).thenThrow(new ObjectOptimisticLockingFailureException(Notification.class, notificationId));

        dispatcher.dispatchOne(notificationId);

        verify(channelConfigRepository, never()).findByTenantIdAndChannelType(any(), any());
    }

    @Test
    void failsPermanentlyWhenChannelNotConfigured() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(true);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.EMAIL)).thenReturn(Optional.empty());

        dispatcher.dispatchOne(notificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
        verify(deliveryAttemptRepository, never()).save(any());
    }

    @Test
    void marksSentOnSuccessfulSend() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(true);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        ChannelConfig config = ChannelConfig.builder().tenant(tenant).channelType(ChannelType.EMAIL).enabled(true).build();
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.EMAIL)).thenReturn(Optional.of(config));
        when(channelSenderRegistry.getSender(ChannelType.EMAIL)).thenReturn(channelSender);
        when(channelSender.send(any(), any(), any())).thenReturn(SendResult.ok());

        dispatcher.dispatchOne(notificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.SENT);
        assertThat(captor.getValue().getAttemptCount()).isEqualTo(1);
        verify(deliveryAttemptRepository).save(any());
    }

    @Test
    void marksRetryingOnFailureWithAttemptsRemaining() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(true);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        ChannelConfig config = ChannelConfig.builder().tenant(tenant).channelType(ChannelType.EMAIL).enabled(true).build();
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.EMAIL)).thenReturn(Optional.of(config));
        when(channelSenderRegistry.getSender(ChannelType.EMAIL)).thenReturn(channelSender);
        when(channelSender.send(any(), any(), any())).thenReturn(SendResult.failure("timeout"));
        when(retryPolicy.shouldRetry(1)).thenReturn(true);
        Instant nextRetry = Instant.parse("2026-01-01T00:00:10Z");
        when(retryPolicy.nextRetryAt(eq(1), any())).thenReturn(nextRetry);

        dispatcher.dispatchOne(notificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.RETRYING);
        assertThat(captor.getValue().getNextRetryAt()).isEqualTo(nextRetry);
    }

    @Test
    void marksFailedWhenRetriesExhausted() {
        when(notificationRepository.findById(notificationId)).thenReturn(Optional.of(pendingNotification()));
        when(rateLimiterService.tryAcquire(2L)).thenReturn(true);
        when(notificationRepository.saveAndFlush(any())).thenAnswer(inv -> inv.getArgument(0));
        ChannelConfig config = ChannelConfig.builder().tenant(tenant).channelType(ChannelType.EMAIL).enabled(true).build();
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.EMAIL)).thenReturn(Optional.of(config));
        when(channelSenderRegistry.getSender(ChannelType.EMAIL)).thenReturn(channelSender);
        when(channelSender.send(any(), any(), any())).thenReturn(SendResult.failure("permanent failure"));
        when(retryPolicy.shouldRetry(1)).thenReturn(false);

        dispatcher.dispatchOne(notificationId);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository, atLeastOnce()).save(captor.capture());
        assertThat(captor.getValue().getStatus()).isEqualTo(NotificationStatus.FAILED);
    }
}