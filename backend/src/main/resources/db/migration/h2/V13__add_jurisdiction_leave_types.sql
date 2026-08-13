CREATE TABLE jurisdiction (
    id VARCHAR(32) NOT NULL,
    code VARCHAR(32) NOT NULL,
    name VARCHAR(255) NOT NULL,
    jurisdiction_type VARCHAR(32) NOT NULL,
    parent_id VARCHAR(32),
    country_code VARCHAR(2) NOT NULL,
    subdivision_code VARCHAR(32),
    active BOOLEAN NOT NULL,
    PRIMARY KEY (id),
    CONSTRAINT UK_jurisdiction_code UNIQUE (code),
    CONSTRAINT FK_jurisdiction_parent FOREIGN KEY (parent_id) REFERENCES jurisdiction(id)
);

CREATE TABLE jurisdiction_leave_type (
    id VARCHAR(128) NOT NULL,
    jurisdiction_id VARCHAR(32) NOT NULL,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    statutory BOOLEAN NOT NULL,
    paid BOOLEAN,
    active BOOLEAN NOT NULL,
    source_url VARCHAR(1000),
    source_name VARCHAR(255),
    effective_from DATE,
    effective_to DATE,
    PRIMARY KEY (id),
    CONSTRAINT UK_jurisdiction_leave_type UNIQUE (jurisdiction_id, code),
    CONSTRAINT FK_jurisdiction_leave_type_jurisdiction FOREIGN KEY (jurisdiction_id) REFERENCES jurisdiction(id)
);

INSERT INTO app_permission (code, description) VALUES ('JURISDICTION_READ', 'Read platform jurisdiction data');
INSERT INTO app_permission (code, description) VALUES ('JURISDICTION_WRITE', 'Create, update and delete platform jurisdictions');
INSERT INTO app_permission (code, description) VALUES ('JURISDICTION_LEAVE_TYPE_READ', 'Read jurisdiction leave type data');
INSERT INTO app_permission (code, description) VALUES ('JURISDICTION_LEAVE_TYPE_WRITE', 'Create, update and delete jurisdiction leave types');
