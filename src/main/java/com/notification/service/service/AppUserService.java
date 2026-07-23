package com.notification.service.service;

import com.notification.service.dto.request.AppUserCreateRequest;
import com.notification.service.dto.response.AppUserResponse;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.Role;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.AppUserRepository;
import com.notification.service.repository.TenantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class AppUserService {

    private final AppUserRepository appUserRepository;
    private final TenantRepository tenantRepository;
    private final PasswordEncoder passwordEncoder;

    public AppUserResponse create(AppUserCreateRequest request) {
        if (appUserRepository.findByUsername(request.username()).isPresent()) {
            throw new IllegalArgumentException("Username already taken: " + request.username());
        }

        Tenant tenant = resolveTenant(request.role(), request.tenantId());

        AppUser appUser = AppUser.builder()
                .tenant(tenant)
                .username(request.username())
                .passwordHash(passwordEncoder.encode(request.password()))
                .role(request.role())
                .build();

        return AppUserResponse.from(appUserRepository.save(appUser));
    }

    private Tenant resolveTenant(Role role, Long tenantId) {
        if (role == Role.PLATFORM_ADMIN) {
            if (tenantId != null) {
                throw new IllegalArgumentException("PLATFORM_ADMIN users must not have a tenantId");
            }
            return null;
        }

        // role == TENANT_ADMIN
        if (tenantId == null) {
            throw new IllegalArgumentException("tenantId is required for TENANT_ADMIN users");
        }
        return tenantRepository.findById(tenantId)
                .orElseThrow(() -> new ResourceNotFoundException("Tenant not found: " + tenantId));
    }
}
