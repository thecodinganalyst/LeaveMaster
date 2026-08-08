CREATE TABLE app_user (
    login_name VARCHAR(255) NOT NULL,
    password VARCHAR(255) NOT NULL,
    active BOOLEAN NOT NULL,
    staff_id VARCHAR(255),
    PRIMARY KEY (login_name)
);
