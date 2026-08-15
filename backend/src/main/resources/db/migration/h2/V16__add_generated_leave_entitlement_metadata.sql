ALTER TABLE leave_entitlement ADD COLUMN policy_id VARCHAR(255);
ALTER TABLE leave_entitlement ADD COLUMN base_entitlement_amount DECIMAL(10,2);
ALTER TABLE leave_entitlement ADD COLUMN carried_forward_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE leave_entitlement ADD COLUMN adjustment_amount DECIMAL(10,2) NOT NULL DEFAULT 0;
ALTER TABLE leave_entitlement ADD COLUMN generated_at TIMESTAMP WITH TIME ZONE;

ALTER TABLE leave_entitlement
    ADD CONSTRAINT FK_leave_entitlement_policy_source
    FOREIGN KEY (policy_id) REFERENCES leave_entitlement_policy(id);

CREATE UNIQUE INDEX UK_leave_entitlement_staff_type_period
    ON leave_entitlement(staff_id, leave_type_id, from_date, to_date);

INSERT INTO app_permission (code, description)
VALUES ('LEAVE_ENTITLEMENT_GENERATE', 'Generate and reconcile employee leave entitlements from policies');

INSERT INTO app_role_permission (role_id, permission_code)
SELECT r.id, 'LEAVE_ENTITLEMENT_GENERATE'
FROM app_role r
WHERE (RIGHT(r.id, 3) = '_HR' OR RIGHT(r.id, 6) = '_Admin' OR r.id = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_code = 'LEAVE_ENTITLEMENT_GENERATE'
  );
