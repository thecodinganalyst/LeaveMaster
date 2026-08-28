package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRoleRepository;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserPendingActivationTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private AppRoleRepository appRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantActivityService tenantActivityService;

    @InjectMocks private AppUserService appUserService;

    @Test
    void shouldCreatePendingStaffUserWithoutGeneratingDefaultPassword() {
        when(appUserRepository.existsScopedLoginName("tenant-a", "alice")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = appUserService.createPendingForStaff(
                "S001", "alice", true, "tenant-a", Set.of());

        assertThat(saved.getPassword()).isNull();
        assertThat(saved.isActive()).isTrue();
        assertThat(saved.getStaffId()).isEqualTo("S001");
        assertThat(saved.getTenantId()).isEqualTo("tenant-a");
        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void sixArgumentStaffCreationShouldIgnoreLegacyDefaultPassword() {
        when(appUserRepository.existsScopedLoginName("tenant-a", "alice")).thenReturn(false);
        when(appUserRepository.save(any(AppUser.class))).thenAnswer(invocation -> invocation.getArgument(0));

        AppUser saved = appUserService.createForStaff(
                "S001", "alice", "alice", true, "tenant-a", Set.of());

        assertThat(saved.getPassword()).isNull();
        verify(passwordEncoder, never()).encode("alice");
    }

    @Test
    void shouldCompleteInitialPasswordForEligiblePendingAccount() {
        AppUser pending = AppUser.builder()
                .userId("user-alice")
                .loginName("alice")
                .active(true)
                .staffId("S001")
                .tenantId("tenant-a")
                .password(null)
                .build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(java.util.Optional.of(pending));
        when(appUserRepository.findById("user-alice")).thenReturn(java.util.Optional.of(pending));
        when(passwordEncoder.encode("strong-pass")).thenReturn("encoded-strong-pass");
        when(appUserRepository.save(pending)).thenReturn(pending);

        AppUser saved = appUserService.completeInitialPassword("alice", "strong-pass");

        assertThat(saved.getPassword()).isEqualTo("encoded-strong-pass");
        verify(tenantActivityService).touch("tenant-a");
    }

    @Test
    void shouldRejectInitialPasswordForActiveAccountThatAlreadyHasPassword() {
        AppUser existing = AppUser.builder()
                .userId("user-alice")
                .loginName("alice")
                .active(true)
                .password("existing-hash")
                .build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(java.util.Optional.of(existing));
        when(appUserRepository.findById("user-alice")).thenReturn(java.util.Optional.of(existing));

        assertThatThrownBy(() -> appUserService.completeInitialPassword("alice", "strong-pass"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not eligible");
        verify(passwordEncoder, never()).encode("strong-pass");
    }

    @Test
    void shouldEnforcePasswordPolicyForInitialPassword() {
        assertThatThrownBy(() -> appUserService.completeInitialPassword("alice", "short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least 8");
    }

    @Test
    void pendingAccountCannotUseNormalPasswordLogin() {
        AppUser pending = AppUser.builder()
                .userId("user-alice")
                .loginName("alice")
                .active(true)
                .password(null)
                .build();
        when(appUserRepository.findUniqueByLoginName("alice")).thenReturn(java.util.Optional.of(pending));

        assertThatThrownBy(() -> appUserService.login("alice", "alice"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Invalid credentials");
        verify(passwordEncoder, never()).matches(any(), any());
    }
}
