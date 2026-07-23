package com.notification.service.controller;

import com.notification.service.dto.request.AppUserCreateRequest;
import com.notification.service.dto.response.AppUserResponse;
import com.notification.service.service.AppUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

// URL-scoped to PLATFORM_ADMIN by SecurityConfig (/api/admin/**). Creates both
// platform admins and tenant admins - basic account provisioning, no
// self-service registration/OAuth (out of scope per assignment).
@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AppUserController {

    private final AppUserService appUserService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AppUserResponse create(@Valid @RequestBody AppUserCreateRequest request) {
        return appUserService.create(request);
    }
}
