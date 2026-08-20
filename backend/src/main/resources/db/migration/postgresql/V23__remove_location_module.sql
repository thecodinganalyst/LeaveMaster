DELETE FROM leave_entitlement_policy_eligibility
WHERE criterion_type = 'LOCATION_ID';

DELETE FROM app_role_permission
WHERE permission_code IN ('LOCATION_READ', 'LOCATION_WRITE');

DELETE FROM app_permission
WHERE code IN ('LOCATION_READ', 'LOCATION_WRITE');

ALTER TABLE staff DROP CONSTRAINT IF EXISTS fk_staff_location;
ALTER TABLE staff DROP COLUMN IF EXISTS location_id CASCADE;

DROP TABLE IF EXISTS location CASCADE;
