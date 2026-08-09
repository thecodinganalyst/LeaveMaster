CREATE TABLE assistant_pending_action (
    confirmation_token VARCHAR(64) PRIMARY KEY,
    tool_name VARCHAR(120) NOT NULL,
    arguments_json VARCHAR(20000) NOT NULL,
    required_authority VARCHAR(120) NOT NULL,
    actor_login_name VARCHAR(255) NOT NULL,
    actor_staff_id VARCHAR(255),
    tenant_id VARCHAR(255),
    conversation_id VARCHAR(255) NOT NULL,
    status VARCHAR(32) NOT NULL,
    result_json VARCHAR(20000),
    created_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    executed_at TIMESTAMP
);

CREATE INDEX idx_assistant_pending_actor ON assistant_pending_action(actor_login_name, created_at);
CREATE INDEX idx_assistant_pending_expiry ON assistant_pending_action(expires_at);

CREATE TABLE assistant_audit_event (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(64) NOT NULL,
    actor_login_name VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255),
    conversation_id VARCHAR(255),
    tool_name VARCHAR(120),
    sanitized_arguments VARCHAR(10000),
    outcome VARCHAR(32) NOT NULL,
    detail VARCHAR(1000),
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_assistant_audit_actor_time ON assistant_audit_event(actor_login_name, created_at);
CREATE INDEX idx_assistant_audit_tenant_time ON assistant_audit_event(tenant_id, created_at);
