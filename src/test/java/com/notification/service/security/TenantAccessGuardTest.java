package com.notification.service.security;

import com.notification.service.entity.AppUser;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.Role;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TenantAccessGuardTest {

    private final TenantAccessGuard guard = new TenantAccessGuard();

    @Test
    void platformAdminCanAccessAnyTenant() {
        AppUserPrincipal principal = principal(Role.PLATFORM_ADMIN, null);

        assertThatCode(() -> guard.assertTenantAccess(principal, 999L)).doesNotThrowAnyException();
    }

    @Test
    void tenantAdminCanAccessOwnTenant() {
        AppUserPrincipal principal = principal(Role.TENANT_ADMIN, 5L);

        assertThatCode(() -> guard.assertTenantAccess(principal, 5L)).doesNotThrowAnyException();
    }

    @Test
    void tenantAdminCannotAccessAnotherTenant() {
        AppUserPrincipal principal = principal(Role.TENANT_ADMIN, 5L);

        assertThatThrownBy(() -> guard.assertTenantAccess(principal, 6L))
                .isInstanceOf(AccessDeniedException.class);
    }

    private AppUserPrincipal principal(Role role, Long tenantId) {
        Tenant tenant = tenantId == null ? null : Tenant.builder().id(tenantId).name("t").build();
        AppUser appUser = AppUser.builder()
                .id(1L)
                .tenant(tenant)
                .username("user")
                .passwordHash("hash")
                .role(role)
                .build();
        return new AppUserPrincipal(appUser);
    }
}