package com.practical.leavemaster.contact;

import com.practical.leavemaster.email.TransactionalEmailSender;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ContactEnquiryService {
    private final ContactEnquiryRepository repository;
    private final TransactionalEmailSender emailSender;

    @Transactional
    public void submit(ContactRequest request) {
        if (request.website() != null && !request.website().isBlank()) return;
        validate(request);
        repository.save(ContactEnquiry.builder()
                .id(UUID.randomUUID().toString())
                .name(request.name().trim()).company(request.company().trim()).email(request.email().trim())
                .phone(blankToNull(request.phone())).companySize(blankToNull(request.companySize()))
                .country(blankToNull(request.country())).enquiryType(request.enquiryType().trim())
                .message(request.message().trim()).status(ContactEnquiryStatus.NEW).createdAt(Instant.now()).build());
    }

    @Transactional(readOnly = true)
    public List<ContactEnquiry> list(ContactEnquiryStatus status) {
        return status == null ? repository.findAllByOrderByCreatedAtDesc() : repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public ContactEnquiry getAndMarkRead(String id) {
        ContactEnquiry enquiry = find(id);
        if (enquiry.getStatus() == ContactEnquiryStatus.NEW) {
            enquiry.setStatus(ContactEnquiryStatus.READ);
            enquiry.setFirstReadAt(Instant.now());
        }
        return enquiry;
    }

    @Transactional
    public ContactEnquiry reply(String id, String body, String actor) {
        if (body == null || body.isBlank()) throw new IllegalArgumentException("Reply body is required");
        if (body.trim().length() > 4000) throw new IllegalArgumentException("Reply body must not exceed 4000 characters");
        ContactEnquiry enquiry = find(id);
        emailSender.sendContactEnquiryReply(enquiry.getEmail(), enquiry.getName(), enquiry.getMessage(), body.trim());
        ContactEnquiryReply reply = ContactEnquiryReply.builder().id(UUID.randomUUID().toString()).enquiry(enquiry)
                .replyBody(body.trim()).repliedBy(actor).createdAt(Instant.now()).build();
        enquiry.getReplies().add(reply);
        enquiry.setStatus(ContactEnquiryStatus.REPLIED);
        return enquiry;
    }

    private ContactEnquiry find(String id) {
        return repository.findById(id).orElseThrow(() -> new IllegalArgumentException("Contact enquiry not found"));
    }

    private static void validate(ContactRequest r) {
        require(r.name(), "Name", 120); require(r.company(), "Company", 160); require(r.email(), "Email", 254);
        require(r.enquiryType(), "Enquiry type", 40); require(r.message(), "Message", 4000);
        if (!r.email().trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) throw new IllegalArgumentException("Enter a valid email address");
    }

    private static void require(String value, String label, int max) {
        if (value == null || value.isBlank()) throw new IllegalArgumentException(label + " is required");
        if (value.trim().length() > max) throw new IllegalArgumentException(label + " is too long");
    }
    private static String blankToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }

    public record ContactRequest(String name, String company, String email, String phone, String companySize,
                                 String country, String enquiryType, String message, String website) {}
}
