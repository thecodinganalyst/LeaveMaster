ALTER TABLE customer_enquiry ADD COLUMN first_read_at TIMESTAMP;

CREATE TABLE customer_enquiry_reply (
    id VARCHAR(36) PRIMARY KEY,
    enquiry_id VARCHAR(36) NOT NULL,
    reply_body VARCHAR(4000) NOT NULL,
    replied_by VARCHAR(120) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    CONSTRAINT fk_customer_enquiry_reply_enquiry FOREIGN KEY (enquiry_id) REFERENCES customer_enquiry(id) ON DELETE CASCADE
);

CREATE INDEX idx_customer_enquiry_reply_enquiry ON customer_enquiry_reply(enquiry_id);
