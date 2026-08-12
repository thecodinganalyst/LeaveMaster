package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformOnlyTenantPermissionTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppRoleService appRoleService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void tenantAdminCannotSeeTenantManagementPermissions() {
        authenticateAsRole("tenant-admin", "TENANT_ADMIN");
        when(appPermissionRepository.findAll()).thenReturn(List.of(
                permission(RbacPermissions.TENANT_READ),
                permission(RbacPermissions.TENANT_WRITE),
                permission(RbacPermissions.STAFF_READ)
        ));

        List<AppPermission> result = appRoleService.findAllPermissions();

        assertThat(result).extracting(AppPermission::getCode)
                .containsExactly(RbacPermissions.STAFF_READ);
    }

    @Test
    void regularTenantUserCannotSeeTenantManagementPermissions() {
        authenticateAsRole("employee", "EMPLOYEE");
        when(appPermissionRepository.findAll()).thenReturn(List.of(
                permission(RbacPermissions.TENANT_READ),
                permission(RbacPermissions.USER_READ)
        ));

        List<AppPermission> result = appRoleService.findAllPermissions();

        assertThat(result).extracting(AppPermission::getCode)
                .containsExactly(RbacPermissions.USER_READ);
    }

    @Test
    void platformAdminCanSeeTenantManagementPermissions() {
        authenticateAsRole("platformadmin", AppRoleService.PLATFORM_ADMIN_ROLE_ID);
        List<AppPermission> permissions = List.of(
                permission(RbacPermissions.TENANT_READ),
                permission(RbacPermissions.TENANT_WRITE),
                permission(RbacPermissions.USER_READ)
        );
        when(appPermissionRepository.findAll()).thenReturn(permissions);

        assertThat(appRoleService.findAllPermissions()).isEqualTo(permissions);
    }

    @Test
    void tenantAdminCannotCreateRoleWithTenantManagementPermission() {
        authenticate("tenant-admin");
        RoleRequest request = request("tenant-role", Set.of(RbacPermissions.TENANT_READ));
        when(appRoleRepository.existsById("tenant-role")).thenReturn(false);

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform-only permission codes cannot be assigned")
                .hasMessageContaining(RbacPermissions.TENANT_READ);

        verify(appPermissionRepository, never()).findAllById(any());
        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void tenantAdminCannotUpdateRoleWithTenantManagementPermission() {
        authenticate("tenant-admin");
        AppRole existing = role("tenant-role");
        when(appRoleRepository.findById("tenant-role")).thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> appRoleService.update(
                "tenant-role",
                request(null, Set.of(RbacPermissions.TENANT_WRITE))))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform-only permission codes cannot be assigned")
                .hasMessageContaining(RbacPermissions.TENANT_WRITE);

        verify(appRoleRepository, never()).save(any());
    }

    @Test
    void platformAdminCannotAssignTenantManagementPermissionToAnotherRole() {
        authenticate("platformadmin");
        RoleRequest request = request("platform-operator", Set.of(RbacPermissions.TENANT_READ));
        when(appRoleRepository.existsById("platform-operator")).thenReturn(false);

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Platform-only permission codes cannot be assigned to managed roles")
                .hasMessageContaining(RbacPermissions.TENANT_READ);

        verify(appPermissionRepository, never()).findAllById(any());
        verify(appRoleRepository, never()).save(any());
    }

    private void authenticateAsRole(String loginName, String roleId) {
        authenticate(loginName);
        when(appUserRepository.findById(loginName))
                .thenReturn(java.util.Optional.of(user(loginName, role(roleId))));
    }

    private void authenticate(String loginName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginName, "n/a", List.of())
        );
    }

    private static RoleRequest request(String id, Set<String> permissionCodes) {
        RoleRequest request = new RoleRequest();
        request.setId(id);
        request.setDescription("Test role");
        request.setActive(true);
        request.setPermissionCodes(permissionCodes);
        return request;
    }

    private static AppPermission permission(String code) {
        return AppPermission.builder().code(code).description(code).build();
    }

    private static AppRole role(String id) {
        return AppRole.builder().id(id).description(id).active(true).build();
    }

    private static AppUser user(String loginName, AppRole role) {
        return AppUser.builder()
                .loginName(loginName)
                .password("encoded")
                .active(true)
                .roles(new HashSet<>(Set.of(role)))
                .build();
    }
}
