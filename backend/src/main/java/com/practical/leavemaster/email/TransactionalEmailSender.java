package com.practical.leavemaster.email;

public interface TransactionalEmailSender {

    void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes);
}
