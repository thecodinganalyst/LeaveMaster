ALTER TABLE tenant ADD COLUMN jurisdiction_id VARCHAR(32) DEFAULT 'SG' NOT NULL;
ALTER TABLE leave_type ADD COLUMN source_jurisdiction_leave_type_id VARCHAR(128);

ALTER TABLE leave_entitlement_policy ALTER COLUMN tenant_id DROP NOT NULL;
ALTER TABLE leave_entitlement_policy ALTER COLUMN leave_type_id DROP NOT NULL;
ALTER TABLE leave_entitlement_policy ADD COLUMN scope VARCHAR(32) DEFAULT 'TENANT' NOT NULL;
ALTER TABLE leave_entitlement_policy ADD COLUMN jurisdiction_id VARCHAR(32);
ALTER TABLE leave_entitlement_policy ADD COLUMN jurisdiction_leave_type_id VARCHAR(128);
ALTER TABLE leave_entitlement_policy ADD COLUMN source_template_id VARCHAR(255);
ALTER TABLE leave_entitlement_policy ADD CONSTRAINT CK_leave_entitlement_policy_scope CHECK (
    (scope = 'PLATFORM_TEMPLATE' AND tenant_id IS NULL AND leave_type_id IS NULL AND jurisdiction_id IS NOT NULL AND jurisdiction_leave_type_id IS NOT NULL)
    OR
    (scope = 'TENANT' AND tenant_id IS NOT NULL AND leave_type_id IS NOT NULL AND jurisdiction_id IS NULL AND jurisdiction_leave_type_id IS NULL)
);
CREATE INDEX IDX_leave_entitlement_policy_scope_jurisdiction ON leave_entitlement_policy(scope, jurisdiction_id);
CREATE INDEX IDX_leave_entitlement_policy_source_template ON leave_entitlement_policy(tenant_id, source_template_id);

ALTER TABLE leave_calendar ADD COLUMN scope VARCHAR(32) DEFAULT 'TENANT' NOT NULL;
ALTER TABLE leave_calendar ADD COLUMN jurisdiction_id VARCHAR(32);
ALTER TABLE leave_calendar ADD COLUMN source_template_id VARCHAR(255);
UPDATE leave_calendar SET scope = 'PLATFORM_TEMPLATE', jurisdiction_id = 'SG' WHERE tenant_id IS NULL;
ALTER TABLE leave_calendar ADD CONSTRAINT CK_leave_calendar_scope CHECK (
    (scope = 'PLATFORM_TEMPLATE' AND tenant_id IS NULL AND jurisdiction_id IS NOT NULL)
    OR
    (scope = 'TENANT' AND tenant_id IS NOT NULL AND jurisdiction_id IS NULL)
);
CREATE INDEX IDX_leave_calendar_scope_jurisdiction ON leave_calendar(scope, jurisdiction_id);
CREATE INDEX IDX_leave_calendar_source_template ON leave_calendar(tenant_id, source_template_id);
