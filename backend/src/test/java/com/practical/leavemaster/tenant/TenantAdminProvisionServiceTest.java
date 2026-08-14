package com.practical.leavemaster.tenant;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppPermissionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TenantAdminProvisionServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private TenantAdminProvisionService service;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(service, "tenantAdminDefaultPassword", "test-password");
    }

    @Test
    void shouldCreateTenantAdminAndThreeDefaultTenantRoles() {
        String tenantId = "ACME";
        when(appRoleRepository.findById(anyString())).thenReturn(Optional.empty());
        when(appPermissionRepository.findAllById(anyCollection())).thenAnswer(invocation -> {
            Iterable<String> codes = invocation.getArgument(0);
            List<AppPermission> permissions = new ArrayList<>();
            codes.forEach(code -> permissions.add(AppPermission.builder().code(code).description(code).build()));
            return permissions;
        });
        when(appRoleRepository.save(any(AppRole.class))).thenAnswer(i -> i.getArgument(0));
        when(appUserRepository.existsById("ACME_Admin")).thenReturn(false);
        when(passwordEncoder.encode("test-password")).thenReturn("$2a$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        service.provision(tenantId);

        ArgumentCaptor<AppRole> roleCaptor = ArgumentCaptor.forClass(AppRole.class);
        verify(appRoleRepository, times(4)).save(roleCaptor.capture());
        Map<String, AppRole> rolesById = new HashMap<>();
        roleCaptor.getAllValues().forEach(role -> rolesById.put(role.getId(), role));

        assertRole(rolesById.get("ACME_Staff"), tenantId, TenantAdminProvisionService.STAFF_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_Manager"), tenantId, TenantAdminProvisionService.MANAGER_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_HR"), tenantId, TenantAdminProvisionService.HR_PERMISSION_CODES);
        assertRole(rolesById.get("ACME_Admin"), tenantId, TenantAdminProvisionService.TENANT_ADMIN_PERMISSION_CODES);

        ArgumentCaptor<AppUser> userCaptor = ArgumentCaptor.forClass(AppUser.class);
        verify(appUserRepository).save(userCaptor.capture());
        AppUser createdUser = userCaptor.getValue();
        assertThat(createdUser.getLoginName()).isEqualTo("ACME_Admin");
        assertThat(createdUser.getTenantId()).isEqualTo(tenantId);
        assertThat(createdUser.isActive()).isTrue();
        assertThat(createdUser.getRoles())
                .extracting(AppRole::getId)
                .containsExactly("ACME_Admin");
    }

    @Test
    void shouldUseExactStaffPermissions() {
        assertThat(TenantAdminProvisionService.STAFF_PERMISSION_CODES)
                .containsExactlyInAnyOrder(
                        RbacPermissions.LEAVE_APPLICATION_READ,
                        RbacPermissions.LEAVE_APPLICATION_WRITE,
                        RbacPermissions.LEAVE_TYPE_READ
                );
    }

    @Test
    void shouldUseExactManagerPermissions() {
        assertThat(TenantAdminProvisionService.MANAGER_PERMISSION_CODES)
                .containsExactlyInAnyOrder(
                        RbacPermissions.LEAVE_APPLICATION_READ,
                        RbacPermissions.LEAVE_APPLICATION_WRITE,
                        RbacPermissions.LEAVE_APPLICATION_APPROVE,
                        RbacPermissions.LEAVE_TYPE_READ
                );
    }

    @Test
    void shouldGiveHrAllTenantHrPermissionsExceptRoleAndTenantManagement() {
        assertThat(TenantAdminProvisionService.HR_PERMISSION_CODES)
                .containsExactlyInAnyOrder(
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
                        RbacPermissions.LEAVE_APPLICATION_APPROVE,
                        RbacPermissions.LEAVE_ENTITLEMENT_POLICY_READ,
                        RbacPermissions.LEAVE_ENTITLEMENT_POLICY_WRITE
                )
                .doesNotContain(
                        RbacPermissions.ROLE_MANAGE,
                        RbacPermissions.TENANT_READ,
                        RbacPermissions.TENANT_WRITE
                );
    }

    @Test
    void shouldBeIdempotentWhenTenantRolesAndAdminAlreadyExist() {
        String tenantId = "ACME";
        when(appRoleRepository.findById(anyString())).thenAnswer(invocation -> {
            String roleId = invocation.getArgument(0);
            return Optional.of(AppRole.builder()
                    .id(roleId)
                    .description(roleId)
                    .active(true)
                    .tenantId(tenantId)
                    .build());
        });
        when(appUserRepository.existsById("ACME_Admin")).thenReturn(true);

        service.provision(tenantId);

        verify(appRoleRepository, never()).save(any());
        verify(appPermissionRepository, never()).findAllById(anyCollection());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void shouldDeprovisionAllTenantRoles() {
        service.deprovision("ACME");

        verify(appRoleRepository).deleteAllByTenantId("ACME");
    }

    private static void assertRole(AppRole role, String tenantId, Set<String> expectedPermissionCodes) {
        assertThat(role).isNotNull();
        assertThat(role.getTenantId()).isEqualTo(tenantId);
        assertThat(role.isActive()).isTrue();
        assertThat(role.getPermissions())
                .extracting(AppPermission::getCode)
                .containsExactlyInAnyOrderElementsOf(expectedPermissionCodes);
    }
}
