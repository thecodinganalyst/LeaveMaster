CREATE TABLE app_user (
    id VARCHAR(255) NOT NULL,
    login_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    staff_id VARCHAR(255),
    PRIMARY KEY (id),
    CONSTRAINT UQ_app_user_login_name UNIQUE (login_name)
);
