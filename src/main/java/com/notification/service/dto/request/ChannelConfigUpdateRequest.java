package com.notification.service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;

// All fields optional - only non-null fields are applied (partial update).
public record ChannelConfigUpdateRequest(
        Boolean enabled,
        String senderIdentity,
        @DecimalMin("0.0") @DecimalMax("1.0") Double simulatedFailureRate
) {
}