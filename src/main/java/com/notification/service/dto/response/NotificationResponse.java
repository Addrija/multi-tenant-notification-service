package com.notification.service.dto.response;

import com.notification.service.entity.Notification;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

public record NotificationResponse(
        UUID id,
        Long tenantId,
        ChannelType channelType,
        Long templateId,
        String recipient,
        Map<String, String> variables,
        String renderedContent,
        NotificationStatus status,
        Instant scheduledAt,
        int attemptCount,
        Instant nextRetryAt,
        Instant createdAt,
        Instant updatedAt
) {
    // variables is passed in already-deserialized since parsing the stored JSON
    // text requires the ObjectMapper bean, which isn't available to a plain DTO.
    public static NotificationResponse from(Notification notification, Map<String, String> variables) {
        return new NotificationResponse(
                notification.getId(),
                notification.getTenant().getId(),
                notification.getChannelType(),
                notification.getTemplate() != null ? notification.getTemplate().getId() : null,
                notification.getRecipient(),
                variables,
                notification.getRenderedContent(),
                notification.getStatus(),
                notification.getScheduledAt(),
                notification.getAttemptCount(),
                notification.getNextRetryAt(),
                notification.getCreatedAt(),
                notification.getUpdatedAt());
    }
}