package com.practical.leavemaster.customerenquiry;

import com.practical.leavemaster.user.AuthenticationRealm;
import com.practical.leavemaster.user.TenantAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/platform/contact-enquiries")
@RequiredArgsConstructor
public class PlatformCustomerEnquiryController {
    private final CustomerEnquiryService service;

    @GetMapping
    public List<CustomerEnquiryResponse> list(
            @RequestParam(required = false) CustomerEnquiryStatus status,
            Authentication authentication) {
        requirePlatform(authentication);
        return service.list(status).stream().map(CustomerEnquiryResponse::from).toList();
    }

    @GetMapping("/{id}")
    public CustomerEnquiryResponse get(@PathVariable String id, Authentication authentication) {
        requirePlatform(authentication);
        return CustomerEnquiryResponse.from(service.getAndMarkRead(id));
    }

    @PostMapping("/{id}/reply")
    public CustomerEnquiryResponse reply(
            @PathVariable String id,
            @RequestBody ReplyRequest request,
            Authentication authentication) {
        TenantAuthenticationToken token = requirePlatform(authentication);
        return CustomerEnquiryResponse.from(service.reply(id, request.body(), token.getLoginName()));
    }

    @ExceptionHandler(CustomerEnquiryValidationException.class)
    ResponseEntity<Map<String, String>> handleValidation(CustomerEnquiryValidationException exception) {
        int status = "Customer enquiry not found".equals(exception.getMessage()) ? 404 : 400;
        return ResponseEntity.status(status).body(Map.of("message", exception.getMessage()));
    }

    private static TenantAuthenticationToken requirePlatform(Authentication authentication) {
        if (!(authentication instanceof TenantAuthenticationToken token)
                || !AuthenticationRealm.isPlatformRealm(token.getTenantId())) {
            throw new AccessDeniedException("Platform administrator access required");
        }
        return token;
    }

    public record ReplyRequest(String body) {}

    public record ReplyResponse(String id, String replyBody, String repliedBy, LocalDateTime createdAt) {
        static ReplyResponse from(CustomerEnquiryReply reply) {
            return new ReplyResponse(reply.getId(), reply.getReplyBody(), reply.getRepliedBy(), reply.getCreatedAt());
        }
    }

    public record CustomerEnquiryResponse(
            String id,
            String name,
            String company,
            String email,
            String phone,
            String companySize,
            String country,
            CustomerEnquiryType enquiryType,
            String message,
            CustomerEnquiryStatus status,
            LocalDateTime createdAt,
            LocalDateTime firstReadAt,
            List<ReplyResponse> replies) {
        static CustomerEnquiryResponse from(CustomerEnquiry enquiry) {
            return new CustomerEnquiryResponse(
                    enquiry.getId(), enquiry.getName(), enquiry.getCompany(), enquiry.getEmail(),
                    enquiry.getPhone(), enquiry.getCompanySize(), enquiry.getCountry(), enquiry.getEnquiryType(),
                    enquiry.getMessage(), enquiry.getStatus(), enquiry.getCreatedAt(), enquiry.getFirstReadAt(),
                    enquiry.getReplies().stream().map(ReplyResponse::from).toList());
        }
    }
}
