package com.practical.leavemaster.assistant;

import org.springframework.data.jpa.repository.JpaRepository;

import java.time.Instant;

interface AssistantAuditEventRepository extends JpaRepository<AssistantAuditEvent, String> {
    long countByEventTypeAndActorLoginNameAndCreatedAtAfter(String eventType, String actorLoginName, Instant after);
    long countByEventTypeAndTenantIdAndCreatedAtAfter(String eventType, String tenantId, Instant after);
}
