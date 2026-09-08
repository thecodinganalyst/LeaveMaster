package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRole;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminRecoveryEmailControllerTest {

    @Mock private AppUserRepository appUserRepository;
    @InjectMocks private PlatformAdminRecoveryEmailController controller;

    @Test
    void platformAdminCanSetOwnRecoveryEmail() {
        AppRole role = AppRole.builder().id("PLATFORM_ADMIN").active(true).build();
        AppUser user = AppUser.builder()
                .userId("platform-user")
                .loginName("PlatformAdmin")
                .active(true)
                .tenantId(null)
                .roles(Set.of(role))
                .build();
        when(appUserRepository.findById("platform-user")).thenReturn(Optional.of(user));

        var response = controller.updateRecoveryEmail(
                new UsernamePasswordAuthenticationToken("platform-user", null),
                Map.of("email", " admin@example.com "));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        assertThat(user.getEmail()).isEqualTo("admin@example.com");
        verify(appUserRepository).save(user);
    }

    @Test
    void invalidEmailIsRejectedBeforePersistence() {
        var response = controller.updateRecoveryEmail(
                new UsernamePasswordAuthenticationToken("platform-user", null),
                Map.of("email", "invalid"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        verify(appUserRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void tenantUserCannotSetPlatformRecoveryEmail() {
        AppRole role = AppRole.builder().id("Bravo_Admin").active(true).build();
        AppUser user = AppUser.builder()
                .userId("tenant-user")
                .loginName("Bravo_Admin")
                .active(true)
                .tenantId("Bravo")
                .roles(Set.of(role))
                .build();
        when(appUserRepository.findById("tenant-user")).thenReturn(Optional.of(user));

        var response = controller.updateRecoveryEmail(
                new UsernamePasswordAuthenticationToken("tenant-user", null),
                Map.of("email", "admin@example.com"));

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        verify(appUserRepository, never()).save(user);
    }

    @Test
    void missingAuthenticationIsUnauthorized() {
        assertThat(controller.updateRecoveryEmail(null, Map.of("email", "admin@example.com")).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
