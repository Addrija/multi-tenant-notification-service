package com.notification.service.integration;

import com.notification.service.dto.request.ChannelConfigCreateRequest;
import com.notification.service.dto.request.ChannelConfigUpdateRequest;
import com.notification.service.dto.request.TemplateRequest;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class TenantAdminCrudIntegrationTest extends AbstractIntegrationTest {

    private Tenant tenant;
    private AppUser tenantAdmin;

    @BeforeEach
    void setUp() {
        tenant = createTenant("crud-tenant");
        tenantAdmin = createTenantAdmin("crud-admin", "Password123", tenant);
    }

    @AfterEach
    void tearDown() {
        cleanUpTenant(tenant);
    }

    @Test
    void fullTemplateCrudLifecycle() throws Exception {
        TemplateRequest createRequest = new TemplateRequest("welcome", ChannelType.EMAIL, "Hi {{name}}!");

        MvcResult createResult = mockMvc.perform(post("/api/tenants/" + tenant.getId() + "/templates")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("welcome"))
                .andReturn();

        Long templateId = extractId(createResult);

        mockMvc.perform(get("/api/tenants/" + tenant.getId() + "/templates/" + templateId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.contentTemplate").value("Hi {{name}}!"));

        TemplateRequest updateRequest = new TemplateRequest("welcome-v2", ChannelType.EMAIL, "Hello {{name}}!!");
        mockMvc.perform(put("/api/tenants/" + tenant.getId() + "/templates/" + templateId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(updateRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("welcome-v2"));

        mockMvc.perform(delete("/api/tenants/" + tenant.getId() + "/templates/" + templateId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123")))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tenants/" + tenant.getId() + "/templates/" + templateId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123")))
                .andExpect(status().isNotFound());
    }

    @Test
    void rejectsBlankTemplateNameWithFieldLevelValidationError() throws Exception {
        TemplateRequest invalidRequest = new TemplateRequest("", ChannelType.EMAIL, "body");

        mockMvc.perform(post("/api/tenants/" + tenant.getId() + "/templates")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.name").exists());
    }

    @Test
    void channelConfigCreateThenRejectsDuplicateThenUpdates() throws Exception {
        ChannelConfigCreateRequest createRequest = new ChannelConfigCreateRequest(ChannelType.SMS, true, "TESTCO", 0.1);

        MvcResult createResult = mockMvc.perform(post("/api/tenants/" + tenant.getId() + "/channels")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isCreated())
                .andReturn();
        Long channelConfigId = extractId(createResult);

        mockMvc.perform(post("/api/tenants/" + tenant.getId() + "/channels")
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(createRequest)))
                .andExpect(status().isBadRequest());

        ChannelConfigUpdateRequest disableRequest = new ChannelConfigUpdateRequest(false, null, null);
        mockMvc.perform(put("/api/tenants/" + tenant.getId() + "/channels/" + channelConfigId)
                        .with(basicAuth(tenantAdmin.getUsername(), "Password123"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(toJson(disableRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.enabled").value(false));
    }

    @SuppressWarnings("unchecked")
    private Long extractId(MvcResult result) throws Exception {
        Map<String, Object> body = jsonMapper.readValue(result.getResponse().getContentAsString(), Map.class);
        Object id = body.get("id");
        assertThat(id).isNotNull();
        return ((Number) id).longValue();
    }
}
