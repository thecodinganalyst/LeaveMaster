package com.practical.leavemaster.customerenquiry;

import com.practical.leavemaster.user.TenantAuthenticationToken;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlatformCustomerEnquiryControllerTest {
    private CustomerEnquiryService service;
    private PlatformCustomerEnquiryController controller;
    private TenantAuthenticationToken platform;

    @BeforeEach
    void setUp() {
        service = mock(CustomerEnquiryService.class);
        controller = new PlatformCustomerEnquiryController(service);
        platform = new TenantAuthenticationToken("PLATFORM", "platformadmin", "U1", List.of());
    }

    @Test
    void shouldAllowPlatformAdminToListOpenAndReply() {
        CustomerEnquiry enquiry = enquiry();
        when(service.list(CustomerEnquiryStatus.NEW)).thenReturn(List.of(enquiry));
        when(service.getAndMarkRead("enquiry-1")).thenReturn(enquiry);
        when(service.reply("enquiry-1", "Reply", "platformadmin")).thenReturn(enquiry);

        assertThat(controller.list(CustomerEnquiryStatus.NEW, platform)).hasSize(1);
        assertThat(controller.get("enquiry-1", platform).id()).isEqualTo("enquiry-1");
        assertThat(controller.reply(
                "enquiry-1", new PlatformCustomerEnquiryController.ReplyRequest("Reply"), platform).email())
                .isEqualTo("jane@example.com");
    }

    @Test
    void shouldRejectTenantAndUnauthenticatedAccess() {
        TenantAuthenticationToken tenant = new TenantAuthenticationToken("TENANT-A", "admin", "U2", List.of());
        assertThatThrownBy(() -> controller.list(null, tenant)).isInstanceOf(AccessDeniedException.class);
        assertThatThrownBy(() -> controller.get("enquiry-1", null)).isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void shouldExposeReplyHistoryInResponse() {
        CustomerEnquiry enquiry = enquiry();
        enquiry.getReplies().add(CustomerEnquiryReply.builder()
                .id("reply-1")
                .enquiry(enquiry)
                .replyBody("Answer")
                .repliedBy("platformadmin")
                .createdAt(LocalDateTime.of(2026, 8, 29, 9, 0))
                .build());
        when(service.getAndMarkRead("enquiry-1")).thenReturn(enquiry);

        PlatformCustomerEnquiryController.CustomerEnquiryResponse response = controller.get("enquiry-1", platform);
        assertThat(response.replies()).singleElement().satisfies(reply -> {
            assertThat(reply.replyBody()).isEqualTo("Answer");
            assertThat(reply.repliedBy()).isEqualTo("platformadmin");
        });
    }

    @Test
    void shouldReturn404ForMissingAnd400ForOtherValidationErrors() {
        assertThat(controller.handleValidation(new CustomerEnquiryValidationException("Customer enquiry not found"))
                .getStatusCode().value()).isEqualTo(404);
        assertThat(controller.handleValidation(new CustomerEnquiryValidationException("replyBody is required"))
                .getStatusCode().value()).isEqualTo(400);
    }

    private CustomerEnquiry enquiry() {
        return CustomerEnquiry.builder()
                .id("enquiry-1")
                .name("Jane Doe")
                .company("Example Pte Ltd")
                .email("jane@example.com")
                .enquiryType(CustomerEnquiryType.PRODUCT_DEMO)
                .message("Original question")
                .status(CustomerEnquiryStatus.NEW)
                .createdAt(LocalDateTime.of(2026, 8, 29, 8, 0))
                .replies(new ArrayList<>())
                .build();
    }
}
