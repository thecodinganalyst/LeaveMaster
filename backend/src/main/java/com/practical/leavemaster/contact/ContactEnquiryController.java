package com.practical.leavemaster.contact;

import com.practical.leavemaster.user.AuthenticationRealm;
import com.practical.leavemaster.user.TenantAuthenticationToken;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
public class ContactEnquiryController {
    private final ContactEnquiryService service;

    @PostMapping("/api/public/contact")
    public Map<String, String> submit(@RequestBody ContactEnquiryService.ContactRequest request) {
        service.submit(request);
        return Map.of("message", "Thanks. Your enquiry has been received.");
    }

    @GetMapping("/api/platform/contact-enquiries")
    public List<ContactEnquiryResponse> list(@RequestParam(required = false) ContactEnquiryStatus status,
                                             Authentication authentication) {
        requirePlatform(authentication);
        return service.list(status).stream().map(ContactEnquiryResponse::from).toList();
    }

    @GetMapping("/api/platform/contact-enquiries/{id}")
    public ContactEnquiryResponse get(@PathVariable String id, Authentication authentication) {
        requirePlatform(authentication);
        return ContactEnquiryResponse.from(service.getAndMarkRead(id));
    }

    @PostMapping("/api/platform/contact-enquiries/{id}/reply")
    public ContactEnquiryResponse reply(@PathVariable String id, @RequestBody ReplyRequest request,
                                        Authentication authentication) {
        TenantAuthenticationToken token = requirePlatform(authentication);
        return ContactEnquiryResponse.from(service.reply(id, request.body(), token.getLoginName()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> handleBadRequest(IllegalArgumentException ex) {
        int status = "Contact enquiry not found".equals(ex.getMessage()) ? 404 : 400;
        return ResponseEntity.status(status).body(Map.of("message", ex.getMessage()));
    }

    private static TenantAuthenticationToken requirePlatform(Authentication authentication) {
        if (!(authentication instanceof TenantAuthenticationToken token)
                || !AuthenticationRealm.isPlatformRealm(token.getTenantId())) {
            throw new org.springframework.security.access.AccessDeniedException("Platform administrator access required");
        }
        return token;
    }

    public record ReplyRequest(String body) {}

    public record ReplyResponse(String id, String replyBody, String repliedBy, Instant createdAt) {
        static ReplyResponse from(ContactEnquiryReply reply) {
            return new ReplyResponse(reply.getId(), reply.getReplyBody(), reply.getRepliedBy(), reply.getCreatedAt());
        }
    }

    public record ContactEnquiryResponse(String id, String name, String company, String email, String phone,
                                         String companySize, String country, String enquiryType, String message,
                                         ContactEnquiryStatus status, Instant createdAt, Instant firstReadAt,
                                         List<ReplyResponse> replies) {
        static ContactEnquiryResponse from(ContactEnquiry enquiry) {
            return new ContactEnquiryResponse(enquiry.getId(), enquiry.getName(), enquiry.getCompany(), enquiry.getEmail(),
                    enquiry.getPhone(), enquiry.getCompanySize(), enquiry.getCountry(), enquiry.getEnquiryType(),
                    enquiry.getMessage(), enquiry.getStatus(), enquiry.getCreatedAt(), enquiry.getFirstReadAt(),
                    enquiry.getReplies().stream().map(ReplyResponse::from).toList());
        }
    }
}
