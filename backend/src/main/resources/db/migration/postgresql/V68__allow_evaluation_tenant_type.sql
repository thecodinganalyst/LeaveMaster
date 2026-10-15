ALTER TABLE tenant DROP CONSTRAINT ck_tenant_type;

ALTER TABLE tenant ADD CONSTRAINT ck_tenant_type
    CHECK (tenant_type IN ('STANDARD', 'DEMO', 'EVALUATION'));
