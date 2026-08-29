package com.practical.leavemaster.accountactivation;

import com.practical.leavemaster.email.EmailService;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAdminAccountActivationServiceTest {

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
    void tenantAdminWithoutPasswordIsEligibleForActivation() {
        when(appUserRepository.findByTenantIdAndLoginName("Bravo", "Bravo_Admin"))
                .thenReturn(Optional.of(pendingTenantAdmin("admin-user", "admin@example.com")));

        assertThat(service.lookup("Bravo", "Bravo_Admin"))
                .isEqualTo(AccountActivationService.NextStep.ACTIVATION);
    }

    @Test
    void tenantAdminPinIsDeliveredToProvisionedEmail() {
        when(appUserRepository.findByTenantIdAndLoginName("Bravo", "Bravo_Admin"))
                .thenReturn(Optional.of(pendingTenantAdmin("admin-user", "admin@example.com")));
        when(accountActivationRepository.findById("admin-user")).thenReturn(Optional.empty());
        when(passwordEncoder.encode(anyString())).thenReturn("hashed-pin");
        when(accountActivationRepository.save(any(AccountActivation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        assertThat(service.requestPin("Bravo", "Bravo_Admin")).isTrue();

        verify(emailService).sendAccountActivationPin(eq("admin@example.com"), eq("Tenant Administrator"), anyString(), eq(15));
    }

    @Test
    void nonStaffUserWithoutTenantAdminRoleCannotUseActivationFlow() {
        AppUser user = AppUser.builder()
                .userId("ordinary-user")
                .loginName("ordinary")
                .tenantId("Bravo")
                .email("ordinary@example.com")
                .active(true)
                .password(null)
                .roles(Set.of())
                .build();
        when(appUserRepository.findByTenantIdAndLoginName("Bravo", "ordinary")).thenReturn(Optional.of(user));

        assertThat(service.requestPin("Bravo", "ordinary")).isFalse();
        verify(emailService, never()).sendAccountActivationPin(anyString(), anyString(), anyString(), anyInt());
    }

    private static AppUser pendingTenantAdmin(String userId, String email) {
        AppRole adminRole = AppRole.builder()
                .id("Bravo_Admin")
                .description("Bravo Tenant Admin")
                .tenantId("Bravo")
                .active(true)
                .build();
        return AppUser.builder()
                .userId(userId)
                .loginName("Bravo_Admin")
                .tenantId("Bravo")
                .email(email)
                .active(true)
                .password(null)
                .roles(Set.of(adminRole))
                .build();
    }
}
