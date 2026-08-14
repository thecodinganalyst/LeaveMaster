CREATE TABLE leave_entitlement_policy_eligibility (
    id VARCHAR(255) NOT NULL,
    policy_id VARCHAR(255) NOT NULL,
    criterion_type VARCHAR(64) NOT NULL,
    operator VARCHAR(64) NOT NULL,
    criterion_value VARCHAR(1024) NOT NULL,
    active BOOLEAN NOT NULL,
    sort_order INTEGER NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT FK_policy_eligibility_policy FOREIGN KEY (policy_id)
        REFERENCES leave_entitlement_policy(id) ON DELETE CASCADE
);

CREATE INDEX IDX_policy_eligibility_policy ON leave_entitlement_policy_eligibility(policy_id);
CREATE INDEX IDX_policy_eligibility_policy_active ON leave_entitlement_policy_eligibility(policy_id, active, sort_order);
