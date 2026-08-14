CREATE TABLE leave_entitlement_policy (
    id VARCHAR(255) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    leave_type_id VARCHAR(255) NOT NULL REFERENCES leave_type(id),
    name VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    priority INTEGER NOT NULL,
    entitlement_unit VARCHAR(32) NOT NULL,
    entitlement_amount NUMERIC(12,4) NOT NULL,
    accrual_method VARCHAR(32) NOT NULL,
    accrual_rate NUMERIC(12,4),
    proration_method VARCHAR(32) NOT NULL,
    carry_forward_allowed BOOLEAN NOT NULL,
    carry_forward_limit NUMERIC(12,4),
    carry_forward_expiry_months INTEGER,
    effective_from DATE NOT NULL,
    effective_to DATE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IDX_leave_entitlement_policy_tenant ON leave_entitlement_policy(tenant_id);
CREATE INDEX IDX_leave_entitlement_policy_tenant_type ON leave_entitlement_policy(tenant_id, leave_type_id);

INSERT INTO app_permission (code, description) VALUES ('LEAVE_ENTITLEMENT_POLICY_READ', 'Read leave entitlement policy data');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_ENTITLEMENT_POLICY_WRITE', 'Create, update and delete leave entitlement policies');

INSERT INTO app_role_permission (role_id, permission_code)
SELECT r.id, 'LEAVE_ENTITLEMENT_POLICY_READ'
FROM app_role r
WHERE (RIGHT(r.id, 3) = '_HR' OR RIGHT(r.id, 6) = '_Admin' OR r.id = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_code = 'LEAVE_ENTITLEMENT_POLICY_READ'
  );

INSERT INTO app_role_permission (role_id, permission_code)
SELECT r.id, 'LEAVE_ENTITLEMENT_POLICY_WRITE'
FROM app_role r
WHERE (RIGHT(r.id, 3) = '_HR' OR RIGHT(r.id, 6) = '_Admin' OR r.id = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_code = 'LEAVE_ENTITLEMENT_POLICY_WRITE'
  );
