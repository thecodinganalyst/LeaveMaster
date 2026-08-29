package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppPermission;
import com.practical.leavemaster.rbac.AppRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TenantAuthenticationProviderTest {

    @Mock private AppUserRepository appUserRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @InjectMocks private TenantAuthenticationProvider provider;

    @Test
    void authenticatesDuplicateLoginOnlyWithinRequestedTenant() {
        AppUser tenantA = user("user-a", "tenant-a", "001", "hash-a");
        when(appUserRepository.findByTenantIdAndLoginName("tenant-a", "001")).thenReturn(Optional.of(tenantA));
        when(passwordEncoder.matches("secret-a", "hash-a")).thenReturn(true);

        Authentication result = provider.authenticate(new TenantAuthenticationToken("tenant-a", "001", "secret-a"));

        assertThat(result.isAuthenticated()).isTrue();
        assertThat(result.getName()).isEqualTo("user-a");
        assertThat(((TenantAuthenticationToken) result).getTenantId()).isEqualTo("tenant-a");
        assertThat(((TenantAuthenticationToken) result).getLoginName()).isEqualTo("001");
        assertThat(result.getAuthorities()).extracting("authority").containsExactly("STAFF_READ");
    }

    @Test
    void rejectsCorrectLoginAndPasswordInWrongTenantGenerically() {
        when(appUserRepository.findByTenantIdAndLoginName("tenant-b", "001")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> provider.authenticate(new TenantAuthenticationToken("tenant-b", "001", "secret-a")))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    @Test
    void rejectsUnknownLoginWrongPasswordInactiveAndPendingAccountsWithSameMessage() {
        AppUser active = user("user-a", "tenant-a", "001", "hash-a");
        when(appUserRepository.findByTenantIdAndLoginName("tenant-a", "missing")).thenReturn(Optional.empty());
        assertInvalid("tenant-a", "missing", "secret");

        when(appUserRepository.findByTenantIdAndLoginName("tenant-a", "001")).thenReturn(Optional.of(active));
        when(passwordEncoder.matches("wrong", "hash-a")).thenReturn(false);
        assertInvalid("tenant-a", "001", "wrong");

        active.setActive(false);
        assertInvalid("tenant-a", "001", "secret-a");

        active.setActive(true);
        active.setPassword(null);
        assertInvalid("tenant-a", "001", "secret-a");
    }

    @Test
    void authenticatesPlatformAdministratorOnlyThroughReservedPlatformRealm() {
        AppPermission permission = AppPermission.builder().code("JURISDICTION_WRITE").description("Write jurisdictions").build();
        AppRole platformAdminRole = AppRole.builder()
                .id("PLATFORM_ADMIN")
                .description("Platform admin")
                .active(true)
                .permissions(Set.of(permission))
                .build();
        AppUser admin = AppUser.builder()
                .userId("platform-user")
                .tenantId(null)
                .loginName("PlatformAdmin")
                .password("hash-platform")
                .active(true)
                .roles(Set.of(platformAdminRole))
                .build();
        when(appUserRepository.findByTenantIdIsNullAndLoginName("PlatformAdmin")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("secret", "hash-platform")).thenReturn(true);

        Authentication result = provider.authenticate(new TenantAuthenticationToken("platform", "PlatformAdmin", "secret"));

        assertThat(result.getName()).isEqualTo("platform-user");
        assertThat(((TenantAuthenticationToken) result).getTenantId()).isEqualTo(AuthenticationRealm.PLATFORM_REALM_ID);
        assertThat(result.getAuthorities()).extracting("authority")
                .containsExactlyInAnyOrder("JURISDICTION_WRITE", "ROLE_PLATFORM_ADMIN");
    }

    @Test
    void doesNotExposePlatformAdminAuthorityToTenantScopedAccount() {
        AppRole platformAdminRole = AppRole.builder()
                .id("PLATFORM_ADMIN")
                .description("Platform admin")
                .active(true)
                .permissions(Set.of())
                .build();
        AppUser tenantUser = AppUser.builder()
                .userId("tenant-user")
                .tenantId("Bravo")
                .loginName("admin")
                .password("hash")
                .active(true)
                .roles(Set.of(platformAdminRole))
                .build();
        when(appUserRepository.findByTenantIdAndLoginName("Bravo", "admin")).thenReturn(Optional.of(tenantUser));
        when(passwordEncoder.matches("secret", "hash")).thenReturn(true);

        Authentication result = provider.authenticate(new TenantAuthenticationToken("Bravo", "admin", "secret"));

        assertThat(result.getAuthorities()).extracting("authority").doesNotContain("ROLE_PLATFORM_ADMIN");
    }

    @Test
    void rejectsMissingFieldsAndDoesNotSupportUnrelatedTokenTypes() {
        assertInvalid(null, "001", "secret");
        assertInvalid("tenant-a", "", "secret");
        assertInvalid("tenant-a", "001", "");
        assertThat(provider.supports(TenantAuthenticationToken.class)).isTrue();
        assertThat(provider.supports(org.springframework.security.authentication.UsernamePasswordAuthenticationToken.class)).isFalse();
    }

    private void assertInvalid(String tenantId, String loginName, String password) {
        assertThatThrownBy(() -> provider.authenticate(new TenantAuthenticationToken(tenantId, loginName, password)))
                .isInstanceOf(BadCredentialsException.class)
                .hasMessage("Invalid credentials");
    }

    private static AppUser user(String userId, String tenantId, String loginName, String password) {
        AppPermission permission = AppPermission.builder().code("STAFF_READ").description("Read staff").build();
        AppRole role = AppRole.builder()
                .id("ROLE")
                .description("Role")
                .active(true)
                .permissions(Set.of(permission))
                .build();
        return AppUser.builder()
                .userId(userId)
                .tenantId(tenantId)
                .loginName(loginName)
                .password(password)
                .active(true)
                .roles(Set.of(role))
                .build();
    }
}
