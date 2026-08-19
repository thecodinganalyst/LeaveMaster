INSERT INTO app_permission (code, description)
SELECT 'PUBLIC_HOLIDAY_READ', 'Read platform public holiday seed data'
WHERE NOT EXISTS (SELECT 1 FROM app_permission WHERE code = 'PUBLIC_HOLIDAY_READ');

INSERT INTO app_permission (code, description)
SELECT 'PUBLIC_HOLIDAY_WRITE', 'Create, update and delete platform public holiday seed data'
WHERE NOT EXISTS (SELECT 1 FROM app_permission WHERE code = 'PUBLIC_HOLIDAY_WRITE');

INSERT INTO app_role_permission (role_id, permission_code)
SELECT 'PLATFORM_ADMIN', 'PUBLIC_HOLIDAY_READ'
WHERE EXISTS (SELECT 1 FROM app_role WHERE id = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission
      WHERE role_id = 'PLATFORM_ADMIN' AND permission_code = 'PUBLIC_HOLIDAY_READ'
  );

INSERT INTO app_role_permission (role_id, permission_code)
SELECT 'PLATFORM_ADMIN', 'PUBLIC_HOLIDAY_WRITE'
WHERE EXISTS (SELECT 1 FROM app_role WHERE id = 'PLATFORM_ADMIN')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission
      WHERE role_id = 'PLATFORM_ADMIN' AND permission_code = 'PUBLIC_HOLIDAY_WRITE'
  );
