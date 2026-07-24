package com.notification.service.service;

import com.notification.service.dto.request.TemplateRequest;
import com.notification.service.dto.response.TemplateResponse;
import com.notification.service.entity.Template;
import com.notification.service.entity.Tenant;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.NotificationRepository;
import com.notification.service.repository.TemplateRepository;
import com.notification.service.repository.TenantRepository;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.security.TenantAccessGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TemplateService {

    private final TemplateRepository templateRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessGuard tenantAccessGuard;
    private final NotificationRepository notificationRepository;

    public TemplateResponse create(AppUserPrincipal principal, Long tenantId, TemplateRequest request) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        Template template = Template.builder()
                .tenant(tenant)
                .name(request.name())
                .channelType(request.channelType())
                .contentTemplate(request.contentTemplate())
                .build();
        return TemplateResponse.from(templateRepository.save(template));
    }

    @Transactional(readOnly = true)
    public List<TemplateResponse> list(AppUserPrincipal principal, Long tenantId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        return templateRepository.findByTenantId(tenantId).stream().map(TemplateResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TemplateResponse get(AppUserPrincipal principal, Long tenantId, Long templateId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        return TemplateResponse.from(findTemplateOrThrow(tenantId, templateId));
    }

    public TemplateResponse update(AppUserPrincipal principal, Long tenantId, Long templateId, TemplateRequest request) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Template template = findTemplateOrThrow(tenantId, templateId);
        template.setName(request.name());
        template.setChannelType(request.channelType());
        template.setContentTemplate(request.contentTemplate());
        return TemplateResponse.from(templateRepository.save(template));
    }

    public void delete(AppUserPrincipal principal, Long tenantId, Long templateId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Template template = findTemplateOrThrow(tenantId, templateId);
        // Historical notifications keep their own rendered snapshot, so detaching
        // the FK here doesn't lose audit data - it just unblocks the delete.
        notificationRepository.detachTemplate(templateId);
        templateRepository.delete(template);
    }

    private Template findTemplateOrThrow(Long tenantId, Long templateId) {
        return templateRepository.findByIdAndTenantId(templateId, tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Template not found: " + templateId));
    }
}