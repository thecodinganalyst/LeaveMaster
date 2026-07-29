package com.practical.leavemaster.email;

import com.practical.leavemaster.leaveapplication.LeaveApplication;
import com.practical.leavemaster.leaveapplication.LeaveDuration;
import com.practical.leavemaster.leaveapplication.LeaveStatus;
import com.practical.leavemaster.staff.Staff;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

class EmailServiceTest {

    private final EmailService emailService = new EmailService();
    private Logger logger;
    private ListAppender<ILoggingEvent> appender;

    @BeforeEach
    void setUp() {
        logger = (Logger) LoggerFactory.getLogger(EmailService.class);
        appender = new ListAppender<>();
        appender.start();
        appender.list.clear();
        logger.addAppender(appender);
    }

    @AfterEach
    void tearDown() {
        logger.detachAppender(appender);
    }

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
    void shouldLogPlaceholderForCancellationRequestNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendCancellationRequestNotification(app, "approver@example.com");

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Email placeholder invoked");
                    assertThat(message).contains("approver@example.com");
                    assertThat(message).contains("Alice Smith");
                    assertThat(message).contains("LA001");
                });
    }

    @Test
    void shouldSkipCancellationNotificationWhenApproverEmailIsNull() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendCancellationRequestNotification(app, null);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Approver email is not set");
                    assertThat(message).contains("S001");
                });
    }

    @Test
    void shouldSkipCancellationNotificationWhenApproverEmailIsBlank() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendCancellationRequestNotification(app, "  ");

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Approver email is not set");
                    assertThat(message).contains("S001");
                });
    }

    @Test
    void shouldLogPlaceholderForLeaveApprovalNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendLeaveApprovalNotification(app);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Email placeholder invoked");
                    assertThat(message).contains("alice@example.com");
                    assertThat(message).contains("Leave Application Approved");
                    assertThat(message).contains("approved");
                    assertThat(message).contains("LA001");
                });
    }

    @Test
    void shouldLogPlaceholderForLeaveRejectionNotification() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "alice@example.com");
        LeaveApplication app = application(staff);

        emailService.sendLeaveRejectionNotification(app);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Email placeholder invoked");
                    assertThat(message).contains("alice@example.com");
                    assertThat(message).contains("Leave Application Rejected");
                    assertThat(message).contains("rejected");
                    assertThat(message).contains("LA001");
                });
    }

    @Test
    void shouldSkipApprovalNotificationWhenRequesterEmailIsNull() {
        Staff staff = staffWithEmail("S001", "Alice Smith", null);
        LeaveApplication app = application(staff);

        emailService.sendLeaveApprovalNotification(app);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Requester email is not set");
                    assertThat(message).contains("S001");
                });
    }

    @Test
    void shouldSkipApprovalNotificationWhenRequesterEmailIsBlank() {
        Staff staff = staffWithEmail("S001", "Alice Smith", "");
        LeaveApplication app = application(staff);

        emailService.sendLeaveApprovalNotification(app);

        assertThat(appender.list)
                .extracting(ILoggingEvent::getFormattedMessage)
                .anySatisfy(message -> {
                    assertThat(message).contains("Requester email is not set");
                    assertThat(message).contains("S001");
                });
    }
}
