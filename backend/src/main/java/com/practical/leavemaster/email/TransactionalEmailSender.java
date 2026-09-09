package com.practical.leavemaster.email;

public interface TransactionalEmailSender {

    void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes);

    void sendPasswordResetPin(String recipient, String displayName, String pin, int expiryMinutes);

    void sendContactEnquiryReply(String recipient, String contactName, String originalMessage, String replyBody);
}
