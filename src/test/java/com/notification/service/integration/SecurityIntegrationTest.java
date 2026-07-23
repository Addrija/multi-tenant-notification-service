package com.notification.service.integration;

import com.notification.service.entity.AppUser;
import com.notification.service.entity.Tenant;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises the real Spring Security filter chain (HTTP Basic against the
// real UserDetailsService/password check) rather than @WithMockUser, which
// would bypass authentication entirely and only prove the authorization
// rule, not the login itself.
class SecurityIntegrationTest extends AbstractIntegrationTest {

    private Tenant tenantA;
    private Tenant tenantB;
    private AppUser tenantAdminA;
    private AppUser platformAdmin;

    @BeforeEach
    void setUp() {
        tenantA = createTenant("tenant-a");
        tenantB = createTenant("tenant-b");
        tenantAdminA = createTenantAdmin("admin-a", "Password123", tenantA);
        platformAdmin = createPlatformAdmin("platform", "Password123");
    }

    @AfterEach
    void tearDown() {
        appUserRepository.delete(platformAdmin);
        cleanUpTenant(tenantA);
        cleanUpTenant(tenantB);
    }

    @Test
    void rejectsRequestWithNoCredentials() throws Exception {
        mockMvc.perform(get("/api/tenants/" + tenantA.getId() + "/templates"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void rejectsWrongPassword() throws Exception {
        mockMvc.perform(get("/api/tenants/" + tenantA.getId() + "/templates")
                        .with(basicAuth(tenantAdminA.getUsername(), "WrongPassword")))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tenantAdminCannotAccessPlatformAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/tenants")
                        .with(basicAuth(tenantAdminA.getUsername(), "Password123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void tenantAdminCanAccessOwnTenant() throws Exception {
        mockMvc.perform(get("/api/tenants/" + tenantA.getId() + "/templates")
                        .with(basicAuth(tenantAdminA.getUsername(), "Password123")))
                .andExpect(status().isOk());
    }

    @Test
    void tenantAdminCannotAccessAnotherTenant() throws Exception {
        mockMvc.perform(get("/api/tenants/" + tenantB.getId() + "/templates")
                        .with(basicAuth(tenantAdminA.getUsername(), "Password123")))
                .andExpect(status().isForbidden());
    }

    @Test
    void platformAdminCanAccessAdminEndpoints() throws Exception {
        mockMvc.perform(get("/api/admin/tenants")
                        .with(basicAuth(platformAdmin.getUsername(), "Password123")))
                .andExpect(status().isOk());
    }
}
