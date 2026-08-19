# RBAC roles

LeaveMaster uses purpose-specific roles rather than a global unrestricted administrator role.

- `PLATFORM_ADMIN` is the platform-wide administrative role initialized by `PlatformAdminInitializer`.
- Tenant administrators use tenant-scoped role IDs in the form `TENANT_ADMIN_<tenantId>` and are provisioned when a tenant is created.
- Custom roles may be created and assigned according to the normal RBAC rules.
- The legacy global `ADMIN` role is no longer part of the supported role model.

## Platform Admin permissions

`PLATFORM_ADMIN` receives only the platform-wide capabilities it needs. In particular, platform public-holiday maintenance is separated from tenant leave-calendar management.

| Permission | Purpose |
|---|---|
| `TENANT_READ` | Read tenant records. |
| `TENANT_WRITE` | Create, update and delete tenant records. |
| `LEAVE_ENTITLEMENT_POLICY_READ` | Read platform entitlement-policy templates. |
| `LEAVE_ENTITLEMENT_POLICY_WRITE` | Manage platform entitlement-policy templates. |
| `LEAVE_ENTITLEMENT_GENERATE` | Run entitlement generation/provisioning operations. |
| `PUBLIC_HOLIDAY_READ` | Read platform jurisdiction public-holiday seed data. |
| `PUBLIC_HOLIDAY_WRITE` | Create, edit and delete platform jurisdiction public-holiday seed data. |

`PLATFORM_ADMIN` does **not** need `LEAVE_CALENDAR_READ` or `LEAVE_CALENDAR_WRITE` to manage public holidays. Those permissions remain tenant-calendar permissions and are independent from the platform public-holiday resource.

The application reconciles the required Platform Admin permissions at startup so existing installations receive the new public-holiday capabilities after the permission migration is applied.

See [Platform public holidays](public-holidays.md) for the ownership model and API details.

## Legacy `ADMIN` cleanup

Flyway migration `V10__remove_legacy_admin_role.sql` removes legacy `ADMIN` assignments, permission mappings, and the role itself in foreign-key-safe order for both H2 and PostgreSQL. The migration is safe when the role is already absent and does not grant users a replacement unrestricted role.

The original `V7__add_rbac.sql` migrations remain unchanged because released Flyway migrations are immutable: editing an already-applied migration would change its checksum and could break upgrades. A fresh database therefore reaches the supported state by applying the full migration chain, including the V10 cleanup.
