package com.notification.service.service;

import com.notification.service.channel.ChannelSender;
import com.notification.service.channel.ChannelSenderRegistry;
import com.notification.service.channel.SendResult;
import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.DeliveryAttempt;
import com.notification.service.entity.Notification;
import com.notification.service.entity.NotificationStatusHistory;
import com.notification.service.entity.enums.DeliveryOutcome;
import com.notification.service.entity.enums.NotificationStatus;
import com.notification.service.repository.ChannelConfigRepository;
import com.notification.service.repository.DeliveryAttemptRepository;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.NotificationStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

// What happens to one notification once NotificationPoller hands it over: claim
// it (optimistic-lock guarded, prevents duplicate dispatch on a concurrent
// claim race), rate-check, send via the right channel, record the outcome.
@Component
@RequiredArgsConstructor
public class NotificationDispatcher {

    private final NotificationRepository notificationRepository;
    private final NotificationStatusHistoryRepository statusHistoryRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final ChannelConfigRepository channelConfigRepository;
    private final RateLimiterService rateLimiterService;
    private final RetryPolicy retryPolicy;
    private final ChannelSenderRegistry channelSenderRegistry;

    @Transactional
    public void dispatchOne(UUID notificationId) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            return;
        }
        if (notification.getStatus() != NotificationStatus.PENDING
                && notification.getStatus() != NotificationStatus.RETRYING) {
            return;
        }

        Long tenantId = notification.getTenant().getId();
        if (!rateLimiterService.tryAcquire(tenantId)) {
            return;
        }

        NotificationStatus claimedFrom = notification.getStatus();
        notification.setStatus(NotificationStatus.IN_PROGRESS);
        notification.setUpdatedAt(Instant.now());
        try {
            notification = notificationRepository.saveAndFlush(notification);
        } catch (ObjectOptimisticLockingFailureException e) {
            // Lost the claim race to another dispatch attempt - it's being handled elsewhere, skip.
            return;
        }
        recordStatusChange(notification, claimedFrom, NotificationStatus.IN_PROGRESS, "Claimed by dispatcher");

        ChannelConfig channelConfig = channelConfigRepository
                .findByTenantIdAndChannelType(tenantId, notification.getChannelType())
                .orElse(null);
        if (channelConfig == null || !channelConfig.isEnabled()) {
            failPermanently(notification,
                    "Channel " + notification.getChannelType() + " is not configured or disabled for this tenant");
            return;
        }

        ChannelSender sender = channelSenderRegistry.getSender(notification.getChannelType());
        SendResult result = sender.send(notification.getRecipient(), notification.getRenderedContent(), channelConfig);

        int attemptNumber = notification.getAttemptCount() + 1;
        notification.setAttemptCount(attemptNumber);

        if (result.success()) {
            handleSendSuccess(notification, attemptNumber);
        } else {
            handleSendFailure(notification, result.errorMessage(), attemptNumber);
        }
    }

    private void handleSendSuccess(Notification notification, int attemptNumber) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);
        deliveryAttemptRepository.save(DeliveryAttempt.builder()
                .notification(notification).attemptNumber(attemptNumber)
                .channelType(notification.getChannelType()).outcome(DeliveryOutcome.SUCCESS)
                .attemptedAt(Instant.now()).build());
        recordStatusChange(notification, NotificationStatus.IN_PROGRESS, NotificationStatus.SENT, "Delivered successfully");
    }

    private void handleSendFailure(Notification notification, String errorMessage, int attemptNumber) {
        NotificationStatus toStatus;
        if (retryPolicy.shouldRetry(attemptNumber)) {
            toStatus = NotificationStatus.RETRYING;
            notification.setNextRetryAt(retryPolicy.nextRetryAt(attemptNumber, Instant.now()));
        } else {
            toStatus = NotificationStatus.FAILED;
        }
        notification.setStatus(toStatus);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);
        deliveryAttemptRepository.save(DeliveryAttempt.builder()
                .notification(notification).attemptNumber(attemptNumber)
                .channelType(notification.getChannelType()).outcome(DeliveryOutcome.FAILURE)
                .errorMessage(errorMessage).attemptedAt(Instant.now()).build());
        recordStatusChange(notification, NotificationStatus.IN_PROGRESS, toStatus, errorMessage);
    }

    private void failPermanently(Notification notification, String reason) {
        notification.setStatus(NotificationStatus.FAILED);
        notification.setUpdatedAt(Instant.now());
        notificationRepository.save(notification);
        recordStatusChange(notification, NotificationStatus.IN_PROGRESS, NotificationStatus.FAILED, reason);
    }

    private void recordStatusChange(Notification notification, NotificationStatus from,
                                     NotificationStatus to, String reason) {
        statusHistoryRepository.save(NotificationStatusHistory.builder()
                .notification(notification).fromStatus(from).toStatus(to).reason(reason).build());
    }
}