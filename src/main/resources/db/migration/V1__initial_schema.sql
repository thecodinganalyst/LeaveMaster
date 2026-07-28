CREATE TABLE leave_calendar (
    id VARCHAR(255) NOT NULL,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE leave_type (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    used BOOLEAN NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE staff (
    id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    email VARCHAR(255),
    join_date DATE NOT NULL,
    term_date DATE,
    PRIMARY KEY (id)
);

CREATE TABLE leave_approver (
    id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    approver_id VARCHAR(255) NOT NULL,
    effective_from DATE NOT NULL,
    effective_to DATE,
    admin_id VARCHAR(255) NOT NULL,
    admin_date DATE NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE leave_entitlement (
    entitlement DECIMAL(10,2) NOT NULL,
    from_date DATE NOT NULL,
    to_date DATE NOT NULL,
    id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    leave_type_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE leave_application (
    leave_date DATE NOT NULL,
    application_date DATE NOT NULL,
    approval_date DATE,
    approver_id VARCHAR(255),
    id VARCHAR(255) NOT NULL,
    leave_type_id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    attachment BYTEA,
    leave_duration VARCHAR(255) NOT NULL,
    status VARCHAR(255) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE public_holiday (
    holiday_date DATE NOT NULL,
    leave_calendar_id VARCHAR(255) NOT NULL,
    holiday_name VARCHAR(255) NOT NULL,
    PRIMARY KEY (leave_calendar_id, holiday_date, holiday_name)
);

CREATE TABLE work_schedule_day (
    day_of_week VARCHAR(255) NOT NULL,
    day_schedule VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    PRIMARY KEY (staff_id, day_of_week)
);

ALTER TABLE IF EXISTS leave_approver
    ADD CONSTRAINT FK_leave_approver_staff FOREIGN KEY (staff_id) REFERENCES staff;

ALTER TABLE IF EXISTS leave_approver
    ADD CONSTRAINT FK_leave_approver_approver FOREIGN KEY (approver_id) REFERENCES staff;

ALTER TABLE IF EXISTS leave_approver
    ADD CONSTRAINT FK_leave_approver_admin FOREIGN KEY (admin_id) REFERENCES staff;

ALTER TABLE IF EXISTS leave_entitlement
    ADD CONSTRAINT FK_leave_entitlement_staff FOREIGN KEY (staff_id) REFERENCES staff;

ALTER TABLE IF EXISTS leave_entitlement
    ADD CONSTRAINT FK_leave_entitlement_type FOREIGN KEY (leave_type_id) REFERENCES leave_type;

ALTER TABLE IF EXISTS leave_application
    ADD CONSTRAINT FK_leave_application_staff FOREIGN KEY (staff_id) REFERENCES staff;

ALTER TABLE IF EXISTS leave_application
    ADD CONSTRAINT FK_leave_application_type FOREIGN KEY (leave_type_id) REFERENCES leave_type;

ALTER TABLE IF EXISTS leave_application
    ADD CONSTRAINT FK_leave_application_approver FOREIGN KEY (approver_id) REFERENCES staff;

ALTER TABLE IF EXISTS public_holiday
    ADD CONSTRAINT FK_public_holiday_calendar FOREIGN KEY (leave_calendar_id) REFERENCES leave_calendar;

ALTER TABLE IF EXISTS work_schedule_day
    ADD CONSTRAINT FK_work_schedule_staff FOREIGN KEY (staff_id) REFERENCES staff;
