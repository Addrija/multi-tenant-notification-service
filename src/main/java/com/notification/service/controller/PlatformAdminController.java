package com.notification.service.controller;

import com.notification.service.dto.request.RateLimitUpdateRequest;
import com.notification.service.dto.request.TenantCreateRequest;
import com.notification.service.dto.response.RateLimitResponse;
import com.notification.service.dto.response.TenantResponse;
import com.notification.service.service.TenantService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// URL-scoped to PLATFORM_ADMIN by SecurityConfig (/api/admin/**) - no per-tenant
// scoping needed here, platform admin manages all tenants globally.
@RestController
@RequestMapping("/api/admin/tenants")
@RequiredArgsConstructor
public class PlatformAdminController {

    private final TenantService tenantService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TenantResponse createTenant(@Valid @RequestBody TenantCreateRequest request) {
        return tenantService.createTenant(request);
    }

    @GetMapping
    public List<TenantResponse> listTenants() {
        return tenantService.listTenants();
    }

    @GetMapping("/{tenantId}")
    public TenantResponse getTenant(@PathVariable Long tenantId) {
        return tenantService.getTenant(tenantId);
    }

    @PutMapping("/{tenantId}/rate-limits")
    public RateLimitResponse updateRateLimit(@PathVariable Long tenantId,
                                              @Valid @RequestBody RateLimitUpdateRequest request) {
        return tenantService.updateRateLimit(tenantId, request);
    }
}