INSERT INTO app_role_permission (role_id, permission_code)
SELECT r.id, 'LEAVE_CALENDAR_READ'
FROM app_role r
WHERE r.tenant_id IS NOT NULL
  AND (r.id = r.tenant_id || '_Staff' OR r.id = r.tenant_id || '_Manager')
  AND NOT EXISTS (
      SELECT 1
      FROM app_role_permission rp
      WHERE rp.role_id = r.id
        AND rp.permission_code = 'LEAVE_CALENDAR_READ'
  );
