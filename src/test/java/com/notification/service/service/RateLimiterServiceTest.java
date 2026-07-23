package com.notification.service.service;

import com.notification.service.entity.RateLimitConfig;
import com.notification.service.entity.Tenant;
import com.notification.service.repository.RateLimitConfigRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RateLimiterServiceTest {

    @Mock private RateLimitConfigRepository rateLimitConfigRepository;

    private RateLimiterService rateLimiterService;

    @BeforeEach
    void setUp() {
        rateLimiterService = new RateLimiterService(rateLimitConfigRepository);
    }

    @Test
    void deniesAfterCapacityExhaustedForConfiguredTenant() {
        Tenant tenant = Tenant.builder().id(1L).name("t").build();
        RateLimitConfig config = RateLimitConfig.builder().tenant(tenant).capacity(2).refillRatePerSec(0.0001).build();
        when(rateLimitConfigRepository.findByTenantId(1L)).thenReturn(Optional.of(config));

        assertThat(rateLimiterService.tryAcquire(1L)).isTrue();
        assertThat(rateLimiterService.tryAcquire(1L)).isTrue();
        assertThat(rateLimiterService.tryAcquire(1L)).isFalse();
    }

    @Test
    void bucketIsCreatedOnceAndCachedAcrossCalls() {
        Tenant tenant = Tenant.builder().id(1L).name("t").build();
        RateLimitConfig config = RateLimitConfig.builder().tenant(tenant).capacity(5).refillRatePerSec(1.0).build();
        when(rateLimitConfigRepository.findByTenantId(1L)).thenReturn(Optional.of(config));

        rateLimiterService.tryAcquire(1L);
        rateLimiterService.tryAcquire(1L);
        rateLimiterService.tryAcquire(1L);

        verify(rateLimitConfigRepository, times(1)).findByTenantId(1L);
    }

    @Test
    void tenantWithNoConfigIsEffectivelyUnlimited() {
        when(rateLimitConfigRepository.findByTenantId(2L)).thenReturn(Optional.empty());

        for (int i = 0; i < 1000; i++) {
            assertThat(rateLimiterService.tryAcquire(2L)).isTrue();
        }
    }
}