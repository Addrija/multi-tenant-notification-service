package com.notification.service.dto.response;

import com.notification.service.entity.AppUser;
import com.notification.service.entity.enums.Role;

// Never includes the password hash.
public record AppUserResponse(
        Long id,
        String username,
        Role role,
        Long tenantId
) {
    public static AppUserResponse from(AppUser appUser) {
        return new AppUserResponse(
                appUser.getId(),
                appUser.getUsername(),
                appUser.getRole(),
                appUser.getTenant() != null ? appUser.getTenant().getId() : null);
    }
}
