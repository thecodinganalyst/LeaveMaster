package com.practical.leavemaster.assistant;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name="assistant_quality_event")
@Data @Builder @NoArgsConstructor @AllArgsConstructor
@lombok.Generated
class AssistantQualityEvent {
 @Id @Column(nullable=false,length=36) private String id;
 @Column(name="correlation_id",nullable=false,length=36) private String correlationId;
 @Column(name="tenant_id") private String tenantId;
 @Column(name="actor_role",length=80) private String actorRole;
 @Column(name="event_type",nullable=false,length=40) private String eventType;
 @Column(name="intent_category",length=80) private String intentCategory;
 @Column(name="provider",length=80) private String provider;
 @Column(name="model",length=120) private String model;
 @Column(name="tool_names",length=1000) private String toolNames;
 @Column(name="failure_category",length=40) private String failureCategory;
 @Column(name="latency_ms") private Long latencyMs;
 @Column(name="retry_count") private Integer retryCount;
 @Column(name="success",nullable=false) private boolean success;
 @Column(name="feedback_rating") private Integer feedbackRating;
 @Column(name="feedback_category",length=80) private String feedbackCategory;
 @Column(name="created_at",nullable=false) private Instant createdAt;
}
