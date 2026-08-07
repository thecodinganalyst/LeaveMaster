package com.practical.leavemaster.mcp;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleService;
import com.practical.leavemaster.rbac.RoleRequest;
import com.practical.leavemaster.user.AppUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppRoleMcpToolsTest {

    @Mock
    private AppRoleService appRoleService;

    @InjectMocks
    private AppRoleMcpTools appRoleMcpTools;

    @Test
    void shouldGetAllRoles() {
        List<AppRole> roles = List.of(AppRole.builder().id("admin").description("Admin").build());
        when(appRoleService.findAll()).thenReturn(roles);

        List<AppRole> result = appRoleMcpTools.getAllRoles();

        assertThat(result).hasSize(1);
        verify(appRoleService).findAll();
    }

    @Test
    void shouldGetRolesByTenantId() {
        List<AppRole> roles = List.of(AppRole.builder().id("admin").tenantId("t1").build());
        when(appRoleService.findByTenantId("t1")).thenReturn(roles);

        List<AppRole> result = appRoleMcpTools.getRolesByTenantId("t1");

        assertThat(result).hasSize(1);
        verify(appRoleService).findByTenantId("t1");
    }

    @Test
    void shouldGetAllPermissions() {
        List<AppPermission> permissions = List.of(AppPermission.builder().code("STAFF_READ").build());
        when(appRoleService.findAllPermissions()).thenReturn(permissions);

        List<AppPermission> result = appRoleMcpTools.getAllPermissions();

        assertThat(result).hasSize(1);
        verify(appRoleService).findAllPermissions();
    }

    @Test
    void shouldGetRoleById() {
        AppRole role = AppRole.builder().id("admin").description("Admin").build();
        when(appRoleService.findById("admin")).thenReturn(Optional.of(role));

        Optional<AppRole> result = appRoleMcpTools.getRoleById("admin");

        assertThat(result).isPresent();
        verify(appRoleService).findById("admin");
    }

    @Test
    void shouldCreateRole() {
        RoleRequest request = new RoleRequest();
        request.setId("admin");
        request.setDescription("Admin Role");
        AppRole role = AppRole.builder().id("admin").description("Admin Role").build();
        when(appRoleService.create(request)).thenReturn(role);

        AppRole result = appRoleMcpTools.createRole(request);

        assertThat(result.getId()).isEqualTo("admin");
        verify(appRoleService).create(request);
    }

    @Test
    void shouldUpdateRole() {
        RoleRequest request = new RoleRequest();
        request.setId("admin");
        request.setDescription("Updated Role");
        AppRole role = AppRole.builder().id("admin").description("Updated Role").build();
        when(appRoleService.update("admin", request)).thenReturn(role);

        AppRole result = appRoleMcpTools.updateRole("admin", request);

        assertThat(result.getDescription()).isEqualTo("Updated Role");
        verify(appRoleService).update("admin", request);
    }

    @Test
    void shouldDisableRole() {
        AppRole role = AppRole.builder().id("admin").active(false).build();
        when(appRoleService.disable("admin")).thenReturn(role);

        AppRole result = appRoleMcpTools.disableRole("admin");

        assertThat(result.isActive()).isFalse();
        verify(appRoleService).disable("admin");
    }

    @Test
    void shouldEnableRole() {
        AppRole role = AppRole.builder().id("admin").active(true).build();
        when(appRoleService.enable("admin")).thenReturn(role);

        AppRole result = appRoleMcpTools.enableRole("admin");

        assertThat(result.isActive()).isTrue();
        verify(appRoleService).enable("admin");
    }

    @Test
    void shouldAddUserToRole() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appRoleService.addUserToRole("admin", "alice")).thenReturn(user);

        AppUser result = appRoleMcpTools.addUserToRole("admin", "alice");

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(appRoleService).addUserToRole("admin", "alice");
    }

    @Test
    void shouldRemoveUserFromRole() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appRoleService.removeUserFromRole("admin", "alice")).thenReturn(user);

        AppUser result = appRoleMcpTools.removeUserFromRole("admin", "alice");

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(appRoleService).removeUserFromRole("admin", "alice");
    }
}
