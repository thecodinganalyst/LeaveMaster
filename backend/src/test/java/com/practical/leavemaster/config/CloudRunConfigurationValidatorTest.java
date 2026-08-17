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
            "https://leavemaster-production.firebaseapp.com,https://leavemaster-production.web.app"
        ).run(NO_ARGS);
    }

    @Test
    void shouldRejectWildcardCorsOrigin() {
        CloudRunConfigurationValidator validator = new CloudRunConfigurationValidator(
            "https://leavemaster-production.firebaseapp.com",
            "https://*.example.com"
        );

        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("wildcard");
    }

    @Test
    void shouldRejectNonHttpsPublicUrl() {
        CloudRunConfigurationValidator validator = new CloudRunConfigurationValidator(
            "http://example.com",
            ""
        );

        assertThatThrownBy(() -> validator.run(NO_ARGS))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("HTTPS origin");
    }
}
