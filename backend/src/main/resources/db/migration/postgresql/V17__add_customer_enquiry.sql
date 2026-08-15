CREATE TABLE customer_enquiry (
    id VARCHAR(36) PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    company VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(40),
    company_size VARCHAR(60),
    country VARCHAR(100),
    enquiry_type VARCHAR(40) NOT NULL,
    message VARCHAR(4000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMP NOT NULL
);

CREATE INDEX idx_customer_enquiry_status ON customer_enquiry(status);
CREATE INDEX idx_customer_enquiry_created_at ON customer_enquiry(created_at);
