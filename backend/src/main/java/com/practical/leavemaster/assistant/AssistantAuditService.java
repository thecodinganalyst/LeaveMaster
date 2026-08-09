package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
class AssistantAuditService {
    static final String CHAT_REQUEST = "CHAT_REQUEST";
    static final String TOOL_EXECUTION = "TOOL_EXECUTION";
    static final String ACTION_CONFIRMATION = "ACTION_CONFIRMATION";

    private final AssistantAuditEventRepository repository;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    void record(String eventType, String actor, String tenant, String conversationId,
                String toolName, Object arguments, String outcome, String detail) {
        repository.save(AssistantAuditEvent.builder()
                .id(UUID.randomUUID().toString())
                .eventType(eventType)
                .actorLoginName(actor)
                .tenantId(tenant)
                .conversationId(conversationId)
                .toolName(toolName)
                .sanitizedArguments(sanitize(arguments))
                .outcome(outcome)
                .detail(truncate(detail, 1000))
                .createdAt(Instant.now())
                .build());
    }

    private String sanitize(Object value) {
        if (value == null) return null;
        try {
            return truncate(objectMapper.writeValueAsString(redact(value)), 10000);
        } catch (Exception ignored) {
            return "[unserializable]";
        }
    }

    private Object redact(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sanitized = new LinkedHashMap<>();
            map.forEach((key, item) -> {
                String name = String.valueOf(key);
                sanitized.put(name, isSensitiveKey(name) ? "[REDACTED]" : redact(item));
            });
            return sanitized;
        }
        if (value instanceof Iterable<?> iterable) {
            java.util.List<Object> sanitized = new java.util.ArrayList<>();
            iterable.forEach(item -> sanitized.add(redact(item)));
            return sanitized;
        }
        return value;
    }

    private boolean isSensitiveKey(String key) {
        String normalized = key.toLowerCase(Locale.ROOT).replace("_", "").replace("-", "");
        return normalized.contains("password") || normalized.contains("secret") || normalized.contains("token")
                || normalized.contains("apikey") || normalized.contains("authorization") || normalized.contains("credential");
    }

    private String truncate(String value, int max) {
        if (value == null || value.length() <= max) return value;
        return value.substring(0, max);
    }
}
