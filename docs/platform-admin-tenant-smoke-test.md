# PlatformAdmin tenant-management smoke test

Use this checklist after a production deployment that changes authentication, RBAC, tenant APIs, frontend routing, or the PlatformAdmin dashboard.

## Preconditions

- Use the production `PlatformAdmin` account through the normal login flow.
- Confirm the environment is healthy before changing tenant data.
- Create a disposable tenant only when it is safe to do so. Do not delete a real tenant to perform this check.

## Verification

1. Sign in as `PlatformAdmin`.
2. Confirm the dashboard shows **Tenant Administration** and the navigation contains **Tenants**.
3. Open **Tenants** and verify the tenant list loads without an authorization or routing error.
4. When safe, create a disposable tenant with an unmistakable test ID/name and verify it appears in the list and dashboard summary.
5. Open the disposable tenant and edit its name or lifecycle fields. Save and verify the updated values are shown after refresh.
6. Delete the disposable tenant, confirm the destructive action when prompted, and verify it disappears from the list.
7. If deletion fails, verify the UI displays the backend failure instead of silently removing the tenant from the page.
8. Sign out when verification is complete.

## Permission regression checks

When test accounts with restricted permissions are available:

- A user with `TENANT_READ` but not `TENANT_WRITE` should be able to view tenant data but should not see create/edit/delete controls.
- A user without `TENANT_READ` should not see the **Tenants** navigation item and direct tenant API requests should be denied.

## Expected API authorization

The tenant API requires:

- `TENANT_READ` for `GET /tenants` and `GET /tenants/{id}`;
- `TENANT_WRITE` for `POST /tenants`, `PUT /tenants/{id}`, and `DELETE /tenants/{id}`.

Do not lower backend JaCoCo or frontend coverage thresholds to make tenant-management changes pass CI. Add focused regression tests instead.
