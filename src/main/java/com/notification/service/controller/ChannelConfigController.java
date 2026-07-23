package com.notification.service.controller;

import com.notification.service.dto.request.ChannelConfigCreateRequest;
import com.notification.service.dto.request.ChannelConfigUpdateRequest;
import com.notification.service.dto.response.ChannelConfigResponse;
import com.notification.service.security.AppUserPrincipal;
import com.notification.service.service.ChannelConfigService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// No DELETE - disabling a channel (PUT ... {enabled:false}) is the realistic
// operation, not hard-deleting the config row.
@RestController
@RequestMapping("/api/tenants/{tenantId}/channels")
@RequiredArgsConstructor
public class ChannelConfigController {

    private final ChannelConfigService channelConfigService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ChannelConfigResponse create(@AuthenticationPrincipal AppUserPrincipal principal,
                                         @PathVariable Long tenantId,
                                         @Valid @RequestBody ChannelConfigCreateRequest request) {
        return channelConfigService.create(principal, tenantId, request);
    }

    @GetMapping
    public List<ChannelConfigResponse> list(@AuthenticationPrincipal AppUserPrincipal principal,
                                             @PathVariable Long tenantId) {
        return channelConfigService.list(principal, tenantId);
    }

    @PutMapping("/{channelConfigId}")
    public ChannelConfigResponse update(@AuthenticationPrincipal AppUserPrincipal principal,
                                         @PathVariable Long tenantId, @PathVariable Long channelConfigId,
                                         @Valid @RequestBody ChannelConfigUpdateRequest request) {
        return channelConfigService.update(principal, tenantId, channelConfigId, request);
    }
}