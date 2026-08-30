package com.practical.leavemaster.tenant;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdminProvisionServiceTest {

    @Mock private AppRoleRepository appRoleRepository;
    @Mock private AppPermissionRepository appPermissionRepository;
    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private TenantAdminProvisionService service;

    @Test
    void shouldCreateTenantAdminWithoutPasswordAndThreeDefaultTenantRoles() {
        String tenantId = "ACME";
        String tenantName = "Acme Corporation";
        String tenantAdminEmail = "admin@acme.example";
        when(appRoleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(appPermissionRepository.findAllById(anyCollection())).thenAnswer(invocation -> {
            Iterable<String> codes = invocation.getArgument(0);
            List<AppPermission> permissions = new ArrayList<>();
            codes.forEach(code -> permissions.add(AppPermission.builder().code(code).description(code).build()));
            return permissions;
        });
        when(appRoleRepository.save(any(AppRole.class))).thenAnswer(i -> i.getArgument(0));
        when(appUserRepository.findByTenantIdAndLoginName("ACME", "ACME_Admin")).thenReturn(Optional.empty());
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.provision(tenantId, tenantName, tenantAdminEmail);

        ArgumentCaptor<AppRole> roleCaptor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository, times(4)).save(roleCaptor.capture());
        Map<String, AppRole> rolesById = new HashMap<>();
        roleCaptor.getAllValues().forEach(role -> rolesById.put(role.getId(), role));
        assertRole(rolesById.get("ACME_Staff"), tenantId, "Acme Corporation Staff", TenantAdminProvisionService.STAFF_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_Manager"), tenantId, "Acme Corporation Manager", TenantAdminProvisionService.MANAGER_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_HR"), tenantId, "Acme Corporation HR", TenantAdminProvisionService.HR_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_Admin"), tenantId, "Acme Corporation Tenant Admin", TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser createdUser = userCaptor.getValue();
        assertThat(createdUser.getLoginName()).isEqualTo("ACME_Admin");
        assertThat(createdUser.getTenantId()).isEqualTo(tenantId);
        assertThat(createdUser.getEmail()).isEqualTo(tenantAdminEmail);
        assertThat(createdUser.getPassword()).isNull();
        assertThat(createdUser.isActive()).isTrue();
        assertThat(createdUser.getRoles()).extracting(AppRole::getId).containsExactly("ACME_Admin");
    }

    @Test
    void shouldRepairPreExistingAdminIntoPendingActivationState() {
        String tenantId = "Bravo";
        String tenantAdminEmail = "admin@bravo.example";
        AppRole adminRole = AppRole.builder()
                .id("Bravo_Admin")
                .description("Bravo Tenant Admin")
                .active(true)
                .tenantId(tenantId)
                .build();
        AppUser staleAdmin = AppUser.builder()
                .userId("existing-user")
                .loginName("Bravo_Admin")
                .tenantId(tenantId)
                .password("legacy-default-password-hash")
                .email(null)
                .active(false)
                .roles(Set.of())
                .build();

        when(appRoleRepository.findById(anyString())).thenAnswer(invocation -> {
            String roleId = invocation.getArgument(0);
            if (roleId.equals("Bravo_Admin")) {
                return Optional.of(adminRole);
            }
            return Optional.of(AppRole.builder().id(roleId).active(true).tenantId(tenantId).build());
        });
        when(appUserRepository.findByTenantIdAndLoginName(tenantId, "Bravo_Admin"))
                .thenReturn(Optional.of(staleAdmin));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.provision(tenantId, "Bravo", tenantAdminEmail);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser repaired = userCaptor.getValue();
        assertThat(repaired.getPassword()).isNull();
        assertThat(repaired.getEmail()).isEqualTo(tenantAdminEmail);
        assertThat(repaired.isActive()).isTrue();
        assertThat(repaired.getRoles()).extracting(AppRole::getId).contains("Bravo_Admin");
    }

    @Test
    void shouldUseExactStaffPermissions() {
        assertThat(TenantAdminProvisionService.STAFF_PERMISSION_CODES).containsExactlyInAnyOrder(
                RbacPermissions.LEAVE_APPLICATION_READ, RbacPermissions.LEAVE_APPLICATION_WRITE, RbacPermissions.LEAVE_TYPE_READ);
    }

    @Test
    void shouldUseExactManagerPermissions() {
        assertThat(TenantAdminProvisionService.MANAGER_PERMISSION_CODES).containsExactlyInAnyOrder(
                RbacPermissions.LEAVE_APPLICATION_READ, RbacPermissions.LEAVE_APPLICATION_WRITE,
                RbacPermissions.LEAVE_APPLICATION_APPROVE, RbacPermissions.LEAVE_TYPE_READ);
    }

    @Test
    void shouldGiveHrAllTenantHrPermissionsExceptRoleAndTenantManagement() {
        assertThat(TenantAdminProvisionService.HR_PERMISSION_CODES)
                .containsExactlyInAnyOrder(
                        RbacPermissions.USER_READ, RbacPermissions.USER_WRITE,
                        RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE,
                        RbacPermissions.JURISDICTION_READ,
                        RbacPermissions.LEAVE_TYPE_READ, RbacPermissions.LEAVE_TYPE_WRITE,
                        RbacPermissions.LEAVE_APPROVER_READ, RbacPermissions.LEAVE_APPROVER_WRITE,
                        RbacPermissions.LEAVE_CALENDAR_READ, RbacPermissions.LEAVE_CALENDAR_WRITE,
                        RbacPermissions.LEAVE_APPLICATION_READ, RbacPermissions.LEAVE_APPLICATION_WRITE,
                        RbacPermissions.LEAVE_APPLICATION_APPROVE,
                        RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
                        RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE,
                        RbacPermissions.LEAVE_ENTITLEMENT_GENERATE)
                .doesNotContain(RbacPermissions.ROLE_MANAGE, RbacPermissions.TENANT_READ, RbacPermissions.TENANT_WRITE);
    }

    @Test
    void shouldGiveTenantAdminJurisdictionReadAccess() {
        assertThat(TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES).contains(RbacPermissions.JURISDICTION_READ);
    }

    @Test
    void shouldBeIdempotentWhenTenantRolesAndPendingAdminAlreadyExist() {
        String tenantId = "ACME";
        AppRole adminRole = AppRole.builder().id("ACME_Admin").description("ACME_Admin").active(true).tenantId(tenantId).build();
        when(appRoleRepository.findById(anyString())).thenAnswer(invocation -> {
            String roleId = invocation.getArgument(0);
            return Optional.of(roleId.equals("ACME_Admin")
                    ? adminRole
                    : AppRole.builder().id(roleId).description(roleId).active(true).tenantId(tenantId).build());
        });
        AppUser pendingAdmin = AppUser.builder()
                .loginName("ACME_Admin")
                .tenantId(tenantId)
                .email("admin@acme.example")
                .password(null)
                .active(true)
                .roles(Set.of(adminRole))
                .build();
        when(appUserRepository.findByTenantIdAndLoginName("ACME", "ACME_Admin"))
                .thenReturn(Optional.of(pendingAdmin));

        service.provision(tenantId, "Acme Corporation", "admin@acme.example");

        verify(appRoleRepository, never()).save(any());
        verify(appPermissionRepository, never()).findAllById(anyCollection());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldDeprovisionAllTenantRoles() {
        service.deprovision("ACME");
        verify(appRoleRepository).deleteAllByTenantId("ACME");
    }

    private static void assertRole(AppRole role, String tenantId, String expectedDescription, Set<String> expectedPermissionCodes) {
        assertThat(role).isNotNull();
        assertThat(role.getTenantId()).isEqualTo(tenantId);
        assertThat(role.getDescription()).isEqualTo(expectedDescription);
        assertThat(role.isActive()).isTrue();
        assertThat(role.getPermissions()).extracting(AppPermission::getCode)
                .containsExactlyInAnyOrderElementsOf(expectedPermissionCodes);
    }
}
