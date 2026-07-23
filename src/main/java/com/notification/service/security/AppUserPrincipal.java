package com.notification.service.security;

import com.notification.service.entity.AppUser;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

// Wraps AppUser as the Spring Security principal so controllers/services can read
// appUserId/tenantId/role straight off the authenticated principal (tenantId is null
// for PLATFORM_ADMIN, who isn't scoped to any single tenant).
@Getter
public class AppUserPrincipal implements UserDetails {

    private final Long appUserId;
    private final Long tenantId;
    private final String username;
    private final String passwordHash;
    private final com.notification.service.entity.enums.Role role;

    public AppUserPrincipal(AppUser appUser) {
        this.appUserId = appUser.getId();
        this.tenantId = appUser.getTenant() != null ? appUser.getTenant().getId() : null;
        this.username = appUser.getUsername();
        this.passwordHash = appUser.getPasswordHash();
        this.role = appUser.getRole();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + role.name()));
    }

    @Override
    public String getPassword() {
        return passwordHash;
    }

    @Override
    public String getUsername() {
        return username;
    }
}