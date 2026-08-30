package com.practical.leavemaster.config;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpSession;

import static org.assertj.core.api.Assertions.assertThat;

class OAuthLinkingContextTest {

    @Test
    void linkFlowSurvivesLinkRequestConsumptionUntilRedirectHandlerConsumesIt() {
        MockHttpSession session = new MockHttpSession();
        OAuthLinkingContext.create(session, "user-1", "google");

        assertThat(OAuthLinkingContext.consume(session, "google"))
            .contains(new OAuthLinkingContext.LinkRequest("user-1", "google"));
        assertThat(OAuthLinkingContext.consumeLinkFlow(session)).isTrue();
        assertThat(OAuthLinkingContext.consumeLinkFlow(session)).isFalse();
    }

    @Test
    void clearRemovesBothRequestAndLinkFlowState() {
        MockHttpSession session = new MockHttpSession();
        OAuthLinkingContext.create(session, "user-1", "github");

        OAuthLinkingContext.clear(session);

        assertThat(OAuthLinkingContext.hasContext(session)).isFalse();
        assertThat(OAuthLinkingContext.consumeLinkFlow(session)).isFalse();
    }

    @Test
    void consumeLinkFlowHandlesMissingSession() {
        assertThat(OAuthLinkingContext.consumeLinkFlow(null)).isFalse();
    }
}
