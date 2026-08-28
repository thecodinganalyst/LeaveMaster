package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AppUserServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private AppRoleRepository appRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantActivityService tenantActivityService;

    @InjectMocks private AppUserService appUserService;

    @Test
    void shouldReturnAllNonPlatformUsersWithoutAuthentication() {
        List<AppUser> users = List.of(
                user("u1", "tenant-a", "alice", true),
                user("u2", "tenant-b", "bob", false)
        );
        when(appUserRepository.findAll()).thenReturn(users);

        List<AppUser> result = appUserService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFindUniqueUserByLoginNameWithoutTenantContext() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserService.findByLoginName("alice");

        assertThat(result).contains(user);
    }

    @Test
    void shouldFindUserByExplicitTenantAndLoginName() {
        AppUser tenantA = user("u1", "tenant-a", "001", true);
        when(appUserRepository.findScopedByLoginName("tenant-a", "001")).thenReturn(Optional.of(tenantA));

        assertThat(appUserService.findByLoginName("tenant-a", "001")).contains(tenantA);
    }

    @Test
    void shouldSaveUserWhenLoginNameIsAvailableWithinTenant() {
        AppUser user = user(null, "tenant-a", "alice", true);
        user.setPassword("pass");
        when(appUserRepository.existsScopedLoginName("tenant-a", "alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$encoded");
        when(appUserRepository.save(user)).thenReturn(user);

        AppUser result = appUserService.save(user);

        assertThat(result.getLoginName()).isEqualTo("alice");
        assertThat(result.getPassword()).isEqualTo("$2a$10$encoded");
        verify(passwordEncoder).encode("pass");
    }

    @Test
    void shouldRejectDuplicateLoginNameWithinSameTenant() {
        AppUser user = user(null, "tenant-a", "001", true);
        user.setPassword("pass");
        when(appUserRepository.existsScopedLoginName("tenant-a", "001")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.save(user))
                .isInstanceOf(DuplicateLoginNameException.class)
                .hasMessageContaining("001");
    }

    @Test
    void shouldAllowSameLoginNameInDifferentTenant() {
        AppUser tenantB = user(null, "tenant-b", "001", true);
        tenantB.setPassword("pass");
        when(appUserRepository.existsScopedLoginName("tenant-b", "001")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");
        when(appUserRepository.save(tenantB)).thenReturn(tenantB);

        AppUser saved = appUserService.save(tenantB);

        assertThat(saved.getTenantId()).isEqualTo("tenant-b");
        assertThat(saved.getLoginName()).isEqualTo("001");
    }

    @Test
    void shouldUpdateUser() {
        AppUser existing = user("u1", "tenant-a", "alice", true);
        existing.setPassword("pass");
        AppUser updated = AppUser.builder()
                .loginName("alice")
                .active(false)
                .oidcProvider("github")
                .oidcSubject("12345")
                .build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.update("alice", updated);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getOidcProvider()).isEqualTo("github");
        assertThat(result.getOidcSubject()).isEqualTo("12345");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentUser() {
        when(appUserRepository.findUniqueByLoginName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update("nonexistent", new AppUser()))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUpdatingUserWithPartialOidcCredentials() {
        AppUser existing = user("u1", "tenant-a", "alice", true);
        AppUser updated = AppUser.builder().active(true).oidcProvider("github").build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appUserService.update("alice", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both oidcProvider and oidcSubject");
    }

    @Test
    void shouldThrowWhenUpdatingUserWithOidcCredentialsAlreadyAssigned() {
        AppUser existing = user("u1", "tenant-a", "alice", true);
        AppUser anotherUser = user("u2", "tenant-b", "bob", true);
        AppUser updated = AppUser.builder().active(true).oidcProvider("github").oidcSubject("12345").build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345"))
                .thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> appUserService.update("alice", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void shouldChangePassword() {
        AppUser existing = user("u1", "tenant-a", "alice", true);
        existing.setPassword("old");
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$encodedNewPass");
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.changePassword("alice", "newPass");

        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedNewPass");
    }

    @Test
    void shouldThrowWhenChangingPasswordToBlank() {
        assertThatThrownBy(() -> appUserService.changePassword("alice", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New password must not be blank");
    }

    @Test
    void shouldActivateAndDeactivateUser() {
        AppUser existing = user("u1", "tenant-a", "alice", false);
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        assertThat(appUserService.activate("alice").isActive()).isTrue();
        assertThat(appUserService.deactivate("alice").isActive()).isFalse();
    }

    @Test
    void shouldDeleteByImmutableUserId() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(user));

        appUserService.delete("alice");

        verify(appUserRepository).deleteById("u1");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        when(appUserRepository.findUniqueByLoginName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.delete("nonexistent"))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldCreateUserForStaff() {
        when(appUserRepository.existsScopedLoginName(null, "alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.createForStaff("S001", "alice", "pass", true);

        assertThat(result.getStaffId()).isEqualTo("S001");
        assertThat(result.getLoginName()).isEqualTo("alice");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldRejectDuplicateStaffLoginWithinTenant() {
        when(appUserRepository.existsScopedLoginName("tenant-a", "001")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.createForStaff("S001", "001", "pass", true, "tenant-a"))
                .isInstanceOf(DuplicateLoginNameException.class);
    }

    @Test
    void shouldUpdateAndReadRolesByTenantScopedStaffIdentity() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        user.setStaffId("S001");
        when(appUserRepository.findByTenantIdAndStaffId("tenant-a", "S001")).thenReturn(Optional.of(user));

        assertThat(appUserService.findRoleIdsByStaffId("S001", "tenant-a")).isEmpty();
        verify(appUserRepository).findByTenantIdAndStaffId("tenant-a", "S001");
    }

    @Test
    void shouldDeactivateUserByTenantScopedStaffId() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        user.setStaffId("S001");
        when(appUserRepository.findByTenantIdAndStaffId("tenant-a", "S001")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        appUserService.deactivateByStaffId("S001", "tenant-a");

        assertThat(user.isActive()).isFalse();
        verify(appUserRepository).save(user);
    }

    @Test
    void shouldLoginOnlyWhenLoginNameIsGloballyUnambiguousUntilTenantAwareAuthIsAdded() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        user.setPassword("$2a$10$encoded");
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "$2a$10$encoded")).thenReturn(true);

        AppUser result = appUserService.login("alice", "pass");

        assertThat(result).isEqualTo(user);
    }

    @Test
    void shouldFailClosedWhenLegacyLoginNameIsAmbiguous() {
        when(appUserRepository.findUniqueByLoginName("001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.login("001", "pass"))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        AppUser user = user("u1", "tenant-a", "alice", true);
        user.setPassword("$2a$10$encoded");
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        assertThatThrownBy(() -> appUserService.login("alice", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void shouldThrowWhenLoginWithInactiveUser() {
        AppUser user = user("u1", "tenant-a", "alice", false);
        user.setPassword("$2a$10$encoded");
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.login("alice", "pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    private static AppUser user(String userId, String tenantId, String loginName, boolean active) {
        return AppUser.builder()
                .userId(userId)
                .tenantId(tenantId)
                .loginName(loginName)
                .active(active)
                .roles(java.util.Set.of())
                .build();
    }
}
