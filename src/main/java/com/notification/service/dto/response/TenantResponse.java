package com.notification.service.dto.response;

import com.notification.service.entity.Tenant;

import java.time.Instant;

public record TenantResponse(
        Long id,
        String name,
        Instant createdAt
) {
    public static TenantResponse from(Tenant tenant) {
        return new TenantResponse(tenant.getId(), tenant.getName(), tenant.getCreatedAt());
    }
}