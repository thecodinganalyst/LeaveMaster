package com.practical.leavemaster.customerenquiry;

import com.practical.leavemaster.email.EmailDeliveryException;
import com.practical.leavemaster.email.TransactionalEmailSender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CustomerEnquiryServiceTest {

    @Mock
    private CustomerEnquiryRepository repository;

    @Mock
    private CustomerEnquiryNotificationService notificationService;

    @Mock
    private TransactionalEmailSender emailSender;

    @InjectMocks
    private CustomerEnquiryService service;

    @Test
    void shouldNormalizePersistAndNotify() {
        when(repository.save(any(CustomerEnquiry.class))).thenAnswer(invocation -> {
            CustomerEnquiry enquiry = invocation.getArgument(0);
            enquiry.setId("enquiry-1");
            return enquiry;
        });

        service.submit(validRequest());

        ArgumentCaptor<CustomerEnquiry> captor = ArgumentCaptor.forClass(CustomerEnquiry.class);
        verify(repository).save(captor.capture());
        CustomerEnquiry saved = captor.getValue();
        assertThat(saved.getName()).isEqualTo("Jane Doe");
        assertThat(saved.getCompany()).isEqualTo("Example Pte Ltd");
        assertThat(saved.getEmail()).isEqualTo("jane@example.com");
        assertThat(saved.getPhone()).isNull();
        assertThat(saved.getStatus()).isEqualTo(CustomerEnquiryStatus.NEW);
        verify(notificationService).notifyNewEnquiry(saved);
    }

    @Test
    void shouldKeepPersistedSubmissionWhenNotificationFails() {
        when(repository.save(any(CustomerEnquiry.class))).thenAnswer(invocation -> {
            CustomerEnquiry enquiry = invocation.getArgument(0);
            enquiry.setId("enquiry-2");
            return enquiry;
        });
        doThrow(new IllegalStateException("mail unavailable"))
                .when(notificationService).notifyNewEnquiry(any(CustomerEnquiry.class));

        service.submit(validRequest());

        verify(repository).save(any(CustomerEnquiry.class));
    }

    @Test
    void shouldRejectMissingAndInvalidFields() {
        assertThatThrownBy(() -> service.submit(null))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("Request body is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "invalid", null, null, null, CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("A valid work email is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", null, null, null, null, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("enquiryType is required");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", null, null, null, CustomerEnquiryType.OTHER, "Hello", "bot")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("Invalid submission");
    }

    @Test
    void shouldEnforceFieldLengths() {
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "x".repeat(121), "Example", "jane@example.com", null, null, null,
                CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessageContaining("name exceeds");
        assertThatThrownBy(() -> service.submit(new CustomerEnquiryRequest(
                "Jane", "Example", "jane@example.com", "x".repeat(41), null, null,
                CustomerEnquiryType.OTHER, "Hello", "")))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessageContaining("phone exceeds");
    }

    @Test
    void shouldListNewestEnquiriesWithOptionalStatusFilter() {
        CustomerEnquiry enquiry = enquiry(CustomerEnquiryStatus.NEW);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(enquiry));
        when(repository.findByStatusOrderByCreatedAtDesc(CustomerEnquiryStatus.NEW)).thenReturn(List.of(enquiry));

        assertThat(service.list(null)).containsExactly(enquiry);
        assertThat(service.list(CustomerEnquiryStatus.NEW)).containsExactly(enquiry);
    }

    @Test
    void shouldMarkNewEnquiryReadOnlyOnce() {
        CustomerEnquiry enquiry = enquiry(CustomerEnquiryStatus.NEW);
        when(repository.findWithRepliesById("enquiry-1")).thenReturn(Optional.of(enquiry));

        CustomerEnquiry firstRead = service.getAndMarkRead("enquiry-1");
        LocalDateTime firstReadAt = firstRead.getFirstReadAt();
        assertThat(firstRead.getStatus()).isEqualTo(CustomerEnquiryStatus.READ);
        assertThat(firstReadAt).isNotNull();

        service.getAndMarkRead("enquiry-1");
        assertThat(enquiry.getFirstReadAt()).isEqualTo(firstReadAt);
    }

    @Test
    void shouldSendAndPersistReplyHistoryAfterSuccessfulEmail() {
        CustomerEnquiry enquiry = enquiry(CustomerEnquiryStatus.READ);
        when(repository.findWithRepliesById("enquiry-1")).thenReturn(Optional.of(enquiry));

        CustomerEnquiry updated = service.reply("enquiry-1", " Thanks for contacting us. ", "platformadmin");

        verify(emailSender).sendContactEnquiryReply(
                "jane@example.com", "Jane Doe", "Original question", "Thanks for contacting us.");
        assertThat(updated.getStatus()).isEqualTo(CustomerEnquiryStatus.REPLIED);
        assertThat(updated.getReplies()).singleElement().satisfies(reply -> {
            assertThat(reply.getReplyBody()).isEqualTo("Thanks for contacting us.");
            assertThat(reply.getRepliedBy()).isEqualTo("platformadmin");
            assertThat(reply.getCreatedAt()).isNotNull();
        });
    }

    @Test
    void shouldNotMarkRepliedWhenEmailDeliveryFails() {
        CustomerEnquiry enquiry = enquiry(CustomerEnquiryStatus.READ);
        when(repository.findWithRepliesById("enquiry-1")).thenReturn(Optional.of(enquiry));
        doThrow(new EmailDeliveryException("mail unavailable")).when(emailSender)
                .sendContactEnquiryReply(any(), any(), any(), any());

        assertThatThrownBy(() -> service.reply("enquiry-1", "Reply", "platformadmin"))
                .isInstanceOf(EmailDeliveryException.class);
        assertThat(enquiry.getStatus()).isEqualTo(CustomerEnquiryStatus.READ);
        assertThat(enquiry.getReplies()).isEmpty();
    }

    @Test
    void shouldValidateReplyAndMissingEnquiry() {
        assertThatThrownBy(() -> service.reply("enquiry-1", " ", "platformadmin"))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("replyBody is required");
        assertThatThrownBy(() -> service.reply("enquiry-1", "x".repeat(4001), "platformadmin"))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessageContaining("replyBody exceeds");

        when(repository.findWithRepliesById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAndMarkRead("missing"))
                .isInstanceOf(CustomerEnquiryValidationException.class)
                .hasMessage("Customer enquiry not found");
    }

    private CustomerEnquiryRequest validRequest() {
        return new CustomerEnquiryRequest(
                " Jane Doe ",
                " Example Pte Ltd ",
                " JANE@EXAMPLE.COM ",
                " ",
                "21-100",
                "Singapore",
                CustomerEnquiryType.PRODUCT_DEMO,
                " Please arrange a demo. ",
                "");
    }

    private CustomerEnquiry enquiry(CustomerEnquiryStatus status) {
        return CustomerEnquiry.builder()
                .id("enquiry-1")
                .name("Jane Doe")
                .company("Example Pte Ltd")
                .email("jane@example.com")
                .enquiryType(CustomerEnquiryType.OTHER)
                .message("Original question")
                .status(status)
                .createdAt(LocalDateTime.of(2026, 8, 29, 8, 0))
                .replies(new ArrayList<>())
                .build();
    }
}
