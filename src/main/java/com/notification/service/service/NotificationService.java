package com.notification.service.service;

import com.notification.service.dto.request.NotificationCreateRequest;
import com.notification.service.dto.response.DeliveryAttemptEntry;
import com.notification.service.dto.response.NotificationHistoryResponse;
import com.notification.service.dto.response.NotificationResponse;
import com.notification.service.dto.response.StatusHistoryEntry;
import com.notification.service.entity.Notification;
import com.notification.service.entity.NotificationStatusHistory;
import com.notification.service.entity.Template;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.DeliveryAttemptRepository;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.NotificationStatusHistoryRepository;
import com.notification.service.repository.TemplateRepository;
import com.notification.service.repository.TenantRepository;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.security.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationService {

    private static final int REPORT_MAX_RESULTS = 200;

    private final NotificationRepository notificationRepository;
    private final NotificationStatusHistoryRepository statusHistoryRepository;
    private final DeliveryAttemptRepository deliveryAttemptRepository;
    private final TenantRepository tenantRepository;
    private final TemplateRepository templateRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final TemplateRenderer templateRenderer;
    private final JsonMapper jsonMapper;

    public NotificationResponse create(AppUserPrincipal principal, Long tenantId, NotificationCreateRequest request) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
        Template template = templateRepository.findByIdAndTenantId(request.templateId(), tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + request.templateId()));

        if (template.getChannelType() != request.channelType()) {
            throw new IllegalArgumentException(
                    "Template channel (" + template.getChannelType() + ") does not match requested channel ("
                            + request.channelType() + ")");
        }

        String renderedContent = templateRenderer.render(template.getContentTemplate(), request.variables());

        Notification notification = Notification.builder()
                .tenant(tenant)
                .channelType(request.channelType())
                .template(template)
                .recipient(request.recipient())
                .variables(toJson(request.variables()))
                .renderedContent(renderedContent)
                .status(NotificationStatus.PENDING)
                .scheduledAt(request.scheduledAt())
                .build();
        notification = notificationRepository.save(notification);

        statusHistoryRepository.save(NotificationStatusHistory.builder()
                .notification(notification)
                .fromStatus(null)
                .toStatus(NotificationStatus.PENDING)
                .reason("Notification created")
                .build());

        return NotificationResponse.from(notification, request.variables());
    }

    @Transactional(readOnly = true)
    public List<NotificationResponse> list(AppUserPrincipal principal, Long tenantId,
                                            NotificationStatus status, ChannelType channelType) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        return notificationRepository
                .findByTenantIdWithFilters(tenantId, status, channelType, PageRequest.of(0, REPORT_MAX_RESULTS))
                .stream()
                .map(n -> NotificationResponse.from(n, fromJson(n.getVariables())))
                .toList();
    }

    @Transactional(readOnly = true)
    public NotificationResponse get(AppUserPrincipal principal, Long tenantId, UUID notificationId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Notification notification = findNotificationOrThrow(tenantId, notificationId);
        return NotificationResponse.from(notification, fromJson(notification.getVariables()));
    }

    @Transactional(readOnly = true)
    public NotificationHistoryResponse getHistory(AppUserPrincipal principal, Long tenantId, UUID notificationId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        findNotificationOrThrow(tenantId, notificationId);

        List<StatusHistoryEntry> statusHistory = statusHistoryRepository
                .findByNotificationIdOrderByChangedAtAsc(notificationId)
                .stream().map(StatusHistoryEntry::from).toList();
        List<DeliveryAttemptEntry> deliveryAttempts = deliveryAttemptRepository
                .findByNotificationIdOrderByAttemptNumberAsc(notificationId)
                .stream().map(DeliveryAttemptEntry::from).toList();

        return new NotificationHistoryResponse(notificationId, statusHistory, deliveryAttempts);
    }

    private Notification findNotificationOrThrow(Long tenantId, UUID notificationId) {
        return notificationRepository.findByIdAndTenantId(notificationId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
    }

    private String toJson(Map<String, String> variables) {
        return jsonMapper.writeValueAsString(variables != null ? variables : Map.of());
    }

    @SuppressWarnings("unchecked")
    private Map<String, String> fromJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        return jsonMapper.readValue(json, Map.class);
    }
}