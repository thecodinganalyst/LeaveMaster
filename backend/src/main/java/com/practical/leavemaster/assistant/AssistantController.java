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

    @PostMapping("/chat")
    public AssistantDtos.ChatResponse chat(@RequestBody AssistantDtos.ChatRequest request, Authentication authentication) {
        return assistantService.chat(request, authentication);
    }

    @PostMapping("/actions/confirm")
    public AssistantDtos.ConfirmationResponse confirm(@RequestBody AssistantDtos.ConfirmationRequest request,
                                                       Authentication authentication) {
        return confirmationService.confirm(request == null ? null : request.confirmationToken(), authentication);
    }

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
