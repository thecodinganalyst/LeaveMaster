package com.practical.leavemaster.customerenquiry;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerEnquiryService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final CustomerEnquiryRepository repository;
    private final CustomerEnquiryNotificationService notificationService;

    public void submit(CustomerEnquiryRequest request) {
        CustomerEnquiry enquiry = normalizeAndValidate(request);
        CustomerEnquiry persisted = repository.save(enquiry);

        try {
            notificationService.notifyNewEnquiry(persisted);
        } catch (RuntimeException ex) {
            log.warn("Customer enquiry {} was persisted but notification delivery failed", persisted.getId(), ex);
        }
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
