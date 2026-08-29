package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("platformAdminAccess")
@RequiredArgsConstructor
public class PlatformAdminAccess {

    private static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";

    private final AppUserRepository appUserRepository;

    public boolean isPlatformAdmin(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            return false;
        }

        return appUserRepository.findById(authentication.getName())
                .filter(user -> user.getTenantId() == null)
                .map(user -> user.getRoles().stream()
                        .anyMatch(role -> role != null
                                && role.isActive()
                                && PLATFORM_ADMIN_ROLE_ID.equalsIgnoreCase(role.getId())))
                .orElse(false);
    }
}
