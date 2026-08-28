package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminInitializer implements ApplicationRunner {

    static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    static final String PLATFORM_ADMIN_LOGIN_NAME = "PlatformAdmin";
    private static final Set<String> REQUIRED_PLATFORM_PERMISSIONS = Set.of(
            RbacPermissions.TENANT_READ,
            RbacPermissions.TENANT_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_GENERATE,
            RbacPermissions.PUBLIC_HOLIDAY_READ,
            RbacPermissions.PUBLIC_HOLIDAY_WRITE
    );

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${platform.admin.password}")
    private String platformAdminPassword;

    @Value("${platform.admin.reset-password:false}")
    private boolean resetPlatformAdminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        AppRole role = appRoleRepository.findById(PLATFORM_ADMIN_ROLE_ID)
                .map(this::reconcilePlatformAdminRole)
                .orElseGet(this::createPlatformAdminRole);

        Optional<AppUser> defaultAdmin = appUserRepository.findByTenantIdIsNullAndLoginName(PLATFORM_ADMIN_LOGIN_NAME);
        boolean hasUsers = appUserRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().stream()
                        .anyMatch(r -> PLATFORM_ADMIN_ROLE_ID.equals(r.getId())));

        if (!hasUsers) {
            log.info("No users in {} role – creating default {} user", PLATFORM_ADMIN_ROLE_ID, PLATFORM_ADMIN_LOGIN_NAME);
            AppUser admin = AppUser.builder()
                    .loginName(PLATFORM_ADMIN_LOGIN_NAME)
                    .password(passwordEncoder.encode(platformAdminPassword))
                    .active(true)
                    .tenantId(null)
                    .roles(Set.of(role))
                    .build();
            appUserRepository.save(admin);
            return;
        }

        if (!resetPlatformAdminPassword) {
            return;
        }

        defaultAdmin.filter(admin -> admin.getRoles().stream()
                        .anyMatch(existingRole -> PLATFORM_ADMIN_ROLE_ID.equals(existingRole.getId())))
                .ifPresentOrElse(admin -> {
                    log.warn("Explicit PlatformAdmin password reset requested; replacing the stored password hash");
                    admin.setPassword(passwordEncoder.encode(platformAdminPassword));
                    appUserRepository.save(admin);
                }, () -> log.warn(
                        "PlatformAdmin password reset requested, but the default {} user is missing or is not assigned to {}. No password was changed.",
                        PLATFORM_ADMIN_LOGIN_NAME,
                        PLATFORM_ADMIN_ROLE_ID));
    }

    private AppRole createPlatformAdminRole() {
        log.info("Creating {} role", PLATFORM_ADMIN_ROLE_ID);
        AppRole newRole = AppRole.builder()
                .id(PLATFORM_ADMIN_ROLE_ID)
                .description("Platform administrator – manages tenants and platform reference data")
                .active(true)
                .permissions(loadRequiredPlatformPermissions())
                .build();
        return appRoleRepository.save(newRole);
    }

    private AppRole reconcilePlatformAdminRole(AppRole role) {
        boolean changed = false;
        if (!role.isActive()) {
            role.setActive(true);
            changed = true;
        }

        Set<AppPermission> permissions = new HashSet<>(Optional.ofNullable(role.getPermissions()).orElseGet(Set::of));
        Set<String> existingCodes = permissions.stream()
                .map(AppPermission::getCode)
                .collect(Collectors.toSet());
        for (AppPermission requiredPermission : loadRequiredPlatformPermissions()) {
            if (existingCodes.add(requiredPermission.getCode())) {
                permissions.add(requiredPermission);
                changed = true;
            }
        }

        if (changed) {
            role.setPermissions(permissions);
            log.info("Reconciling {} role with required platform-management permissions", PLATFORM_ADMIN_ROLE_ID);
            appRoleRepository.save(role);
        }
        return role;
    }

    private Set<AppPermission> loadRequiredPlatformPermissions() {
        return Set.copyOf(appPermissionRepository.findAllById(REQUIRED_PLATFORM_PERMISSIONS));
    }
}
