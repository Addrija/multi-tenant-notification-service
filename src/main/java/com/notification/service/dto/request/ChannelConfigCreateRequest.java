package com.notification.service.dto.request;

import com.notification.service.entity.enums.ChannelType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

public record ChannelConfigCreateRequest(
        @NotNull ChannelType channelType,
        Boolean enabled,
        String senderIdentity,
        @DecimalMin("0.0") @DecimalMax("1.0") Double simulatedFailureRate
) {
}