package com.practical.leavemaster.tenant;

import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAdminProvisionService {

    static final String TENANT_ADMIN_ROLE_ID = "TENANT_ADMIN";

    static final Set<String> TENANT_ADMIN_PERMISSION_CODES = Set.of(
            RbacPermissions.USER_READ,
            RbacPermissions.USER_WRITE,
            RbacPermissions.ROLE_MANAGE,
            RbacPermissions.STAFF_READ,
            RbacPermissions.STAFF_WRITE,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_TYPE_WRITE,
            RbacPermissions.LEAVE_APPROVER_READ,
            RbacPermissions.LEAVE_APPROVER_WRITE,
            RbacPermissions.LEAVE_CALENDAR_READ,
            RbacPermissions.LEAVE_CALENDAR_WRITE,
            RbacPermissions.LOCATION_READ,
            RbacPermissions.LOCATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE
    );

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final AppUserRepository appUserRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${tenant.admin.default.password}")
    private String tenantAdminDefaultPassword;

    @Transactional
    public void provision(String tenantId) {
        String roleId = TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        AppRole role = appRoleRepository.findById(roleId).orElseGet(() -> {
            log.info("Creating {} role for tenant {}", TENANT_ADMIN_ROLE_ID, tenantId);
            AppRole newRole = AppRole.builder()
                    .id(roleId)
                    .description("Tenant administrator for tenant " + tenantId)
                    .active(true)
                    .tenantId(tenantId)
                    .permissions(Set.copyOf(appPermissionRepository.findAllById(TENANT_ADMIN_PERMISSION_CODES)))
                    .build();
            return appRoleRepository.save(newRole);
        });

        String loginName = TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        if (!appUserRepository.existsById(loginName)) {
            log.info("Creating tenant admin user {} for tenant {}", loginName, tenantId);
            AppUser admin = AppUser.builder()
                    .loginName(loginName)
                    .password(passwordEncoder.encode(tenantAdminDefaultPassword))
                    .active(true)
                    .tenantId(tenantId)
                    .roles(Set.of(role))
                    .build();
            appUserRepository.save(admin);
        }
    }

    @Transactional
    public void deprovision(String tenantId) {
        appRoleRepository.deleteAllByTenantId(tenantId);
    }
}
