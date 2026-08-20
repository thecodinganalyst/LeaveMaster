ALTER TABLE leave_calendar DROP CONSTRAINT IF EXISTS CK_leave_calendar_scope;

UPDATE leave_calendar
SET jurisdiction_id = (
    SELECT jurisdiction_id
    FROM tenant
    WHERE tenant.id = leave_calendar.tenant_id
)
WHERE scope = 'TENANT'
  AND jurisdiction_id IS NULL;

ALTER TABLE leave_calendar ALTER COLUMN jurisdiction_id VARCHAR(32) NOT NULL;
ALTER TABLE leave_calendar ADD CONSTRAINT CK_leave_calendar_scope CHECK (
    (scope = 'PLATFORM_TEMPLATE' AND tenant_id IS NULL)
    OR
    (scope = 'TENANT' AND tenant_id IS NOT NULL)
);

CREATE INDEX IF NOT EXISTS IDX_leave_calendar_tenant_jurisdiction_dates
    ON leave_calendar(tenant_id, jurisdiction_id, start_date, end_date);

ALTER TABLE staff ADD COLUMN IF NOT EXISTS jurisdiction_id VARCHAR(32);
UPDATE staff
SET jurisdiction_id = (
    SELECT jurisdiction_id
    FROM tenant
    WHERE tenant.id = staff.tenant_id
)
WHERE jurisdiction_id IS NULL;

ALTER TABLE public_holiday DROP COLUMN IF EXISTS location_id;
