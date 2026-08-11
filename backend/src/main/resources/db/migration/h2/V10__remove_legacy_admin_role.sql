DELETE FROM app_user_role
WHERE role_id = 'ADMIN';

DELETE FROM app_role_permission
WHERE role_id = 'ADMIN';

DELETE FROM app_role
WHERE id = 'ADMIN';
