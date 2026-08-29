package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserService;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class ExistingUserOnlyOAuth2UserService implements OAuth2UserService<OAuth2UserRequest, OAuth2User> {

    private static final Set<String> LINKABLE_PROVIDERS = Set.of("google", "github");
    private static final String USER_ID_ATTRIBUTE = "leavemaster_user_id";

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

        AppUser appUser = appUserRepository.findByOidcProviderAndOidcSubject(provider, subject)
                .filter(AppUser::isActive)
                .orElseGet(() -> linkFromCurrentSession(provider, subject));

        return toLeaveMasterPrincipal(appUser, oauth2User);
    }

    private AppUser linkFromCurrentSession(String provider, String subject) {
        if (!LINKABLE_PROVIDERS.contains(provider)) {
            throw oauthError("not_linked", "OAuth login is only allowed for existing active users");
        }

        HttpSession session = currentSession();
        OAuthLinkingContext.LinkRequest linkRequest = OAuthLinkingContext.consume(session, provider)
                .orElseThrow(() -> oauthError("not_linked", "OAuth account is not linked"));

        AppUser appUser = appUserRepository.findById(linkRequest.userId())
                .filter(AppUser::isActive)
                .orElseThrow(() -> oauthError("account_inactive", "LeaveMaster account is inactive"));

        if (appUser.getOidcProvider() != null || appUser.getOidcSubject() != null) {
            throw oauthError("already_linked", "LeaveMaster account already has an OAuth provider linked");
        }

        if (appUserRepository.findByOidcProviderAndOidcSubject(provider, subject).isPresent()) {
            throw oauthError("identity_in_use", "OAuth account is already linked");
        }

        appUser.setOidcProvider(provider);
        appUser.setOidcSubject(subject);
        try {
            return appUserRepository.saveAndFlush(appUser);
        } catch (DataIntegrityViolationException exception) {
            throw oauthError("identity_in_use", "OAuth account is already linked");
        }
    }

    private HttpSession currentSession() {
        if (!(RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes)) {
            throw oauthError("link_context_invalid", "OAuth linking session is unavailable");
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw oauthError("link_context_invalid", "OAuth linking session is unavailable");
        }
        return session;
    }

    private OAuth2User toLeaveMasterPrincipal(AppUser appUser, OAuth2User oauth2User) {
        Map<String, Object> attributes = new HashMap<>(oauth2User.getAttributes());
        attributes.put(USER_ID_ATTRIBUTE, appUser.getUserId());
        Set<GrantedAuthority> authorities = appUser.getRoles().stream()
                .filter(role -> role != null && role.isActive())
                .flatMap(role -> role.getPermissions().stream())
                .filter(permission -> permission != null && permission.getCode() != null)
                .map(permission -> (GrantedAuthority) new SimpleGrantedAuthority(permission.getCode()))
                .collect(Collectors.toSet());
        return new DefaultOAuth2User(authorities, attributes, USER_ID_ATTRIBUTE);
    }

    private String resolveSubject(Map<String, Object> attributes) {
        Object subject = attributes.get("sub");
        if (subject == null) {
            subject = attributes.get("id");
        }
        if (subject == null) {
            throw oauthError("invalid_request", "OIDC subject is missing from provider response");
        }
        return subject.toString();
    }

    private OAuth2AuthenticationException oauthError(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
