package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingUserOnlyOAuth2UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldAllowOauthLoginForExistingActiveMappedUserAndUseLeaveMasterUserIdAsPrincipal() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        OAuth2User oauth2User = oauthUser(Map.of("id", "12345"));
        AppUser appUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .password("encoded")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("12345")
                .build();
        when(delegate.loadUser(request)).thenReturn(oauth2User);
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.of(appUser));

        OAuth2User result = service.loadUser(request);

        assertThat(result.getName()).isEqualTo("user-1");
    }

    @Test
    void shouldRejectUnlinkedOauthLoginWithoutLinkingContext() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        when(delegate.loadUser(request)).thenReturn(oauthUser(Map.of("id", "12345")));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo("not_linked"));
    }

    @Test
    void shouldLinkUnmappedGithubIdentityUsingSingleUseVerifiedSessionContext() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        when(delegate.loadUser(request)).thenReturn(oauthUser(Map.of("id", "12345")));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());

        AppUser appUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .tenantId("tenant-a")
                .active(true)
                .build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(appUser));
        when(appUserRepository.saveAndFlush(appUser)).thenReturn(appUser);

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        OAuthLinkingContext.create(servletRequest.getSession(), "user-1", "github");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        OAuth2User result = service.loadUser(request);

        assertThat(result.getName()).isEqualTo("user-1");
        assertThat(appUser.getOidcProvider()).isEqualTo("github");
        assertThat(appUser.getOidcSubject()).isEqualTo("12345");
        verify(appUserRepository).saveAndFlush(appUser);
        assertThat(OAuthLinkingContext.consume(servletRequest.getSession(), "github")).isEmpty();
    }

    @Test
    void shouldRejectLinkWhenLeaveMasterAccountAlreadyHasProvider() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("google");
        when(delegate.loadUser(request)).thenReturn(oauthUser(Map.of("sub", "google-1")));
        when(appUserRepository.findByOidcProviderAndOidcSubject("google", "google-1")).thenReturn(Optional.empty());

        AppUser appUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .tenantId("tenant-a")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("github-1")
                .build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(appUser));

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        OAuthLinkingContext.create(servletRequest.getSession(), "user-1", "google");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo("already_linked"));
    }

    @Test
    void shouldRejectProviderMismatchAndConsumeLinkingContext() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        when(delegate.loadUser(request)).thenReturn(oauthUser(Map.of("id", "12345")));
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());

        MockHttpServletRequest servletRequest = new MockHttpServletRequest();
        OAuthLinkingContext.create(servletRequest.getSession(), "user-1", "google");
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(servletRequest));

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo("link_context_invalid"));
        assertThat(OAuthLinkingContext.consume(servletRequest.getSession(), "google")).isEmpty();
    }

    private OAuth2User oauthUser(Map<String, Object> attributes) {
        String key = attributes.containsKey("sub") ? "sub" : "id";
        return new DefaultOAuth2User(Set.of(new SimpleGrantedAuthority("ROLE_USER")), attributes, key);
    }

    private OAuth2UserRequest userRequest(String registrationId) {
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId(registrationId)
                .clientId("client-id")
                .clientSecret("client-secret")
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .redirectUri("{baseUrl}/login/oauth2/code/{registrationId}")
                .authorizationUri("https://example.com/oauth2/authorize")
                .tokenUri("https://example.com/oauth2/token")
                .userInfoUri("https://example.com/userinfo")
                .userNameAttributeName("id")
                .scope("openid")
                .clientName(registrationId)
                .build();

        OAuth2AccessToken accessToken = new OAuth2AccessToken(
                OAuth2AccessToken.TokenType.BEARER,
                "access-token",
                Instant.now(),
                Instant.now().plusSeconds(3600),
                Set.of("openid")
        );

        return new OAuth2UserRequest(clientRegistration, accessToken, Map.of(OAuth2ParameterNames.REGISTRATION_ID, registrationId));
    }
}
