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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountActivationServiceTest {

    private static final String TENANT_A = "tenant-a";
    private static final String USER_A = "user-alice-a";

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
    void shouldReturnActivationForEligibleTenantAccount() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");

        assertThat(service.lookup(" tenant-a ", " alice "))
                .isEqualTo(AccountActivationService.NextStep.ACTIVATION);
    }

    @Test
    void shouldReturnPasswordForUnknownActivatedMissingTenantOrPlatformAccount() {
        when(appUserRepository.findByTenantIdAndLoginName(TENANT_A, "missing")).thenReturn(Optional.empty());
        assertThat(service.lookup(TENANT_A, "missing")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
        assertThat(service.lookup(null, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
        assertThat(service.lookup("PLATFORM", "PlatformAdmin")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        AppUser activated = pendingUser(TENANT_A, USER_A, "S001");
        activated.setPassword("encoded-password");
        when(appUserRepository.findByTenantIdAndLoginName(TENANT_A, "alice")).thenReturn(Optional.of(activated));
        assertThat(service.lookup(TENANT_A, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
    }

    @Test
    void shouldGenerateAndStoreOnlyHashedPinForResolvedTenantUser() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        when(accountActivationRepository.save(any(AccountActivation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.requestPin(TENANT_A, "alice")).isTrue();

        ArgumentCaptor<String> pinCaptor = ArgumentCaptor.forClass(String.class);
        verify(emailService).sendAccountActivationPin(eq("alice@example.com"), eq("Alice"), pinCaptor.capture(), eq(15));
        assertThat(pinCaptor.getValue()).matches("\\d{6}");

        ArgumentCaptor<AccountActivation> activationCaptor = ArgumentCaptor.forClass(AccountActivation.class);
        verify(accountActivationRepository).save(activationCaptor.capture());
        AccountActivation stored = activationCaptor.getValue();
        assertThat(stored.getUserId()).isEqualTo(USER_A);
        assertThat(stored.getPinHash()).isEqualTo("hashed-pin");
        assertThat(stored.getPinHash()).isNotEqualTo(pinCaptor.getValue());
        assertThat(stored.getFailedAttempts()).isZero();
        assertThat(stored.getExpiresAt()).isAfter(stored.getRequestedAt());
    }

    @Test
    void duplicateLoginNamesAcrossTenantsUseIndependentActivationRecords() {
        String tenantB = "tenant-b";
        String userB = "user-alice-b";
        AppUser userA = pendingUser(TENANT_A, USER_A, "S001");
        AppUser userBAccount = pendingUser(tenantB, userB, "S002");
        when(appUserRepository.findByTenantIdAndLoginName(TENANT_A, "alice")).thenReturn(Optional.of(userA));
        when(appUserRepository.findByTenantIdAndLoginName(tenantB, "alice")).thenReturn(Optional.of(userBAccount));
        when(staffRepository.findByIdAndTenantId("S001", TENANT_A)).thenReturn(Optional.of(staff(TENANT_A, "S001", "alice-a@example.com")));
        when(staffRepository.findByIdAndTenantId("S002", tenantB)).thenReturn(Optional.of(staff(tenantB, "S002", "alice-b@example.com")));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.empty());
        when(accountActivationRepository.findById(userB)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");

        assertThat(service.requestPin(TENANT_A, "alice")).isTrue();
        assertThat(service.requestPin(tenantB, "alice")).isTrue();

        verify(accountActivationRepository).findById(USER_A);
        verify(accountActivationRepository).findById(userB);
        verify(emailService).sendAccountActivationPin(eq("alice-a@example.com"), eq("Alice"), anyString(), eq(15));
        verify(emailService).sendAccountActivationPin(eq("alice-b@example.com"), eq("Alice"), anyString(), eq(15));
    }

    @Test
    void shouldNotGeneratePinForWrongTenant() {
        when(appUserRepository.findByTenantIdAndLoginName("tenant-b", "alice")).thenReturn(Optional.empty());

        assertThat(service.requestPin("tenant-b", "alice")).isFalse();

        verify(passwordEncoder, never()).encode(anyString());
        verify(emailService, never()).sendAccountActivationPin(anyString(), anyString(), anyString(), anyInt());
    }

    @Test
    void shouldThrottleRequestsPerResolvedUser() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        AccountActivation existing = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        existing.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(existing));

        assertThat(service.requestPin(TENANT_A, "alice")).isFalse();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldThrottleHourlyRequestLimitPerResolvedUser() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        AccountActivation existing = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        existing.setRequestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(2));
        existing.setRequestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(30));
        existing.setRequestCount(5);
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(existing));

        assertThat(service.requestPin(TENANT_A, "alice")).isFalse();
        verify(passwordEncoder, never()).encode(anyString());
    }

    @Test
    void shouldInvalidateOnlyResolvedUserActivationWhenDeliveryFails() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        doThrow(new IllegalStateException("provider unavailable"))
                .when(emailService).sendAccountActivationPin(anyString(), anyString(), anyString(), anyInt());

        assertThat(service.requestPin(TENANT_A, "alice")).isFalse();
        verify(accountActivationRepository).deleteById(USER_A);
    }

    @Test
    void shouldVerifyCorrectPinForResolvedTenantUser() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        AccountActivation activation = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(activation));
        when(passwordEncoder.matches("123456", "hashed-pin")).thenReturn(true);

        assertThat(service.verifyPin(TENANT_A, "alice", "123456")).isTrue();
        assertThat(activation.getVerifiedAt()).isNotNull();
        verify(accountActivationRepository).save(activation);
    }

    @Test
    void wrongTenantCannotVerifyAnotherTenantsPin() {
        when(appUserRepository.findByTenantIdAndLoginName("tenant-b", "alice")).thenReturn(Optional.empty());

        assertThat(service.verifyPin("tenant-b", "alice", "123456")).isFalse();

        verify(accountActivationRepository, never()).findById(anyString());
        verify(passwordEncoder, never()).matches(anyString(), anyString());
    }

    @Test
    void shouldCountWrongPinAttemptsAndRejectMalformedExpiredOrMaxedPins() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        AccountActivation activation = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(activation));
        when(passwordEncoder.matches("654321", "hashed-pin")).thenReturn(false);

        assertThat(service.verifyPin(TENANT_A, "alice", "654321")).isFalse();
        assertThat(activation.getFailedAttempts()).isEqualTo(1);

        assertThat(service.verifyPin(TENANT_A, "alice", "12ab56")).isFalse();

        AccountActivation expired = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(expired));
        assertThat(service.verifyPin(TENANT_A, "alice", "123456")).isFalse();

        AccountActivation maxed = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        maxed.setFailedAttempts(5);
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(maxed));
        assertThat(service.verifyPin(TENANT_A, "alice", "123456")).isFalse();
    }

    @Test
    void shouldSetInitialPasswordOnlyForVerifiedResolvedTenantUser() {
        stubEligibleAccount(TENANT_A, USER_A, "S001", "alice@example.com");
        AccountActivation activation = activation(USER_A, LocalDateTime.now(ZoneOffset.UTC).plusMinutes(10));
        activation.setVerifiedAt(LocalDateTime.now(ZoneOffset.UTC).minusSeconds(1));
        when(accountActivationRepository.findById(USER_A)).thenReturn(Optional.of(activation));
        when(appUserService.completeInitialPasswordByUserId(USER_A, "strong-pass"))
                .thenReturn(pendingUser(TENANT_A, USER_A, "S001"));

        assertThat(service.setInitialPassword(TENANT_A, "alice", "strong-pass")).isTrue();

        verify(appUserService).completeInitialPasswordByUserId(USER_A, "strong-pass");
        assertThat(activation.getConsumedAt()).isNotNull();
        assertThat(activation.getPinHash()).isNull();
    }

    @Test
    void wrongTenantCannotSetPasswordForAnotherTenantUser() {
        when(appUserRepository.findByTenantIdAndLoginName("tenant-b", "alice")).thenReturn(Optional.empty());

        assertThat(service.setInitialPassword("tenant-b", "alice", "strong-pass")).isFalse();

        verify(appUserService, never()).completeInitialPasswordByUserId(anyString(), anyString());
        verify(accountActivationRepository, never()).findById(anyString());
    }

    @Test
    void shouldRejectIneligibleStaffStatesAndTenantMismatches() {
        AppUser user = pendingUser(TENANT_A, USER_A, "S001");
        user.setActive(false);
        when(appUserRepository.findByTenantIdAndLoginName(TENANT_A, "alice")).thenReturn(Optional.of(user));
        assertThat(service.lookup(TENANT_A, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        user.setActive(true);
        Staff future = staff(TENANT_A, "S001", "alice@example.com");
        future.setJoinDate(LocalDate.now(ZoneOffset.UTC).plusDays(1));
        when(staffRepository.findByIdAndTenantId("S001", TENANT_A)).thenReturn(Optional.of(future));
        assertThat(service.lookup(TENANT_A, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        Staff terminated = staff(TENANT_A, "S001", "alice@example.com");
        terminated.setTermDate(LocalDate.now(ZoneOffset.UTC));
        when(staffRepository.findByIdAndTenantId("S001", TENANT_A)).thenReturn(Optional.of(terminated));
        assertThat(service.lookup(TENANT_A, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);

        when(staffRepository.findByIdAndTenantId("S001", TENANT_A)).thenReturn(Optional.empty());
        assertThat(service.lookup(TENANT_A, "alice")).isEqualTo(AccountActivationService.NextStep.PASSWORD);
    }

    private void stubEligibleAccount(String tenantId, String userId, String staffId, String email) {
        when(appUserRepository.findByTenantIdAndLoginName(tenantId, "alice"))
                .thenReturn(Optional.of(pendingUser(tenantId, userId, staffId)));
        when(staffRepository.findByIdAndTenantId(staffId, tenantId))
                .thenReturn(Optional.of(staff(tenantId, staffId, email)));
    }

    private static AppUser pendingUser(String tenantId, String userId, String staffId) {
        return AppUser.builder()
                .userId(userId)
                .loginName("alice")
                .password(null)
                .active(true)
                .staffId(staffId)
                .tenantId(tenantId)
                .build();
    }

    private static Staff staff(String tenantId, String staffId, String email) {
        return Staff.builder()
                .id(staffId)
                .name("Alice")
                .email(email)
                .joinDate(LocalDate.of(2025, 1, 1))
                .tenantId(tenantId)
                .build();
    }

    private static AccountActivation activation(String userId, LocalDateTime expiresAt) {
        return AccountActivation.builder()
                .userId(userId)
                .pinHash("hashed-pin")
                .requestedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1))
                .expiresAt(expiresAt)
                .failedAttempts(0)
                .requestWindowStartedAt(LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5))
                .requestCount(1)
                .build();
    }
}
