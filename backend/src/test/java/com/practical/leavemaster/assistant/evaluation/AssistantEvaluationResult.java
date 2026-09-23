package com.practical.leavemaster.assistant.evaluation;

import java.util.List;

public record AssistantEvaluationResult(
        String scenarioId,
        String category,
        boolean critical,
        boolean passed,
        List<String> failures,
        List<TurnResult> turns
) {
    public record TurnResult(
            int turn,
            String prompt,
            String response,
            List<String> tools,
            List<String> assertions,
            List<String> failures
    ) {}
}
