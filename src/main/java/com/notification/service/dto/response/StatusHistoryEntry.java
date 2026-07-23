package com.notification.service.dto.response;

import com.notification.service.entity.NotificationStatusHistory;
import com.notification.service.entity.enums.NotificationStatus;

import java.time.Instant;

public record StatusHistoryEntry(
        NotificationStatus fromStatus,
        NotificationStatus toStatus,
        Instant changedAt,
        String reason
) {
    public static StatusHistoryEntry from(NotificationStatusHistory history) {
        return new StatusHistoryEntry(
                history.getFromStatus(), history.getToStatus(), history.getChangedAt(), history.getReason());
    }
}