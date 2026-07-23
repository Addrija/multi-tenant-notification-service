package com.notification.service.dto.response;

import com.notification.service.entity.DeliveryAttempt;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.DeliveryOutcome;

import java.time.Instant;

public record DeliveryAttemptEntry(
        int attemptNumber,
        ChannelType channelType,
        DeliveryOutcome outcome,
        String errorMessage,
        Instant attemptedAt
) {
    public static DeliveryAttemptEntry from(DeliveryAttempt attempt) {
        return new DeliveryAttemptEntry(
                attempt.getAttemptNumber(),
                attempt.getChannelType(),
                attempt.getOutcome(),
                attempt.getErrorMessage(),
                attempt.getAttemptedAt());
    }
}