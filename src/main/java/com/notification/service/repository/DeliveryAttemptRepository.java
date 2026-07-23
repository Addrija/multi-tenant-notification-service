package com.notification.service.repository;

import com.notification.service.entity.DeliveryAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface DeliveryAttemptRepository extends JpaRepository<DeliveryAttempt, Long> {
    List<DeliveryAttempt> findByNotificationIdOrderByAttemptNumberAsc(UUID notificationId);
}