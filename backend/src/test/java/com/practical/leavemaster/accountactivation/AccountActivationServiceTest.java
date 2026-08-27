package com.practical.leavemaster.accountactivation;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private AccountActivationRepository accountActivationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AppUserService appUserService;
    @Mock private EmailService emailService;

    @InjectMocks private AccountActivationService service;

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "pinExpiryMinutes", 15);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "maxRequestsPerHour", 5);
    }

    @Test
    void shouldReturnActivationForEligiblePendingAccount() {
        stubEligibleAccount();

        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.ACTIVATION);
    }

    @Test
    void shouldReturnPasswordForUnknownOrAlreadyActivatedAccount() {
        when(appUserRepository.findById("missing")).thenReturn(Optional.empty());
        assertThat(service.lookup("missing")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        AppUser active = pendingUser();
        active.setPassword("encoded-password");
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(active));
        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
    }

    @Test
    void shouldGenerateAndStoreOnlyHashedPinOnExplicitRequest() {
        stubEligibleAccount();
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        when(accountActivationRepository.save(any(AccountActivation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.requestPin("alice")).isTrue();

        ArgumentCaptor<String> pinCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendAccountActivationPin(
                org.mockito.ArgumentMatchers.eq("alice@example.com"),
                org.mockito.ArgumentMatchers.eq("Alice"),
                pinCaptor.capture(),
                org.mockito.ArgumentMatchers.eq(15));
        assertThat(pinCaptor.getValue()).matches("\\d{6}");

        ArgumentCaptor<AccountActivation> activationCaptor = ArgumentCaptor.forClass(AccountActivation.class);
        verify(accountActivationRepository).save(activationCaptor.capture());
        AccountActivation stored = activationCaptor.getValue();
        assertThat(stored.getPinHash()).isEqualTo("hashed-pin");
        assertThat(stored.getPinHash()).isNotEqualTo(pinCaptor.getValue());
        assertThat(stored.getFailedAttempts()).isZero();
        assertThat(stored.getExpiresAt()).isAfter(stored.getRequestedAt());
    }

    @Test
    void shouldNotGeneratePinForIneligibleAccount() {
        when(appUserRepository.findById("alice")).thenReturn(Optional.empty());

        assertThat(service.requestPin("alice")).isFalse();

        verify(passwordEncoder, never()).encode(anyString());
        verify(emailService, never()).sendAccountActivationPin(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void shouldThrottleRequestsDuringCooldown() {
        stubEligibleAccount();
        AccountActivation existing = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        existing.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(existing));

        assertThat(service.requestPin("alice")).isFalse();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldThrottleHourlyRequestLimit() {
        stubEligibleAccount();
        AccountActivation existing = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        existing.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        existing.setRequestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30));
        existing.setRequestCount(5);
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(existing));

        assertThat(service.requestPin("alice")).isFalse();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldInvalidateActivationRecordWhenEmailDeliveryFails() {
        stubEligibleAccount();
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        doThrow(new IllegalStateException("provider unavailable"))
                .when(emailService).sendAccountActivationPin(anyString(), anyString(), anyString(), org.mockito.ArgumentMatchers.anyInt());

        assertThat(service.requestPin("alice")).isFalse();
        verify(accountActivationRepository).deleteById("alice");
    }

    @Test
    void shouldVerifyCorrectPinOnce() {
        stubEligibleAccount();
        AccountActivation activation = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(activation));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);

        assertThat(service.verifyPin("alice", "123456")).isTrue();
        assertThat(activation.getVerifiedAt()).isNotNull();
        verify(accountActivationRepository).save(activation);
    }

    @Test
    void shouldCountWrongPinAttempts() {
        stubEligibleAccount();
        AccountActivation activation = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(activation));
        when(passwordEncoder.matches("654321", "hashed-pin")).thenReturn(false);

        assertThat(service.verifyPin("alice", "654321")).isFalse();
        assertThat(activation.getFailedAttempts()).isEqualTo(1);
        verify(accountActivationRepository).save(activation);
    }

    @Test
    void shouldRejectExpiredMalformedAndMaxAttemptPins() {
        stubEligibleAccount();
        AccountActivation expired = activation(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(expired));
        assertThat(service.verifyPin("alice", "123456")).isFalse();

        assertThat(service.verifyPin("alice", "12ab56")).isFalse();

        AccountActivation maxed = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        maxed.setFailedAttempts(5);
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(maxed));
        assertThat(service.verifyPin("alice", "123456")).isFalse();
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldSetInitialPasswordOnlyAfterVerificationAndConsumeActivation() {
        stubEligibleAccount();
        AccountActivation activation = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        activation.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(activation));
        when(appUserService.completeInitialPassword("alice", "strong-pass")).thenReturn(pendingUser());

        assertThat(service.setInitialPassword("alice", "strong-pass")).isTrue();

        verify(appUserService).completeInitialPassword("alice", "strong-pass");
        assertThat(activation.getConsumedAt()).isNotNull();
        assertThat(activation.getPinHash()).isNull();
        verify(accountActivationRepository).save(activation);
    }

    @Test
    void shouldRejectPasswordSetupBeforeVerificationOrAfterConsumption() {
        stubEligibleAccount();
        AccountActivation unverified = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(unverified));
        assertThat(service.setInitialPassword("alice", "strong-pass")).isFalse();

        AccountActivation consumed = activation(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        consumed.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(2));
        consumed.setConsumedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(accountActivationRepository.findById("alice")).thenReturn(Optional.of(consumed));
        assertThat(service.setInitialPassword("alice", "strong-pass")).isFalse();
        verify(appUserService, never()).completeInitialPassword(anyString(), anyString());
    }

    @Test
    void shouldRejectDisabledFutureTerminatedOrCrossTenantAccounts() {
        AppUser user = pendingUser();
        user.setActive(false);
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(user));
        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        user.setActive(true);
        Staff future = staff();
        future.setJoinDate(LocalDate.now(ZoneOffset.UTC).plusDays(1));
        when(staffRepository.findById("S001")).thenReturn(Optional.of(future));
        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        Staff terminated = staff();
        terminated.setTermDate(LocalDate.now(ZoneOffset.UTC));
        when(staffRepository.findById("S001")).thenReturn(Optional.of(terminated));
        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        Staff otherTenant = staff();
        otherTenant.setTenantId("tenant-b");
        when(staffRepository.findById("S001")).thenReturn(Optional.of(otherTenant));
        assertThat(service.lookup("alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
    }

    private void stubEligibleAccount() {
        when(appUserRepository.findById("alice")).thenReturn(Optional.of(pendingUser()));
        when(staffRepository.findById("S001")).thenReturn(Optional.of(staff()));
    }

    private static AppUser pendingUser() {
        return AppUser.builder()
                .loginName("alice")
                .password(null)
                .active(true)
                .staffId("S001")
                .tenantId("tenant-a")
                .build();
    }

    private static Staff staff() {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .email("alice@example.com")
                .joinDate(LocalDate.of(2025, 1, 1))
                .tenantId("tenant-a")
                .build();
    }

    private static AccountActivation activation(LocalDateTime expiresAt) {
        return AccountActivation.builder()
                .loginName("alice")
                .pinHash("hashed-pin")
                .requestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .expiresAt(expiresAt)
                .failedAttempts(0)
                .requestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .requestCount(1)
                .build();
    }
}
