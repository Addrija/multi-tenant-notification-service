package com.notification.service.service;

import com.notification.service.dto.request.AppUserCreateRequest;
import com.notification.service.dto.response.AppUserResponse;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.Role;
import com.notification.service.exception.ResourceNotFoundException;
import com.notification.service.repository.AppUserRepository;
import com.notification.service.repository.TenantRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private PasswordEncoder passwordEncoder;

    private AppUserService appUserService;

    @BeforeEach
    void setUp() {
        appUserService = new AppUserService(appUserRepository, tenantRepository, passwordEncoder);
    }

    @Test
    void createsPlatformAdminWithoutTenant() {
        when(appUserRepository.findByUsername("platformadmin2")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUserCreateRequest request = new AppUserCreateRequest("platformadmin2", "Password123", Role.PLATFORM_ADMIN, null);
        AppUserResponse response = appUserService.create(request);

        assertThat(response.tenantId()).isNull();
        assertThat(response.role()).isEqualTo(Role.PLATFORM_ADMIN);

        ArgumentCaptor<AppUser> captor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("hashed");
        assertThat(captor.getValue().getTenant()).isNull();
    }

    @Test
    void createsTenantAdminWithTenant() {
        Tenant tenant = Tenant.builder().id(2L).name("Acme").build();
        when(appUserRepository.findByUsername("acme_admin2")).thenReturn(Optional.empty());
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(passwordEncoder.encode("Password123")).thenReturn("hashed");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(inv -> inv.getArgument(0));

        AppUserCreateRequest request = new AppUserCreateRequest("acme_admin2", "Password123", Role.TENANT_ADMIN, 2L);
        AppUserResponse response = appUserService.create(request);

        assertThat(response.tenantId()).isEqualTo(2L);
        assertThat(response.role()).isEqualTo(Role.TENANT_ADMIN);
    }

    @Test
    void rejectsDuplicateUsername() {
        when(appUserRepository.findByUsername("taken")).thenReturn(Optional.of(AppUser.builder().build()));

        AppUserCreateRequest request = new AppUserCreateRequest("taken", "Password123", Role.PLATFORM_ADMIN, null);

        assertThatThrownBy(() -> appUserService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already taken");
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void rejectsPlatformAdminWithTenantId() {
        when(appUserRepository.findByUsername("x")).thenReturn(Optional.empty());

        AppUserCreateRequest request = new AppUserCreateRequest("x", "Password123", Role.PLATFORM_ADMIN, 2L);

        assertThatThrownBy(() -> appUserService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("must not have a tenantId");
    }

    @Test
    void rejectsTenantAdminWithoutTenantId() {
        when(appUserRepository.findByUsername("x")).thenReturn(Optional.empty());

        AppUserCreateRequest request = new AppUserCreateRequest("x", "Password123", Role.TENANT_ADMIN, null);

        assertThatThrownBy(() -> appUserService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("tenantId is required");
    }

    @Test
    void rejectsTenantAdminWithNonexistentTenant() {
        when(appUserRepository.findByUsername("x")).thenReturn(Optional.empty());
        when(tenantRepository.findById(999L)).thenReturn(Optional.empty());

        AppUserCreateRequest request = new AppUserCreateRequest("x", "Password123", Role.TENANT_ADMIN, 999L);

        assertThatThrownBy(() -> appUserService.create(request))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
