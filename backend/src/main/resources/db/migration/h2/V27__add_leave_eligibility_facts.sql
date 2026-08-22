CREATE TABLE staff_dependant (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    name VARCHAR(255) NOT NULL,
    relationship_code VARCHAR(64) NOT NULL,
    date_of_birth DATE,
    citizenship_code VARCHAR(64),
    residency_code VARCHAR(64),
    adoption_date DATE,
    effective_from DATE,
    effective_to DATE,
    active BOOLEAN NOT NULL,
    CONSTRAINT fk_staff_dependant_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE
);

CREATE INDEX idx_staff_dependant_tenant_staff ON staff_dependant(tenant_id, staff_id);

CREATE TABLE qualifying_leave_event (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(255) NOT NULL,
    staff_id VARCHAR(255) NOT NULL,
    dependant_id VARCHAR(36),
    event_type_code VARCHAR(100) NOT NULL,
    event_date DATE NOT NULL,
    start_date DATE,
    end_date DATE,
    external_reference VARCHAR(255),
    supporting_document_reference VARCHAR(255),
    status VARCHAR(32) NOT NULL,
    CONSTRAINT fk_qualifying_event_staff FOREIGN KEY (staff_id) REFERENCES staff(id) ON DELETE CASCADE,
    CONSTRAINT fk_qualifying_event_dependant FOREIGN KEY (dependant_id) REFERENCES staff_dependant(id)
);

CREATE INDEX idx_qualifying_event_tenant_staff ON qualifying_leave_event(tenant_id, staff_id);
CREATE INDEX idx_qualifying_event_dependant ON qualifying_leave_event(dependant_id);
CREATE INDEX idx_qualifying_event_type_date ON qualifying_leave_event(event_type_code, event_date);
