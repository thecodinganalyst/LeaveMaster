package com.practical.leavemaster.email;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.staff.Staff;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock
    private JavaMailSender mailSender;

    @InjectMocks
    private EmailService emailService;

    private Staff staffWithEmail(String id, String name, String email) {
        return Staff.builder()
                .id(id)
                .name(name)
                .email(email)
                .joinDate(LocalDate.of(2023, 1, 1))
                .build();
    }

    private LeaveApplication application(Staff staff) {
        return LeaveApplication.builder()
                .id("LA001")
                .staff(staff)
                .leaveDate(LocalDate.of(2026, 8, 1))
                .leaveDuration(LeaveDuration.FULL)
                .status(LeaveStatus.APPROVED)
                .applicationDate(LocalDate.of(2026, 7, 1))
                .build();
    }

    @Test
    void shouldSendCancellationRequestNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendCancellationRequestNotification(app, "approver@example.com");

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("approver@example.com");
        assertThat(message.getSubject()).contains("Alice Smith");
        assertThat(message.getText()).contains("Alice Smith");
        assertThat(message.getText()).contains("LA001");
    }

    @Test
    void shouldSkipCancellationNotificationWhenApproverEmailIsNull() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendCancellationRequestNotification(app, null);

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldSkipCancellationNotificationWhenApproverEmailIsBlank() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendCancellationRequestNotification(app, "  ");

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldLogErrorAndContinueWhenCancellationEmailFails() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);
        doThrow(new RuntimeException("SMTP failure")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendCancellationRequestNotification(app, "approver@example.com");

        verify(mailSender).send(any(SimpleMailMessage.class));
    }

    @Test
    void shouldSendLeaveApprovalNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendLeaveApprovalNotification(app);

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).contains("Approved");
        assertThat(message.getText()).contains("approved");
        assertThat(message.getText()).contains("LA001");
    }

    @Test
    void shouldSendLeaveRejectionNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);
        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);

        emailService.sendLeaveRejectionNotification(app);

        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getTo()).containsExactly("alice@example.com");
        assertThat(message.getSubject()).contains("Rejected");
        assertThat(message.getText()).contains("rejected");
    }

    @Test
    void shouldSkipApprovalNotificationWhenRequesterEmailIsNull() {
        Staff staff = staffWithEmail("S001", "Alice Smith", null);
        LeaveApplication app = application(staff);

        emailService.sendLeaveApprovalNotification(app);

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldSkipApprovalNotificationWhenRequesterEmailIsBlank() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "");
        LeaveApplication app = application(staff);

        emailService.sendLeaveApprovalNotification(app);

        verifyNoInteractions(mailSender);
    }

    @Test
    void shouldLogErrorAndContinueWhenApprovalEmailFails() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);
        doThrow(new RuntimeException("SMTP failure")).when(mailSender).send(any(SimpleMailMessage.class));

        emailService.sendLeaveApprovalNotification(app);

        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
