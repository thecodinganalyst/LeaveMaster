package com.practical.leavemaster.passwordreset;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetControllerTest {

    @Mock private PasswordResetService passwordResetService;
    @InjectMocks private PasswordResetController controller;

    @Test
    void requestAlwaysReturnsGenericAcceptedResponse() {
        ResponseEntity<Map<String, String>> response = controller.requestPin(Map.of(
                "tenantId", "Bravo", "loginName", "alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry(
                "message", "If the account is eligible for password reset, a verification PIN will be sent.");
    }

    @Test
    void verifyReturnsOkOrGenericBadRequest() {
        when(passwordResetService.verifyPin("Bravo", "alice", "123456")).thenReturn(true);
        when(passwordResetService.verifyPin("Bravo", "alice", "654321")).thenReturn(false);

        assertThat(controller.verifyPin(Map.of("tenantId", "Bravo", "loginName", "alice", "pin", "123456"))
                .getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(controller.verifyPin(Map.of("tenantId", "Bravo", "loginName", "alice", "pin", "654321"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setPasswordReturnsNoContentBadRequestAndValidationError() {
        when(passwordResetService.setPassword("Bravo", "alice", "new-password")).thenReturn(true);
        when(passwordResetService.setPassword("Bravo", "alice", "not-ready")).thenReturn(false);
        when(passwordResetService.setPassword("Bravo", "alice", "short"))
                .thenThrow(new IllegalArgumentException("New password must be at least 8 characters long"));

        assertThat(controller.setPassword(Map.of("tenantId", "Bravo", "loginName", "alice", "password", "new-password"))
                .getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(controller.setPassword(Map.of("tenantId", "Bravo", "loginName", "alice", "password", "not-ready"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(controller.setPassword(Map.of("tenantId", "Bravo", "loginName", "alice", "password", "short"))
                .getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }
}
