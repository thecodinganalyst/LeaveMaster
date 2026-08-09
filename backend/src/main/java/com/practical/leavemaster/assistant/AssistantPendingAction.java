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
@Table(name = "assistant_pending_action")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
class AssistantPendingAction {
    @Id
    @Column(name = "confirmation_token", nullable = false, length = 64)
    private String confirmationToken;

    @Column(name = "tool_name", nullable = false, length = 120)
    private String toolName;

    @Column(name = "arguments_json", nullable = false, length = 20000)
    private String argumentsJson;

    @Column(name = "required_authority", nullable = false, length = 120)
    private String requiredAuthority;

    @Column(name = "actor_login_name", nullable = false)
    private String actorLoginName;

    @Column(name = "actor_staff_id")
    private String actorStaffId;

    @Column(name = "tenant_id")
    private String tenantId;

    @Column(name = "conversation_id", nullable = false)
    private String conversationId;

    @Column(nullable = false, length = 32)
    private String status;

    @Column(name = "result_json", length = 20000)
    private String resultJson;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "executed_at")
    private Instant executedAt;
}
