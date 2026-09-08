CREATE TABLE password_reset (
    user_id VARCHAR(36) PRIMARY KEY,
    pin_hash VARCHAR(255),
    requested_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    verified_at TIMESTAMP,
    consumed_at TIMESTAMP,
    request_window_started_at TIMESTAMP NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 0,
    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE
);
