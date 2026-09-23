package com.practical.leavemaster.assistant.evaluation;

import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class AssistantEvaluationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void deterministicScenarioCataloguePassesAndWritesReports() throws Exception {
        List<AssistantEvaluationScenario> scenarios =
                new AssistantEvaluationScenarioLoader(objectMapper).load("/assistant-evaluation/scenarios.json");
        var runner = new AssistantEvaluationRunner(objectMapper);
        List<AssistantEvaluationResult> results = scenarios.stream().map(runner::run).toList();

        Path reports = Path.of("build", "reports", "assistant-evaluation");
        new AssistantEvaluationReportWriter(objectMapper).write(reports, results);

        assertThat(scenarios).extracting(AssistantEvaluationScenario::category)
                .contains("leave-balance", "eligibility-policy", "employment-boundary",
                        "calendar-work-schedule", "approver", "tenant-isolation", "rbac",
                        "multi-turn", "failure-handling");
        assertThat(results).allSatisfy(result ->
                assertThat(result.failures()).as(result.scenarioId()).isEmpty());
        assertThat(results.stream().filter(AssistantEvaluationResult::critical)).allMatch(AssistantEvaluationResult::passed);
    }

    @Test
    void runnerExplainsFailuresPrecisely() {
        var scenario = new AssistantEvaluationScenario(
                "failure-example", "framework", true,
                new AssistantEvaluationScenario.Actor("employee", "S1", List.of("STAFF")),
                "T1",
                List.of(new AssistantEvaluationScenario.Turn(
                        "balance?",
                        new AssistantEvaluationScenario.StubbedModelTurn(
                                "You have 4 days.", List.of(), "ALLOWED"),
                        new AssistantEvaluationScenario.Expectation(
                                List.of("getMyLeaveBalance"), null, List.of("5 days"),
                                List.of("other tenant"), "ALLOWED"))));
        AssistantEvaluationResult result = new AssistantEvaluationRunner(objectMapper).run(scenario);
        assertThat(result.passed()).isFalse();
        assertThat(result.failures())
                .anyMatch(value -> value.contains("expected tools"))
                .anyMatch(value -> value.contains("missing required fact"));
    }
}
