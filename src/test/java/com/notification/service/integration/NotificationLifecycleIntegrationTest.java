package com.notification.service.integration;

import com.notification.service.dto.request.NotificationCreateRequest;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.Template;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Exercises the real @Scheduled poller/dispatcher against real MySQL - not
// mocked. Deterministic outcomes are forced via simulatedFailureRate (0.0 =
// always succeeds, 1.0 = always fails) rather than relying on random luck,
// same technique used for the manual checkpoint verification during
// development.
class NotificationLifecycleIntegrationTest extends AbstractIntegrationTest {

    private Tenant tenant;
    private AppUser tenantAdmin;
    private ChannelConfig successChannel;
    private ChannelConfig failureChannel;
    private Template emailTemplate;

    @BeforeEach
    void setUp() {
        tenant = createTenant("lifecycle-tenant");
        tenantAdmin = createTenantAdmin("lifecycle-admin", "Password123", tenant);
        successChannel = channelConfigRepository.save(ChannelConfig.builder()
                .tenant(tenant).channelType(ChannelType.EMAIL).enabled(true)
                .senderIdentity("test@example.com").simulatedFailureRate(0.0).build());
        failureChannel = channelConfigRepository.save(ChannelConfig.builder()
                .tenant(tenant).channelType(ChannelType.SMS).enabled(true)
                .senderIdentity("TESTCO").simulatedFailureRate(1.0).build());
        emailTemplate = templateRepository.save(Template.builder()
                .tenant(tenant).name("welcome").channelType(ChannelType.EMAIL)
                .contentTemplate("Hi {{name}}!").build());
    }

    @AfterEach
    void tearDown() {
        cleanUpTenant(tenant);
    }

    @Test
    void immediateSendIsDispatchedAndReachesSent() throws Exception {
        String notificationId = submitNotification(ChannelType.EMAIL, emailTemplate.getId(), null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() ->
                assertThat(fetchNotification(notificationId).get("status")).isEqualTo("SENT"));

        Map<String, Object> history = fetchHistory(notificationId);
        assertThat((java.util.List<?>) history.get("statusHistory")).hasSizeGreaterThanOrEqualTo(3);
        assertThat((java.util.List<?>) history.get("deliveryAttempts")).hasSize(1);
    }

    @Test
    void failingSendMovesToRetryingWithBackoffScheduled() throws Exception {
        Template smsTemplate = templateRepository.save(Template.builder()
                .tenant(tenant).name("sms-test").channelType(ChannelType.SMS)
                .contentTemplate("Hi {{name}}!").build());

        String notificationId = submitNotification(ChannelType.SMS, smsTemplate.getId(), null);

        await().atMost(Duration.ofSeconds(15)).untilAsserted(() -> {
            Map<String, Object> notification = fetchNotification(notificationId);
            assertThat(notification.get("status")).isEqualTo("RETRYING");
            assertThat(((Number) notification.get("attemptCount")).intValue()).isGreaterThanOrEqualTo(1);
            assertThat(notification.get("nextRetryAt")).isNotNull();
        });
    }

    @Test
    void scheduledSendWaitsUntilDueBeforeDispatch() throws Exception {
        Instant scheduledAt = Instant.now().plusSeconds(6);
        String notificationId = submitNotification(ChannelType.EMAIL, emailTemplate.getId(), scheduledAt);

        assertThat(fetchNotification(notificationId).get("status")).isEqualTo("PENDING");

        await().atMost(Duration.ofSeconds(20)).untilAsserted(() ->
                assertThat(fetchNotification(notificationId).get("status")).isEqualTo("SENT"));
    }

    private String submitNotification(ChannelType channelType, Long templateId, Instant scheduledAt) throws Exception {
        NotificationCreateRequest request = new NotificationCreateRequest(
                channelType, templateId, "jane@example.com", Map.of("name", "Jane"), scheduledAt);

        MvcResult result = mockMvc.perform(post("/api/tenants/" + tenant.getId() + "/notifications")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(request)))
                .andExpect(status().isCreated())
                .andReturn();

        return (String) parseBody(result).get("id");
    }

    private Map<String, Object> fetchNotification(String notificationId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tenants/" + tenant.getId() + "/notifications/" + notificationId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123")))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result);
    }

    private Map<String, Object> fetchHistory(String notificationId) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/tenants/" + tenant.getId() + "/notifications/" + notificationId + "/history")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123")))
                .andExpect(status().isOk())
                .andReturn();
        return parseBody(result);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseBody(MvcResult result) throws Exception {
        return jsonMapper.readValue(result.getResponse().getContentAsString(), Map.class);
    }
}
