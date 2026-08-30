package com.practical.leavemaster.email;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;

class TransactionalEmailConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(RestClient.Builder.class, RestClient::builder)
            .withUserConfiguration(SenderConfiguration.class);

    @Test
    void shouldActivateResendSenderWhenProviderIsResend() {
        contextRunner
                .withPropertyValues(
                        "app.email.provider=resend",
                        "app.email.resend.api-key=test-api-key",
                        "app.email.from-address=onboarding@resend.dev")
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionalEmailSender.class);
                    assertThat(context.getBean(TransactionalEmailSender.class))
                            .isInstanceOf(ResendTransactionalEmailSender.class);
                });
    }

    @Test
    void shouldUseDisabledSenderOnlyWhenProviderIsDisabled() {
        contextRunner
                .withPropertyValues("app.email.provider=disabled")
                .run(context -> {
                    assertThat(context).hasSingleBean(TransactionalEmailSender.class);
                    assertThat(context.getBean(TransactionalEmailSender.class))
                            .isInstanceOf(DisabledTransactionalEmailSender.class);
                });
    }

    @Test
    void shouldFailStartupWhenResendHasNoApiKey() {
        contextRunner
                .withPropertyValues("app.email.provider=resend")
                .run(context -> {
                    assertThat(context).hasFailed();
                    assertThat(context.getStartupFailure())
                            .hasRootCauseInstanceOf(IllegalStateException.class)
                            .hasMessageContaining("RESEND_API_KEY");
                });
    }

    @Configuration(proxyBeanMethods = false)
    @Import({ResendTransactionalEmailSender.class, DisabledTransactionalEmailSender.class})
    static class SenderConfiguration {
    }
}
