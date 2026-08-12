package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.tenant.TenantActivityService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AppUserVisibilityTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private TenantActivityService tenantActivityService;

    @InjectMocks
    private AppUserService appUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldHidePlatformAdminUsersFromNonPlatformAdminUserLists() {
        AppUser tenantAdmin = user("tenant-admin", role("TENANT_ADMIN"));
        AppUser platformAdmin = user("platform-admin", role("PLATFORM_ADMIN"));
        authenticateAs(tenantAdmin);

        when(appUserRepository.findAll()).thenReturn(List.of(tenantAdmin, platformAdmin));
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(tenantAdmin));

        List<AppUser> result = appUserService.findAll();

        assertThat(result)
                .extracting(AppUser::getLoginName)
                .containsExactly("tenant-admin");
    }

    @Test
    void shouldReturnPlatformAdminUsersToPlatformAdminUserLists() {
        AppUser platformAdmin = user("platform-admin", role("PLATFORM_ADMIN"));
        AppUser anotherPlatformAdmin = user("platform-admin-2", role("PLATFORM_ADMIN"));
        authenticateAs(platformAdmin);

        when(appUserRepository.findAll()).thenReturn(List.of(platformAdmin, anotherPlatformAdmin));
        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(platformAdmin));

        List<AppUser> result = appUserService.findAll();

        assertThat(result)
                .extracting(AppUser::getLoginName)
                .containsExactly("platform-admin", "platform-admin-2");
    }

    @Test
    void shouldHidePlatformAdminUserDirectLookupFromNonPlatformAdminUsers() {
        AppUser tenantAdmin = user("tenant-admin", role("TENANT_ADMIN"));
        AppUser platformAdmin = user("platform-admin", role("PLATFORM_ADMIN"));
        authenticateAs(tenantAdmin);

        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(platformAdmin));
        when(appUserRepository.findById("tenant-admin")).thenReturn(Optional.of(tenantAdmin));

        Optional<AppUser> result = appUserService.findByLoginName("platform-admin");

        assertThat(result).isEmpty();
    }

    @Test
    void shouldAllowPlatformAdminUserDirectLookupFromPlatformAdminUsers() {
        AppUser platformAdmin = user("platform-admin", role("PLATFORM_ADMIN"));
        AppUser anotherPlatformAdmin = user("platform-admin-2", role("PLATFORM_ADMIN"));
        authenticateAs(platformAdmin);

        when(appUserRepository.findById("platform-admin-2")).thenReturn(Optional.of(anotherPlatformAdmin));
        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(platformAdmin));

        Optional<AppUser> result = appUserService.findByLoginName("platform-admin-2");

        assertThat(result).contains(anotherPlatformAdmin);
    }

    @Test
    void shouldHidePlatformAdminUserFromAnonymousDirectLookup() {
        AppUser platformAdmin = user("platform-admin", role("PLATFORM_ADMIN"));
        when(appUserRepository.findById("platform-admin")).thenReturn(Optional.of(platformAdmin));

        Optional<AppUser> result = appUserService.findByLoginName("platform-admin");

        assertThat(result).isEmpty();
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getLoginName(), "ignored", List.of())
        );
    }

    private AppUser user(String loginName, AppRole... roles) {
        return AppUser.builder()
                .loginName(loginName)
                .password("encoded")
                .active(true)
                .roles(Set.of(roles))
                .build();
    }

    private AppRole role(String id) {
        return AppRole.builder()
                .id(id)
                .description(id)
                .active(true)
                .build();
    }
}
