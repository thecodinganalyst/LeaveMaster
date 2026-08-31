package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserRequest;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.oidc.OidcUserInfo;
import org.springframework.security.oauth2.core.oidc.user.DefaultOidcUser;
import org.springframework.security.oauth2.core.oidc.user.OidcUser;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExistingUserOnlyOidcUserService implements OAuth2UserService<OidcUserRequest, OidcUser> {

    static final String USER_ID_ATTRIBUTE = "leavemaster_user_id";

    private final ExistingUserOAuthAccountResolver accountResolver;
    private final OAuth2UserService<OidcUserRequest, OidcUser> delegate;

    public ExistingUserOnlyOidcUserService(AppUserRepository appUserRepository) {
        this(new ExistingUserOAuthAccountResolver(appUserRepository), new OidcUserService());
    }

    ExistingUserOnlyOidcUserService(
            ExistingUserOAuthAccountResolver accountResolver,
            OAuth2UserService<OidcUserRequest, OidcUser> delegate
    ) {
        this.accountResolver = accountResolver;
        this.delegate = delegate;
    }

    @Override
    public OidcUser loadUser(OidcUserRequest userRequest) throws OAuth2AuthenticationException {
        OidcUser oidcUser = delegate.loadUser(userRequest);
        String provider = userRequest.getClientRegistration().getRegistrationId();
        String subject = oidcUser.getSubject();
        if (subject == null || subject.isBlank()) {
            throw oauthError("invalid_request", "OIDC subject is missing from provider response");
        }

        AppUser appUser = accountResolver.resolve(provider, subject);
        return toLeaveMasterPrincipal(appUser, oidcUser);
    }

    private OidcUser toLeaveMasterPrincipal(AppUser appUser, OidcUser oidcUser) {
        Map<String, Object> userInfoClaims = new HashMap<>();
        if (oidcUser.getUserInfo() != null) {
            userInfoClaims.putAll(oidcUser.getUserInfo().getClaims());
        }
        userInfoClaims.put(USER_ID_ATTRIBUTE, appUser.getUserId());

        Set<GrantedAuthority> authorities = appUser.getRoles().stream()
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> permission != null && permission.getCode() != null)
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());

        return new DefaultOidcUser(
                authorities,
                oidcUser.getIdToken(),
                new OidcUserInfo(userInfoClaims),
                USER_ID_ATTRIBUTE
        );
    }

    private OAuth2AuthenticationException oauthError(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
