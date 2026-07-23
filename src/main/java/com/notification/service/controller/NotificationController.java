package com.notification.service.controller;

import com.notification.service.dto.request.NotificationCreateRequest;
import com.notification.service.dto.response.NotificationHistoryResponse;
import com.notification.service.dto.response.NotificationResponse;
import com.notification.service.entity.enums.ChannelType;
import com.notification.service.entity.enums.NotificationStatus;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/tenants/{tenantId}/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NotificationResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
                                        @PathVariable Long tenantId,
                                        @Valid @RequestBody NotificationCreateRequest request) {
        return notificationService.create(principal, tenantId, request);
    }

    // Delivery report - status/channelType are optional filters.
    @GetMapping
    public List<NotificationResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                            @PathVariable Long tenantId,
                                            @RequestParam(required = false) NotificationStatus status,
                                            @RequestParam(required = false) ChannelType channelType) {
        return notificationService.list(principal, tenantId, status, channelType);
    }

    @GetMapping("/{notificationId}")
    public NotificationResponse get(@AuthenticationPrincipal AppUserPrincipal principal,
                                     @PathVariable Long tenantId, @PathVariable UUID notificationId) {
        return notificationService.get(principal, tenantId, notificationId);
    }

    @GetMapping("/{notificationId}/history")
    public NotificationHistoryResponse getHistory(@AuthenticationPrincipal AppUserPrincipal principal,
                                                    @PathVariable Long tenantId, @PathVariable UUID notificationId) {
        return notificationService.getHistory(principal, tenantId, notificationId);
    }
}