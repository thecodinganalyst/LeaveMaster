package com.practical.leavemaster.contact;

import com.practical.leavemaster.user.TenantAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class ContactEnquiryControllerTest {
    private ContactEnquiryService service;
    private ContactEnquiryController controller;
    private TenantAuthenticationToken platform;

    @BeforeEach
    void setUp() {
        service = mock(ContactEnquiryService.class);
        controller = new ContactEnquiryController(service);
        platform = new TenantAuthenticationToken("PLATFORM", "platformadmin", "U1", List.of());
    }

    @Test
    void publicSubmissionReturnsSafeSuccessMessage() {
        var request = new ContactEnquiryService.ContactRequest("Alice", "Acme", "alice@example.com", null, null, null, "OTHER", "Hi", "");
        assertThat(controller.submit(request)).containsEntry("message", "Thanks. Your enquiry has been received.");
        verify(service).submit(request);
    }

    @Test
    void platformAdminCanListReadAndReply() {
        ContactEnquiry enquiry = enquiry();
        when(service.list(ContactEnquiryStatus.NEW)).thenReturn(List.of(enquiry));
        when(service.getAndMarkRead("id")).thenReturn(enquiry);
        when(service.reply("id", "Reply", "platformadmin")).thenReturn(enquiry);

        assertThat(controller.list(ContactEnquiryStatus.NEW, platform)).hasSize(1);
        assertThat(controller.get("id", platform).id()).isEqualTo("id");
        assertThat(controller.reply("id", new ContactEnquiryController.ReplyRequest("Reply"), platform).email())
                .isEqualTo("alice@example.com");
    }

    @Test
    void tenantAuthenticationCannotAccessPlatformInbox() {
        TenantAuthenticationToken tenant = new TenantAuthenticationToken("TENANT-A", "admin", "U2", List.of());
        assertThatThrownBy(() -> controller.list(null, tenant)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.get("id", null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void responseIncludesReplyHistory() {
        ContactEnquiry enquiry = enquiry();
        ContactEnquiryReply reply = ContactEnquiryReply.builder()
                .id("r1").enquiry(enquiry).replyBody("Answer").repliedBy("platformadmin")
                .createdAt(Instant.parse("2026-08-29T01:00:00Z")).build();
        enquiry.getReplies().add(reply);
        when(service.getAndMarkRead("id")).thenReturn(enquiry);

        ContactEnquiryController.ContactEnquiryResponse response = controller.get("id", platform);
        assertThat(response.replies()).singleElement().satisfies(item -> {
            assertThat(item.replyBody()).isEqualTo("Answer");
            assertThat(item.repliedBy()).isEqualTo("platformadmin");
        });
    }

    @Test
    void badRequestHandlerUses404OnlyForMissingEnquiry() {
        assertThat(controller.handleBadRequest(new IllegalArgumentException("Contact enquiry not found")).getStatusCode().value()).isEqualTo(404);
        assertThat(controller.handleBadRequest(new IllegalArgumentException("Reply body is required")).getStatusCode().value()).isEqualTo(400);
    }

    private static ContactEnquiry enquiry() {
        return ContactEnquiry.builder()
                .id("id").name("Alice").company("Acme").email("alice@example.com")
                .enquiryType("OTHER").message("Question").status(ContactEnquiryStatus.NEW)
                .createdAt(Instant.parse("2026-08-29T00:00:00Z")).replies(new ArrayList<>()).build();
    }
}
