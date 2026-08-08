package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
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

import java.time.Instant;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingUserOnlyOAuth2UserServiceTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    @Test
    void shouldAllowOidcLoginForExistingActiveMappedUser() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "12345"),
                "id"
        );
        AppUser appUser = AppUser.builder()
                .loginName("alice")
                .password("encoded")
                .active(true)
                .oidcProvider("github")
                .oidcSubject("12345")
                .build();
        when(delegate.loadUser(request)).thenReturn(oauth2User);
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.of(appUser));

        service.loadUser(request);
    }

    @Test
    void shouldRejectOidcLoginForMissingMapping() {
        ExistingUserOnlyOAuth2UserService service = new ExistingUserOnlyOAuth2UserService(appUserRepository, delegate);
        OAuth2UserRequest request = userRequest("github");
        OAuth2User oauth2User = new DefaultOAuth2User(
                Set.of(new SimpleGrantedAuthority("ROLE_USER")),
                Map.of("id", "12345"),
                "id"
        );
        when(delegate.loadUser(request)).thenReturn(oauth2User);
        when(appUserRepository.findByOidcProviderAndOidcSubject("github", "12345")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUser(request))
                .isInstanceOf(OAuth2AuthenticationException.class)
                .hasMessageContaining("only allowed for existing active users");
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
