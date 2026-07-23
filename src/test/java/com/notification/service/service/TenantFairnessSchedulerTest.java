package com.notification.service.service;

import com.notification.service.entity.Notification;
import com.notification.service.entity.Tenant;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TenantFairnessSchedulerTest {

    private final TenantFairnessScheduler scheduler = new TenantFairnessScheduler();

    @Test
    void interleavesAcrossTenantsRoundRobin() {
        Tenant tenantA = Tenant.builder().id(1L).name("A").build();
        Tenant tenantB = Tenant.builder().id(2L).name("B").build();

        Notification a1 = notificationFor(tenantA);
        Notification a2 = notificationFor(tenantA);
        Notification a3 = notificationFor(tenantA);
        Notification b1 = notificationFor(tenantB);
        Notification b2 = notificationFor(tenantB);

        List<Notification> result = scheduler.interleave(List.of(a1, a2, a3, b1, b2));

        assertThat(result).containsExactly(a1, b1, a2, b2, a3);
    }

    @Test
    void singleTenantPreservesOriginalOrder() {
        Tenant tenant = Tenant.builder().id(1L).name("A").build();
        Notification n1 = notificationFor(tenant);
        Notification n2 = notificationFor(tenant);
        Notification n3 = notificationFor(tenant);

        List<Notification> result = scheduler.interleave(List.of(n1, n2, n3));

        assertThat(result).containsExactly(n1, n2, n3);
    }

    @Test
    void emptyBatchReturnsEmptyList() {
        assertThat(scheduler.interleave(List.of())).isEmpty();
    }

    private Notification notificationFor(Tenant tenant) {
        return Notification.builder().id(UUID.randomUUID()).tenant(tenant).build();
    }
}