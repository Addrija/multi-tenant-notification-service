package com.notification.service.dto.request;

import com.notification.service.entity.enums.ChannelType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.Instant;
import java.util.Map;

// scheduledAt null or in the past = immediate send; future = scheduled send.
public record NotificationCreateRequest(
        @NotNull ChannelType channelType,
        @NotNull Long templateId,
        @NotBlank String recipient,
        Map<String, String> variables,
        Instant scheduledAt
) {
}