package com.practical.leavemaster.assistant;

import org.springframework.ai.tool.ToolCallback;

import java.util.Arrays;
import java.util.Locale;
import java.util.Set;

final class AssistantToolRoutingPolicy {

    private static final String STAFF_ENTITLEMENT_TOOL = "getStaffLeaveEntitlement";
    private static final Set<String> EXPLANATION_MARKERS = Set.of(
            "why", "explain", "calculated", "calculation", "calculate", "worked out", "derived",
            "how was", "how is", "how did", "how does");
    private static final Set<String> BALANCE_MARKERS = Set.of(
            "balance", "remaining", "left", "available");

    private AssistantToolRoutingPolicy() {
    }

    static ToolCallback[] route(ToolCallback[] callbacks, String userMessage) {
        if (callbacks == null || callbacks.length == 0 || !isEntitlementExplanation(userMessage)) {
            return callbacks;
        }

        ToolCallback[] focused = Arrays.stream(callbacks)
                .filter(callback -> STAFF_ENTITLEMENT_TOOL.equals(callback.getToolDefinition().name()))
                .toArray(ToolCallback[]::new);

        return focused.length == 0 ? callbacks : focused;
    }

    static boolean isEntitlementExplanation(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return false;
        }

        String normalized = userMessage.toLowerCase(Locale.ROOT).replaceAll("\\s+", " ").trim();
        boolean asksForExplanation = EXPLANATION_MARKERS.stream().anyMatch(normalized::contains);
        if (!asksForExplanation) {
            return false;
        }

        boolean mentionsEntitlement = normalized.contains("entitlement") || normalized.contains("entitled");
        if (mentionsEntitlement) {
            return true;
        }

        boolean asksAboutLeaveAmount = normalized.contains("leave")
                && normalized.matches(".*\\b\\d+(?:\\.\\d+)?\\s+days?\\b.*");
        boolean clearlyAsksForBalance = BALANCE_MARKERS.stream().anyMatch(normalized::contains);
        return asksAboutLeaveAmount && !clearlyAsksForBalance;
    }
}
