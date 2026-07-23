package com.notification.service.repository;

import com.notification.service.entity.RateLimitConfig;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface RateLimitConfigRepository extends JpaRepository<RateLimitConfig, Long> {
    Optional<RateLimitConfig> findByTenantId(Long tenantId);
}