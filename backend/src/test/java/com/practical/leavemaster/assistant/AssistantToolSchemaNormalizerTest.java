package com.practical.leavemaster.assistant;

import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.ObjectMapper;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AssistantToolSchemaNormalizerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void shouldInlineDefsReferencesAndRemoveUnsupportedDefsKeyword() {
        ToolCallback callback = callback("createStaff", """
                {
                  "type":"object",
                  "properties":{
                    "staff":{
                      "$defs":{
                        "Schedule":{"type":"object","properties":{"day":{"type":"string"}}}
                      },
                      "type":"object",
                      "properties":{
                        "workSchedule":{"type":"array","items":{"$ref":"#/$defs/Schedule"}}
                      }
                    }
                  }
                }
                """);

        ToolCallback normalized = AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper)[0];
        String schema = normalized.getToolDefinition().inputSchema();

        assertThat(schema)
                .doesNotContain("\"$defs\"")
                .doesNotContain("\"$ref\"")
                .contains("workSchedule")
                .contains("day");
        assertThat(normalized.getToolDefinition().name()).isEqualTo("createStaff");
        assertThat(normalized.getToolDefinition().description()).isEqualTo("createStaff description");
    }

    @Test
    void shouldPreserveReferenceSiblingsWhenInliningDefinition() {
        ToolCallback callback = callback("tool", """
                {
                  "$defs":{"Input":{"type":"object","properties":{"name":{"type":"string"}}}},
                  "type":"object",
                  "properties":{"input":{"$ref":"#/$defs/Input","description":"Assistant input"}}
                }
                """);

        String schema = AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper)[0]
                .getToolDefinition().inputSchema();

        assertThat(schema)
                .doesNotContain("$defs")
                .doesNotContain("$ref")
                .contains("Assistant input")
                .contains("name");
    }

    @Test
    void shouldCollapseRecursiveReferenceWithoutLeavingUnsupportedSchemaKeywords() {
        ToolCallback callback = callback("recursiveTool", """
                {
                  "$defs":{"Node":{"type":"object","properties":{"child":{"$ref":"#/$defs/Node"}}}},
                  "type":"object",
                  "properties":{"root":{"$ref":"#/$defs/Node"}}
                }
                """);

        String schema = AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper)[0]
                .getToolDefinition().inputSchema();

        assertThat(schema)
                .doesNotContain("$defs")
                .doesNotContain("$ref")
                .contains("child")
                .contains("\"type\":\"object\"");
    }

    @Test
    void shouldLeaveSimpleSchemaCallbackUntouched() {
        ToolCallback callback = callback("simple", "{\"type\":\"object\",\"properties\":{\"id\":{\"type\":\"string\"}}}");

        ToolCallback normalized = AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper)[0];

        assertThat(normalized).isSameAs(callback);
    }

    @Test
    void shouldDelegateBothToolInvocationFormsAndMetadata() {
        ToolMetadata metadata = ToolMetadata.builder().build();
        ToolCallback callback = new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name("delegated")
                        .description("delegated description")
                        .inputSchema("{\"$defs\":{\"Value\":{\"type\":\"string\"}},\"type\":\"object\",\"properties\":{\"value\":{\"$ref\":\"#/$defs/Value\"}}}")
                        .build();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return metadata;
            }

            @Override
            public String call(String toolInput) {
                return "plain:" + toolInput;
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return "context:" + toolInput;
            }
        };

        ToolCallback normalized = AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper)[0];

        assertThat(normalized.getToolMetadata()).isSameAs(metadata);
        assertThat(normalized.call("{}" )).isEqualTo("plain:{}");
        assertThat(normalized.call("{}", null)).isEqualTo("context:{}");
    }

    @Test
    void shouldFailFastWhenSchemaThatNeedsNormalizationIsInvalidJson() {
        ToolCallback callback = callback("broken", "{\"$defs\": not-json }");

        assertThatThrownBy(() -> AssistantToolSchemaNormalizer.normalize(new ToolCallback[]{callback}, objectMapper))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("broken");
    }

    private ToolCallback callback(String name, String schema) {
        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                        .name(name)
                        .description(name + " description")
                        .inputSchema(schema)
                        .build();
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return ToolMetadata.builder().build();
            }

            @Override
            public String call(String toolInput) {
                return toolInput;
            }
        };
    }
}
