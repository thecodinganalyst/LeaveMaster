package com.practical.leavemaster.rbac;

import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminRoleManagementTest {

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private AppPermissionRepository appPermissionRepository;

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private AppRoleService appRoleService;

    @Test
    void shouldHidePlatformAdminFromAllRoleLists() {
        AppRole platformAdmin = AppRole.builder().id("PLATFORM_ADMIN").description("Internal").active(true).build();
        AppRole tenantAdmin = AppRole.builder().id("TENANT_ADMIN").description("Tenant admin").active(true).build();
        when(appRoleRepository.findAll()).thenReturn(List.of(platformAdmin, tenantAdmin));
        when(appRoleRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(platformAdmin, tenantAdmin));

        assertThat(appRoleService.findAll()).extracting(AppRole::getId).containsExactly("TENANT_ADMIN");
        assertThat(appRoleService.findByTenantId("tenant-a")).extracting(AppRole::getId).containsExactly("TENANT_ADMIN");
    }

    @Test
    void shouldHideDirectPlatformAdminLookupWithoutQueryingRepository() {
        Optional<AppRole> result = appRoleService.findById("PLATFORM_ADMIN");

        assertThat(result).isEmpty();
        verify(appRoleRepository, never()).findById("PLATFORM_ADMIN");
    }

    @Test
    void shouldRejectPlatformAdminCreation() {
        RoleRequest request = new RoleRequest();
        request.setId("PLATFORM_ADMIN");
        request.setDescription("Reserved role");
        request.setActive(true);
        request.setPermissionCodes(Set.of());

        assertThatThrownBy(() -> appRoleService.create(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("reserved");
        verify(appRoleRepository, never()).existsById("PLATFORM_ADMIN");
    }

    @Test
    void shouldRejectPlatformAdminMutationAndAssignmentWithoutQueryingRepository() {
        RoleRequest request = new RoleRequest();
        request.setDescription("Changed");
        request.setActive(false);
        request.setPermissionCodes(Set.of());

        assertThatThrownBy(() -> appRoleService.update("PLATFORM_ADMIN", request)).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> appRoleService.disable("PLATFORM_ADMIN")).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> appRoleService.enable("PLATFORM_ADMIN")).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> appRoleService.addUserToRole("PLATFORM_ADMIN", "alice")).isInstanceOf(RoleNotFoundException.class);
        assertThatThrownBy(() -> appRoleService.removeUserFromRole("PLATFORM_ADMIN", "alice")).isInstanceOf(RoleNotFoundException.class);

        verify(appRoleRepository, never()).findById("PLATFORM_ADMIN");
        verify(appUserRepository, never()).findById("alice");
    }

    @Test
    void shouldTreatPlatformAdminIdCaseInsensitively() {
        assertThat(appRoleService.findById(" platform_admin ")).isEmpty();
    }
}
