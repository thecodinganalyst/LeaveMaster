package com.practical.leavemaster.staff;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.rbac.RbacPermissions;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class StaffRoleAssignmentPolicyTest {

    private AppRoleRepository appRoleRepository;
    private AppUserRepository appUserRepository;
    private StaffRoleAssignmentPolicy policy;

    @BeforeEach
    void setUp() {
        appRoleRepository = mock(AppRoleRepository.class);
        appUserRepository = mock(AppUserRepository.class);
        policy = new StaffRoleAssignmentPolicy(appRoleRepository, appUserRepository);
        authenticate("hr-user");
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void hrCanSeeOnlyActiveTenantRolesWithinItsPermissionCeiling() {
        AppRole hrRole = role("ACME_HR", true,
                RbacPermissions.STAFF_READ,
                RbacPermissions.STAFF_WRITE,
                RbacPermissions.LEAVE_APPLICATION_READ,
                RbacPermissions.LEAVE_APPLICATION_APPROVE);
        AppUser hrUser = user("hr-user", "ACME", hrRole);
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.of(hrUser));

        AppRole staffRole = role("ACME_Staff", true, RbacPermissions.LEAVE_APPLICATION_READ);
        AppRole managerRole = role("ACME_Manager", true,
                RbacPermissions.LEAVE_APPLICATION_READ,
                RbacPermissions.LEAVE_APPLICATION_APPROVE);
        AppRole anotherHrRole = role("ACME_HR", true,
                RbacPermissions.STAFF_READ,
                RbacPermissions.STAFF_WRITE,
                RbacPermissions.LEAVE_APPLICATION_READ,
                RbacPermissions.LEAVE_APPLICATION_APPROVE);
        AppRole adminRole = role("ACME_Admin", true,
                RbacPermissions.STAFF_READ,
                RbacPermissions.STAFF_WRITE,
                RbacPermissions.ROLE_MANAGE);
        AppRole inactiveRole = role("ACME_Inactive", false, RbacPermissions.LEAVE_APPLICATION_READ);
        AppRole platformAdmin = role("PLATFORM_ADMIN", true, RbacPermissions.STAFF_WRITE);
        when(appRoleRepository.findAllByTenantId("ACME"))
                .thenReturn(List.of(adminRole, managerRole, inactiveRole, platformAdmin, staffRole, anotherHrRole));

        assertThat(policy.findAssignableRoles())
                .extracting(AppRole::getId)
                .containsExactly("ACME_HR", "ACME_Manager", "ACME_Staff");
    }

    @Test
    void rejectsRoleThatWouldGrantPermissionCurrentUserDoesNotHave() {
        AppRole hrRole = role("ACME_HR", true, RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE);
        AppUser hrUser = user("hr-user", "ACME", hrRole);
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.of(hrUser));
        when(appRoleRepository.findAllByTenantId("ACME")).thenReturn(List.of(
                role("ACME_Staff", true, RbacPermissions.STAFF_READ),
                role("ACME_Admin", true, RbacPermissions.STAFF_READ, RbacPermissions.ROLE_MANAGE)));

        assertThatThrownBy(() -> policy.validateAssignableRoleIds(Set.of("ACME_Admin")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role is not assignable by the current user: ACME_Admin");
    }

    @Test
    void acceptsAssignableRolesAndNormalizesWhitespace() {
        AppRole hrRole = role("ACME_HR", true, RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE);
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.of(user("hr-user", "ACME", hrRole)));
        when(appRoleRepository.findAllByTenantId("ACME"))
                .thenReturn(List.of(role("ACME_Staff", true, RbacPermissions.STAFF_READ)));

        assertThatCode(() -> policy.validateAssignableRoleIds(Set.of("  ACME_Staff  "))).doesNotThrowAnyException();
    }

    @Test
    void rejectsBlankRoleId() {
        AppRole hrRole = role("ACME_HR", true, RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE);
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.of(user("hr-user", "ACME", hrRole)));
        when(appRoleRepository.findAllByTenantId("ACME")).thenReturn(List.of());

        assertThatThrownBy(() -> policy.validateAssignableRoleIds(Set.of(" ")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("Role id must not be blank");
    }

    @Test
    void nullAndEmptyRoleSetsNeedNoValidation() {
        assertThatCode(() -> policy.validateAssignableRoleIds(null)).doesNotThrowAnyException();
        assertThatCode(() -> policy.validateAssignableRoleIds(Set.of())).doesNotThrowAnyException();
    }

    @Test
    void tenantAdminCanAssignRolesWithinItsOwnPermissionSet() {
        AppRole adminRole = role("ACME_Admin", true,
                RbacPermissions.STAFF_READ,
                RbacPermissions.STAFF_WRITE,
                RbacPermissions.ROLE_MANAGE);
        AppUser adminUser = user("hr-user", "ACME", adminRole);
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.of(adminUser));
        when(appRoleRepository.findAllByTenantId("ACME")).thenReturn(List.of(
                adminRole,
                role("ACME_HR", true, RbacPermissions.STAFF_READ, RbacPermissions.STAFF_WRITE)));

        assertThat(policy.findAssignableRoles())
                .extracting(AppRole::getId)
                .containsExactly("ACME_Admin", "ACME_HR");
    }

    @Test
    void returnsNoRolesWithoutAuthenticatedActiveTenantUser() {
        SecurityContextHolder.clearContext();
        assertThat(policy.findAssignableRoles()).isEmpty();

        authenticate("inactive-user");
        AppUser inactive = user("inactive-user", "ACME");
        inactive.setActive(false);
        when(appUserRepository.findById("inactive-user")).thenReturn(Optional.of(inactive));
        assertThat(policy.findAssignableRoles()).isEmpty();

        authenticate("no-tenant-user");
        when(appUserRepository.findById("no-tenant-user"))
                .thenReturn(Optional.of(user("no-tenant-user", " ")));
        assertThat(policy.findAssignableRoles()).isEmpty();
    }

    @Test
    void fallsBackToLoginNameAndHandlesRolesWithoutPermissions() {
        when(appUserRepository.findById("hr-user")).thenReturn(Optional.empty());
        AppUser current = user("hr-user", "ACME");
        current.setRoles(null);
        when(appUserRepository.findUniqueByLoginName("hr-user")).thenReturn(Optional.of(current));

        AppRole permissionless = role("ACME_Observer", true);
        permissionless.setPermissions(null);
        when(appRoleRepository.findAllByTenantId("ACME")).thenReturn(List.of(permissionless));

        assertThat(policy.findAssignableRoles())
                .extracting(AppRole::getId)
                .containsExactly("ACME_Observer");
    }

    private void authenticate(String principal) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, "n/a", List.of()));
    }

    private static AppUser user(String loginName, String tenantId, AppRole... roles) {
        return AppUser.builder()
                .loginName(loginName)
                .tenantId(tenantId)
                .active(true)
                .roles(Set.of(roles))
                .build();
    }

    private static AppRole role(String id, boolean active, String... permissionCodes) {
        Set<AppPermission> permissions = java.util.Arrays.stream(permissionCodes)
                .map(code -> AppPermission.builder().code(code).description(code).build())
                .collect(java.util.stream.Collectors.toSet());
        return AppRole.builder()
                .id(id)
                .description(id)
                .tenantId("ACME")
                .active(active)
                .permissions(permissions)
                .build();
    }
}
