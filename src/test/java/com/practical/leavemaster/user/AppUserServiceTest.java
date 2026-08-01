package com.practical.leavemaster.user;

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

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void shouldReturnAllUsers() {
        List<AppUser> users = List.of(
                AppUser.builder().loginName("alice").password("pass").active(true).build(),
                AppUser.builder().loginName("bob").password("pass").active(false).build()
        );
        when(appUserRepository.findAll()).thenReturn(users);

        List<AppUser> result = appUserService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFindUserByLoginName() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserService.findByLoginName("alice");

        assertThat(result).isPresent();
        assertThat(result.get().getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldSaveUser() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$encoded");
        when(appUserRepository.save(user)).thenReturn(user);

        AppUser result = appUserService.save(user);

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(passwordEncoder).encode("pass");
    }

    @Test
    void shouldThrowWhenSavingWithDuplicateLoginName() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.existsById("alice")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.save(user))
                .isInstanceOf(DuplicateLoginNameException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void shouldUpdateUser() {
        AppUser existing = AppUser.builder().loginName("alice").password("pass").active(true).build();
        AppUser updated = AppUser.builder()
                .loginName("alice")
                .password("pass")
                .active(false)
                .oidcProvider("github")
                .oidcSubject("12345")
                .build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.update("alice", updated);

        assertThat(result.isActive()).isFalse();
        assertThat(result.getOidcProvider()).isEqualTo("github");
        assertThat(result.getOidcSubject()).isEqualTo("12345");
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentUser() {
        when(appUserRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update("nonexistent", new AppUser()))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUpdatingUserWithPartialOidcCredentials() {
        AppUser existing = AppUser.builder().loginName("alice").password("pass").active(true).build();
        AppUser updated = AppUser.builder().active(true).oidcProvider("github").build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> appUserService.update("alice", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Both oidcProvider and oidcSubject");
    }

    @Test
    void shouldThrowWhenUpdatingUserWithOidcCredentialsAlreadyAssigned() {
        AppUser existing = AppUser.builder().loginName("alice").password("pass").active(true).build();
        AppUser anotherUser = AppUser.builder().loginName("bob").password("pass").active(true).build();
        AppUser updated = AppUser.builder().active(true).oidcProvider("github").oidcSubject("12345").build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345"))
                .thenReturn(Optional.of(anotherUser));

        assertThatThrownBy(() -> appUserService.update("alice", updated))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("already assigned");
    }

    @Test
    void shouldChangePassword() {
        AppUser existing = AppUser.builder().loginName("alice").password("old").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode("newPass")).thenReturn("$2a$10$encodedNewPass");
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.changePassword("alice", "newPass");

        assertThat(result.getPassword()).isEqualTo("$2a$10$encodedNewPass");
        verify(passwordEncoder).encode("newPass");
    }

    @Test
    void shouldThrowWhenChangingPasswordToBlank() {
        assertThatThrownBy(() -> appUserService.changePassword("alice", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New password must not be blank");
    }

    @Test
    void shouldActivateUser() {
        AppUser existing = AppUser.builder().loginName("alice").password("pass").active(false).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.activate("alice");

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldDeactivateUser() {
        AppUser existing = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.deactivate("alice");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldDeleteUser() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));

        appUserService.delete("alice");

        verify(appUserRepository).deleteById("alice");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        when(appUserRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.delete("nonexistent"))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldCreateUserForStaff() {
        when(appUserRepository.existsById("alice")).thenReturn(false);
        when(passwordEncoder.encode("pass")).thenReturn("$2a$10$encoded");
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.createForStaff("S001", "alice", "pass", true);

        assertThat(result.getStaffId()).isEqualTo("S001");
        assertThat(result.getLoginName()).isEqualTo("alice");
        assertThat(result.isActive()).isTrue();
        verify(passwordEncoder).encode("pass");
    }

    @Test
    void shouldThrowWhenCreatingForStaffWithDuplicateLoginName() {
        when(appUserRepository.existsById("alice")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.createForStaff("S001", "alice", "pass", true))
                .isInstanceOf(DuplicateLoginNameException.class);
    }

    @Test
    void shouldDeactivateUserByStaffId() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).staffId("S001").build();
        when(appUserRepository.findByStaffId("S001")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        appUserService.deactivateByStaffId("S001");

        assertThat(user.isActive()).isFalse();
        verify(appUserRepository).save(user);
    }

    @Test
    void shouldLoginSuccessfully() {
        AppUser user = AppUser.builder().loginName("alice").password("$2a$10$encoded").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("pass", "$2a$10$encoded")).thenReturn(true);

        AppUser result = appUserService.login("alice", "pass");

        assertThat(result.getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        AppUser user = AppUser.builder().loginName("alice").password("$2a$10$encoded").active(true).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong", "$2a$10$encoded")).thenReturn(false);

        assertThatThrownBy(() -> appUserService.login("alice", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void shouldThrowWhenLoginWithInactiveUser() {
        AppUser user = AppUser.builder().loginName("alice").password("$2a$10$encoded").active(false).build();
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.login("alice", "pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldThrowWhenLoginWithNonExistentUser() {
        when(appUserRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.login("nonexistent", "pass"))
                .isInstanceOf(AppUserNotFoundException.class);
    }
}
