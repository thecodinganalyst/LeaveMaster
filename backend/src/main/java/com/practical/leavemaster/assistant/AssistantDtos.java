package com.practical.leavemaster.assistant;

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
            String tenantId
    ) {
    }

    public record ChatResponse(
            String conversationId,
            String message,
            List<PendingAction> pendingActions
    ) {
    }
}
