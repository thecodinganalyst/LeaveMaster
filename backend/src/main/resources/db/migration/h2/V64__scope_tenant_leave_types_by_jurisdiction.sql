ALTER TABLE leave_type ADD COLUMN jurisdiction_id VARCHAR(32);

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
