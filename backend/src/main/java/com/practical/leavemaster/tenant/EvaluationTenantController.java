package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/internal/evaluation")
@RequiredArgsConstructor
public class EvaluationTenantController {

    private final DemoTenantSeedService seedService;

    @Value("${evaluation.tenant.enabled:false}")
    private boolean enabled;

    @Value("${evaluation.tenant.reset-token:}")
    private String resetToken;

    @PostMapping("/reset")
    public ResponseEntity<DemoTenantSeedService.DemoSeedResult> reset(
            @RequestHeader(value = "X-Evaluation-Reset-Token", required = false) String suppliedToken) {
        if (!enabled || resetToken == null || resetToken.isBlank() || suppliedToken == null ||
                !MessageDigest.isEqual(resetToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        return ResponseEntity.ok(seedService.resetEvaluationTenant());
    }
}
