package com.practical.leavemaster.config;

import jakarta.servlet.http.HttpSession;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

final class OAuthLinkingContext {

    static final String USER_ID_ATTRIBUTE = OAuthLinkingContext.class.getName() + ".userId";
    static final String PROVIDER_ATTRIBUTE = OAuthLinkingContext.class.getName() + ".provider";
    static final String EXPIRES_AT_ATTRIBUTE = OAuthLinkingContext.class.getName() + ".expiresAt";
    static final Duration TTL = Duration.ofMinutes(10);

    private OAuthLinkingContext() {
    }

    static void create(HttpSession session, String userId, String provider) {
        session.setAttribute(USER_ID_ATTRIBUTE, userId);
        session.setAttribute(PROVIDER_ATTRIBUTE, provider);
        session.setAttribute(EXPIRES_AT_ATTRIBUTE, Instant.now().plus(TTL).toEpochMilli());
    }

    static Optional<LinkRequest> consume(HttpSession session, String provider) {
        Object userId = session.getAttribute(USER_ID_ATTRIBUTE);
        Object expectedProvider = session.getAttribute(PROVIDER_ATTRIBUTE);
        Object expiresAt = session.getAttribute(EXPIRES_AT_ATTRIBUTE);
        clear(session);

        if (!(userId instanceof String userIdValue)
                || !(expectedProvider instanceof String providerValue)
                || !(expiresAt instanceof Long expiresAtValue)
                || !providerValue.equals(provider)
                || Instant.now().toEpochMilli() > expiresAtValue) {
            return Optional.empty();
        }

        return Optional.of(new LinkRequest(userIdValue, providerValue));
    }

    static void clear(HttpSession session) {
        session.removeAttribute(USER_ID_ATTRIBUTE);
        session.removeAttribute(PROVIDER_ATTRIBUTE);
        session.removeAttribute(EXPIRES_AT_ATTRIBUTE);
    }

    record LinkRequest(String userId, String provider) {
    }
}
