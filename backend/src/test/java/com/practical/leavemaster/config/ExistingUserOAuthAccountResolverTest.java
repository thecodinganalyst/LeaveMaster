package com.practical.leavemaster.config;

import com.practical.leavemaster.user.AppUser;
import com.practical.leavemaster.user.AppUserRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExistingUserOAuthAccountResolverTest {

    @Mock
    private AppUserRepository appUserRepository;

    @AfterEach
    void tearDown() {
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void shouldRejectUnsupportedProvider() {
        ExistingUserOAuthAccountResolver resolver = resolverWithUnmappedIdentity("microsoft", "subject-1");

        assertOauthError(() -> resolver.resolve("microsoft", "subject-1"), "not_linked");
    }

    @Test
    void shouldRejectUnlinkedIdentityWhenRequestHasNoSession() {
        ExistingUserOAuthAccountResolver resolver = resolverWithUnmappedIdentity("google", "subject-1");
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        assertOauthError(() -> resolver.resolve("google", "subject-1"), "not_linked");
    }

    @Test
    void shouldRejectIdentityAlreadyLinkedToAnotherUser() {
        ExistingUserOAuthAccountResolver resolver = new ExistingUserOAuthAccountResolver(appUserRepository);
        AppUser linkingUser = activeUser("user-1");
        AppUser otherUser = activeUser("user-2");
        when(appUserRepository.findByOidcProviderAndOidcSubject("google", "subject-1"))
                .thenReturn(Optional.empty(), Optional.of(otherUser));
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(linkingUser));
        bindLinkingSession("user-1", "google");

        assertOauthError(() -> resolver.resolve("google", "subject-1"), "identity_in_use");
    }

    @Test
    void shouldTranslateUniqueConstraintRaceToIdentityInUse() {
        ExistingUserOAuthAccountResolver resolver = resolverWithUnmappedIdentity("google", "subject-1");
        AppUser linkingUser = activeUser("user-1");
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(linkingUser));
        when(appUserRepository.saveAndFlush(linkingUser))
                .thenThrow(new DataIntegrityViolationException("duplicate oauth identity"));
        bindLinkingSession("user-1", "google");

        assertOauthError(() -> resolver.resolve("google", "subject-1"), "identity_in_use");
    }

    @Test
    void shouldRejectInactiveLinkingAccount() {
        ExistingUserOAuthAccountResolver resolver = resolverWithUnmappedIdentity("google", "subject-1");
        AppUser inactiveUser = AppUser.builder()
                .userId("user-1")
                .loginName("alice")
                .active(false)
                .build();
        when(appUserRepository.findById("user-1")).thenReturn(Optional.of(inactiveUser));
        bindLinkingSession("user-1", "google");

        assertOauthError(() -> resolver.resolve("google", "subject-1"), "account_inactive");
    }

    private ExistingUserOAuthAccountResolver resolverWithUnmappedIdentity(String provider, String subject) {
        when(appUserRepository.findByOidcProviderAndOidcSubject(provider, subject)).thenReturn(Optional.empty());
        return new ExistingUserOAuthAccountResolver(appUserRepository);
    }

    private AppUser activeUser(String userId) {
        return AppUser.builder()
                .userId(userId)
                .loginName(userId)
                .tenantId("tenant-a")
                .active(true)
                .build();
    }

    private void bindLinkingSession(String userId, String provider) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        OAuthLinkingContext.create(request.getSession(), userId, provider);
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
    }

    private void assertOauthError(org.assertj.core.api.ThrowableAssert.ThrowingCallable callable, String errorCode) {
        assertThatThrownBy(callable)
                .isInstanceOf(OAuth2AuthenticationException.class)
                .satisfies(exception -> assertThat(((OAuth2AuthenticationException) exception).getError().getErrorCode())
                        .isEqualTo(errorCode));
    }
}
