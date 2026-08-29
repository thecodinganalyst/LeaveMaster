CREATE UNIQUE INDEX uk_app_user_oauth_identity
    ON app_user (oidc_provider, oidc_subject);
