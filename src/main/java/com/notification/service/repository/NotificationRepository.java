package com.notification.service.repository;

import com.notification.service.entity.Notification;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Optional<Notification> findByIdAndTenantId(UUID id, Long tenantId);

    // Delivery report: status/channelType are optional filters (null = no filter on that field).
    @Query("""
        SELECT n FROM Notification n
        WHERE n.tenant.id = :tenantId
          AND (:status IS NULL OR n.status = :status)
          AND (:channelType IS NULL OR n.channelType = :channelType)
        ORDER BY n.createdAt DESC
        """)
    List<Notification> findByTenantIdWithFilters(
            @Param("tenantId") Long tenantId,
            @Param("status") NotificationStatus status,
            @Param("channelType") ChannelType channelType,
            Pageable pageable);

    // Batch of due work for the poller: newly PENDING sends whose scheduledAt has
    // arrived, plus RETRYING sends whose backoff window (nextRetryAt) has elapsed.
    @Query("""
        SELECT n FROM Notification n
        WHERE (n.status = com.notification.service.entity.enums.NotificationStatus.PENDING
               AND (n.scheduledAt IS NULL OR n.scheduledAt <= :now))
           OR (n.status = com.notification.service.entity.enums.NotificationStatus.RETRYING
               AND n.nextRetryAt <= :now)
        ORDER BY n.scheduledAt ASC NULLS FIRST
        """)
    List<Notification> findDueForDispatch(@Param("now") Instant now, Pageable pageable);
}