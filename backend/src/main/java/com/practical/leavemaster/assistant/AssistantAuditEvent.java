package com.practical.leavemaster.assistant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Entity
@Table(name = "assistant_audit_event")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AssistantAuditEvent {
    @Id
    @Column(nullable = false, length = 36)
    private String id;

    @Column(name = "event_type", nullable = false, length = 64)
    private String eventType;

    @Column(name = "actor_login_name", nullable = false)
    private String actorLoginName;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "conversation_id")
    private String conversationId;

    @Column(name = "tool_name", length = 120)
    private String toolName;

    @Column(name = "sanitized_arguments", length = 10000)
    private String sanitizedArguments;

    @Column(nullable = false, length = 32)
    private String outcome;

    @Column(length = 1000)
    private String detail;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
}
