package com.practical.leavemaster.config;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlatformAdminAccessTest {

    @Mock
    private AppUserRepository appUserRepository;

    @InjectMocks
    private PlatformAdminAccess platformAdminAccess;

    @Test
    void shouldAllowActivePlatformAdminWithoutTenant() {
        AppRole role = AppRole.builder().id("PLATFORM_ADMIN").active(true).permissions(Set.of()).build();
        AppUser user = AppUser.builder().userId("platform-admin").tenantId(null).active(true).roles(Set.of(role)).build();
        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(user));

        var authentication = UsernamePasswordAuthenticationToken.authenticated("platform-admin", null, Set.of());

        assertThat(platformAdminAccess.isPlatformAdmin(authentication)).isTrue();
    }

    @Test
    void shouldRejectTenantAdminEvenWhenRoleNameLooksAdministrative() {
        AppRole role = AppRole.builder().id("Bravo_Admin").active(true).permissions(Set.of()).build();
        AppUser user = AppUser.builder().userId("tenant-admin").tenantId("Bravo").active(true).roles(Set.of(role)).build();
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(user));

        var authentication = UsernamePasswordAuthenticationToken.authenticated("tenant-admin", null, Set.of());

        assertThat(platformAdminAccess.isPlatformAdmin(authentication)).isFalse();
    }

    @Test
    void shouldRejectPlatformAdminRoleAttachedToTenantUser() {
        AppRole role = AppRole.builder().id("PLATFORM_ADMIN").active(true).permissions(Set.of()).build();
        AppUser user = AppUser.builder().userId("invalid-admin").tenantId("Bravo").active(true).roles(Set.of(role)).build();
        when(appUserRepository.findById("invalid-admin")).thenReturn(Optional.of(user));

        var authentication = UsernamePasswordAuthenticationToken.authenticated("invalid-admin", null, Set.of());

        assertThat(platformAdminAccess.isPlatformAdmin(authentication)).isFalse();
    }

    @Test
    void shouldRejectUnauthenticatedRequest() {
        var authentication = UsernamePasswordAuthenticationToken.unauthenticated("anonymous", null);

        assertThat(platformAdminAccess.isPlatformAdmin(authentication)).isFalse();
    }
}
