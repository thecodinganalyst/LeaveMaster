package com.practical.leavemaster.assistant;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public final class AssistantDtos {
    private AssistantDtos() {
    }

    public record ChatRequest(String message, String conversationId) {
    }

    public record PendingAction(
            String toolName,
            Map<String, Object> arguments,
            String requiredAuthority,
            String actorLoginName,
            String actorStaffId,
            String tenantId,
            String confirmationToken,
            Instant expiresAt
    ) {
    }

    public record StructuredResult(
            String toolName,
            Object data
    ) {
    }

    public record ChatResponse(
            String conversationId,
            String message,
            List<PendingAction> pendingActions,
            List<StructuredResult> structuredResults
    ) {
    }

    public record ConfirmationRequest(String confirmationToken) {
    }

    public record ConfirmationResponse(
            String toolName,
            String status,
            String result,
            boolean replayed
    ) {
    }
}
