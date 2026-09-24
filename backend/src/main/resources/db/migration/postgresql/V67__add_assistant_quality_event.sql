CREATE TABLE assistant_quality_event (
 id VARCHAR(36) PRIMARY KEY,
 correlation_id VARCHAR(36) NOT NULL,
 tenant_id VARCHAR(255),
 actor_role VARCHAR(80),
 event_type VARCHAR(40) NOT NULL,
 intent_category VARCHAR(80),
 provider VARCHAR(80),
 model VARCHAR(120),
 tool_names VARCHAR(1000),
 failure_category VARCHAR(40),
 latency_ms BIGINT,
 retry_count INTEGER,
 success BOOLEAN NOT NULL,
 feedback_rating INTEGER,
 feedback_category VARCHAR(80),
 created_at TIMESTAMP NOT NULL
);
CREATE INDEX idx_assistant_quality_tenant_created ON assistant_quality_event(tenant_id, created_at);
CREATE INDEX idx_assistant_quality_correlation ON assistant_quality_event(correlation_id);
