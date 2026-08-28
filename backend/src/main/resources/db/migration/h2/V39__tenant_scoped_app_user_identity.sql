ALTER TABLE app_user ADD COLUMN user_id VARCHAR(36);
UPDATE app_user SET user_id = CAST(RANDOM_UUID() AS VARCHAR(36)) WHERE user_id IS NULL;

ALTER TABLE app_user_role ADD COLUMN user_id VARCHAR(36);
UPDATE app_user_role aur
SET user_id = (SELECT au.user_id FROM app_user au WHERE au.login_name = aur.login_name);

ALTER TABLE account_activation ADD COLUMN user_id VARCHAR(36);
UPDATE account_activation aa
SET user_id = (SELECT au.user_id FROM app_user au WHERE au.login_name = aa.login_name);

ALTER TABLE app_user_role DROP CONSTRAINT FK_app_user_role_user;
ALTER TABLE account_activation DROP CONSTRAINT fk_account_activation_user;

ALTER TABLE app_user_role DROP PRIMARY KEY;
ALTER TABLE account_activation DROP PRIMARY KEY;
ALTER TABLE app_user DROP PRIMARY KEY;

ALTER TABLE app_user ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE app_user_role ALTER COLUMN user_id SET NOT NULL;
ALTER TABLE account_activation ALTER COLUMN user_id SET NOT NULL;

ALTER TABLE app_user ADD CONSTRAINT PK_app_user PRIMARY KEY (user_id);
ALTER TABLE app_user ADD CONSTRAINT UK_app_user_tenant_login UNIQUE (tenant_id, login_name);

ALTER TABLE app_user_role DROP COLUMN login_name;
ALTER TABLE app_user_role ADD CONSTRAINT PK_app_user_role PRIMARY KEY (user_id, role_id);
ALTER TABLE app_user_role ADD CONSTRAINT FK_app_user_role_user
    FOREIGN KEY (user_id) REFERENCES app_user(user_id);

ALTER TABLE account_activation DROP COLUMN login_name;
ALTER TABLE account_activation ADD CONSTRAINT PK_account_activation PRIMARY KEY (user_id);
ALTER TABLE account_activation ADD CONSTRAINT fk_account_activation_user
    FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE;
