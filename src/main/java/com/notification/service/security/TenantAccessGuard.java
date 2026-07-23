package com.notification.service.security;

import com.notification.service.entity.enums.Role;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Component;

// Service-layer check for tenant-admin endpoints: PLATFORM_ADMIN isn't scoped to any
// tenant and always passes; TENANT_ADMIN must match the tenantId in the request path.
@Component
public class TenantAccessGuard {

    public void assertTenantAccess(AppUserPrincipal principal, Long pathTenantId) {
        if (principal.getRole() == Role.PLATFORM_ADMIN) {
            return;
        }
        if (!principal.getTenantId().equals(pathTenantId)) {
            throw new AccessDeniedException(
                    "User is not authorized to access tenant " + pathTenantId);
        }
    }
}