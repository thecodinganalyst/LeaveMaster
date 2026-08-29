package com.practical.leavemaster.contact;

import com.practical.leavemaster.email.EmailDeliveryException;
import com.practical.leavemaster.email.TransactionalEmailSender;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ContactEnquiryServiceTest {
    private ContactEnquiryRepository repository;
    private TransactionalEmailSender emailSender;
    private ContactEnquiryService service;

    @BeforeEach
    void setUp() {
        repository = mock(ContactEnquiryRepository.class);
        emailSender = mock(TransactionalEmailSender.class);
        service = new ContactEnquiryService(repository, emailSender);
    }

    @Test
    void submitPersistsTrimmedLegitimateEnquiry() {
        service.submit(new ContactEnquiryService.ContactRequest(
                " Alice ", " Acme ", " alice@example.com ", " 123 ", " 21-100 ", " SG ",
                "PRODUCT_DEMO", " Hello ", ""));

        ArgumentCaptor<ContactEnquiry> captor = ArgumentCaptor.forClass(ContactEnquiry.class);
        verify(repository).save(captor.capture());
        ContactEnquiry saved = captor.getValue();
        assertThat(saved.getId()).isNotBlank();
        assertThat(saved.getName()).isEqualTo("Alice");
        assertThat(saved.getCompany()).isEqualTo("Acme");
        assertThat(saved.getEmail()).isEqualTo("alice@example.com");
        assertThat(saved.getPhone()).isEqualTo("123");
        assertThat(saved.getCompanySize()).isEqualTo("21-100");
        assertThat(saved.getCountry()).isEqualTo("SG");
        assertThat(saved.getStatus()).isEqualTo(ContactEnquiryStatus.NEW);
        assertThat(saved.getCreatedAt()).isNotNull();
    }

    @Test
    void honeypotSubmissionIsIgnored() {
        service.submit(new ContactEnquiryService.ContactRequest(
                "Bot", "BotCo", "bot@example.com", null, null, null, "OTHER", "spam", "https://spam.example"));
        verifyNoInteractions(repository);
    }

    @Test
    void validationRejectsMissingInvalidAndOversizedFields() {
        assertThatThrownBy(() -> service.submit(new ContactEnquiryService.ContactRequest(
                "", "Acme", "alice@example.com", null, null, null, "OTHER", "Hello", "")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Name is required");
        assertThatThrownBy(() -> service.submit(new ContactEnquiryService.ContactRequest(
                "Alice", "Acme", "invalid", null, null, null, "OTHER", "Hello", "")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Enter a valid email address");
        assertThatThrownBy(() -> service.submit(new ContactEnquiryService.ContactRequest(
                "A".repeat(121), "Acme", "alice@example.com", null, null, null, "OTHER", "Hello", "")))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Name is too long");
    }

    @Test
    void listSupportsAllAndStatusFilter() {
        ContactEnquiry enquiry = enquiry(ContactEnquiryStatus.NEW);
        when(repository.findAllByOrderByCreatedAtDesc()).thenReturn(List.of(enquiry));
        when(repository.findByStatusOrderByCreatedAtDesc(ContactEnquiryStatus.NEW)).thenReturn(List.of(enquiry));

        assertThat(service.list(null)).containsExactly(enquiry);
        assertThat(service.list(ContactEnquiryStatus.NEW)).containsExactly(enquiry);
    }

    @Test
    void openingNewEnquiryMarksItReadOnlyOnce() {
        ContactEnquiry enquiry = enquiry(ContactEnquiryStatus.NEW);
        when(repository.findById("id")).thenReturn(Optional.of(enquiry));

        assertThat(service.getAndMarkRead("id").getStatus()).isEqualTo(ContactEnquiryStatus.READ);
        assertThat(enquiry.getFirstReadAt()).isNotNull();
        Instant firstRead = enquiry.getFirstReadAt();
        service.getAndMarkRead("id");
        assertThat(enquiry.getFirstReadAt()).isEqualTo(firstRead);
    }

    @Test
    void replySendsEmailThenPersistsReplyAndStatus() {
        ContactEnquiry enquiry = enquiry(ContactEnquiryStatus.READ);
        when(repository.findById("id")).thenReturn(Optional.of(enquiry));

        ContactEnquiry updated = service.reply("id", " Thanks for contacting us. ", "platformadmin");

        verify(emailSender).sendContactEnquiryReply("alice@example.com", "Alice", "Original question", "Thanks for contacting us.");
        assertThat(updated.getStatus()).isEqualTo(ContactEnquiryStatus.REPLIED);
        assertThat(updated.getReplies()).hasSize(1);
        assertThat(updated.getReplies().getFirst().getRepliedBy()).isEqualTo("platformadmin");
        assertThat(updated.getReplies().getFirst().getReplyBody()).isEqualTo("Thanks for contacting us.");
    }

    @Test
    void failedEmailDoesNotRecordSuccessfulReply() {
        ContactEnquiry enquiry = enquiry(ContactEnquiryStatus.READ);
        when(repository.findById("id")).thenReturn(Optional.of(enquiry));
        doThrow(new EmailDeliveryException("failed")).when(emailSender)
                .sendContactEnquiryReply(any(), any(), any(), any());

        assertThatThrownBy(() -> service.reply("id", "Reply", "platformadmin"))
                .isInstanceOf(EmailDeliveryException.class);
        assertThat(enquiry.getStatus()).isEqualTo(ContactEnquiryStatus.READ);
        assertThat(enquiry.getReplies()).isEmpty();
    }

    @Test
    void replyAndLookupValidationAreExplicit() {
        when(repository.findById("missing")).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.getAndMarkRead("missing"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Contact enquiry not found");
        assertThatThrownBy(() -> service.reply("missing", " ", "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Reply body is required");
        assertThatThrownBy(() -> service.reply("missing", "x".repeat(4001), "admin"))
                .isInstanceOf(IllegalArgumentException.class).hasMessage("Reply body must not exceed 4000 characters");
    }

    private static ContactEnquiry enquiry(ContactEnquiryStatus status) {
        return ContactEnquiry.builder()
                .id("id").name("Alice").company("Acme").email("alice@example.com")
                .enquiryType("OTHER").message("Original question").status(status)
                .createdAt(Instant.parse("2026-08-29T00:00:00Z")).replies(new ArrayList<>()).build();
    }
}
