package com.notification.service.dto.response;

import java.util.List;
import java.util.UUID;

public record NotificationHistoryResponse(
        UUID notificationId,
        List<StatusHistoryEntry> statusHistory,
        List<DeliveryAttemptEntry> deliveryAttempts
) {
}