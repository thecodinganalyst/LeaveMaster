package com.practical.leavemaster.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "app.email.provider", havingValue = "disabled", matchIfMissing = true)
public class DisabledTransactionalEmailSender implements TransactionalEmailSender {

    @Override
    public void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes) {
        throw new EmailDeliveryException("Transactional email provider is not configured");
    }

    @Override
    public void sendContactEnquiryReply(String recipient, String contactName, String originalMessage, String replyBody) {
        throw new EmailDeliveryException("Transactional email provider is not configured");
    }
}
