package com.notification.service.integration;

import com.notification.service.entity.AppUser;
import com.notification.service.entity.Notification;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.Role;
import com.notification.service.repository.AppUserRepository;
import com.notification.service.repository.ChannelConfigRepository;
import com.notification.service.repository.DeliveryAttemptRepository;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.NotificationStatusHistoryRepository;
import com.notification.service.repository.RateLimitConfigRepository;
import com.notification.service.repository.TemplateRepository;
import com.notification.service.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;
import tools.jackson.databind.json.JsonMapper;

import java.util.UUID;

// Boots the full app context (real Spring Security filter chain, real
// scheduled poller, real MySQL via the "local" profile) rather than mocking
// layers - these tests exercise the actual wiring, not isolated units.
// A uniqueSuffix per test avoids username/tenant-name collisions across
// classes and repeated runs since this is the same DB the app itself uses.
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
@ActiveProfiles("local")
public abstract class AbstractIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected TenantRepository tenantRepository;
    @Autowired protected AppUserRepository appUserRepository;
    @Autowired protected PasswordEncoder passwordEncoder;
    @Autowired protected JsonMapper jsonMapper;
    @Autowired protected ChannelConfigRepository channelConfigRepository;
    @Autowired protected TemplateRepository templateRepository;
    @Autowired protected RateLimitConfigRepository rateLimitConfigRepository;
    @Autowired protected NotificationRepository notificationRepository;
    @Autowired protected NotificationStatusHistoryRepository notificationStatusHistoryRepository;
    @Autowired protected DeliveryAttemptRepository deliveryAttemptRepository;

    protected String uniqueSuffix;

    @BeforeEach
    void setUpUniqueSuffix() {
        uniqueSuffix = UUID.randomUUID().toString().substring(0, 8);
    }

    protected Tenant createTenant(String namePrefix) {
        return tenantRepository.save(Tenant.builder().name(namePrefix + "-" + uniqueSuffix).build());
    }

    protected AppUser createTenantAdmin(String usernamePrefix, String rawPassword, Tenant tenant) {
        return appUserRepository.save(AppUser.builder()
                .tenant(tenant)
                .username(usernamePrefix + "-" + uniqueSuffix)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.TENANT_ADMIN)
                .build());
    }

    protected AppUser createPlatformAdmin(String usernamePrefix, String rawPassword) {
        return appUserRepository.save(AppUser.builder()
                .tenant(null)
                .username(usernamePrefix + "-" + uniqueSuffix)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role(Role.PLATFORM_ADMIN)
                .build());
    }

    protected String toJson(Object value) {
        return jsonMapper.writeValueAsString(value);
    }

    protected RequestPostProcessor basicAuth(String username, String password) {
        return SecurityMockMvcRequestPostProcessors.httpBasic(username, password);
    }

    // Deletes everything under a tenant in FK-safe order (attempts/history -> notifications
    // -> channel configs/templates/rate limit -> app users -> tenant). Tests create their own
    // fixtures against the real shared DB (no separate test schema), so cleanup must be
    // thorough regardless of which test methods created rows along the way.
    protected void cleanUpTenant(Tenant tenant) {
        for (Notification notification : notificationRepository
                .findByTenantIdWithFilters(tenant.getId(), null, null, PageRequest.of(0, 1000))) {
            deliveryAttemptRepository.deleteAll(
                    deliveryAttemptRepository.findByNotificationIdOrderByAttemptNumberAsc(notification.getId()));
            notificationStatusHistoryRepository.deleteAll(
                    notificationStatusHistoryRepository.findByNotificationIdOrderByChangedAtAsc(notification.getId()));
        }
        notificationRepository.deleteAll(
                notificationRepository.findByTenantIdWithFilters(tenant.getId(), null, null, PageRequest.of(0, 1000)));
        channelConfigRepository.deleteAll(channelConfigRepository.findByTenantId(tenant.getId()));
        templateRepository.deleteAll(templateRepository.findByTenantId(tenant.getId()));
        rateLimitConfigRepository.findByTenantId(tenant.getId()).ifPresent(rateLimitConfigRepository::delete);
        appUserRepository.deleteAll(appUserRepository.findAll().stream()
                .filter(u -> u.getTenant() != null && u.getTenant().getId().equals(tenant.getId()))
                .toList());
        tenantRepository.delete(tenant);
    }
}
