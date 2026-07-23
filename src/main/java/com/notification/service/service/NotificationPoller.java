package com.notification.service.service;

import com.notification.service.entity.Notification;
import com.notification.service.repository.NotificationRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

// When to look for work: pull a bounded due batch (the DB is the queue - no
// separate broker needed), interleave by tenant for fairness, hand each
// notification to the bounded worker pool. What happens to one notification
// once picked up lives in NotificationDispatcher.
@Component
public class NotificationPoller {

    private final NotificationRepository notificationRepository;
    private final TenantFairnessScheduler fairnessScheduler;
    private final NotificationDispatcher notificationDispatcher;
    private final ThreadPoolTaskExecutor notificationDispatchExecutor;
    private final int batchSize;

    public NotificationPoller(NotificationRepository notificationRepository,
                               TenantFairnessScheduler fairnessScheduler,
                               NotificationDispatcher notificationDispatcher,
                               ThreadPoolTaskExecutor notificationDispatchExecutor,
                               @Value("${notification.dispatcher.batch-size}") int batchSize) {
        this.notificationRepository = notificationRepository;
        this.fairnessScheduler = fairnessScheduler;
        this.notificationDispatcher = notificationDispatcher;
        this.notificationDispatchExecutor = notificationDispatchExecutor;
        this.batchSize = batchSize;
    }

    @Scheduled(fixedDelayString = "${notification.dispatcher.poll-interval-ms}")
    public void poll() {
        List<Notification> due = notificationRepository.findDueForDispatch(
                Instant.now(), PageRequest.of(0, batchSize));
        for (Notification notification : fairnessScheduler.interleave(due)) {
            UUID notificationId = notification.getId();
            notificationDispatchExecutor.execute(() -> notificationDispatcher.dispatchOne(notificationId));
        }
    }
}