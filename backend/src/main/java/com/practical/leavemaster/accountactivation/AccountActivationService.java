package com.practical.leavemaster.accountactivation;

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
public class AccountActivationService {

    public enum NextStep {
        PASSWORD,
        ACTIVATION
    }

    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    private static final int PIN_BOUND = 1_000_000;
    private static final String TENANT_ADMIN_ROLE_SUFFIX = "_Admin";

    private final AppUserRepository appUserRepository;
    private final StaffRepository staffRepository;
    private final AccountActivationRepository accountActivationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AppUserService appUserService;
    private final EmailService emailService;

    @Value("${app.account-activation.pin-expiry-minutes:15}")
    private int pinExpiryMinutes;

    @Value("${app.account-activation.resend-cooldown-seconds:60}")
    private long resendCooldownSeconds;

    @Value("${app.account-activation.max-attempts:5}")
    private int maxAttempts;

    @Value("${app.account-activation.max-requests-per-hour:5}")
    private int maxRequestsPerHour;

    public NextStep lookup(String tenantId, String loginName) {
        return eligibleContext(normalizeTenantId(tenantId), normalizeLoginName(loginName)).isPresent()
                ? NextStep.ACTIVATION
                : NextStep.PASSWORD;
    }

    @Transactional
    public boolean requestPin(String tenantId, String loginName) {
        Optional<ActivationContext> context = eligibleContext(
                normalizeTenantId(tenantId), normalizeLoginName(loginName));
        if (context.isEmpty()) {
            log.info("Account activation PIN request ignored for an ineligible or unknown account");
            return false;
        }

        ActivationContext activationContext = context.get();
        String userId = activationContext.user().getUserId();
        LocalDateTime now = now();
        AccountActivation activation = accountActivationRepository.findById(userId)
                .orElseGet(() -> AccountActivation.builder()
                        .userId(userId)
                        .requestWindowStartedAt(now)
                        .requestCount(0)
                        .build());

        if (activation.getRequestedAt() != null
                && now.isBefore(activation.getRequestedAt().plusSeconds(resendCooldownSeconds))) {
            log.info("Account activation PIN request throttled by resend cooldown");
            return false;
        }

        if (activation.getRequestWindowStartedAt() == null
                || !now.isBefore(activation.getRequestWindowStartedAt().plusHours(1))) {
            activation.setRequestWindowStartedAt(now);
            activation.setRequestCount(0);
        }
        if (activation.getRequestCount() >= maxRequestsPerHour) {
            log.info("Account activation PIN request throttled by hourly limit");
            return false;
        }

        String pin = String.format(Locale.ROOT, "%06d", SECURE_RANDOM.nextInt(PIN_BOUND));
        activation.setPinHash(passwordEncoder.encode(pin));
        activation.setRequestedAt(now);
        activation.setExpiresAt(now.plusMinutes(pinExpiryMinutes));
        activation.setFailedAttempts(0);
        activation.setVerifiedAt(null);
        activation.setConsumedAt(null);
        activation.setRequestCount(activation.getRequestCount() + 1);
        accountActivationRepository.save(activation);

        try {
            emailService.sendAccountActivationPin(
                    activationContext.email(),
                    activationContext.displayName(),
                    pin,
                    pinExpiryMinutes);
            log.info("Account activation PIN delivery requested successfully");
            return true;
        } catch (RuntimeException ex) {
            accountActivationRepository.deleteById(userId);
            log.warn("Account activation PIN delivery failed; activation record invalidated");
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

        Optional<ActivationContext> context = eligibleContext(normalizedTenantId, normalizedLoginName);
        if (context.isEmpty()) {
            return false;
        }

        Optional<AccountActivation> activationOptional = accountActivationRepository.findById(context.get().user().getUserId());
        if (activationOptional.isEmpty()) {
            return false;
        }

        AccountActivation activation = activationOptional.get();
        LocalDateTime now = now();
        if (activation.getConsumedAt() != null
                || activation.getVerifiedAt() != null
                || activation.getExpiresAt() == null
                || !now.isBefore(activation.getExpiresAt())
                || activation.getPinHash() == null
                || activation.getFailedAttempts() >= maxAttempts) {
            return false;
        }

        if (!passwordEncoder.matches(pin, activation.getPinHash())) {
            activation.setFailedAttempts(activation.getFailedAttempts() + 1);
            accountActivationRepository.save(activation);
            log.info("Account activation PIN verification failed");
            return false;
        }

        activation.setVerifiedAt(now);
        accountActivationRepository.save(activation);
        log.info("Account activation PIN verified successfully");
        return true;
    }

    @Transactional
    public boolean setInitialPassword(String tenantId, String loginName, String newPassword) {
        Optional<ActivationContext> context = eligibleContext(
                normalizeTenantId(tenantId), normalizeLoginName(loginName));
        if (context.isEmpty()) {
            return false;
        }

        String userId = context.get().user().getUserId();
        Optional<AccountActivation> activationOptional = accountActivationRepository.findById(userId);
        if (activationOptional.isEmpty()) {
            return false;
        }

        AccountActivation activation = activationOptional.get();
        LocalDateTime now = now();
        if (activation.getVerifiedAt() == null
                || activation.getConsumedAt() != null
                || activation.getExpiresAt() == null
                || !now.isBefore(activation.getExpiresAt())) {
            return false;
        }

        appUserService.completeInitialPasswordByUserId(userId, newPassword);
        activation.setConsumedAt(now);
        activation.setPinHash(null);
        accountActivationRepository.save(activation);
        log.info("Account activation completed successfully");
        return true;
    }

    private Optional<ActivationContext> eligibleContext(String tenantId, String loginName) {
        if (tenantId == null || loginName == null) {
            logEligibilityFailure("tenant or login missing");
            return Optional.empty();
        }
        if (AuthenticationRealm.isPlatformRealm(tenantId)) {
            logEligibilityFailure("platform realm is not eligible");
            return Optional.empty();
        }

        Optional<AppUser> userOptional = appUserRepository.findByTenantIdAndLoginName(tenantId, loginName);
        if (userOptional.isEmpty()) {
            logEligibilityFailure("account not found in requested tenant");
            return Optional.empty();
        }

        AppUser user = userOptional.get();
        if (!user.isActive()) {
            logEligibilityFailure("account is inactive");
            return Optional.empty();
        }
        if (user.getPassword() != null) {
            logEligibilityFailure("permanent password is already set");
            return Optional.empty();
        }

        if (user.getStaffId() != null && !user.getStaffId().isBlank()) {
            return eligibleStaffContext(tenantId, user);
        }
        if (!isTenantAdmin(tenantId, user)) {
            logEligibilityFailure("tenant admin role is not present");
            return Optional.empty();
        }
        if (user.getEmail() == null || user.getEmail().isBlank()) {
            logEligibilityFailure("tenant admin email is missing");
            return Optional.empty();
        }
        return Optional.of(new ActivationContext(user, user.getEmail().trim(), "Tenant Administrator"));
    }

    private Optional<ActivationContext> eligibleStaffContext(String tenantId, AppUser user) {
        Optional<Staff> staffOptional = staffRepository.findByIdAndTenantId(user.getStaffId(), tenantId);
        if (staffOptional.isEmpty()) {
            logEligibilityFailure("staff record not found in requested tenant");
            return Optional.empty();
        }
        Staff staff = staffOptional.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (staff.getEmail() == null || staff.getEmail().isBlank()) {
            logEligibilityFailure("staff email is missing");
            return Optional.empty();
        }
        if (staff.getJoinDate() == null || staff.getJoinDate().isAfter(today)) {
            logEligibilityFailure("staff is not yet eligible by join date");
            return Optional.empty();
        }
        if (staff.getTermDate() != null && !staff.getTermDate().isAfter(today)) {
            logEligibilityFailure("staff employment has ended");
            return Optional.empty();
        }
        return Optional.of(new ActivationContext(user, staff.getEmail(), staff.getName()));
    }

    private boolean isTenantAdmin(String tenantId, AppUser user) {
        String tenantAdminRoleId = tenantId + TENANT_ADMIN_ROLE_SUFFIX;
        return user.getRoles() != null && user.getRoles().stream()
                .anyMatch(role -> tenantAdminRoleId.equals(role.getId()));
    }

    private void logEligibilityFailure(String reason) {
        log.info("Account activation eligibility rejected: {}", reason);
    }

    private String normalizeTenantId(String tenantId) {
        if (tenantId == null || tenantId.isBlank()) {
            return null;
        }
        return tenantId.trim();
    }

    private String normalizeLoginName(String loginName) {
        if (loginName == null || loginName.isBlank()) {
            return null;
        }
        return loginName.trim();
    }

    private LocalDateTime now() {
        return LocalDateTime.now(ZoneOffset.UTC);
    }

    private record ActivationContext(AppUser user, String email, String displayName) {
    }
}
