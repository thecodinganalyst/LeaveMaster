package com.practical.leavemaster.config;

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

import java.util.Optional;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class PlatformAdminInitializer implements ApplicationRunner {

    static final String PLATFORM_ADMIN_ROLE_ID = "PLATFORM_ADMIN";
    static final String PLATFORM_ADMIN_LOGIN_NAME = "PlatformAdmin";

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
        AppRole role = appRoleRepository.findById(PLATFORM_ADMIN_ROLE_ID).orElseGet(() -> {
            log.info("Creating {} role", PLATFORM_ADMIN_ROLE_ID);
            AppRole newRole = AppRole.builder()
                    .id(PLATFORM_ADMIN_ROLE_ID)
                    .description("Platform administrator – manages tenants")
                    .active(true)
                    .permissions(Set.copyOf(appPermissionRepository.findAllById(
                            Set.of(RbacPermissions.TENANT_READ, RbacPermissions.TENANT_WRITE))))
                    .build();
            return appRoleRepository.save(newRole);
        });

        Optional<AppUser> defaultAdmin = appUserRepository.findById(PLATFORM_ADMIN_LOGIN_NAME);
        boolean hasUsers = appUserRepository.findAll().stream()
                .anyMatch(u -> u.getRoles().stream()
                        .anyMatch(r -> PLATFORM_ADMIN_ROLE_ID.equals(r.getId())));

        if (!hasUsers) {
            log.info("No users in {} role – creating default {} user", PLATFORM_ADMIN_ROLE_ID, PLATFORM_ADMIN_LOGIN_NAME);
            AppUser admin = AppUser.builder()
                    .loginName(PLATFORM_ADMIN_LOGIN_NAME)
                    .password(passwordEncoder.encode(platformAdminPassword))
                    .active(true)
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
}
