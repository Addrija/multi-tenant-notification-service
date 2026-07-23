package com.notification.service.service;

import com.notification.service.entity.Notification;
import org.springframework.stereotype.Component;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

// Groups a batch by tenant (preserving each tenant's relative order) then
// round-robins across tenants - one from tenant A, one from B, one from C,
// repeat - so a tenant with a large backlog can't crowd out a quiet tenant
// within the same poll cycle's batch.
@Component
public class TenantFairnessScheduler {

    public List<Notification> interleave(List<Notification> notifications) {
        Map<Long, Deque<Notification>> byTenant = new LinkedHashMap<>();
        for (Notification notification : notifications) {
            byTenant.computeIfAbsent(notification.getTenant().getId(), id -> new ArrayDeque<>())
                    .add(notification);
        }

        List<Notification> result = new ArrayList<>(notifications.size());
        boolean addedAny = true;
        while (addedAny) {
            addedAny = false;
            for (Deque<Notification> queue : byTenant.values()) {
                Notification next = queue.poll();
                if (next != null) {
                    result.add(next);
                    addedAny = true;
                }
            }
        }
        return result;
    }
}