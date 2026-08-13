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
    static final String STAFF_ROLE_SUFFIX = "Staff";
    static final String MANAGER_ROLE_SUFFIX = "Manager";
    static final String HR_ROLE_SUFFIX = "HR";

    static final Set<String> STAFF_PERMISSION_CODES = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_TYPE_READ
    );

    static final Set<String> MANAGER_PERMISSION_CODES = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE,
            RbacPermissions.LEAVE_TYPE_READ
    );

    static final Set<String> HR_PERMISSION_CODES = Set.of(
            RbacPermissions.USER_READ,
            RbacPermissions.USER_WRITE,
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
        AppRole tenantAdminRole = provisionRole(
                TENANT_ADMIN_ROLE_ID + "_" + tenantId,
                "Tenant administrator for tenant " + tenantId,
                tenantId,
                TENANT_ADMIN_PERMISSION_CODES
        );

        provisionRole(
                tenantRoleId(tenantId, STAFF_ROLE_SUFFIX),
                "Standard staff role for tenant " + tenantId,
                tenantId,
                STAFF_PERMISSION_CODES
        );
        provisionRole(
                tenantRoleId(tenantId, MANAGER_ROLE_SUFFIX),
                "Manager and leave approver role for tenant " + tenantId,
                tenantId,
                MANAGER_PERMISSION_CODES
        );
        provisionRole(
                tenantRoleId(tenantId, HR_ROLE_SUFFIX),
                "HR operations role for tenant " + tenantId,
                tenantId,
                HR_PERMISSION_CODES
        );

        String loginName = TENANT_ADMIN_ROLE_ID + "_" + tenantId;
        if (!appUserRepository.existsById(loginName)) {
            log.info("Creating tenant admin user {} for tenant {}", loginName, tenantId);
            AppUser admin = AppUser.builder()
                    .loginName(loginName)
                    .password(passwordEncoder.encode(tenantAdminDefaultPassword))
                    .active(true)
                    .tenantId(tenantId)
                    .roles(Set.of(tenantAdminRole))
                    .build();
            appUserRepository.save(admin);
        }
    }

    private AppRole provisionRole(String roleId, String description, String tenantId, Set<String> permissionCodes) {
        return appRoleRepository.findById(roleId).orElseGet(() -> {
            log.info("Creating role {} for tenant {}", roleId, tenantId);
            AppRole newRole = AppRole.builder()
                    .id(roleId)
                    .description(description)
                    .active(true)
                    .tenantId(tenantId)
                    .permissions(Set.copyOf(appPermissionRepository.findAllById(permissionCodes)))
                    .build();
            return appRoleRepository.save(newRole);
        });
    }

    private String tenantRoleId(String tenantId, String suffix) {
        return tenantId + "_" + suffix;
    }

    @Transactional
    public void deprovision(String tenantId) {
        appRoleRepository.deleteAllByTenantId(tenantId);
    }
}
