# RBAC roles

LeaveMaster uses purpose-specific roles rather than a global unrestricted administrator role.

- `PLATFORM_ADMIN` is the platform-wide administrative role initialized by `PlatformAdminInitializer`.
- Tenant administrators use tenant-scoped role IDs in the form `TENANT_ADMIN_<tenantId>` and are provisioned when a tenant is created.
- Custom roles may be created and assigned according to the normal RBAC rules.
- The legacy global `ADMIN` role is no longer part of the supported role model.

## Legacy `ADMIN` cleanup

Flyway migration `V10__remove_legacy_admin_role.sql` removes legacy `ADMIN` assignments, permission mappings, and the role itself in foreign-key-safe order for both H2 and PostgreSQL. The migration is safe when the role is already absent and does not grant users a replacement unrestricted role.

The original `V7__add_rbac.sql` migrations remain unchanged because released Flyway migrations are immutable: editing an already-applied migration would change its checksum and could break upgrades. A fresh database therefore reaches the supported state by applying the full migration chain, including the V10 cleanup.
