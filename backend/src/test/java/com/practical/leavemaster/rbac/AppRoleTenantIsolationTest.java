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
class AppRoleTenantIsolationTest {

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
    void tenantUserShouldOnlyListRolesFromOwnTenant() {
        authenticate("admin-a");
        AppUser currentUser = user("admin-a", "tenant-a");
        AppRole ownRole = role("tenant-a_Staff", "tenant-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(currentUser));
        when(appRoleRepository.findAllByTenantId("tenant-a")).thenReturn(List.of(ownRole));

        List<AppRole> result = appRoleService.findAll();

        assertThat(result).containsExactly(ownRole);
        verify(appRoleRepository, never()).findAll();
    }

    @Test
    void tenantUserShouldNotFindRoleFromAnotherTenant() {
        authenticate("admin-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(user("admin-a", "tenant-a")));
        when(appRoleRepository.findById("tenant-b_Staff"))
                .thenReturn(Optional.of(role("tenant-b_Staff", "tenant-b")));

        assertThat(appRoleService.findById("tenant-b_Staff")).isEmpty();
    }

    @Test
    void createdRoleShouldInheritAuthenticatedUsersTenant() {
        authenticate("admin-a");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(user("admin-a", "tenant-a")));
        when(appRoleRepository.existsById("tenant-a_Custom")).thenReturn(false);
        when(appPermissionRepository.findAllById(Set.of())).thenReturn(List.of());
        when(appRoleRepository.save(any(AppRole.class))).thenAnswer(invocation -> invocation.getArgument(0));

        RoleRequest request = new RoleRequest();
        request.setId("tenant-a_Custom");
        request.setDescription("Custom tenant role");
        request.setActive(true);
        request.setPermissionCodes(Set.of());

        AppRole created = appRoleService.create(request);

        assertThat(created.getTenantId()).isEqualTo("tenant-a");
    }

    @Test
    void shouldRejectAssigningTenantRoleToUserFromAnotherTenant() {
        authenticate("admin-a");
        AppUser currentUser = user("admin-a", "tenant-a");
        AppRole staffRole = role("tenant-a_Staff", "tenant-a");
        AppUser otherTenantUser = user("bob", "tenant-b");
        when(appUserRepository.findById("admin-a")).thenReturn(Optional.of(currentUser));
        when(appRoleRepository.findById("tenant-a_Staff")).thenReturn(Optional.of(staffRole));
        when(appUserRepository.findById("bob")).thenReturn(Optional.of(otherTenantUser));

        assertThatThrownBy(() -> appRoleService.addUserToRole("tenant-a_Staff", "bob"))
                .isInstanceOf(RoleNotFoundException.class)
                .hasMessageContaining("tenant-a_Staff");

        verify(appUserRepository, never()).save(otherTenantUser);
    }

    private static void authenticate(String loginName) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(loginName, "n/a", List.of())
        );
    }

    private static AppUser user(String loginName, String tenantId) {
        return AppUser.builder()
                .loginName(loginName)
                .password("encoded")
                .active(true)
                .tenantId(tenantId)
                .build();
    }

    private static AppRole role(String id, String tenantId) {
        return AppRole.builder()
                .id(id)
                .description(id)
                .active(true)
                .tenantId(tenantId)
                .build();
    }
}
