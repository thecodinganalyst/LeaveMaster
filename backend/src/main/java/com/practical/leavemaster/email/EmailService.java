package com.practical.leavemaster.email;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    private final TransactionalEmailSender transactionalEmailSender;

    public EmailService() {
        this.transactionalEmailSender = null;
    }

    @Autowired
    public EmailService(TransactionalEmailSender transactionalEmailSender) {
        this.transactionalEmailSender = transactionalEmailSender;
    }

    public void sendCancellationRequestNotification(LeaveApplication application, String approverEmail) {
        if (approverEmail == null || approverEmail.isBlank()) {
            log.warn("Approver email is not set for staff {}; skipping email notification",
                    application.getStaff().getId());
            return;
        }
        logPlaceholderNotification(
                approverEmail,
                "Leave Cancellation Request - " + application.getStaff().getName(),
                String.format(
                        "%s requested cancellation of leave application %s for %s (%s)",
                        application.getStaff().getName(),
                        application.getId(),
                        application.getLeaveDate(),
                        application.getLeaveDuration()));
    }

    public void sendLeaveApprovalNotification(LeaveApplication application) {
        sendLeaveDecisionNotification(application, "approved", "Leave Application Approved");
    }

    public void sendLeaveRejectionNotification(LeaveApplication application) {
        sendLeaveDecisionNotification(application, "rejected", "Leave Application Rejected");
    }

    public void sendAccountActivationPin(String recipient, String staffName, String pin, int expiryMinutes) {
        if (recipient == null || recipient.isBlank()) {
            throw new IllegalArgumentException("Activation email recipient must not be blank");
        }
        if (transactionalEmailSender == null) {
            throw new EmailDeliveryException("Transactional email provider is not configured");
        }
        transactionalEmailSender.sendAccountActivationPin(recipient, staffName, pin, expiryMinutes);
    }

    private void sendLeaveDecisionNotification(LeaveApplication application, String decision, String subject) {
        String requesterEmail = application.getStaff().getEmail();
        if (requesterEmail == null || requesterEmail.isBlank()) {
            log.warn("Requester email is not set for staff {}; skipping email notification",
                    application.getStaff().getId());
            return;
        }
        logPlaceholderNotification(
                requesterEmail,
                subject,
                String.format(
                        "Leave application %s for %s (%s) was %s",
                        application.getId(),
                        application.getLeaveDate(),
                        application.getLeaveDuration(),
                        decision));
    }

    private void logPlaceholderNotification(String recipient, String subject, String message) {
        log.info("Email placeholder invoked for recipient {} with subject '{}': {}",
                recipient, subject, message);
    }
}
