package com.practical.leavemaster.mcp;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserService;
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
class AppUserMcpToolsTest {

    @Mock
    private AppUserService appUserService;

    @InjectMocks
    private AppUserMcpTools appUserMcpTools;

    @Test
    void shouldGetAllUsers() {
        List<AppUser> users = List.of(AppUser.builder().loginName("alice").build());
        when(appUserService.findAll()).thenReturn(users);

        List<AppUser> result = appUserMcpTools.getAllUsers();

        assertThat(result).hasSize(1);
        verify(appUserService).findAll();
    }

    @Test
    void shouldGetUserByLoginName() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appUserService.findByLoginName("alice")).thenReturn(Optional.of(user));

        Optional<AppUser> result = appUserMcpTools.getUserByLoginName("alice");

        assertThat(result).isPresent();
        verify(appUserService).findByLoginName("alice");
    }

    @Test
    void shouldCreateUser() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appUserService.save(user)).thenReturn(user);

        AppUser result = appUserMcpTools.createUser(user);

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(appUserService).save(user);
    }

    @Test
    void shouldUpdateUser() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appUserService.update("alice", user)).thenReturn(user);

        AppUser result = appUserMcpTools.updateUser("alice", user);

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(appUserService).update("alice", user);
    }

    @Test
    void shouldChangePassword() {
        AppUser user = AppUser.builder().loginName("alice").build();
        when(appUserService.changePassword("alice", "newpass")).thenReturn(user);

        AppUser result = appUserMcpTools.changePassword("alice", "newpass");

        assertThat(result.getLoginName()).isEqualTo("alice");
        verify(appUserService).changePassword("alice", "newpass");
    }

    @Test
    void shouldActivateUser() {
        AppUser user = AppUser.builder().loginName("alice").active(true).build();
        when(appUserService.activate("alice")).thenReturn(user);

        AppUser result = appUserMcpTools.activateUser("alice");

        assertThat(result.isActive()).isTrue();
        verify(appUserService).activate("alice");
    }

    @Test
    void shouldDeactivateUser() {
        AppUser user = AppUser.builder().loginName("alice").active(false).build();
        when(appUserService.deactivate("alice")).thenReturn(user);

        AppUser result = appUserMcpTools.deactivateUser("alice");

        assertThat(result.isActive()).isFalse();
        verify(appUserService).deactivate("alice");
    }

    @Test
    void shouldDeleteUser() {
        doNothing().when(appUserService).delete("alice");

        appUserMcpTools.deleteUser("alice");

        verify(appUserService).delete("alice");
    }
}
