package com.practical.leavemaster.passwordreset;

import com.practical.leavemaster.email.EmailDeliveryException;
import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import com.practical.leavemaster.user.AuthenticationRealm;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PIN_BOUND = 1_000_000;

    private final AppUserRepository appUserRepository;
    private final StaffRepository staffRepository;
    private final PasswordResetRepository passwordResetRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;
    private final EmailService emailService;

    @Value("${app.password-reset.pin-expiry-minutes:${app.account-activation.pin-expiry-minutes:15}}")
    private int pinExpiryMinutes;

    @Value("${app.password-reset.resend-cooldown-seconds:${app.account-activation.resend-cooldown-seconds:60}}")
    private long resendCooldownSeconds;

    @Value("${app.password-reset.max-attempts:${app.account-activation.max-attempts:5}}")
    private int maxAttempts;

    @Value("${app.password-reset.max-requests-per-hour:${app.account-activation.max-requests-per-hour:5}}")
    private int maxRequestsPerHour;

    @Transactional
    public boolean requestPin(String tenantId, String loginName) {
        Optional<ResetContext> context = eligibleContext(normalizeTenantId(tenantId), normalizeLoginName(loginName));
        if (context.isEmpty()) {
            log.info("Password reset PIN request ignored for an ineligible or unknown account");
            return false;
        }

        ResetContext resetContext = context.get();
        String userId = resetContext.user().getUserId();
        LocalDateTime now = now();
        PasswordReset reset = passwordResetRepository.findById(userId)
                .orElseGet(() -> PasswordReset.builder()
                        .userId(userId)
                        .requestWindowStartedAt(now)
                        .requestCount(0)
                        .build());

        if (reset.getRequestedAt() != null
                && now.isBefore(reset.getRequestedAt().plusSeconds(resendCooldownSeconds))) {
            log.info("Password reset PIN request throttled by resend cooldown");
            return false;
        }

        if (reset.getRequestWindowStartedAt() == null
                || !now.isBefore(reset.getRequestWindowStartedAt().plusHours(1))) {
            reset.setRequestWindowStartedAt(now);
            reset.setRequestCount(0);
        }
        if (reset.getRequestCount() >= maxRequestsPerHour) {
            log.info("Password reset PIN request throttled by hourly limit");
            return false;
        }

        String pin = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(PIN_BOUND));
        reset.setPinHash(passwordEncoder.encode(pin));
        reset.setRequestedAt(now);
        reset.setExpiresAt(now.plusMinutes(pinExpiryMinutes));
        reset.setFailedAttempts(0);
        reset.setVerifiedAt(null);
        reset.setConsumedAt(null);
        reset.setRequestCount(reset.getRequestCount() + 1);
        passwordResetRepository.save(reset);

        try {
            emailService.sendPasswordResetPin(resetContext.email(), resetContext.displayName(), pin, pinExpiryMinutes);
            log.info("Password reset PIN delivery requested successfully");
            return true;
        } catch (EmailDeliveryException ex) {
            invalidateGeneratedPin(reset);
            log.warn("Password reset PIN delivery failed; PIN invalidated; category={} reason={}",
                    ex.getClass().getSimpleName(), ex.getMessage());
            return false;
        } catch (RuntimeException ex) {
            invalidateGeneratedPin(reset);
            log.warn("Password reset PIN delivery failed; PIN invalidated; category=unexpected type={}",
                    ex.getClass().getSimpleName());
            return false;
        }
    }

    @Transactional
    public boolean verifyPin(String tenantId, String loginName, String pin) {
        String normalizedTenantId = normalizeTenantId(tenantId);
        String normalizedLoginName = normalizeLoginName(loginName);
        if (normalizedTenantId == null || normalizedLoginName == null || pin == null || !pin.matches("\\d{6}")) {
            return false;
        }

        Optional<ResetContext> context = eligibleContext(normalizedTenantId, normalizedLoginName);
        if (context.isEmpty()) {
            return false;
        }

        Optional<PasswordReset> resetOptional = passwordResetRepository.findById(context.get().user().getUserId());
        if (resetOptional.isEmpty()) {
            return false;
        }

        PasswordReset reset = resetOptional.get();
        LocalDateTime now = now();
        if (reset.getConsumedAt() != null
                || reset.getVerifiedAt() != null
                || reset.getExpiresAt() == null
                || !now.isBefore(reset.getExpiresAt())
                || reset.getPinHash() == null
                || reset.getFailedAttempts() >= maxAttempts) {
            return false;
        }

        if (!passwordEncoder.matches(pin, reset.getPinHash())) {
            reset.setFailedAttempts(reset.getFailedAttempts() + 1);
            passwordResetRepository.save(reset);
            log.info("Password reset PIN verification failed");
            return false;
        }

        reset.setVerifiedAt(now);
        passwordResetRepository.save(reset);
        log.info("Password reset PIN verified successfully");
        return true;
    }

    @Transactional
    public boolean setPassword(String tenantId, String loginName, String newPassword) {
        Optional<ResetContext> context = eligibleContext(normalizeTenantId(tenantId), normalizeLoginName(loginName));
        if (context.isEmpty()) {
            return false;
        }

        PasswordReset reset = passwordResetRepository.findById(context.get().user().getUserId()).orElse(null);
        LocalDateTime now = now();
        if (reset == null
                || reset.getVerifiedAt() == null
                || reset.getConsumedAt() != null
                || reset.getExpiresAt() == null
                || !now.isBefore(reset.getExpiresAt())) {
            return false;
        }

        appUserService.resetPasswordByUserId(context.get().user().getUserId(), newPassword);
        reset.setConsumedAt(now);
        reset.setPinHash(null);
        passwordResetRepository.save(reset);
        log.info("Password reset completed successfully");
        return true;
    }

    private Optional<ResetContext> eligibleContext(String tenantId, String loginName) {
        if (tenantId == null || loginName == null) {
            return Optional.empty();
        }

        Optional<AppUser> userOptional = AuthenticationRealm.isPlatformRealm(tenantId)
                ? appUserRepository.findByTenantIdIsNullAndLoginName(loginName)
                : appUserRepository.findByTenantIdAndLoginName(tenantId, loginName);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }

        AppUser user = userOptional.get();
        if (!user.isActive() || user.getPassword() == null || user.getPassword().isBlank()) {
            return Optional.empty();
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            return Optional.empty();
        }

        if (!AuthenticationRealm.isPlatformRealm(tenantId)
                && user.getStaffId() != null && !user.getStaffId().isBlank()) {
            Optional<Staff> staffOptional = staffRepository.findByIdAndTenantId(user.getStaffId(), tenantId);
            if (staffOptional.isEmpty() || !eligibleEmployment(staffOptional.get())) {
                return Optional.empty();
            }
            String displayName = staffOptional.get().getName() == null || staffOptional.get().getName().isBlank()
                    ? user.getLoginName() : staffOptional.get().getName();
            return Optional.of(new ResetContext(user, user.getEmail().trim(), displayName));
        }

        return Optional.of(new ResetContext(user, user.getEmail().trim(), user.getLoginName()));
    }

    private boolean eligibleEmployment(Staff staff) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        return staff.getJoinDate() != null
                && !staff.getJoinDate().isAfter(today)
                && (staff.getTermDate() == null || staff.getTermDate().isAfter(today));
    }

    private void invalidateGeneratedPin(PasswordReset reset) {
        reset.setPinHash(null);
        reset.setExpiresAt(now());
        reset.setVerifiedAt(null);
        passwordResetRepository.save(reset);
    }

    private String normalizeTenantId(String tenantId) {
        return tenantId == null || tenantId.isBlank() ? null : tenantId.trim();
    }

    private String normalizeLoginName(String loginName) {
        return loginName == null || loginName.isBlank() ? null : loginName.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private record ResetContext(AppUser user, String email, String displayName) {
    }
}
