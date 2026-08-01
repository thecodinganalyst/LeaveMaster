CREATE TABLE tenant (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE,
    status VARCHAR(50) NOT NULL,
    PRIMARY KEY (id)
);

ALTER TABLE location
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE leave_type
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE staff
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE leave_calendar
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE leave_application
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE leave_approver
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE leave_entitlement
    ADD COLUMN tenant_id VARCHAR(255);

ALTER TABLE app_user
    ADD COLUMN tenant_id VARCHAR(255);
