package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserNotFoundException;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppRoleServiceTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppRoleService appRoleService;

    @Test
    void shouldReturnAllRoles() {
        List<AppRole> roles = List.of(AppRole.builder().id("admin").description("Admin").active(true).build());
        when(appRoleRepository.findAll()).thenReturn(roles);

        List<AppRole> result = appRoleService.findAll();

        assertThat(result).isEqualTo(roles);
    }

    @Test
    void shouldReturnRolesByTenantId() {
        List<AppRole> roles = List.of(AppRole.builder().id("manager").description("Manager").active(true).tenantId("tenant-a").build());
        when(appRoleRepository.findAllByTenantId("tenant-a")).thenReturn(roles);

        List<AppRole> result = appRoleService.findByTenantId("tenant-a");

        assertThat(result).isEqualTo(roles);
    }

    @Test
    void shouldReturnAllPermissions() {
        List<AppPermission> permissions = List.of(AppPermission.builder().code("staff.read").description("Read").build());
        when(appPermissionRepository.findAll()).thenReturn(permissions);

        List<AppPermission> result = appRoleService.findAllPermissions();

        assertThat(result).isEqualTo(permissions);
    }

    @Test
    void shouldFindRoleById() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));

        Optional<AppRole> result = appRoleService.findById("admin");

        assertThat(result).contains(role);
    }

    @Test
    void shouldCreateRole() {
        AppPermission permission = AppPermission.builder().code("staff.read").description("Read").build();
        RoleRequest request = roleRequest("admin", "Admin role", true, Set.of("staff.read"));
        when(appRoleRepository.existsById("admin")).thenReturn(false);
        when(appPermissionRepository.findAllById(Set.of("staff.read"))).thenReturn(List.of(permission));
        when(appRoleRepository.save(any(AppRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppRole result = appRoleService.create(request);

        assertThat(result.getId()).isEqualTo("admin");
        assertThat(result.getDescription()).isEqualTo("Admin role");
        assertThat(result.isActive()).isTrue();
        assertThat(result.getPermissions()).containsExactly(permission);
    }

    @Test
    void shouldCreateRoleWithNoPermissionCodesWhenNull() {
        RoleRequest request = roleRequest("admin", "Admin role", true, null);
        when(appRoleRepository.existsById("admin")).thenReturn(false);
        when(appPermissionRepository.findAllById(Set.of())).thenReturn(List.of());
        when(appRoleRepository.save(any(AppRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppRole result = appRoleService.create(request);

        assertThat(result.getPermissions()).isEmpty();
    }

    @Test
    void shouldThrowWhenCreatingWithBlankId() {
        RoleRequest request = roleRequest(" ", "Admin role", true, Set.of());

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role id must not be blank");
    }

    @Test
    void shouldThrowWhenCreatingWithBlankDescription() {
        RoleRequest request = roleRequest("admin", "", true, Set.of());

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role description must not be blank");
    }

    @Test
    void shouldThrowWhenCreatingExistingRole() {
        RoleRequest request = roleRequest("admin", "Admin role", true, Set.of());
        when(appRoleRepository.existsById("admin")).thenReturn(true);

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role already exists: admin");
    }

    @Test
    void shouldThrowWhenCreatingWithUnknownPermissionCodes() {
        RoleRequest request = roleRequest("admin", "Admin role", true, Set.of("unknown.permission"));
        when(appRoleRepository.existsById("admin")).thenReturn(false);
        when(appPermissionRepository.findAllById(Set.of("unknown.permission"))).thenReturn(List.of());

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown permission codes");
    }

    @Test
    void shouldUpdateRole() {
        AppRole existingRole = AppRole.builder().id("admin").description("Old").active(true).build();
        AppPermission permission = AppPermission.builder().code("staff.write").description("Write").build();
        RoleRequest request = roleRequest(null, "Updated", false, Set.of("staff.write"));
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(existingRole));
        when(appPermissionRepository.findAllById(Set.of("staff.write"))).thenReturn(List.of(permission));
        when(appRoleRepository.save(existingRole)).thenReturn(existingRole);

        AppRole result = appRoleService.update("admin", request);

        assertThat(result.getDescription()).isEqualTo("Updated");
        assertThat(result.isActive()).isFalse();
        assertThat(result.getPermissions()).containsExactly(permission);
    }

    @Test
    void shouldThrowWhenUpdatingUnknownRole() {
        when(appRoleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.update("missing", roleRequest(null, "Updated", true, Set.of())))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldThrowWhenUpdatingWithBlankDescription() {
        AppRole existingRole = AppRole.builder().id("admin").description("Old").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(existingRole));

        assertThatThrownBy(() -> appRoleService.update("admin", roleRequest(null, " ", true, Set.of())))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role description must not be blank");
    }

    @Test
    void shouldDisableRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));
        when(appRoleRepository.save(role)).thenReturn(role);

        AppRole result = appRoleService.disable("admin");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldEnableRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(false).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));
        when(appRoleRepository.save(role)).thenReturn(role);

        AppRole result = appRoleService.enable("admin");

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenDisablingUnknownRole() {
        when(appRoleRepository.findById("missing")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.disable("missing"))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("missing");
    }

    @Test
    void shouldAddUserToRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(true).build();
        AppUser user = AppUser.builder().loginName("alice").password("$2a$encoded-password").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        AppUser result = appRoleService.addUserToRole("admin", "alice");

        assertThat(result.getRoles()).contains(role);
    }

    @Test
    void shouldThrowWhenAddingUserToDisabledRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(false).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));

        assertThatThrownBy(() -> appRoleService.addUserToRole("admin", "alice"))
                .isInstanceOf(RoleDisabledException.class)
                .hasMessageContaining("admin");

        verify(appUserRepository, never()).findById("alice");
    }

    @Test
    void shouldThrowWhenAddingMissingUserToRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));
        when(appUserRepository.findById("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.addUserToRole("admin", "alice"))
                .isInstanceOf(AppUserNotFoundException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void shouldRemoveUserFromRole() {
        AppRole adminRole = AppRole.builder().id("admin").description("Admin").active(true).build();
        AppRole managerRole = AppRole.builder().id("manager").description("Manager").active(true).build();
        AppUser user = AppUser.builder()
                .loginName("alice")
                .password("$2a$encoded-password")
                .active(true)
                .roles(new HashSet<>(Set.of(adminRole, managerRole)))
                .build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(adminRole));
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        AppUser result = appRoleService.removeUserFromRole("admin", "alice");

        assertThat(result.getRoles()).containsExactly(managerRole);
    }

    @Test
    void shouldThrowWhenRemovingUserFromUnknownRole() {
        when(appRoleRepository.findById("admin")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.removeUserFromRole("admin", "alice"))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("admin");
    }

    @Test
    void shouldThrowWhenRemovingMissingUserFromRole() {
        AppRole role = AppRole.builder().id("admin").description("Admin").active(true).build();
        when(appRoleRepository.findById("admin")).thenReturn(Optional.of(role));
        when(appUserRepository.findById("alice")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appRoleService.removeUserFromRole("admin", "alice"))
                .isInstanceOf(AppUserNotFoundException.class)
                .hasMessageContaining("alice");
    }

    private static RoleRequest roleRequest(String id, String description, boolean active, Set<String> permissionCodes) {
        RoleRequest request = new RoleRequest();
        request.setId(id);
        request.setDescription(description);
        request.setActive(active);
        request.setPermissionCodes(permissionCodes);
        return request;
    }
}
