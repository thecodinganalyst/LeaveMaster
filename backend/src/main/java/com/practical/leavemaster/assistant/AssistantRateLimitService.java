package com.practical.leavemaster.assistant;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
@RequiredArgsConstructor
class AssistantRateLimitService {
    private final AssistantAuditEventRepository repository;
    private final AssistantAuditService auditService;

    @Value("${app.assistant.rate-limit.user-per-minute:20}")
    private int userPerMinute;

    @Value("${app.assistant.rate-limit.tenant-per-minute:100}")
    private int tenantPerMinute;

    @Value("${app.assistant.max-message-chars:4000}")
    private int maxMessageChars;

    void checkAndRecord(String actor, String tenant, String conversationId, String message) {
        if (message != null && message.length() > maxMessageChars) {
            throw new AssistantRateLimitException("Assistant message exceeds the configured size limit");
        }
        Instant after = Instant.now().minus(Duration.ofMinutes(1));
        if (repository.countByEventTypeAndActorLoginNameAndCreatedAtAfter(AssistantAuditService.CHAT_REQUEST, actor, after) >= userPerMinute) {
            throw new AssistantRateLimitException("Assistant request rate limit exceeded for this user");
        }
        if (tenant != null && repository.countByEventTypeAndTenantIdAndCreatedAtAfter(AssistantAuditService.CHAT_REQUEST, tenant, after) >= tenantPerMinute) {
            throw new AssistantRateLimitException("Assistant request rate limit exceeded for this tenant");
        }
        auditService.record(AssistantAuditService.CHAT_REQUEST, actor, tenant, conversationId, null,
                java.util.Map.of("messageLength", message == null ? 0 : message.length()), "ACCEPTED", null);
    }
}
