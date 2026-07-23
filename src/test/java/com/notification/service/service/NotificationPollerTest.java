package com.notification.service.service;

import com.notification.service.entity.Notification;
import com.notification.service.entity.Tenant;
import com.notification.service.repository.NotificationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationPollerTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private TenantFairnessScheduler fairnessScheduler;
    @Mock private NotificationDispatcher notificationDispatcher;
    @Mock private ThreadPoolTaskExecutor executor;

    private NotificationPoller poller;

    @BeforeEach
    void setUp() {
        poller = new NotificationPoller(notificationRepository, fairnessScheduler, notificationDispatcher, executor, 500);
    }

    @Test
    void submitsEachInterleavedNotificationToTheExecutor() {
        // executor.execute runs the Runnable synchronously in this test, so we can verify the effect.
        doAnswer(inv -> {
            inv.getArgument(0, Runnable.class).run();
            return null;
        }).when(executor).execute(any());

        Tenant tenant = Tenant.builder().id(1L).name("t").build();
        Notification n1 = Notification.builder().id(UUID.randomUUID()).tenant(tenant).build();
        Notification n2 = Notification.builder().id(UUID.randomUUID()).tenant(tenant).build();

        when(notificationRepository.findDueForDispatch(any(), any())).thenReturn(List.of(n1, n2));
        when(fairnessScheduler.interleave(List.of(n1, n2))).thenReturn(List.of(n1, n2));

        poller.poll();

        verify(notificationDispatcher).dispatchOne(n1.getId());
        verify(notificationDispatcher).dispatchOne(n2.getId());
    }

    @Test
    void submitsNothingWhenNoNotificationsAreDue() {
        when(notificationRepository.findDueForDispatch(any(), any())).thenReturn(List.of());
        when(fairnessScheduler.interleave(List.of())).thenReturn(List.of());

        poller.poll();

        verify(notificationDispatcher, org.mockito.Mockito.never()).dispatchOne(any());
    }
}