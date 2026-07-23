package com.notification.service.service;

import com.notification.service.dto.request.NotificationCreateRequest;
import com.notification.service.dto.response.NotificationResponse;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.Notification;
import com.notification.service.entity.NotificationStatusHistory;
import com.notification.service.entity.Template;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import com.notification.service.entity.enums.Role;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.DeliveryAttemptRepository;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.NotificationStatusHistoryRepository;
import com.notification.service.repository.TemplateRepository;
import com.notification.service.repository.TenantRepository;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.security.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import tools.jackson.databind.json.JsonMapper;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private NotificationStatusHistoryRepository statusHistoryRepository;
    @Mock private DeliveryAttemptRepository deliveryAttemptRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TemplateRepository templateRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;
    @Mock private JsonMapper jsonMapper;

    private final TemplateRenderer templateRenderer = new TemplateRenderer();

    private NotificationService notificationService;

    private Tenant tenant;
    private AppUserPrincipal principal;

    @BeforeEach
    void setUp() {
        notificationService = new NotificationService(
                notificationRepository, statusHistoryRepository, deliveryAttemptRepository,
                tenantRepository, templateRepository, tenantAccessGuard, templateRenderer, jsonMapper);

        tenant = Tenant.builder().id(2L).name("Acme Corp").build();
        AppUser appUser = AppUser.builder().id(1L).tenant(tenant).username("acme_admin").role(Role.TENANT_ADMIN).build();
        principal = new AppUserPrincipal(appUser);
    }

    @Test
    void createRendersTemplateAndPersistsWithInitialHistoryRow() {
        Template template = Template.builder()
                .id(1L).tenant(tenant).name("order_shipped").channelType(ChannelType.EMAIL)
                .contentTemplate("Hi {{name}}!").build();
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(templateRepository.findByIdAndTenantId(1L, 2L)).thenReturn(Optional.of(template));
        when(jsonMapper.writeValueAsString(any())).thenReturn("{\"name\":\"Jane\"}");
        when(notificationRepository.save(any(Notification.class))).thenAnswer(inv -> inv.getArgument(0));

        NotificationCreateRequest request = new NotificationCreateRequest(
                ChannelType.EMAIL, 1L, "jane@example.com", Map.of("name", "Jane"), null);

        NotificationResponse response = notificationService.create(principal, 2L, request);

        assertThat(response.renderedContent()).isEqualTo("Hi Jane!");
        assertThat(response.status()).isEqualTo(NotificationStatus.PENDING);

        verify(tenantAccessGuard).assertTenantAccess(principal, 2L);

        ArgumentCaptor<NotificationStatusHistory> historyCaptor = ArgumentCaptor.forClass(NotificationStatusHistory.class);
        verify(statusHistoryRepository).save(historyCaptor.capture());
        assertThat(historyCaptor.getValue().getFromStatus()).isNull();
        assertThat(historyCaptor.getValue().getToStatus()).isEqualTo(NotificationStatus.PENDING);
    }

    @Test
    void createRejectsChannelTemplateMismatch() {
        Template smsTemplate = Template.builder()
                .id(1L).tenant(tenant).name("t").channelType(ChannelType.SMS).contentTemplate("hi").build();
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(templateRepository.findByIdAndTenantId(1L, 2L)).thenReturn(Optional.of(smsTemplate));

        NotificationCreateRequest request = new NotificationCreateRequest(
                ChannelType.EMAIL, 1L, "jane@example.com", Map.of(), null);

        assertThatThrownBy(() -> notificationService.create(principal, 2L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not match");

        verify(notificationRepository, never()).save(any());
    }

    @Test
    void createThrowsWhenTemplateNotFound() {
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(templateRepository.findByIdAndTenantId(eq(99L), eq(2L))).thenReturn(Optional.empty());

        NotificationCreateRequest request = new NotificationCreateRequest(
                ChannelType.EMAIL, 99L, "jane@example.com", Map.of(), null);

        assertThatThrownBy(() -> notificationService.create(principal, 2L, request))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getThrowsWhenNotificationNotFound() {
        UUID id = UUID.randomUUID();
        when(notificationRepository.findByIdAndTenantId(id, 2L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> notificationService.get(principal, 2L, id))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}