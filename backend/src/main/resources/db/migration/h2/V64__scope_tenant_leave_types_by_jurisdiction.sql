ALTER TABLE leave_type ADD COLUMN jurisdiction_id VARCHAR(32);

-- V18 required tenant policies to have jurisdiction_id IS NULL. Tenant policies are now
-- scoped to their applicable jurisdiction, so relax that legacy constraint before any
-- backfill writes a jurisdiction id. Platform-template invariants remain unchanged.
ALTER TABLE leave_entitlement_policy DROP CONSTRAINT CK_leave_entitlement_policy_scope;
ALTER TABLE leave_entitlement_policy ADD CONSTRAINT CK_leave_entitlement_policy_scope CHECK (
    (scope = 'PLATFORM_TEMPLATE' AND tenant_id IS NULL AND leave_type_id IS NULL AND jurisdiction_id IS NOT NULL AND jurisdiction_leave_type_id IS NOT NULL)
    OR
    (scope = 'TENANT' AND tenant_id IS NOT NULL AND leave_type_id IS NOT NULL AND jurisdiction_leave_type_id IS NULL)
);

UPDATE leave_type lt
SET jurisdiction_id = (
    SELECT jlt.jurisdiction_id
    FROM jurisdiction_leave_type jlt
    WHERE jlt.id = lt.source_jurisdiction_leave_type_id
)
WHERE lt.jurisdiction_id IS NULL
  AND lt.source_jurisdiction_leave_type_id IS NOT NULL
  AND EXISTS (
      SELECT 1
      FROM jurisdiction_leave_type jlt
      WHERE jlt.id = lt.source_jurisdiction_leave_type_id
        AND EXISTS (
            SELECT 1
            FROM tenant_jurisdiction tj
            WHERE tj.tenant_id = lt.tenant_id
              AND tj.jurisdiction_id = jlt.jurisdiction_id
        )
  );

UPDATE leave_type lt
SET jurisdiction_id = (
    SELECT MIN(tj.jurisdiction_id)
    FROM tenant_jurisdiction tj
    WHERE tj.tenant_id = lt.tenant_id
)
WHERE lt.jurisdiction_id IS NULL
  AND 1 = (
      SELECT COUNT(*)
      FROM tenant_jurisdiction tj
      WHERE tj.tenant_id = lt.tenant_id
  );

UPDATE leave_entitlement_policy lep
SET jurisdiction_id = (
    SELECT lt.jurisdiction_id
    FROM leave_type lt
    WHERE lt.id = lep.leave_type_id
)
WHERE lep.scope = 'TENANT'
  AND lep.jurisdiction_id IS NULL
  AND EXISTS (
      SELECT 1
      FROM leave_type lt
      WHERE lt.id = lep.leave_type_id
        AND lt.jurisdiction_id IS NOT NULL
  );

CREATE INDEX IDX_leave_type_tenant_jurisdiction
    ON leave_type(tenant_id, jurisdiction_id);
