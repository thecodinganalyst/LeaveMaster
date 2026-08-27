ALTER TABLE app_user ALTER COLUMN password DROP NOT NULL;

CREATE TABLE account_activation (
    login_name VARCHAR(255) NOT NULL,
    pin_hash VARCHAR(255),
    requested_at TIMESTAMP NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    failed_attempts INTEGER NOT NULL DEFAULT 0,
    verified_at TIMESTAMP,
    consumed_at TIMESTAMP,
    request_window_started_at TIMESTAMP NOT NULL,
    request_count INTEGER NOT NULL DEFAULT 1,
    PRIMARY KEY (login_name),
    CONSTRAINT fk_account_activation_user FOREIGN KEY (login_name)
        REFERENCES app_user(login_name) ON DELETE CASCADE
);

CREATE INDEX idx_account_activation_expires_at ON account_activation(expires_at);
