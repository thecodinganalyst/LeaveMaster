package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUserRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.OAuth2User;

import java.util.Map;

public class ExistingUserOnlyOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private final AppUserRepository appUserRepository;
    private final OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate;

    public ExistingUserOnlyOAuth2UserService(AppUserRepository appUserRepository) {
        this(appUserRepository, new DefaultOAuth2UserService());
    }

    ExistingUserOnlyOAuth2UserService(
            AppUserRepository appUserRepository,
            OAuth2UserService<OAuth2UserRequest, OAuth2User> delegate
    ) {
        this.appUserRepository = appUserRepository;
        this.delegate = delegate;
    }

    @Override
    public OAuth2User loadUser(OAuth2UserRequest userRequest) throws OAuth2AuthenticationException {
        OAuth2User oauth2User = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String subject = resolveSubject(oauth2User.getAttributes());

        appUserRepository.findByOidcProviderAndOidcSubject(provider, subject)
                .filter(appUser -> appUser.isActive())
                .orElseThrow(() -> new OAuth2AuthenticationException(
                        new OAuth2Error("access_denied"),
                        "OIDC login is only allowed for existing active users"
                ));

        return oauth2User;
    }

    private String resolveSubject(Map<String, Object> attributes) {
        Object subject = attributes.get("sub");
        if (subject == null) {
            subject = attributes.get("id");
        }
        if (subject == null) {
            throw new OAuth2AuthenticationException(
                    new OAuth2Error("invalid_request"),
                    "OIDC subject is missing from provider response"
            );
        }
        return subject.toString();
    }
}
