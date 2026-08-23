package com.practical.leavemaster.assistant;

import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Normalizes Spring AI generated tool schemas to the conservative JSON Schema subset
 * accepted by providers such as Google Gemini. In particular, Gemini rejects the
 * {@code $defs} keyword used by generated schemas for nested Java types.
 */
final class AssistantToolSchemaNormalizer {

    private static final String DEFS = "$defs";
    private static final String REF = "$ref";
    private static final String DEFS_REF_PREFIX = "#/$defs/";

    private AssistantToolSchemaNormalizer() {
    }

    static ToolCallback[] normalize(ToolCallback[] callbacks, ObjectMapper objectMapper) {
        return Arrays.stream(callbacks)
                .map(callback -> normalize(callback, objectMapper))
                .toArray(ToolCallback[]::new);
    }

    private static ToolCallback normalize(ToolCallback delegate, ObjectMapper objectMapper) {
        ToolDefinition definition = normalize(delegate.getToolDefinition(), objectMapper);
        if (definition == delegate.getToolDefinition()) return delegate;

        return new ToolCallback() {
            @Override
            public ToolDefinition getToolDefinition() {
                return definition;
            }

            @Override
            public ToolMetadata getToolMetadata() {
                return delegate.getToolMetadata();
            }

            @Override
            public String call(String toolInput) {
                return delegate.call(toolInput);
            }

            @Override
            public String call(String toolInput, ToolContext toolContext) {
                return delegate.call(toolInput, toolContext);
            }
        };
    }

    private static ToolDefinition normalize(ToolDefinition definition, ObjectMapper objectMapper) {
        String schema = definition.inputSchema();
        if (schema == null || (!schema.contains("\"$defs\"") && !schema.contains("\"$ref\""))) {
            return definition;
        }

        try {
            @SuppressWarnings("unchecked")
            Map<String, Object> root = objectMapper.readValue(schema, Map.class);
            Object normalized = normalizeNode(root, Map.of(), new LinkedHashSet<>());
            String normalizedSchema = objectMapper.writeValueAsString(normalized);
            return ToolDefinition.builder()
                    .name(definition.name())
                    .description(definition.description())
                    .inputSchema(normalizedSchema)
                    .build();
        } catch (Exception e) {
            throw new IllegalArgumentException("Unable to normalize schema for assistant tool " + definition.name(), e);
        }
    }

    private static Object normalizeNode(Object node, Map<String, Object> inheritedDefinitions, Set<String> resolving) {
        if (node instanceof List<?> list) {
            List<Object> normalized = new ArrayList<>(list.size());
            for (Object item : list) normalized.add(normalizeNode(item, inheritedDefinitions, resolving));
            return normalized;
        }
        if (!(node instanceof Map<?, ?> rawMap)) return node;

        Map<String, Object> map = stringKeyMap(rawMap);
        Map<String, Object> definitions = new LinkedHashMap<>(inheritedDefinitions);
        Object localDefinitions = map.get(DEFS);
        if (localDefinitions instanceof Map<?, ?> localDefinitionMap) {
            definitions.putAll(stringKeyMap(localDefinitionMap));
        }

        Object ref = map.get(REF);
        if (ref instanceof String reference && reference.startsWith(DEFS_REF_PREFIX)) {
            String key = reference.substring(DEFS_REF_PREFIX.length());
            Object target = definitions.get(key);
            if (target != null) {
                if (!resolving.add(key)) {
                    return Map.of("type", "object");
                }
                Object resolved = normalizeNode(target, definitions, resolving);
                resolving.remove(key);
                if (resolved instanceof Map<?, ?> resolvedMap) {
                    Map<String, Object> merged = stringKeyMap(resolvedMap);
                    map.forEach((name, value) -> {
                        if (!REF.equals(name) && !DEFS.equals(name)) {
                            merged.put(name, normalizeNode(value, definitions, resolving));
                        }
                    });
                    return merged;
                }
                return resolved;
            }
        }

        Map<String, Object> normalized = new LinkedHashMap<>();
        map.forEach((name, value) -> {
            if (!DEFS.equals(name)) {
                normalized.put(name, normalizeNode(value, definitions, resolving));
            }
        });
        return normalized;
    }

    private static Map<String, Object> stringKeyMap(Map<?, ?> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        source.forEach((key, value) -> result.put(String.valueOf(key), value));
        return result;
    }
}
