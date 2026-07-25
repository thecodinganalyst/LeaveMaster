package com.practical.leavemaster.email;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendCancellationRequestNotification(LeaveApplication application, String approverEmail) {
        if (approverEmail == null || approverEmail.isBlank()) {
            log.warn("Approver email is not set for staff {}; skipping email notification",
                    application.getStaff().getId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(approverEmail);
        message.setSubject("Leave Cancellation Request - " + application.getStaff().getName());
        message.setText(String.format(
                "Dear Approver,%n%n" +
                "%s has requested cancellation of their approved leave on %s (%s).%n%n" +
                "Please review and approve or reject this cancellation request.%n%n" +
                "Leave Application ID: %s",
                application.getStaff().getName(),
                application.getLeaveDate(),
                application.getLeaveDuration(),
                application.getId()));
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send cancellation request email to {}: {}", approverEmail, e.getMessage());
        }
    }

    public void sendLeaveApprovalNotification(LeaveApplication application) {
        sendLeaveDecisionNotification(application, "approved", "Leave Application Approved");
    }

    public void sendLeaveRejectionNotification(LeaveApplication application) {
        sendLeaveDecisionNotification(application, "rejected", "Leave Application Rejected");
    }

    private void sendLeaveDecisionNotification(LeaveApplication application, String decision, String subject) {
        String requesterEmail = application.getStaff().getEmail();
        if (requesterEmail == null || requesterEmail.isBlank()) {
            log.warn("Requester email is not set for staff {}; skipping email notification",
                    application.getStaff().getId());
            return;
        }
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(requesterEmail);
        message.setSubject(subject);
        message.setText(String.format(
                "Dear %s,%n%n" +
                "Your leave application for %s (%s) has been %s.%n%n" +
                "Leave Application ID: %s",
                application.getStaff().getName(),
                application.getLeaveDate(),
                application.getLeaveDuration(),
                decision,
                application.getId()));
        try {
            mailSender.send(message);
        } catch (Exception e) {
            log.error("Failed to send leave decision email to {}: {}", requesterEmail, e.getMessage());
        }
    }
}
