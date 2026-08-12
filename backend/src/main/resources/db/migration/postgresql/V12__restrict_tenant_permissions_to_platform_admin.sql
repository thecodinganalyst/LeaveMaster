DELETE FROM app_role_permission
WHERE permission_code IN ('TENANT_READ', 'TENANT_WRITE')
  AND UPPER(role_id) <> 'PLATFORM_ADMIN';
