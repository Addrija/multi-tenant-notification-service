package com.notification.service.service;

import com.notification.service.dto.request.RateLimitUpdateRequest;
import com.notification.service.dto.request.TenantCreateRequest;
import com.notification.service.dto.response.RateLimitResponse;
import com.notification.service.dto.response.TenantResponse;
import com.notification.service.entity.RateLimitConfig;
import com.notification.service.entity.Tenant;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.RateLimitConfigRepository;
import com.notification.service.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TenantService {

    private final TenantRepository tenantRepository;
    private final RateLimitConfigRepository rateLimitConfigRepository;

    public TenantResponse createTenant(TenantCreateRequest request) {
        Tenant tenant = Tenant.builder().name(request.name()).build();
        return TenantResponse.from(tenantRepository.save(tenant));
    }

    @Transactional(readOnly = true)
    public List<TenantResponse> listTenants() {
        return tenantRepository.findAll().stream().map(TenantResponse::from).toList();
    }

    @Transactional(readOnly = true)
    public TenantResponse getTenant(Long tenantId) {
        return TenantResponse.from(findTenantOrThrow(tenantId));
    }

    public RateLimitResponse updateRateLimit(Long tenantId, RateLimitUpdateRequest request) {
        Tenant tenant = findTenantOrThrow(tenantId);
        RateLimitConfig config = rateLimitConfigRepository.findByTenantId(tenantId)
                .orElseGet(() -> RateLimitConfig.builder().tenant(tenant).build());
        config.setCapacity(request.capacity());
        config.setRefillRatePerSec(request.refillRatePerSec());
        return RateLimitResponse.from(rateLimitConfigRepository.save(config));
    }

    private Tenant findTenantOrThrow(Long tenantId) {
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }
}