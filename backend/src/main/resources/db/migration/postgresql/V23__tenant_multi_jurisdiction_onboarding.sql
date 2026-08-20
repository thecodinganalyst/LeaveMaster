CREATE TABLE tenant_jurisdiction (
    id VARCHAR(255) NOT NULL,
    tenant_id VARCHAR(255) NOT NULL,
    jurisdiction_id VARCHAR(32) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    CONSTRAINT UK_tenant_jurisdiction UNIQUE (tenant_id, jurisdiction_id),
    CONSTRAINT FK_tenant_jurisdiction_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    CONSTRAINT FK_tenant_jurisdiction_jurisdiction FOREIGN KEY (jurisdiction_id) REFERENCES jurisdiction(id)
);

CREATE INDEX IDX_tenant_jurisdiction_tenant ON tenant_jurisdiction(tenant_id);
CREATE INDEX IDX_tenant_jurisdiction_jurisdiction ON tenant_jurisdiction(jurisdiction_id);

INSERT INTO tenant_jurisdiction (id, tenant_id, jurisdiction_id, created_at)
SELECT t.id || ':' || t.jurisdiction_id, t.id, t.jurisdiction_id, CURRENT_TIMESTAMP
FROM tenant t
WHERE t.jurisdiction_id IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM tenant_jurisdiction tj
      WHERE tj.tenant_id = t.id AND tj.jurisdiction_id = t.jurisdiction_id
  );

INSERT INTO app_role_permission (role_id, permission_code)
SELECT r.id, 'JURISDICTION_READ'
FROM app_role r
WHERE (RIGHT(r.id, 3) = '_HR' OR RIGHT(r.id, 6) = '_Admin')
  AND EXISTS (SELECT 1 FROM app_permission p WHERE p.code = 'JURISDICTION_READ')
  AND NOT EXISTS (
      SELECT 1 FROM app_role_permission rp
      WHERE rp.role_id = r.id AND rp.permission_code = 'JURISDICTION_READ'
  );
