package com.notification.service.service;

import com.notification.service.dto.request.ChannelConfigCreateRequest;
import com.notification.service.dto.request.ChannelConfigUpdateRequest;
import com.notification.service.dto.response.ChannelConfigResponse;
import com.notification.service.entity.AppUser;
import com.notification.service.entity.ChannelConfig;
import com.notification.service.entity.Tenant;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.Role;
import com.notification.service.repository.ChannelConfigRepository;
import com.notification.service.repository.TenantRepository;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.security.TenantAccessGuard;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ChannelConfigServiceTest {

    @Mock private ChannelConfigRepository channelConfigRepository;
    @Mock private TenantRepository tenantRepository;
    @Mock private TenantAccessGuard tenantAccessGuard;

    private ChannelConfigService channelConfigService;
    private Tenant tenant;
    private AppUserPrincipal principal;

    @BeforeEach
    void setUp() {
        channelConfigService = new ChannelConfigService(channelConfigRepository, tenantRepository, tenantAccessGuard);
        tenant = Tenant.builder().id(2L).name("Acme Corp").build();
        AppUser appUser = AppUser.builder().id(1L).tenant(tenant).username("acme_admin").role(Role.TENANT_ADMIN).build();
        principal = new AppUserPrincipal(appUser);
    }

    @Test
    void createRejectsDuplicateChannelForTenant() {
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.EMAIL))
                .thenReturn(Optional.of(ChannelConfig.builder().id(1L).tenant(tenant).channelType(ChannelType.EMAIL).build()));

        ChannelConfigCreateRequest request = new ChannelConfigCreateRequest(ChannelType.EMAIL, true, "from@acme.com", 0.1);

        assertThatThrownBy(() -> channelConfigService.create(principal, 2L, request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already configured");

        verify(channelConfigRepository, never()).save(any());
    }

    @Test
    void createDefaultsEnabledTrueAndFailureRateWhenOmitted() {
        when(tenantRepository.findById(2L)).thenReturn(Optional.of(tenant));
        when(channelConfigRepository.findByTenantIdAndChannelType(2L, ChannelType.SMS)).thenReturn(Optional.empty());
        when(channelConfigRepository.save(any(ChannelConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelConfigCreateRequest request = new ChannelConfigCreateRequest(ChannelType.SMS, null, "ACME", null);

        ChannelConfigResponse response = channelConfigService.create(principal, 2L, request);

        assertThat(response.enabled()).isTrue();
        assertThat(response.simulatedFailureRate()).isEqualTo(0.1);
    }

    @Test
    void updateOnlyAppliesNonNullFields() {
        ChannelConfig existing = ChannelConfig.builder()
                .id(5L).tenant(tenant).channelType(ChannelType.SMS)
                .enabled(true).senderIdentity("OLD").simulatedFailureRate(0.2).build();
        when(channelConfigRepository.findById(5L)).thenReturn(Optional.of(existing));
        when(channelConfigRepository.save(any(ChannelConfig.class))).thenAnswer(inv -> inv.getArgument(0));

        ChannelConfigUpdateRequest request = new ChannelConfigUpdateRequest(false, null, null);
        ChannelConfigResponse response = channelConfigService.update(principal, 2L, 5L, request);

        assertThat(response.enabled()).isFalse();
        assertThat(response.senderIdentity()).isEqualTo("OLD");
        assertThat(response.simulatedFailureRate()).isEqualTo(0.2);
    }
}