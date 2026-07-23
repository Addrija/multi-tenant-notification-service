package com.notification.service.dto.request;

import com.notification.service.entity.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

// tenantId is required when role=TENANT_ADMIN and must be omitted when
// role=PLATFORM_ADMIN - that cross-field rule is checked in AppUserService,
// not expressible cleanly as a single-field bean validation annotation.
public record AppUserCreateRequest(
        @NotBlank String username,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String password,
        @NotNull Role role,
        Long tenantId
) {
}
