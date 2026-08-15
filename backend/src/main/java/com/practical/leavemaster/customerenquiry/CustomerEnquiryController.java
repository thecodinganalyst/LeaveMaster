package com.practical.leavemaster.customerenquiry;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/public/contact")
@RequiredArgsConstructor
public class CustomerEnquiryController {

    private final CustomerEnquiryService service;
    private final CustomerEnquiryRateLimiter rateLimiter;

    @Value("${app.customer-enquiry.max-request-bytes:16384}")
    private long maxRequestBytes;

    @PostMapping
    public ResponseEntity<Map<String, String>> submit(
            @RequestBody CustomerEnquiryRequest request,
            HttpServletRequest servletRequest) {
        long contentLength = servletRequest.getContentLengthLong();
        if (contentLength > maxRequestBytes) {
            return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE)
                    .body(Map.of("message", "Submission is too large"));
        }

        if (!rateLimiter.tryAcquire(servletRequest.getRemoteAddr())) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                    .body(Map.of("message", "Too many submissions. Please try again later."));
        }

        service.submit(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Thanks. Your enquiry has been received."));
    }

    @ExceptionHandler(CustomerEnquiryValidationException.class)
    ResponseEntity<Map<String, String>> handleValidation(CustomerEnquiryValidationException exception) {
        return ResponseEntity.badRequest().body(Map.of("message", exception.getMessage()));
    }
}
