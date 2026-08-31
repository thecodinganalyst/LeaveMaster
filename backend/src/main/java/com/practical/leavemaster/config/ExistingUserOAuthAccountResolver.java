package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Set;

final class ExistingUserOAuthAccountResolver {

    private static final Set<String> LINKABLE_PROVIDERS = Set.of("google", "github");

    private final AppUserRepository appUserRepository;

    ExistingUserOAuthAccountResolver(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    AppUser resolve(String provider, String subject) {
        return appUserRepository.findByOidcProviderAndOidcSubject(provider, subject)
                .filter(AppUser::isActive)
                .orElseGet(() -> linkFromCurrentSession(provider, subject));
    }

    private AppUser linkFromCurrentSession(String provider, String subject) {
        if (!LINKABLE_PROVIDERS.contains(provider)) {
            throw oauthError("not_linked", "OAuth login is only allowed for existing active users");
        }

        HttpSession session = currentSession();
        if (!OAuthLinkingContext.hasContext(session)) {
            throw oauthError("not_linked", "OAuth account is not linked");
        }

        OAuthLinkingContext.LinkRequest linkRequest = OAuthLinkingContext.consume(session, provider)
                .orElseThrow(() -> oauthError("link_context_invalid", "OAuth linking session is invalid or expired"));

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
            throw oauthError("not_linked", "OAuth account is not linked");
        }
        HttpServletRequest request = attributes.getRequest();
        HttpSession session = request.getSession(false);
        if (session == null) {
            throw oauthError("not_linked", "OAuth account is not linked");
        }
        return session;
    }

    private OAuth2AuthenticationException oauthError(String code, String message) {
        return new OAuth2AuthenticationException(new OAuth2Error(code), message);
    }
}
