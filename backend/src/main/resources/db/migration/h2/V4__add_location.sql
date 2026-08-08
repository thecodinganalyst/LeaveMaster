CREATE TABLE location (
    id VARCHAR(255) NOT NULL,
    location_name VARCHAR(255) NOT NULL,
    country VARCHAR(255) NOT NULL,
    state VARCHAR(255),
    PRIMARY KEY (id)
);

ALTER TABLE staff
    ADD COLUMN location_id VARCHAR(255);

ALTER TABLE staff
    ADD CONSTRAINT FK_staff_location FOREIGN KEY (location_id) REFERENCES location;

ALTER TABLE public_holiday
    ADD COLUMN location_id VARCHAR(255);
