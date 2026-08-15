package com.practical.leavemaster.customerenquiry;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class CustomerEnquiryNotificationService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final String recipient;
    private final String from;

    public CustomerEnquiryNotificationService(
            ObjectProvider<JavaMailSender> mailSenderProvider,
            @Value("${app.customer-enquiry.notification-recipient:}") String recipient,
            @Value("${app.customer-enquiry.notification-from:noreply@leavemaestro.com}") String from) {
        this.mailSenderProvider = mailSenderProvider;
        this.recipient = recipient;
        this.from = from;
    }

    public void notifyNewEnquiry(CustomerEnquiry enquiry) {
        if (recipient == null || recipient.isBlank()) {
            log.info("Customer enquiry {} persisted; notification recipient is not configured", enquiry.getId());
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Customer enquiry {} persisted; mail transport is not configured", enquiry.getId());
            return;
        }

        SimpleMailMessage email = new SimpleMailMessage();
        email.setFrom(from);
        email.setTo(recipient);
        email.setReplyTo(enquiry.getEmail());
        email.setSubject("New LeaveMaestro enquiry: " + enquiry.getEnquiryType());
        email.setText("A new customer enquiry was received.\n\n"
                + "Reference: " + enquiry.getId() + "\n"
                + "Name: " + singleLine(enquiry.getName()) + "\n"
                + "Company: " + singleLine(enquiry.getCompany()) + "\n"
                + "Email: " + singleLine(enquiry.getEmail()) + "\n"
                + "Type: " + enquiry.getEnquiryType() + "\n\n"
                + "Review the persisted enquiry before responding.");
        mailSender.send(email);
    }

    private String singleLine(String value) {
        return value == null ? "" : value.replace('\r', ' ').replace('\n', ' ');
    }
}
