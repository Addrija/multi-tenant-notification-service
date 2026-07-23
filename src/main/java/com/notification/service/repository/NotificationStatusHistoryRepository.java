package com.notification.service.repository;

import com.notification.service.entity.NotificationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NotificationStatusHistoryRepository extends JpaRepository<NotificationStatusHistory, Long> {
    List<NotificationStatusHistory> findByNotificationIdOrderByChangedAtAsc(UUID notificationId);
}