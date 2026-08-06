ALTER TABLE leave_application
    ADD COLUMN attachment_url VARCHAR(1024);

ALTER TABLE leave_application
    DROP COLUMN attachment;
