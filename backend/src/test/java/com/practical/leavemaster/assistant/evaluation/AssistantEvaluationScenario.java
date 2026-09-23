package com.practical.leavemaster.assistant.evaluation;

import java.util.List;
import java.util.Map;

/**
 * Declarative AskLeaveMaestro regression scenario.
 *
 * <p>Scenarios intentionally describe observable assistant behaviour rather than
 * implementation-specific test code. The model turn is stubbed, so these scenarios
 * never require a live LLM.</p>
 */
public record AssistantEvaluationScenario(
        String id,
        String category,
        boolean critical,
        Actor actor,
        String tenantId,
        List<Turn> turns
) {
    public record Actor(String login, String staffId, List<String> authorities) {}

    public record Turn(
            String prompt,
            StubbedModelTurn model,
            Expectation expect
    ) {}

    public record StubbedModelTurn(
            String response,
            List<ToolCall> toolCalls,
            String authorizationOutcome
    ) {}

    public record ToolCall(String name, Map<String, Object> arguments, Object result) {}

    public record Expectation(
            List<String> tools,
            Map<String, Map<String, Object>> toolArguments,
            List<String> requiredFacts,
            List<String> forbiddenFacts,
            String authorizationOutcome
    ) {}
}
