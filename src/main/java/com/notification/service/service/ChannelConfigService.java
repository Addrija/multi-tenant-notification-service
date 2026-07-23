package com.notification.service.service;

import com.notification.service.dto.request.ChannelConfigCreateRequest;
import com.notification.service.dto.request.ChannelConfigUpdateRequest;
import com.notification.service.dto.response.ChannelConfigResponse;
import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.Tenant;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.ChannelConfigRepository;
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
public class ChannelConfigService {

    private final ChannelConfigRepository channelConfigRepository;
    private final TenantRepository tenantRepository;
    private final TenantAccessGuard tenantAccessGuard;

    public ChannelConfigResponse create(AppUserPrincipal principal, Long tenantId, ChannelConfigCreateRequest request) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        Tenant tenant = tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));

        if (channelConfigRepository.findByTenantIdAndChannelType(tenantId, request.channelType()).isPresent()) {
            throw new IllegalArgumentException(
                    "Channel " + request.channelType() + " is already configured for this tenant - use PUT to update it");
        }

        ChannelConfig config = ChannelConfig.builder()
                .tenant(tenant)
                .channelType(request.channelType())
                .enabled(request.enabled() == null || request.enabled())
                .senderIdentity(request.senderIdentity())
                .simulatedFailureRate(request.simulatedFailureRate() != null ? request.simulatedFailureRate() : 0.1)
                .build();
        return ChannelConfigResponse.from(channelConfigRepository.save(config));
    }

    @Transactional(readOnly = true)
    public List<ChannelConfigResponse> list(AppUserPrincipal principal, Long tenantId) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        return channelConfigRepository.findByTenantId(tenantId).stream().map(ChannelConfigResponse::from).toList();
    }

    public ChannelConfigResponse update(AppUserPrincipal principal, Long tenantId, Long channelConfigId,
                                         ChannelConfigUpdateRequest request) {
        tenantAccessGuard.assertTenantAccess(principal, tenantId);
        ChannelConfig config = channelConfigRepository.findById(channelConfigId)
                .filter(c -> c.getTenant().getId().equals(tenantId))
                .orElseThrow(() -> new ResourceNotFoundException("Channel config not found: " + channelConfigId));

        if (request.enabled() != null) {
            config.setEnabled(request.enabled());
        }
        if (request.senderIdentity() != null) {
            config.setSenderIdentity(request.senderIdentity());
        }
        if (request.simulatedFailureRate() != null) {
            config.setSimulatedFailureRate(request.simulatedFailureRate());
        }
        return ChannelConfigResponse.from(channelConfigRepository.save(config));
    }
}