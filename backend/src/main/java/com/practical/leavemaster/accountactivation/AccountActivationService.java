package com.practical.leavemaster.accountactivation;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
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
import java.util.Objects;
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

    public NextStep lookup(String loginName) {
        return eligibleContext(normalizeLoginName(loginName)).isPresent()
                ? NextStep.ACTIVATION
                : NextStep.PASSWORD;
    }

    @Transactional
    public boolean requestPin(String loginName) {
        String normalizedLoginName = normalizeLoginName(loginName);
        Optional<ActivationContext> context = eligibleContext(normalizedLoginName);
        if (context.isEmpty()) {
            log.info("Account activation PIN request ignored for an ineligible or unknown account");
            return false;
        }

        LocalDateTime now = now();
        AccountActivation activation = accountActivationRepository.findById(normalizedLoginName)
                .orElseGet(() -> AccountActivation.builder()
                        .loginName(normalizedLoginName)
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

        ActivationContext activationContext = context.get();
        try {
            emailService.sendAccountActivationPin(
                    activationContext.staff().getEmail(),
                    activationContext.staff().getName(),
                    pin,
                    pinExpiryMinutes);
            log.info("Account activation PIN delivery requested successfully");
            return true;
        } catch (RuntimeException ex) {
            accountActivationRepository.deleteById(normalizedLoginName);
            log.warn("Account activation PIN delivery failed; activation record invalidated");
            return false;
        }
    }

    @Transactional
    public boolean verifyPin(String loginName, String pin) {
        String normalizedLoginName = normalizeLoginName(loginName);
        if (normalizedLoginName == null || pin == null || !pin.matches("\\d{6}")) {
            return false;
        }

        Optional<ActivationContext> context = eligibleContext(normalizedLoginName);
        if (context.isEmpty()) {
            return false;
        }

        Optional<AccountActivation> activationOptional = accountActivationRepository.findById(normalizedLoginName);
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
    public boolean setInitialPassword(String loginName, String newPassword) {
        String normalizedLoginName = normalizeLoginName(loginName);
        if (normalizedLoginName == null || eligibleContext(normalizedLoginName).isEmpty()) {
            return false;
        }

        Optional<AccountActivation> activationOptional = accountActivationRepository.findById(normalizedLoginName);
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

        appUserService.completeInitialPassword(normalizedLoginName, newPassword);
        activation.setConsumedAt(now);
        activation.setPinHash(null);
        accountActivationRepository.save(activation);
        log.info("Account activation completed successfully");
        return true;
    }

    private Optional<ActivationContext> eligibleContext(String loginName) {
        if (loginName == null) {
            return Optional.empty();
        }
        Optional<AppUser> userOptional = appUserRepository.findById(loginName);
        if (userOptional.isEmpty()) {
            return Optional.empty();
        }
        AppUser user = userOptional.get();
        if (!user.isActive() || user.getPassword() != null || user.getStaffId() == null || user.getStaffId().isBlank()) {
            return Optional.empty();
        }

        Optional<Staff> staffOptional = staffRepository.findById(user.getStaffId());
        if (staffOptional.isEmpty()) {
            return Optional.empty();
        }
        Staff staff = staffOptional.get();
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        if (staff.getEmail() == null || staff.getEmail().isBlank()
                || staff.getJoinDate() == null || staff.getJoinDate().isAfter(today)
                || (staff.getTermDate() != null && !staff.getTermDate().isAfter(today))
                || !Objects.equals(user.getTenantId(), staff.getTenantId())) {
            return Optional.empty();
        }
        return Optional.of(new ActivationContext(user, staff));
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

    private record ActivationContext(AppUser user, Staff staff) {
    }
}
