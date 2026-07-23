package com.notification.service.service;

import com.notification.service.repository.RateLimitConfigRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;

// In-process per-tenant token buckets - no Redis needed since dispatch is
// single-process. A tenant with no RateLimitConfig row gets an effectively
// unlimited bucket rather than being blocked (platform admin hasn't set a
// limit yet != "block everything").
@Component
@RequiredArgsConstructor
public class RateLimiterService {

    private final RateLimitConfigRepository rateLimitConfigRepository;
    private final ConcurrentHashMap<Long, TokenBucket> buckets = new ConcurrentHashMap<>();

    public boolean tryAcquire(Long tenantId) {
        return buckets.computeIfAbsent(tenantId, this::createBucket).tryConsume();
    }

    private TokenBucket createBucket(Long tenantId) {
        return rateLimitConfigRepository.findByTenantId(tenantId)
                .map(config -> new TokenBucket(config.getCapacity(), config.getRefillRatePerSec(), Clock.systemUTC()))
                .orElseGet(() -> new TokenBucket(Double.MAX_VALUE, Double.MAX_VALUE, Clock.systemUTC()));
    }
}