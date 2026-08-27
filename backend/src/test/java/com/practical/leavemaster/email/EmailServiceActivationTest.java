package com.practical.leavemaster.email;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class EmailServiceActivationTest {

    @Test
    void shouldDelegateAccountActivationMailToConfiguredProvider() {
        TransactionalEmailSender sender = mock(TransactionalEmailSender.class);
        EmailService service = new EmailService(sender);

        service.sendAccountActivationPin("alice@example.com", "Alice", "123456", 15);

        verify(sender).sendAccountActivationPin("alice@example.com", "Alice", "123456", 15);
    }

    @Test
    void shouldRejectBlankActivationRecipientBeforeProviderCall() {
        TransactionalEmailSender sender = mock(TransactionalEmailSender.class);
        EmailService service = new EmailService(sender);

        assertThatThrownBy(() -> service.sendAccountActivationPin(" ", "Alice", "123456", 15))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("recipient");
    }

    @Test
    void shouldFailSafelyWhenProviderIsNotConfigured() {
        EmailService service = new EmailService();

        assertThatThrownBy(() -> service.sendAccountActivationPin("alice@example.com", "Alice", "123456", 15))
                .isInstanceOf(EmailDeliveryException.class)
                .hasMessageNotContaining("123456");
    }
}
