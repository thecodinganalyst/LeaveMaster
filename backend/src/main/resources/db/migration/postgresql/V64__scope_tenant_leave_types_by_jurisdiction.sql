ALTER TABLE leave_type ADD COLUMN jurisdiction_id VARCHAR(32);

-- Existing tenant leave types that were sourced directly from a jurisdiction can be
-- attributed safely when that jurisdiction is associated with the tenant.
UPDATE leave_type lt
SET jurisdiction_id = jlt.jurisdiction_id
FROM jurisdiction_leave_type jlt
WHERE lt.jurisdiction_id IS NULL
  AND lt.source_jurisdiction_leave_type_id = jlt.id
  AND EXISTS (
      SELECT 1
      FROM tenant_jurisdiction tj
      WHERE tj.tenant_id = lt.tenant_id
        AND tj.jurisdiction_id = jlt.jurisdiction_id
  );

-- For legacy single-jurisdiction tenants the applicable jurisdiction is unambiguous,
-- including leave types inherited from a parent jurisdiction.
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

-- Preserve existing policy ids and references; only fill the already-existing policy
-- jurisdiction column from the linked tenant leave type when it was previously omitted.
UPDATE leave_entitlement_policy lep
SET jurisdiction_id = lt.jurisdiction_id
FROM leave_type lt
WHERE lep.scope = 'TENANT'
  AND lep.jurisdiction_id IS NULL
  AND lep.leave_type_id = lt.id
  AND lt.jurisdiction_id IS NOT NULL;

CREATE INDEX IDX_leave_type_tenant_jurisdiction
    ON leave_type(tenant_id, jurisdiction_id);
