package com.practical.leavemaster.tenant;

import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class TenantAdminProvisionService {

    static final String ADMIN_ROLE_SUFFIX = "Admin";
    static final String STAFF_ROLE_SUFFIX = "Staff";
    static final String MANAGER_ROLE_SUFFIX = "Manager";
    static final String HR_ROLE_SUFFIX = "HR";

    static final Set<String> STAFF_PERMISSION_CODES = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_CALENDAR_READ
    );

    static final Set<String> MANAGER_PERMISSION_CODES = Set.of(
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_CALENDAR_READ
    );

    static final Set<String> HR_PERMISSION_CODES = Set.of(
            RbacPermissions.USER_READ,
            RbacPermissions.USER_WRITE,
            RbacPermissions.STAFF_READ,
            RbacPermissions.STAFF_WRITE,
            RbacPermissions.JURISDICTION_READ,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_TYPE_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_GENERATE,
            RbacPermissions.LEAVE_APPROVER_READ,
            RbacPermissions.LEAVE_APPROVER_WRITE,
            RbacPermissions.LEAVE_CALENDAR_READ,
            RbacPermissions.LEAVE_CALENDAR_WRITE,
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
            RbacPermissions.JURISDICTION_READ,
            RbacPermissions.LEAVE_TYPE_READ,
            RbacPermissions.LEAVE_TYPE_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
            RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
            RbacPermissions.LEAVE_ENTITLEMENT_GENERATE,
            RbacPermissions.LEAVE_APPROVER_READ,
            RbacPermissions.LEAVE_APPROVER_WRITE,
            RbacPermissions.LEAVE_CALENDAR_READ,
            RbacPermissions.LEAVE_CALENDAR_WRITE,
            RbacPermissions.LEAVE_APPLICATION_READ,
            RbacPermissions.LEAVE_APPLICATION_WRITE,
            RbacPermissions.LEAVE_APPLICATION_APPROVE
    );

    private final AppRoleRepository appRoleRepository;
    private final AppPermissionRepository appPermissionRepository;
    private final AppUserRepository appUserRepository;

    @Transactional
    public void provision(String tenantId, String tenantName, String tenantAdminEmail) {
        String tenantAdminLogin = tenantRoleId(tenantId, ADMIN_ROLE_SUFFIX);
        AppRole tenantAdminRole = provisionRole(
                tenantAdminLogin,
                tenantName + " Tenant Admin",
                tenantId,
                TENANT_ADMIN_PERMISSION_CODES
        );

        provisionRole(tenantRoleId(tenantId, STAFF_ROLE_SUFFIX), tenantName + " Staff",
                tenantId, STAFF_PERMISSION_CODES);
        provisionRole(tenantRoleId(tenantId, MANAGER_ROLE_SUFFIX), tenantName + " Manager",
                tenantId, MANAGER_PERMISSION_CODES);
        provisionRole(tenantRoleId(tenantId, HR_ROLE_SUFFIX), tenantName + " HR",
                tenantId, HR_PERMISSION_CODES);

        appUserRepository.findByTenantIdAndLoginName(tenantId, tenantAdminLogin)
                .ifPresentOrElse(
                        existing -> reconcilePendingTenantAdmin(
                                existing, tenantId, tenantAdminLogin, tenantAdminEmail, tenantAdminRole),
                        () -> createPendingTenantAdmin(
                                tenantId, tenantAdminLogin, tenantAdminEmail, tenantAdminRole));
    }

    private void createPendingTenantAdmin(
            String tenantId,
            String tenantAdminLogin,
            String tenantAdminEmail,
            AppRole tenantAdminRole) {
        log.info("Creating tenant admin user for tenant {}", tenantId);
        AppUser admin = AppUser.builder()
                .loginName(tenantAdminLogin)
                .password(null)
                .email(tenantAdminEmail)
                .active(true)
                .tenantId(tenantId)
                .roles(Set.of(tenantAdminRole))
                .build();
        appUserRepository.save(admin);
    }

    private void reconcilePendingTenantAdmin(
            AppUser admin,
            String tenantId,
            String tenantAdminLogin,
            String tenantAdminEmail,
            AppRole tenantAdminRole) {
        Set<AppRole> roles = admin.getRoles() == null
                ? new HashSet<>()
                : new HashSet<>(admin.getRoles());
        boolean hasAdminRole = roles.stream().anyMatch(role -> tenantAdminRole.getId().equals(role.getId()));
        boolean needsRepair = admin.getPassword() != null
                || !tenantAdminEmail.equals(admin.getEmail())
                || !admin.isActive()
                || !tenantId.equals(admin.getTenantId())
                || !tenantAdminLogin.equals(admin.getLoginName())
                || !hasAdminRole;

        if (!needsRepair) {
            return;
        }

        log.warn("Repairing pre-existing tenant admin account into pending activation state for tenant {}", tenantId);
        roles.add(tenantAdminRole);
        admin.setLoginName(tenantAdminLogin);
        admin.setTenantId(tenantId);
        admin.setEmail(tenantAdminEmail);
        admin.setPassword(null);
        admin.setActive(true);
        admin.setRoles(roles);
        appUserRepository.save(admin);
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
