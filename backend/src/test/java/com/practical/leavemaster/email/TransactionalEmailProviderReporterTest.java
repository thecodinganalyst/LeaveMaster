package com.practical.leavemaster.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TransactionalEmailProviderReporterTest {

    @Test
    void shouldAcceptSupportedProviders() {
        assertThatNoException().isThrownBy(() -> new TransactionalEmailProviderReporter("resend").reportProvider());
        assertThatNoException().isThrownBy(() -> new TransactionalEmailProviderReporter(" DISABLED ").reportProvider());
    }

    @Test
    void shouldRejectUnsupportedProviderAtStartup() {
        assertThatThrownBy(() -> new TransactionalEmailProviderReporter("unknown").reportProvider())
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Unsupported transactional email provider")
                .hasMessageContaining("unknown");
    }
}
