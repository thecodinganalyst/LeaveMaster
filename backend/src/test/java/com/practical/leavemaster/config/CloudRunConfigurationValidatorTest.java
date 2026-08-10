package com.practical.leavemaster.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CloudRunConfigurationValidatorTest {

    private static final DefaultApplicationArguments NO_ARGS = new DefaultApplicationArguments(new String[0]);

    @Test
    void shouldAcceptSecureProductionConfiguration() throws Exception {
        new CloudRunConfigurationValidator(
            "https://leavemaster-production.firebaseapp.com",
            "https://leavemaster-production.firebaseapp.com,https://leavemaster-production.web.app",
            true,
            "test-api-key"
        ).run(NO_ARGS);
    }

    @Test
    void shouldRejectWildcardCorsOrigin() {
        CloudRunConfigurationValidator validator = new CloudRunConfigurationValidator(
            "https://leavemaster-production.firebaseapp.com",
            "https://*.example.com",
            false,
            ""
        );

        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("wildcard");
    }

    @Test
    void shouldRequireOpenAiKeyWhenAssistantEnabled() {
        CloudRunConfigurationValidator validator = new CloudRunConfigurationValidator(
            "https://leavemaster-production.firebaseapp.com",
            "https://leavemaster-production.firebaseapp.com",
            true,
            ""
        );

        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("OPENAI_API_KEY");
    }

    @Test
    void shouldRejectNonHttpsPublicUrl() {
        CloudRunConfigurationValidator validator = new CloudRunConfigurationValidator(
            "http://example.com",
            "",
            false,
            ""
        );

        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HTTPS origin");
    }
}
