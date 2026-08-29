package com.practical.leavemaster.customerenquiry;

import com.practical.leavemaster.email.TransactionalEmailSender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerEnquiryService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerEnquiryRepository repository;
    private final CustomerEnquiryNotificationService notificationService;
    private final TransactionalEmailSender emailSender;

    public void submit(CustomerEnquiryRequest request) {
        CustomerEnquiry enquiry = normalizeAndValidate(request);
        CustomerEnquiry persisted = repository.save(enquiry);

        try {
            notificationService.notifyNewEnquiry(persisted);
        } catch (RuntimeException ex) {
            log.warn("Customer enquiry {} was persisted but notification delivery failed", persisted.getId(), ex);
        }
    }

    @Transactional(readOnly = true)
    public List<CustomerEnquiry> list(CustomerEnquiryStatus status) {
        return status == null
                ? repository.findAllByOrderByCreatedAtDesc()
                : repository.findByStatusOrderByCreatedAtDesc(status);
    }

    @Transactional
    public CustomerEnquiry getAndMarkRead(String id) {
        CustomerEnquiry enquiry = findWithReplies(id);
        if (enquiry.getStatus() == CustomerEnquiryStatus.NEW) {
            enquiry.setStatus(CustomerEnquiryStatus.READ);
            enquiry.setFirstReadAt(LocalDateTime.now());
        }
        return enquiry;
    }

    @Transactional
    public CustomerEnquiry reply(String id, String replyBody, String actor) {
        String normalizedReply = required(replyBody, "replyBody", 4000);
        CustomerEnquiry enquiry = findWithReplies(id);

        emailSender.sendContactEnquiryReply(
                enquiry.getEmail(), enquiry.getName(), enquiry.getMessage(), normalizedReply);

        enquiry.getReplies().add(CustomerEnquiryReply.builder()
                .id(UUID.randomUUID().toString())
                .enquiry(enquiry)
                .replyBody(normalizedReply)
                .repliedBy(actor)
                .createdAt(LocalDateTime.now())
                .build());
        enquiry.setStatus(CustomerEnquiryStatus.REPLIED);
        return enquiry;
    }

    CustomerEnquiry normalizeAndValidate(CustomerEnquiryRequest request) {
        if (request == null) {
            throw new CustomerEnquiryValidationException("Request body is required");
        }
        if (request.website() != null && !request.website().isBlank()) {
            throw new CustomerEnquiryValidationException("Invalid submission");
        }

        String name = required(request.name(), "name", 120);
        String company = required(request.company(), "company", 160);
        String email = required(request.email(), "email", 254).toLowerCase(Locale.ROOT);
        String message = required(request.message(), "message", 4000);
        String phone = optional(request.phone(), "phone", 40);
        String companySize = optional(request.companySize(), "companySize", 60);
        String country = optional(request.country(), "country", 100);

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new CustomerEnquiryValidationException("A valid work email is required");
        }
        if (request.enquiryType() == null) {
            throw new CustomerEnquiryValidationException("enquiryType is required");
        }

        return CustomerEnquiry.builder()
                .name(name)
                .company(company)
                .email(email)
                .phone(phone)
                .companySize(companySize)
                .country(country)
                .enquiryType(request.enquiryType())
                .message(message)
                .status(CustomerEnquiryStatus.NEW)
                .build();
    }

    private CustomerEnquiry findWithReplies(String id) {
        return repository.findWithRepliesById(id)
                .orElseThrow(() -> new CustomerEnquiryValidationException("Customer enquiry not found"));
    }

    private String required(String value, String field, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            throw new CustomerEnquiryValidationException(field + " is required");
        }
        enforceLength(normalized, field, maxLength);
        return normalized;
    }

    private String optional(String value, String field, int maxLength) {
        String normalized = normalize(value);
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        enforceLength(normalized, field, maxLength);
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim();
    }

    private void enforceLength(String value, String field, int maxLength) {
        if (value.length() > maxLength) {
            throw new CustomerEnquiryValidationException(field + " exceeds the maximum length of " + maxLength);
        }
    }
}
