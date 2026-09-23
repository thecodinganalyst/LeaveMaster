package com.practical.leavemaster.assistant.evaluation;

import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

final class AssistantEvaluationRunner {
    private final ObjectMapper objectMapper;

    AssistantEvaluationRunner(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    AssistantEvaluationResult run(AssistantEvaluationScenario scenario) {
        List<String> scenarioFailures = new ArrayList<>();
        List<AssistantEvaluationResult.TurnResult> turnResults = new ArrayList<>();
        Map<String, Object> conversation = new LinkedHashMap<>();

        int turnNumber = 0;
        for (AssistantEvaluationScenario.Turn turn : scenario.turns()) {
            turnNumber++;
            List<String> assertions = new ArrayList<>();
            List<String> failures = new ArrayList<>();
            var actual = executeStubbedTurn(turn, conversation);

            assertTools(turn.expect(), actual, assertions, failures);
            assertArguments(turn.expect(), actual, assertions, failures);
            assertFacts(turn.expect(), actual.response(), assertions, failures);
            assertAuthorization(turn.expect(), actual.authorizationOutcome(), assertions, failures);

            if (!failures.isEmpty()) {
                scenarioFailures.addAll(failures.stream()
                        .map(failure -> "turn " + turnNumber + ": " + failure)
                        .toList());
            }
            turnResults.add(new AssistantEvaluationResult.TurnResult(
                    turnNumber, turn.prompt(), actual.response(),
                    actual.toolCalls().stream().map(AssistantEvaluationScenario.ToolCall::name).toList(),
                    List.copyOf(assertions), List.copyOf(failures)));
        }

        return new AssistantEvaluationResult(
                scenario.id(), scenario.category(), scenario.critical(),
                scenarioFailures.isEmpty(), List.copyOf(scenarioFailures), List.copyOf(turnResults));
    }

    private ActualTurn executeStubbedTurn(AssistantEvaluationScenario.Turn turn, Map<String, Object> conversation) {
        // This boundary deliberately replaces the live model. Tool calls/results are supplied by the
        // scenario and recorded exactly as an orchestration layer would observe them.
        conversation.put("lastPrompt", turn.prompt());
        conversation.put("turnCount", ((Integer) conversation.getOrDefault("turnCount", 0)) + 1);
        var model = turn.model();
        return new ActualTurn(
                model.response() == null ? "" : model.response(),
                model.toolCalls() == null ? List.of() : List.copyOf(model.toolCalls()),
                model.authorizationOutcome() == null ? "ALLOWED" : model.authorizationOutcome());
    }

    private void assertTools(AssistantEvaluationScenario.Expectation expected, ActualTurn actual,
                             List<String> assertions, List<String> failures) {
        List<String> actualTools = actual.toolCalls().stream().map(AssistantEvaluationScenario.ToolCall::name).toList();
        List<String> expectedTools = expected.tools() == null ? List.of() : expected.tools();
        assertions.add("tools expected=" + expectedTools + " actual=" + actualTools);
        if (!actualTools.equals(expectedTools)) {
            failures.add("expected tools " + expectedTools + " but got " + actualTools);
        }
    }

    private void assertArguments(AssistantEvaluationScenario.Expectation expected, ActualTurn actual,
                                 List<String> assertions, List<String> failures) {
        if (expected.toolArguments() == null) return;
        Map<String, AssistantEvaluationScenario.ToolCall> calls = new LinkedHashMap<>();
        actual.toolCalls().forEach(call -> calls.put(call.name(), call));
        expected.toolArguments().forEach((tool, arguments) -> {
            var actualCall = calls.get(tool);
            assertions.add("arguments " + tool + " expected=" + arguments);
            if (actualCall == null) {
                failures.add("cannot assert arguments for missing tool " + tool);
            } else if (!containsEntries(actualCall.arguments(), arguments)) {
                failures.add("tool " + tool + " arguments expected " + arguments + " but got " + actualCall.arguments());
            }
        });
    }

    private boolean containsEntries(Map<String, Object> actual, Map<String, Object> expected) {
        if (expected == null || expected.isEmpty()) return true;
        if (actual == null) return false;
        return expected.entrySet().stream().allMatch(entry ->
                objectMapper.valueToTree(actual.get(entry.getKey())).equals(objectMapper.valueToTree(entry.getValue())));
    }

    private void assertFacts(AssistantEvaluationScenario.Expectation expected, String response,
                             List<String> assertions, List<String> failures) {
        for (String fact : expected.requiredFacts() == null ? List.<String>of() : expected.requiredFacts()) {
            assertions.add("required fact: " + fact);
            if (!response.contains(fact)) failures.add("response missing required fact: " + fact);
        }
        for (String fact : expected.forbiddenFacts() == null ? List.<String>of() : expected.forbiddenFacts()) {
            assertions.add("forbidden fact: " + fact);
            if (response.contains(fact)) failures.add("response contained forbidden fact: " + fact);
        }
    }

    private void assertAuthorization(AssistantEvaluationScenario.Expectation expected, String actual,
                                     List<String> assertions, List<String> failures) {
        if (expected.authorizationOutcome() == null) return;
        assertions.add("authorization expected=" + expected.authorizationOutcome() + " actual=" + actual);
        if (!expected.authorizationOutcome().equals(actual)) {
            failures.add("authorization expected " + expected.authorizationOutcome() + " but got " + actual);
        }
    }

    private record ActualTurn(String response, List<AssistantEvaluationScenario.ToolCall> toolCalls,
                              String authorizationOutcome) {}
}
