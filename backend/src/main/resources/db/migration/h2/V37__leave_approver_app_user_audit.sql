ALTER TABLE leave_approver ALTER COLUMN admin_id DROP NOT NULL;
ALTER TABLE leave_approver ADD COLUMN admin_login_name VARCHAR(255);
