package com.notification.service.dto.request;

import jakarta.validation.constraints.NotBlank;

public record TenantCreateRequest(
        @NotBlank String name
) {
}