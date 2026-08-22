ALTER TABLE leave_entitlement_policy
    ADD COLUMN event_entitlement_amount_mode VARCHAR(48) DEFAULT 'FIXED' NOT NULL;

ALTER TABLE qualifying_leave_event
    ADD COLUMN approved_entitlement_amount NUMERIC(12,4);
