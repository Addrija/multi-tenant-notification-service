package com.notification.service.controller;

import com.notification.service.dto.request.TemplateRequest;
import com.notification.service.dto.response.TemplateResponse;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.service.TemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/tenants/{tenantId}/templates")
@RequiredArgsConstructor
public class TemplateController {

    private final TemplateService templateService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TemplateResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long tenantId,
                                    @Valid @RequestBody TemplateRequest request) {
        return templateService.create(principal, tenantId, request);
    }

    @GetMapping
    public List<TemplateResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @PathVariable Long tenantId) {
        return templateService.list(principal, tenantId);
    }

    @GetMapping("/{templateId}")
    public TemplateResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                 @PathVariable Long tenantId, @PathVariable Long templateId) {
        return templateService.get(principal, tenantId, templateId);
    }

    @PutMapping("/{templateId}")
    public TemplateResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                    @PathVariable Long tenantId, @PathVariable Long templateId,
                                    @Valid @RequestBody TemplateRequest request) {
        return templateService.update(principal, tenantId, templateId, request);
    }

    @DeleteMapping("/{templateId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@AuthenticationPrincipal AppUserPrincipal principal,
                        @PathVariable Long tenantId, @PathVariable Long templateId) {
        templateService.delete(principal, tenantId, templateId);
    }
}