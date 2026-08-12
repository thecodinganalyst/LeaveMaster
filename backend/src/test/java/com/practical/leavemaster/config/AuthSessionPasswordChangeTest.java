package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class AuthSessionPasswordChangeTest {

    private AppUserService appUserService;
    private AuthSessionController controller;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        appUserService = mock(AppUserService.class);
        controller = new AuthSessionController(mock(AppUserRepository.class), appUserService);
        authentication = new UsernamePasswordAuthenticationToken("alice", "n/a", List.of());
    }

    @Test
    void shouldChangePasswordForAuthenticatedPrincipal() {
        var request = new AuthSessionController.ChangePasswordRequest(
            "old-password",
            "new-password",
            "new-password"
        );

        var response = controller.changePassword(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(appUserService).changeOwnPassword("alice", "old-password", "new-password");
    }

    @Test
    void shouldRejectMismatchedConfirmation() {
        var request = new AuthSessionController.ChangePasswordRequest(
            "old-password",
            "new-password",
            "different-password"
        );

        var response = controller.changePassword(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verifyNoInteractions(appUserService);
    }

    @Test
    void shouldReturnBadRequestWhenCurrentPasswordIsIncorrect() {
        doThrow(new IllegalArgumentException("Current password is incorrect"))
            .when(appUserService)
            .changeOwnPassword("alice", "wrong-password", "new-password");
        var request = new AuthSessionController.ChangePasswordRequest(
            "wrong-password",
            "new-password",
            "new-password"
        );

        var response = controller.changePassword(authentication, request);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
    }
}
