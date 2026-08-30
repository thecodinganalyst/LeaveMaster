package com.practical.leavemaster.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OAuthProductionConfigurationValidatorTest {

    private static final DefaultApplicationArguments NO_ARGS = new DefaultApplicationArguments(new String[0]);

    @Test
    void shouldAcceptConfiguredProductionOAuthCredentials() {
        OAuthProductionConfigurationValidator validator = new OAuthProductionConfigurationValidator(
                "github-client-id",
                "github-client-secret",
                "google-client-id",
                "google-client-secret");

        assertThatCode(() -> validator.run(NO_ARGS)).doesNotThrowAnyException();
    }

    @Test
    void shouldRejectPlaceholderGithubCredentials() {
        OAuthProductionConfigurationValidator validator = new OAuthProductionConfigurationValidator(
                "replace-me",
                "replace-me",
                "google-client-id",
                "google-client-secret");

        assertThatThrownBy(() -> validator.run(NO_ARGS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GitHub OAuth is not configured")
                .hasMessageContaining("GH_CLIENT_ID")
                .hasMessageContaining("GH_CLIENT_SECRET")
                .hasMessageNotContaining("github-client-secret");
    }

    @Test
    void shouldRejectBlankGoogleCredentials() {
        OAuthProductionConfigurationValidator validator = new OAuthProductionConfigurationValidator(
                "github-client-id",
                "github-client-secret",
                " ",
                "");

        assertThatThrownBy(() -> validator.run(NO_ARGS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Google OAuth is not configured")
                .hasMessageContaining("GOOGLE_CLIENT_ID")
                .hasMessageContaining("GOOGLE_CLIENT_SECRET");
    }
}
