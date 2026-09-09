package com.practical.leavemaster.passwordreset;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping({"/password-reset", "/api/password-reset"})
@RequiredArgsConstructor
public class PasswordResetController {

    private static final String GENERIC_REQUEST_MESSAGE =
            "If the account is eligible for password reset, a verification PIN will be sent.";

    private final PasswordResetService passwordResetService;

    @PostMapping("/request")
    public ResponseEntity<Map<String, String>> requestPin(@RequestBody Map<String, String> body) {
        passwordResetService.requestPin(body.get("tenantId"), body.get("loginName"));
        return ResponseEntity.status(HttpStatus.ACCEPTED)
                .body(Map.of("message", GENERIC_REQUEST_MESSAGE));
    }

    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verifyPin(@RequestBody Map<String, String> body) {
        boolean verified = passwordResetService.verifyPin(
                body.get("tenantId"), body.get("loginName"), body.get("pin"));
        if (!verified) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid or expired verification PIN"));
        }
        return ResponseEntity.ok(Map.of("message", "Verification PIN accepted"));
    }

    @PostMapping("/set-password")
    public ResponseEntity<?> setPassword(@RequestBody Map<String, String> body) {
        try {
            boolean completed = passwordResetService.setPassword(
                    body.get("tenantId"), body.get("loginName"), body.get("password"));
            if (!completed) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Password reset is not ready for password setup"));
            }
            return ResponseEntity.noContent().build();
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Password reset is not ready for password setup"));
        }
    }
}
