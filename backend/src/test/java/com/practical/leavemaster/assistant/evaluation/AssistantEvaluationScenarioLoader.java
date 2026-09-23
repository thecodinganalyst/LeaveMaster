package com.practical.leavemaster.assistant.evaluation;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

final class AssistantEvaluationScenarioLoader {
    private final ObjectMapper objectMapper;

    AssistantEvaluationScenarioLoader(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    List<AssistantEvaluationScenario> load(String resource) throws IOException {
        try (InputStream input = getClass().getResourceAsStream(resource)) {
            if (input == null) throw new IllegalArgumentException("Scenario resource not found: " + resource);
            return Arrays.asList(objectMapper.readValue(input, AssistantEvaluationScenario[].class));
        }
    }
}
