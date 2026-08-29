package com.practical.leavemaster.accountactivation;

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
class AccountActivationControllerTest {

    @Mock private AccountActivationService accountActivationService;
    @InjectMocks private AccountActivationController controller;

    @Test
    void lookupShouldPassTenantAndReturnActivationStep() {
        when(accountActivationService.lookup("tenant-a", "alice"))
                .thenReturn(AccountActivationService.NextStep.ACTIVATION);

        ResponseEntity<Map<String, String>> response = controller.lookup(
                Map.of("tenantId", "tenant-a", "loginName", "alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).containsEntry("nextStep", "ACTIVATION");
    }

    @Test
    void pinRequestShouldAlwaysReturnSameAcceptedResponse() {
        when(accountActivationService.requestPin("tenant-a", "alice")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.requestPin(
                Map.of("tenantId", "tenant-a", "loginName", "alice"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getBody()).containsEntry(
                "message", "If the account is eligible for activation, a verification PIN will be sent.");
    }

    @Test
    void verifyShouldReturnOkForValidPin() {
        when(accountActivationService.verifyPin("tenant-a", "alice", "123456")).thenReturn(true);

        ResponseEntity<Map<String, String>> response = controller.verifyPin(
                Map.of("tenantId", "tenant-a", "loginName", "alice", "pin", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    @Test
    void verifyShouldReturnBadRequestForInvalidPin() {
        when(accountActivationService.verifyPin("tenant-a", "alice", "123456")).thenReturn(false);

        ResponseEntity<Map<String, String>> response = controller.verifyPin(
                Map.of("tenantId", "tenant-a", "loginName", "alice", "pin", "123456"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).containsEntry("error", "Invalid or expired verification PIN");
    }

    @Test
    void setPasswordShouldReturnNoContentWhenCompleted() {
        when(accountActivationService.setInitialPassword("tenant-a", "alice", "strong-pass")).thenReturn(true);

        ResponseEntity<?> response = controller.setInitialPassword(
                Map.of("tenantId", "tenant-a", "loginName", "alice", "password", "strong-pass"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void setPasswordShouldReturnBadRequestWhenActivationNotReady() {
        when(accountActivationService.setInitialPassword("tenant-a", "alice", "strong-pass")).thenReturn(false);

        ResponseEntity<?> response = controller.setInitialPassword(
                Map.of("tenantId", "tenant-a", "loginName", "alice", "password", "strong-pass"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void setPasswordShouldExposeOnlyPasswordPolicyValidation() {
        when(accountActivationService.setInitialPassword("tenant-a", "alice", "short"))
                .thenThrow(new IllegalArgumentException("New password must be at least 8 characters long"));

        ResponseEntity<?> response = controller.setInitialPassword(
                Map.of("tenantId", "tenant-a", "loginName", "alice", "password", "short"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(Map.of("error", "New password must be at least 8 characters long"));
    }
}
