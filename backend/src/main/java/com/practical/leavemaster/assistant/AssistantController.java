package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.MediaType;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantConfirmationService confirmationService;
    private final AssistantQualityService qualityService;
    private final com.practical.leavemaster.leaveapplication.LeaveApplicationService leaveApplicationService;
    private final com.practical.leavemaster.user.AppUserRepository appUserRepository;

    @PostMapping("/chat")
    public AssistantDtos.ChatResponse chat(@RequestBody AssistantDtos.ChatRequest request, Authentication authentication) {
        return assistantService.chat(request, authentication);
    }

    @PostMapping("/actions/confirm")
    public AssistantDtos.ConfirmationResponse confirm(@RequestBody AssistantDtos.ConfirmationRequest request,
                                                       Authentication authentication) {
        return confirmationService.confirm(request == null ? null : request.confirmationToken(), authentication);
    }

    @PostMapping(value = "/actions/confirm-with-attachment", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public AssistantDtos.ConfirmationResponse confirmWithAttachment(
            @RequestPart("confirmationToken") String confirmationToken,
            @RequestPart("file") MultipartFile file,
            Authentication authentication) {
        AssistantDtos.ConfirmationResponse response = confirmationService.confirm(confirmationToken, authentication);
        if (!"applyForLeave".equals(response.toolName())) {
            throw new IllegalArgumentException("Attachments are supported only when confirming a leave application");
        }
        String applicationId = firstCreatedLeaveApplicationId(response.result());
        leaveApplicationService.uploadAttachment(applicationId, file);
        return response;
    }

    private String firstCreatedLeaveApplicationId(String result) {
        if (result == null || result.isBlank()) throw new IllegalArgumentException("Confirmed leave application did not return an application id");
        try {
            var node = new tools.jackson.databind.ObjectMapper().readTree(result);
            var first = node.isArray() && !node.isEmpty() ? node.get(0) : node;
            var id = first.get("id");
            if (id == null || id.asText().isBlank()) throw new IllegalArgumentException("Confirmed leave application did not return an application id");
            return id.asText();
        } catch (tools.jackson.core.JacksonException exception) {
            throw new IllegalArgumentException("Confirmed leave application result could not be read", exception);
        }
    }

    @PostMapping("/feedback")
    public Map<String, String> feedback(@RequestBody FeedbackRequest request, Authentication authentication) {
        if (request == null || request.correlationId() == null || request.correlationId().isBlank()) {
            throw new IllegalArgumentException("correlationId is required");
        }
        var user = appUserRepository.findById(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        qualityService.recordFeedback(request.correlationId(), user.getTenantId(), authentication, request.rating(), request.category());
        return Map.of("status", "RECORDED");
    }

    @org.springframework.security.access.prepost.PreAuthorize("hasAuthority('PLATFORM_ADMIN')")
    @org.springframework.web.bind.annotation.GetMapping("/quality")
    public AssistantQualityService.QualitySummary quality(Authentication authentication) {
        var user = appUserRepository.findById(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Authenticated user was not found"));
        return qualityService.summary(user.getTenantId());
    }

    public record FeedbackRequest(String correlationId, int rating, String category) {}

    @ExceptionHandler(IllegalArgumentException.class)
    ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException exception) {
        return ResponseEntity.badRequest().body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AssistantRateLimitException.class)
    ResponseEntity<Map<String, String>> rateLimited(AssistantRateLimitException exception) {
        return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AssistantUnavailableException.class)
    ResponseEntity<Map<String, String>> unavailable(AssistantUnavailableException exception) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", exception.getMessage()));
    }

    @ExceptionHandler(AssistantToolExecutionException.class)
    ResponseEntity<Map<String, String>> toolFailure(AssistantToolExecutionException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        if (exception.getConversationId() != null && !exception.getConversationId().isBlank()) {
            body.put("conversationId", exception.getConversationId());
        }
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(body);
    }

    @ExceptionHandler(AssistantProviderCapacityException.class)
    ResponseEntity<Map<String, String>> providerCapacityFailure(AssistantProviderCapacityException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        if (exception.getConversationId() != null && !exception.getConversationId().isBlank()) {
            body.put("conversationId", exception.getConversationId());
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
    }

    @ExceptionHandler(AssistantProviderException.class)
    ResponseEntity<Map<String, String>> providerFailure(AssistantProviderException exception) {
        Map<String, String> body = new LinkedHashMap<>();
        body.put("error", exception.getMessage());
        if (exception.getConversationId() != null && !exception.getConversationId().isBlank()) {
            body.put("conversationId", exception.getConversationId());
        }
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(body);
    }
}
