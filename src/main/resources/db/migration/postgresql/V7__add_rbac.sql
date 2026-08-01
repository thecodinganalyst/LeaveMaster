CREATE TABLE app_permission (
    code VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    PRIMARY KEY (code)
);

CREATE TABLE app_role (
    id VARCHAR(255) NOT NULL,
    description VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE app_role_permission (
    role_id VARCHAR(255) NOT NULL,
    permission_code VARCHAR(255) NOT NULL,
    PRIMARY KEY (role_id, permission_code)
);

CREATE TABLE app_user_role (
    login_name VARCHAR(255) NOT NULL,
    role_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (login_name, role_id)
);

ALTER TABLE IF EXISTS app_role_permission
    ADD CONSTRAINT FK_app_role_permission_role FOREIGN KEY (role_id) REFERENCES app_role;

ALTER TABLE IF EXISTS app_role_permission
    ADD CONSTRAINT FK_app_role_permission_permission FOREIGN KEY (permission_code) REFERENCES app_permission;

ALTER TABLE IF EXISTS app_user_role
    ADD CONSTRAINT FK_app_user_role_user FOREIGN KEY (login_name) REFERENCES app_user;

ALTER TABLE IF EXISTS app_user_role
    ADD CONSTRAINT FK_app_user_role_role FOREIGN KEY (role_id) REFERENCES app_role;

INSERT INTO app_permission (code, description) VALUES ('TENANT_READ', 'Read tenant data');
INSERT INTO app_permission (code, description) VALUES ('TENANT_WRITE', 'Create, update and delete tenants');
INSERT INTO app_permission (code, description) VALUES ('USER_READ', 'Read user data');
INSERT INTO app_permission (code, description) VALUES ('USER_WRITE', 'Create, update and delete users');
INSERT INTO app_permission (code, description) VALUES ('ROLE_MANAGE', 'Create, update, enable and disable roles and role assignments');
INSERT INTO app_permission (code, description) VALUES ('STAFF_READ', 'Read staff data');
INSERT INTO app_permission (code, description) VALUES ('STAFF_WRITE', 'Create, update and delete staff');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_TYPE_READ', 'Read leave type data');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_TYPE_WRITE', 'Create, update and delete leave types');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_APPROVER_READ', 'Read leave approver data');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_APPROVER_WRITE', 'Create, update and delete leave approvers');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_CALENDAR_READ', 'Read leave calendar data');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_CALENDAR_WRITE', 'Create, update and delete leave calendars');
INSERT INTO app_permission (code, description) VALUES ('LOCATION_READ', 'Read location data');
INSERT INTO app_permission (code, description) VALUES ('LOCATION_WRITE', 'Create, update and delete locations');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_APPLICATION_READ', 'Read leave application data');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_APPLICATION_WRITE', 'Create, update and delete leave applications');
INSERT INTO app_permission (code, description) VALUES ('LEAVE_APPLICATION_APPROVE', 'Approve or reject leave applications and cancellations');

INSERT INTO app_role (id, description, active) VALUES ('ADMIN', 'Default administrator role', TRUE);

INSERT INTO app_role_permission (role_id, permission_code)
SELECT 'ADMIN', code FROM app_permission;

INSERT INTO app_user_role (login_name, role_id)
SELECT login_name, 'ADMIN' FROM app_user;
