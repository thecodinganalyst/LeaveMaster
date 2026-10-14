package com.practical.leavemaster.tenant;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Slf4j
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
        String requestId = java.util.UUID.randomUUID().toString();
        log.info("evaluation.reset received requestId={}", requestId);
        if (!enabled || resetToken == null || resetToken.isBlank() || suppliedToken == null ||
                !MessageDigest.isEqual(resetToken.getBytes(StandardCharsets.UTF_8),
                        suppliedToken.getBytes(StandardCharsets.UTF_8))) {
            log.info("evaluation.reset rejected requestId={}", requestId);
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        log.info("evaluation.reset token_validated requestId={}", requestId);
        log.info("evaluation.reset started requestId={}", requestId);
        try {
            var result = seedService.resetEvaluationTenant();
            log.info("evaluation.reset completed requestId={}", requestId);
            return ResponseEntity.ok(result);
        } catch (RuntimeException exception) {
            log.error("evaluation.reset failed requestId={} exceptionType={}", requestId,
                    exception.getClass().getName(), exception);
            throw exception;
        }
    }
}
