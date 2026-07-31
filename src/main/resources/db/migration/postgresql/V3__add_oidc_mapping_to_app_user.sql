ALTER TABLE app_user
    ADD COLUMN oidc_provider VARCHAR(100);

ALTER TABLE app_user
    ADD COLUMN oidc_subject VARCHAR(255);

CREATE UNIQUE INDEX uq_app_user_oidc_identity ON app_user (oidc_provider, oidc_subject);
