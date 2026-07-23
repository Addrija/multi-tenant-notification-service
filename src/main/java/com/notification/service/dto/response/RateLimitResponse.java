package com.notification.service.dto.response;

import com.notification.service.entity.RateLimitConfig;

public record RateLimitResponse(
        Long tenantId,
        int capacity,
        double refillRatePerSec
) {
    public static RateLimitResponse from(RateLimitConfig config) {
        return new RateLimitResponse(config.getTenant().getId(), config.getCapacity(), config.getRefillRatePerSec());
    }
}