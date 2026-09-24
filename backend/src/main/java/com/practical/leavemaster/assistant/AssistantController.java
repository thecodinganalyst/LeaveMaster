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

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/assistant")
@RequiredArgsConstructor
public class AssistantController {

    private final AssistantService assistantService;
    private final AssistantConfirmationService confirmationService;
    private final AssistantQualityService qualityService;
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
