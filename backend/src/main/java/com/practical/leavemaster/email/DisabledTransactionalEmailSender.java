package com.practical.leavemaster.email;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnMissingBean(TransactionalEmailSender.class)
public class DisabledTransactionalEmailSender implements TransactionalEmailSender {

    @Override
    public void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes) {
        throw new EmailDeliveryException("Transactional email provider is not configured");
    }
}
