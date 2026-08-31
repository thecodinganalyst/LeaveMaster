package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.oidc.IdTokenClaimNames;
import org.springframework.security.oauth2.core.oidc.OidcIdToken;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingUserOnlyOidcUserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAuthenticateAlreadyLinkedGoogleUserWithLeaveMasterUserIdAsPrincipal() {
        ExistingUserOnlyOidcUserService service = service();
        OidcUserRequest request = userRequest("google", "google-123");
        OidcUser googleUser = oidcUser(request, "google-123", Map.of("email", "alice@example.com"));
        AppUser appUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .active(true)
                .oidcProvider("google")
                .oidcSubject("google-123")
                .build();

        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(appUserRepository.findByOidcProviderAndOidcSubject("google", "google-123"))
                .thenReturn(Optional.of(appUser));

        OidcUser result = service.loadUser(request);

        assertThat(result.getName()).isEqualTo("user-1");
        assertThat(result.getSubject()).isEqualTo("google-123");
        assertThat(result.getEmail()).isEqualTo("alice@example.com");
        assertThat(result.getClaimAsString(ExistingUserOnlyOidcUserService.USER_ID_ATTRIBUTE)).isEqualTo("user-1");
    }

    @Test
    void shouldLinkGoogleIdentityFromAuthenticatedLinkingSession() {
        ExistingUserOnlyOidcUserService service = service();
        OidcUserRequest request = userRequest("google", "google-123");
        when(delegate.loadUser(request)).thenReturn(oidcUser(request, "google-123", Map.of()));
        when(appUserRepository.findByOidcProviderAndOidcSubject("google", "google-123"))
                .thenReturn(Optional.empty());

        AppUser appUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .tenantId("tenant-a")
                .active(true)
                .build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(appUser));
        when(appUserRepository.saveAndFlush(appUser)).thenReturn(appUser);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        OAuthLinkingContext.create(servletRequest.getSession(), "user-1", "google");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        OidcUser result = service.loadUser(request);

        assertThat(result.getName()).isEqualTo("user-1");
        assertThat(appUser.getOidcProvider()).isEqualTo("google");
        assertThat(appUser.getOidcSubject()).isEqualTo("google-123");
        verify(appUserRepository).saveAndFlush(appUser);
        assertThat(OAuthLinkingContext.consume(servletRequest.getSession(), "google")).isEmpty();
    }

    @Test
    void shouldRejectUnlinkedGoogleLoginWithoutLinkingContext() {
        ExistingUserOnlyOidcUserService service = service();
        OidcUserRequest request = userRequest("google", "google-123");
        when(delegate.loadUser(request)).thenReturn(oidcUser(request, "google-123", Map.of()));
        when(appUserRepository.findByOidcProviderAndOidcSubject("google", "google-123"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo("not_linked"));
    }

    @Test
    void shouldRejectOidcResponseWithoutSubject() {
        ExistingUserOnlyOidcUserService service = service();
        OidcUserRequest request = userRequest("google", "google-123");
        OidcUser googleUser = mock(OidcUser.class);
        when(delegate.loadUser(request)).thenReturn(googleUser);
        when(googleUser.getSubject()).thenReturn(null);

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo("invalid_request"));
    }

    private ExistingUserOnlyOidcUserService service() {
        return new ExistingUserOnlyOidcUserService(
                new ExistingUserOAuthAccountResolver(appUserRepository),
                delegate
        );
    }

    private OidcUserRequest userRequest(String registrationId, String subject) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://accounts.example.com/o/oauth2/v2/auth")
                .tokenUri("https://accounts.example.com/oauth2/token")
                .jwkSetUri("https://accounts.example.com/oauth2/v3/certs")
                .userInfoUri("https://accounts.example.com/oauth2/v3/userinfo")
                .userNameAttributeName(IdTokenClaimNames.SUB)
                .scope("openid", "profile", "email")
                .clientName(registrationId)
                .build();

        Instant issuedAt = Instant.now();
        OidcIdToken idToken = new OidcIdToken(
                "id-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Map.of(
                        IdTokenClaimNames.SUB, subject,
                        IdTokenClaimNames.ISS, "https://accounts.example.com",
                        IdTokenClaimNames.AUD, Set.of("client-id")
                )
        );
        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                issuedAt,
                issuedAt.plusSeconds(3600),
                Set.of("openid", "profile", "email")
        );
        return new OidcUserRequest(clientRegistration, accessToken, idToken);
    }

    private OidcUser oidcUser(OidcUserRequest request, String subject, Map<String, Object> userInfoClaims) {
        Map<String, Object> claims = new java.util.HashMap<>(userInfoClaims);
        claims.put(IdTokenClaimNames.SUB, subject);
        return new DefaultOidcUser(
                Set.of(),
                request.getIdToken(),
                new OidcUserInfo(claims),
                IdTokenClaimNames.SUB
        );
    }
}
