package com.practical.leavemaster.user;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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

    @InjectMocks
    private AppUserService appUserService;

    @Test
    void shouldReturnAllUsers() {
        List<AppUser> users = List.of(
                AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build(),
                AppUser.builder().id("U002").loginName("bob").password("pass").active(false).build()
        );
        when(appUserRepository.findAll()).thenReturn(users);

        List<AppUser> result = appUserService.findAll();

        assertThat(result).hasSize(2);
    }

    @Test
    void shouldFindUserById() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserService.findById("U001");

        assertThat(result).isPresent();
        assertThat(result.get().getLoginName()).isEqualTo("alice");
    }

    @Test
    void shouldSaveUser() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.existsByLoginName("alice")).thenReturn(false);
        when(appUserRepository.save(user)).thenReturn(user);

        AppUser result = appUserService.save(user);

        assertThat(result.getId()).isEqualTo("U001");
    }

    @Test
    void shouldGenerateIdWhenSavingUserWithoutId() {
        AppUser user = AppUser.builder().loginName("alice").password("pass").active(true).build();
        when(appUserRepository.existsByLoginName("alice")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.save(user);

        assertThat(result.getId()).isNotBlank();
    }

    @Test
    void shouldThrowWhenSavingWithDuplicateLoginName() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.existsByLoginName("alice")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.save(user))
                .isInstanceOf(DuplicateLoginNameException.class)
                .hasMessageContaining("alice");
    }

    @Test
    void shouldUpdateUser() {
        AppUser existing = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        AppUser updated = AppUser.builder().id("U001").loginName("alice-updated").password("pass").active(false).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(existing));
        when(appUserRepository.existsByLoginName("alice-updated")).thenReturn(false);
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.update("U001", updated);

        assertThat(result.getLoginName()).isEqualTo("alice-updated");
        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldThrowWhenUpdatingNonExistentUser() {
        when(appUserRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.update("nonexistent", new AppUser()))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldThrowWhenUpdatingWithDuplicateLoginName() {
        AppUser existing = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        AppUser updated = AppUser.builder().id("U001").loginName("bob").password("pass").active(true).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(existing));
        when(appUserRepository.existsByLoginName("bob")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.update("U001", updated))
                .isInstanceOf(DuplicateLoginNameException.class)
                .hasMessageContaining("bob");
    }

    @Test
    void shouldChangePassword() {
        AppUser existing = AppUser.builder().id("U001").loginName("alice").password("old").active(true).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.changePassword("U001", "newPass");

        assertThat(result.getPassword()).isEqualTo("newPass");
    }

    @Test
    void shouldThrowWhenChangingPasswordToBlank() {
        assertThatThrownBy(() -> appUserService.changePassword("U001", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("New password must not be blank");
    }

    @Test
    void shouldActivateUser() {
        AppUser existing = AppUser.builder().id("U001").loginName("alice").password("pass").active(false).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.activate("U001");

        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldDeactivateUser() {
        AppUser existing = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(existing));
        when(appUserRepository.save(existing)).thenReturn(existing);

        AppUser result = appUserService.deactivate("U001");

        assertThat(result.isActive()).isFalse();
    }

    @Test
    void shouldDeleteUser() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findById("U001")).thenReturn(Optional.of(user));

        appUserService.delete("U001");

        verify(appUserRepository).deleteById("U001");
    }

    @Test
    void shouldThrowWhenDeletingNonExistentUser() {
        when(appUserRepository.findById("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.delete("nonexistent"))
                .isInstanceOf(AppUserNotFoundException.class);
    }

    @Test
    void shouldCreateUserForStaff() {
        when(appUserRepository.existsByLoginName("alice")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(i -> i.getArgument(0));

        AppUser result = appUserService.createForStaff("S001", "alice", "pass", true);

        assertThat(result.getStaffId()).isEqualTo("S001");
        assertThat(result.getLoginName()).isEqualTo("alice");
        assertThat(result.isActive()).isTrue();
    }

    @Test
    void shouldThrowWhenCreatingForStaffWithDuplicateLoginName() {
        when(appUserRepository.existsByLoginName("alice")).thenReturn(true);

        assertThatThrownBy(() -> appUserService.createForStaff("S001", "alice", "pass", true))
                .isInstanceOf(DuplicateLoginNameException.class);
    }

    @Test
    void shouldDeactivateUserByStaffId() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).staffId("S001").build();
        when(appUserRepository.findByStaffId("S001")).thenReturn(Optional.of(user));
        when(appUserRepository.save(user)).thenReturn(user);

        appUserService.deactivateByStaffId("S001");

        assertThat(user.isActive()).isFalse();
        verify(appUserRepository).save(user);
    }

    @Test
    void shouldLoginSuccessfully() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findByLoginName("alice")).thenReturn(Optional.of(user));

        AppUser result = appUserService.login("alice", "pass");

        assertThat(result.getId()).isEqualTo("U001");
    }

    @Test
    void shouldThrowWhenLoginWithWrongPassword() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(true).build();
        when(appUserRepository.findByLoginName("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.login("alice", "wrong"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void shouldThrowWhenLoginWithInactiveUser() {
        AppUser user = AppUser.builder().id("U001").loginName("alice").password("pass").active(false).build();
        when(appUserRepository.findByLoginName("alice")).thenReturn(Optional.of(user));

        assertThatThrownBy(() -> appUserService.login("alice", "pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void shouldThrowWhenLoginWithNonExistentUser() {
        when(appUserRepository.findByLoginName("nonexistent")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> appUserService.login("nonexistent", "pass"))
                .isInstanceOf(AppUserNotFoundException.class);
    }
}
