package com.notification.service.repository;

import com.notification.service.entity.Template;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TemplateRepository extends JpaRepository<Template, Long> {
    List<Template> findByTenantId(Long tenantId);
    Optional<Template> findByIdAndTenantId(Long id, Long tenantId);
}