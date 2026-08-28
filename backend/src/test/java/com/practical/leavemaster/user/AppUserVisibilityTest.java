package com.practical.leavemaster.user;

import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.rbac.AppRoleRepository;
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

    @Mock private AppUserRepository appUserRepository;
    @Mock private AppRoleRepository appRoleRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TenantActivityService tenantActivityService;

    @InjectMocks private AppUserService appUserService;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void shouldReturnOnlyCurrentTenantUsersToTenantUserLists() {
        AppUser tenantAdmin = user("u-tenant-admin", "tenant-a", "tenant-admin", role("TENANT_ADMIN"));
        AppUser sameTenant = user("u-staff", "tenant-a", "staff", role("TENANT_STAFF"));
        AppUser otherTenant = user("u-other", "tenant-b", "other", role("TENANT_STAFF"));
        AppUser platformAdmin = user("u-platform", null, "platform-admin", role("PLATFORM_ADMIN"));
        authenticateAs(tenantAdmin);

        when(appUserRepository.findAll()).thenReturn(List.of(tenantAdmin, sameTenant, otherTenant, platformAdmin));
        when(appUserRepository.findById("u-tenant-admin")).thenReturn(Optional.of(tenantAdmin));

        List<AppUser> result = appUserService.findAll();

        assertThat(result)
                .extracting(AppUser::getLoginName)
                .containsExactly("tenant-admin", "staff");
    }

    @Test
    void shouldReturnAllUsersToPlatformAdminUserLists() {
        AppUser platformAdmin = user("u-platform", null, "platform-admin", role("PLATFORM_ADMIN"));
        AppUser tenantUser = user("u-tenant", "tenant-a", "001", role("TENANT_STAFF"));
        authenticateAs(platformAdmin);

        when(appUserRepository.findAll()).thenReturn(List.of(platformAdmin, tenantUser));
        when(appUserRepository.findById("u-platform")).thenReturn(Optional.of(platformAdmin));

        List<AppUser> result = appUserService.findAll();

        assertThat(result).containsExactly(platformAdmin, tenantUser);
    }

    @Test
    void shouldResolveDirectLookupInsideCurrentTenant() {
        AppUser tenantAdmin = user("u-admin", "tenant-a", "tenant-admin", role("TENANT_ADMIN"));
        AppUser tenantUser = user("u-user", "tenant-a", "001", role("TENANT_STAFF"));
        authenticateAs(tenantAdmin);

        when(appUserRepository.findById("u-admin")).thenReturn(Optional.of(tenantAdmin));
        when(appUserRepository.findScopedByLoginName("tenant-a", "001")).thenReturn(Optional.of(tenantUser));

        assertThat(appUserService.findByLoginName("001")).contains(tenantUser);
    }

    @Test
    void shouldNotResolveAnotherTenantUserBySameLogin() {
        AppUser tenantAdmin = user("u-admin", "tenant-a", "tenant-admin", role("TENANT_ADMIN"));
        authenticateAs(tenantAdmin);

        when(appUserRepository.findById("u-admin")).thenReturn(Optional.of(tenantAdmin));
        when(appUserRepository.findScopedByLoginName("tenant-a", "001")).thenReturn(Optional.empty());

        assertThat(appUserService.findByLoginName("001")).isEmpty();
    }

    @Test
    void shouldAllowPlatformAdminToResolvePlatformScopedUser() {
        AppUser platformAdmin = user("u-platform", null, "platform-admin", role("PLATFORM_ADMIN"));
        AppUser anotherPlatformAdmin = user("u-platform-2", null, "platform-admin-2", role("PLATFORM_ADMIN"));
        authenticateAs(platformAdmin);

        when(appUserRepository.findById("u-platform")).thenReturn(Optional.of(platformAdmin));
        when(appUserRepository.findByTenantIdIsNullAndLoginName("platform-admin-2")).thenReturn(Optional.of(anotherPlatformAdmin));

        assertThat(appUserService.findByLoginName("platform-admin-2")).contains(anotherPlatformAdmin);
    }

    @Test
    void shouldHidePlatformAdminUserFromAnonymousDirectLookup() {
        AppUser platformAdmin = user("u-platform", null, "platform-admin", role("PLATFORM_ADMIN"));
        when(appUserRepository.findUniqueByLoginName("platform-admin")).thenReturn(Optional.of(platformAdmin));

        assertThat(appUserService.findByLoginName("platform-admin")).isEmpty();
    }

    private void authenticateAs(AppUser user) {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user.getUserId(), "ignored", List.of())
        );
    }

    private AppUser user(String userId, String tenantId, String loginName, AppRole... roles) {
        return AppUser.builder()
                .userId(userId)
                .tenantId(tenantId)
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
