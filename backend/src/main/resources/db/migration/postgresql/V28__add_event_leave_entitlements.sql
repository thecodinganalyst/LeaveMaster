ALTER TABLE leave_entitlement_policy ADD COLUMN qualifying_event_type_code VARCHAR(100);
ALTER TABLE leave_entitlement_policy ADD COLUMN event_requires_verification BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE leave_entitlement_policy ADD COLUMN event_validity_days_before INTEGER;
ALTER TABLE leave_entitlement_policy ADD COLUMN event_validity_days_after INTEGER;

CREATE TABLE event_leave_entitlement (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    leave_type_id VARCHAR(255) NOT NULL,
    policy_id VARCHAR(255) NOT NULL,
    qualifying_event_id VARCHAR(36) NOT NULL,
    valid_from DATE NOT NULL,
    valid_to DATE NOT NULL,
    granted_amount NUMERIC(12,4) NOT NULL,
    used_amount NUMERIC(12,4) NOT NULL DEFAULT 0,
    status VARCHAR(32) NOT NULL,
    generated_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT FK_event_entitlement_staff FOREIGN KEY (staff_id) REFERENCES staff(id),
    CONSTRAINT FK_event_entitlement_leave_type FOREIGN KEY (leave_type_id) REFERENCES leave_type(id),
    CONSTRAINT FK_event_entitlement_policy FOREIGN KEY (policy_id) REFERENCES leave_entitlement_policy(id),
    CONSTRAINT FK_event_entitlement_event FOREIGN KEY (qualifying_event_id) REFERENCES qualifying_leave_event(id),
    CONSTRAINT UK_event_entitlement_event_policy UNIQUE (qualifying_event_id, policy_id)
);

CREATE INDEX IDX_event_entitlement_tenant_staff_type
    ON event_leave_entitlement(tenant_id, staff_id, leave_type_id);

ALTER TABLE leave_application ADD COLUMN event_entitlement_id VARCHAR(36);
ALTER TABLE leave_application
    ADD CONSTRAINT FK_leave_application_event_entitlement
    FOREIGN KEY (event_entitlement_id) REFERENCES event_leave_entitlement(id);
CREATE INDEX IDX_leave_application_event_entitlement ON leave_application(event_entitlement_id);
