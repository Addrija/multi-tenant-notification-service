package com.notification.service.dto.request;

import jakarta.validation.constraints.Positive;

public record RateLimitUpdateRequest(
        @Positive int capacity,
        @Positive double refillRatePerSec
) {
}