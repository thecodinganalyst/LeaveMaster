package com.practical.leavemaster.assistant.evaluation;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

final class AssistantEvaluationReportWriter {
    private final ObjectMapper objectMapper;

    AssistantEvaluationReportWriter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    void write(Path reportDirectory, List<AssistantEvaluationResult> results) throws IOException {
        Files.createDirectories(reportDirectory);
        objectMapper.writerWithDefaultPrettyPrinter()
                .writeValue(reportDirectory.resolve("assistant-evaluation.json").toFile(), results);
        Files.writeString(reportDirectory.resolve("assistant-evaluation.md"), markdown(results));
    }

    String markdown(List<AssistantEvaluationResult> results) {
        long passed = results.stream().filter(AssistantEvaluationResult::passed).count();
        StringBuilder out = new StringBuilder("# AskLeaveMaestro deterministic evaluation\n\n")
                .append("Scenarios: ").append(results.size())
                .append(" | Passed: ").append(passed)
                .append(" | Failed: ").append(results.size() - passed).append("\n\n")
                .append("| Scenario | Category | Critical | Result |\n")
                .append("|---|---|---:|---|\n");
        results.forEach(result -> out.append("| ").append(result.scenarioId())
                .append(" | ").append(result.category())
                .append(" | ").append(result.critical() ? "yes" : "no")
                .append(" | ").append(result.passed() ? "PASS" : "FAIL").append(" |\n"));
        for (AssistantEvaluationResult result : results) {
            if (!result.failures().isEmpty()) {
                out.append("\n## ").append(result.scenarioId()).append(" failures\n\n");
                result.failures().forEach(failure -> out.append("- ").append(failure).append("\n"));
            }
        }
        return out.toString();
    }
}
