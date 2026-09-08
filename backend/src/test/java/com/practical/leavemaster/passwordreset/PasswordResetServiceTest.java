package com.practical.leavemaster.passwordreset;

import com.practical.leavemaster.email.EmailDeliveryException;
import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.staff.Staff;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PasswordResetServiceTest {

    private static final String TENANT = "Bravo";
    private static final String USER_ID = "user-1";

    @Mock private AppUserRepository appUserRepository;
    @Mock private StaffRepository staffRepository;
    @Mock private PasswordResetRepository passwordResetRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private EmailService emailService;

    @InjectMocks private PasswordResetService service;

    @BeforeEach
    void configure() {
        ReflectionTestUtils.setField(service, "pinExpiryMinutes", 15);
        ReflectionTestUtils.setField(service, "resendCooldownSeconds", 60L);
        ReflectionTestUtils.setField(service, "maxAttempts", 5);
        ReflectionTestUtils.setField(service, "maxRequestsPerHour", 5);
    }

    @Test
    void staffRequestUsesStaffEmailAndStoresOnlyHashedPin() {
        stubEligibleStaff();
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        when(passwordResetRepository.save(any(PasswordReset.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(service.requestPin(" Bravo ", " alice ")).isTrue();

        ArgumentCaptor<String> pin = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendPasswordResetPin(eq("alice@example.com"), eq("Alice"), pin.capture(), eq(15));
        assertThat(pin.getValue()).matches("\\d{6}");

        ArgumentCaptor<PasswordReset> reset = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository).save(reset.capture());
        assertThat(reset.getValue().getPinHash()).isEqualTo("hashed-pin");
        assertThat(reset.getValue().getPinHash()).isNotEqualTo(pin.getValue());
    }

    @Test
    void platformAdminRequestUsesAppUserEmailWithoutStaffLookup() {
        AppUser admin = user(null, null, "admin@example.com");
        admin.setLoginName("PlatformAdmin");
        when(appUserRepository.findByTenantIdIsNullAndLoginName("PlatformAdmin")).thenReturn(Optional.of(admin));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");

        assertThat(service.requestPin("PLATFORM", "PlatformAdmin")).isTrue();

        verify(emailService).sendPasswordResetPin(eq("admin@example.com"), eq("PlatformAdmin"), anyString(), eq(15));
        verify(staffRepository, never()).findByIdAndTenantId(anyString(), anyString());
    }

    @Test
    void unknownInactiveNoPasswordAndMissingEmailAccountsFailSafely() {
        when(appUserRepository.findByTenantIdAndLoginName(TENANT, "missing")).thenReturn(Optional.empty());
        assertThat(service.requestPin(TENANT, "missing")).isFalse();
        assertThat(service.requestPin(null, "alice")).isFalse();

        AppUser inactive = user(TENANT, null, "admin@example.com");
        inactive.setActive(false);
        when(appUserRepository.findByTenantIdAndLoginName(TENANT, "inactive")).thenReturn(Optional.of(inactive));
        assertThat(service.requestPin(TENANT, "inactive")).isFalse();

        AppUser oauthOnly = user(TENANT, null, "admin@example.com");
        oauthOnly.setPassword(null);
        when(appUserRepository.findByTenantIdAndLoginName(TENANT, "oauth")).thenReturn(Optional.of(oauthOnly));
        assertThat(service.requestPin(TENANT, "oauth")).isFalse();

        AppUser noEmail = user(null, null, null);
        noEmail.setLoginName("PlatformAdmin");
        when(appUserRepository.findByTenantIdIsNullAndLoginName("PlatformAdmin")).thenReturn(Optional.of(noEmail));
        assertThat(service.requestPin("PLATFORM", "PlatformAdmin")).isFalse();

        verify(emailService, never()).sendPasswordResetPin(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void ineligibleStaffEmploymentAndMissingStaffEmailFailSafely() {
        AppUser staffUser = user(TENANT, "S001", null);
        when(appUserRepository.findByTenantIdAndLoginName(TENANT, "alice")).thenReturn(Optional.of(staffUser));

        when(staffRepository.findByIdAndTenantId("S001", TENANT)).thenReturn(Optional.empty());
        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        Staff future = staff("alice@example.com");
        future.setJoinDate(LocalDate.now(ZoneOffset.UTC).plusDays(1));
        when(staffRepository.findByIdAndTenantId("S001", TENANT)).thenReturn(Optional.of(future));
        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        Staff terminated = staff("alice@example.com");
        terminated.setTermDate(LocalDate.now(ZoneOffset.UTC));
        when(staffRepository.findByIdAndTenantId("S001", TENANT)).thenReturn(Optional.of(terminated));
        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        Staff noEmail = staff(null);
        when(staffRepository.findByIdAndTenantId("S001", TENANT)).thenReturn(Optional.of(noEmail));
        assertThat(service.requestPin(TENANT, "alice")).isFalse();
    }

    @Test
    void resendCooldownAndHourlyLimitAreEnforced() {
        stubEligibleStaff();
        PasswordReset cooldown = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        cooldown.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(cooldown));
        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        PasswordReset maxed = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        maxed.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        maxed.setRequestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30));
        maxed.setRequestCount(5);
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(maxed));
        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void oldHourlyWindowIsResetBeforeIssuingNewPin() {
        stubEligibleStaff();
        PasswordReset existing = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        existing.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        existing.setRequestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusHours(2));
        existing.setRequestCount(5);
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(existing));
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");

        assertThat(service.requestPin(TENANT, "alice")).isTrue();
        assertThat(existing.getRequestCount()).isEqualTo(1);
    }

    @Test
    void providerFailureInvalidatesPinButKeepsThrottleRecord() {
        stubEligibleStaff();
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        doThrow(new EmailDeliveryException("provider failed"))
                .when(emailService).sendPasswordResetPin(anyString(), anyString(), anyString(), anyInt());

        assertThat(service.requestPin(TENANT, "alice")).isFalse();

        ArgumentCaptor<PasswordReset> captor = ArgumentCaptor.forClass(PasswordReset.class);
        verify(passwordResetRepository, org.mockito.Mockito.atLeast(2)).save(captor.capture());
        PasswordReset last = captor.getAllValues().get(captor.getAllValues().size() - 1);
        assertThat(last.getPinHash()).isNull();
        assertThat(last.getRequestCount()).isEqualTo(1);
    }

    @Test
    void correctPinVerifiesAndWrongPinCountsAttempt() {
        stubEligibleStaff();
        PasswordReset reset = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(reset));
        when(passwordEncoder.matches("654321", "hashed-pin")).thenReturn(false);
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);

        assertThat(service.verifyPin(TENANT, "alice", "654321")).isFalse();
        assertThat(reset.getFailedAttempts()).isEqualTo(1);
        assertThat(service.verifyPin(TENANT, "alice", "123456")).isTrue();
        assertThat(reset.getVerifiedAt()).isNotNull();
    }

    @Test
    void malformedExpiredConsumedAlreadyVerifiedAndMaxedPinsAreRejected() {
        stubEligibleStaff();
        assertThat(service.verifyPin(TENANT, "alice", "12ab56")).isFalse();

        PasswordReset expired = reset(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(expired));
        assertThat(service.verifyPin(TENANT, "alice", "123456")).isFalse();

        PasswordReset consumed = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        consumed.setConsumedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(consumed));
        assertThat(service.verifyPin(TENANT, "alice", "123456")).isFalse();

        PasswordReset verified = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        verified.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(verified));
        assertThat(service.verifyPin(TENANT, "alice", "123456")).isFalse();

        PasswordReset maxed = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        maxed.setFailedAttempts(5);
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(maxed));
        assertThat(service.verifyPin(TENANT, "alice", "123456")).isFalse();
    }

    @Test
    void verifiedResetChangesPasswordPreservesOauthAndConsumesPin() {
        stubEligibleStaff();
        PasswordReset reset = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        reset.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(reset));
        AppUser persisted = user(TENANT, "S001", null);
        persisted.setOidcProvider("google");
        persisted.setOidcSubject("subject-1");
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(persisted));
        when(passwordEncoder.matches("new-password", "old-hash")).thenReturn(false);
        when(passwordEncoder.encode("new-password")).thenReturn("new-hash");

        assertThat(service.setPassword(TENANT, "alice", "new-password")).isTrue();

        assertThat(persisted.getPassword()).isEqualTo("new-hash");
        assertThat(persisted.getOidcProvider()).isEqualTo("google");
        assertThat(persisted.getOidcSubject()).isEqualTo("subject-1");
        assertThat(reset.getConsumedAt()).isNotNull();
        assertThat(reset.getPinHash()).isNull();
        verify(appUserRepository).save(persisted);
    }

    @Test
    void setPasswordRejectsUnverifiedWeakSameAndStaleAccount() {
        stubEligibleStaff();
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.empty());
        assertThat(service.setPassword(TENANT, "alice", "new-password")).isFalse();

        PasswordReset verified = reset(LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        verified.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(passwordResetRepository.findById(USER_ID)).thenReturn(Optional.of(verified));
        assertThatThrownBy(() -> service.setPassword(TENANT, "alice", "short"))
                .isInstanceOf(IllegalArgumentException.class);

        AppUser persisted = user(TENANT, "S001", null);
        when(appUserRepository.findById(USER_ID)).thenReturn(Optional.of(persisted));
        when(passwordEncoder.matches("same-password", "old-hash")).thenReturn(true);
        assertThatThrownBy(() -> service.setPassword(TENANT, "alice", "same-password"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("different");

        persisted.setActive(false);
        assertThatThrownBy(() -> service.setPassword(TENANT, "alice", "other-password"))
                .isInstanceOf(IllegalStateException.class);
    }

    private void stubEligibleStaff() {
        when(appUserRepository.findByTenantIdAndLoginName(TENANT, "alice"))
                .thenReturn(Optional.of(user(TENANT, "S001", null)));
        when(staffRepository.findByIdAndTenantId("S001", TENANT))
                .thenReturn(Optional.of(staff("alice@example.com")));
    }

    private static AppUser user(String tenantId, String staffId, String email) {
        return AppUser.builder()
                .userId(USER_ID)
                .loginName("alice")
                .password("old-hash")
                .email(email)
                .active(true)
                .staffId(staffId)
                .tenantId(tenantId)
                .build();
    }

    private static Staff staff(String email) {
        return Staff.builder()
                .id("S001")
                .name("Alice")
                .email(email)
                .joinDate(LocalDate.of(2025, 1, 1))
                .tenantId(TENANT)
                .build();
    }

    private static PasswordReset reset(LocalDateTime expiresAt) {
        return PasswordReset.builder()
                .userId(USER_ID)
                .pinHash("hashed-pin")
                .requestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .expiresAt(expiresAt)
                .failedAttempts(0)
                .requestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .requestCount(1)
                .build();
    }
}
