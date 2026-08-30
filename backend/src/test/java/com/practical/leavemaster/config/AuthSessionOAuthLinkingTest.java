package com.practical.leavemaster.config;

import com.practical.leavemaster.jurisdiction.JurisdictionRepository;
import com.practical.leavemaster.rbac.AppRole;
import com.practical.leavemaster.staff.StaffRepository;
import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import com.practical.leavemaster.user.AppUserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthSessionOAuthLinkingTest {

    private AppUserRepository appUserRepository;
    private AuthSessionController controller;
    private UsernamePasswordAuthenticationToken authentication;

    @BeforeEach
    void setUp() {
        appUserRepository = mock(AppUserRepository.class);
        controller = new AuthSessionController(
                appUserRepository,
                mock(AppUserService.class),
                mock(StaffRepository.class),
                mock(JurisdictionRepository.class));
        authentication = new UsernamePasswordAuthenticationToken("user-1", "n/a", List.of());
    }

    @Test
    void shouldReturnUnlinkedStatusForEligibleTenantUser() {
        AppUser user = tenantUser();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.oauthLinkStatus(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().linked()).isFalse();
        assertThat(response.getBody().provider()).isNull();
    }

    @Test
    void shouldReturnUnlinkedStatusForPlatformAdminWithoutTenant() {
        AppUser user = platformAdmin();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.oauthLinkStatus(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().linked()).isFalse();
        assertThat(response.getBody().provider()).isNull();
    }

    @Test
    void shouldReturnLinkedProviderStatus() {
        AppUser user = tenantUser();
        user.setOidcProvider("google");
        user.setOidcSubject("google-subject");
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.oauthLinkStatus(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().linked()).isTrue();
        assertThat(response.getBody().provider()).isEqualTo("google");
    }

    @Test
    void shouldReturnLinkedProviderStatusForPlatformAdminWithoutTenant() {
        AppUser user = platformAdmin();
        user.setOidcProvider("github");
        user.setOidcSubject("github-subject");
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.oauthLinkStatus(authentication);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().linked()).isTrue();
        assertThat(response.getBody().provider()).isEqualTo("github");
    }

    @Test
    void shouldStartGithubLinkForVerifiedTenantUser() {
        AppUser user = tenantUser();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        MockHttpSession session = new MockHttpSession();

        var response = controller.startOAuthLink(" GitHub ", authentication, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("/oauth2/authorization/github");
        assertThat(OAuthLinkingContext.hasContext(session)).isTrue();
        assertThat(OAuthLinkingContext.consume(session, "github"))
                .get().extracting(OAuthLinkingContext.LinkRequest::userId).isEqualTo("user-1");
    }

    @Test
    void shouldStartGoogleLinkForPlatformAdminWithoutTenant() {
        AppUser user = platformAdmin();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        MockHttpSession session = new MockHttpSession();

        var response = controller.startOAuthLink("google", authentication, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("/oauth2/authorization/google");
        assertThat(OAuthLinkingContext.hasContext(session)).isTrue();
        assertThat(OAuthLinkingContext.consume(session, "google"))
                .get().extracting(OAuthLinkingContext.LinkRequest::userId).isEqualTo("user-1");
    }

    @Test
    void shouldStartGithubLinkForPlatformAdminWithoutTenant() {
        AppUser user = platformAdmin();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));
        MockHttpSession session = new MockHttpSession();

        var response = controller.startOAuthLink("github", authentication, session);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.FOUND);
        assertThat(response.getHeaders().getLocation()).hasToString("/oauth2/authorization/github");
        assertThat(OAuthLinkingContext.hasContext(session)).isTrue();
        assertThat(OAuthLinkingContext.consume(session, "github"))
                .get().extracting(OAuthLinkingContext.LinkRequest::userId).isEqualTo("user-1");
    }

    @Test
    void shouldRejectUnsupportedProvider() {
        var response = controller.startOAuthLink("microsoft", authentication, new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("error", "unsupported_provider"));
    }

    @Test
    void shouldRejectLinkWhenUserAlreadyHasProvider() {
        AppUser user = tenantUser();
        user.setOidcProvider("github");
        user.setOidcSubject("existing-subject");
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.startOAuthLink("google", authentication, new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("error", "already_linked"));
    }

    @Test
    void shouldRejectSecondProviderForPlatformAdmin() {
        AppUser user = platformAdmin();
        user.setOidcProvider("github");
        user.setOidcSubject("existing-subject");
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var response = controller.startOAuthLink("google", authentication, new MockHttpSession());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
        assertThat(response.getBody()).isEqualTo(java.util.Map.of("error", "already_linked"));
    }

    @Test
    void shouldRejectNonPlatformUserWithoutTenant() {
        AppUser user = tenantUser();
        user.setTenantId(null);
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var statusResponse = controller.oauthLinkStatus(authentication);
        var startResponse = controller.startOAuthLink("google", authentication, new MockHttpSession());

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRejectInactivePlatformAdmin() {
        AppUser user = platformAdmin();
        user.setActive(false);
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(user));

        var statusResponse = controller.oauthLinkStatus(authentication);
        var startResponse = controller.startOAuthLink("google", authentication, new MockHttpSession());

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void shouldRejectUnknownUser() {
        when(appUserRepository.findById("user-1")).thenReturn(Optional.empty());

        var statusResponse = controller.oauthLinkStatus(authentication);
        var startResponse = controller.startOAuthLink("google", authentication, new MockHttpSession());

        assertThat(statusResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(startResponse.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    private AppUser tenantUser() {
        return AppUser.builder()
                .userId("user-1")
                .loginName("001")
                .tenantId("tenant-a")
                .active(true)
                .build();
    }

    private AppUser platformAdmin() {
        AppRole platformAdminRole = AppRole.builder()
                .id("PLATFORM_ADMIN")
                .description("Platform administrator")
                .active(true)
                .build();
        return AppUser.builder()
                .userId("user-1")
                .loginName("PlatformAdmin")
                .tenantId(null)
                .active(true)
                .roles(Set.of(platformAdminRole))
                .build();
    }
}
