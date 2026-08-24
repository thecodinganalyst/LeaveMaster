package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserRoleAssignmentTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private AppRoleRepository appRoleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void shouldCreateStaffUserWithMultipleTenantRoles() {
        AppRole employee = role("EMPLOYEE", "tenant-a");
        AppRole approver = role("APPROVER", "tenant-a");
        when(appUserRepository.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(appRoleRepository.findById("EMPLOYEE")).thenReturn(Optional.of(employee));
        when(appRoleRepository.findById("APPROVER")).thenReturn(Optional.of(approver));
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = appUserService.createForStaff(
                "S001", "alice", "pass", true, "tenant-a", Set.of("EMPLOYEE", "APPROVER"));

        assertThat(saved.getRoles()).extracting(AppRole::getId)
                .containsExactlyInAnyOrder("EMPLOYEE", "APPROVER");
    }

    @Test
    void shouldRejectRoleFromAnotherTenant() {
        when(appUserRepository.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(appRoleRepository.findById("OTHER_ROLE")).thenReturn(Optional.of(role("OTHER_ROLE", "tenant-b")));

        assertThatThrownBy(() -> appUserService.createForStaff(
                "S001", "alice", "pass", true, "tenant-a", Set.of("OTHER_ROLE")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("does not belong to the staff tenant");
    }

    @Test
    void shouldRejectUnknownAndPlatformAdministratorRoles() {
        when(appUserRepository.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(appRoleRepository.findById("MISSING")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.createForStaff(
                "S001", "alice", "pass", true, "tenant-a", Set.of("MISSING")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Role not found");

        assertThatThrownBy(() -> appUserService.createForStaff(
                "S001", "alice", "pass", true, "tenant-a", Set.of("PLATFORM_ADMIN")))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("cannot be assigned to staff");
    }

    @Test
    void shouldReplaceStaffRolesAndAllowClearingAllRoles() {
        AppUser user = AppUser.builder()
                .loginName("alice")
                .staffId("S001")
                .tenantId("tenant-a")
                .roles(Set.of(role("EMPLOYEE", "tenant-a")))
                .build();
        AppRole approver = role("APPROVER", "tenant-a");
        when(appUserRepository.findByStaffId("S001")).thenReturn(Optional.of(user));
        when(appRoleRepository.findById("APPROVER")).thenReturn(Optional.of(approver));
        when(appUserRepository.save(user)).thenReturn(user);

        appUserService.updateRolesByStaffId("S001", Set.of("APPROVER"), "tenant-a");
        assertThat(user.getRoles()).extracting(AppRole::getId).containsExactly("APPROVER");

        appUserService.updateRolesByStaffId("S001", Set.of(), "tenant-a");
        assertThat(user.getRoles()).isEmpty();
        verify(appUserRepository, org.mockito.Mockito.times(2)).save(user);
    }

    @Test
    void shouldReturnAssignedRoleIdsForStaff() {
        AppUser user = AppUser.builder()
                .loginName("alice")
                .staffId("S001")
                .roles(Set.of(role("EMPLOYEE", "tenant-a"), role("APPROVER", "tenant-a")))
                .build();
        when(appUserRepository.findByStaffId("S001")).thenReturn(Optional.of(user));

        assertThat(appUserService.findRoleIdsByStaffId("S001"))
                .containsExactlyInAnyOrder("EMPLOYEE", "APPROVER");
        assertThat(appUserService.findRoleIdsByStaffId("missing")).isEmpty();
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
