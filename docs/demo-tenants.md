# DEMO tenants

LeaveMaster supports two tenant types:

- `STANDARD` — the default for production/customer tenants.
- `DEMO` — an isolated sandbox tenant intended for product demonstrations and hands-on evaluation.

## Persistence and migration

`tenant.tenant_type` is persisted as `STANDARD` or `DEMO`. Migration V66 adds the column with a database default of `STANDARD`, so existing tenants retain normal production behavior without manual data changes.

## Isolation

DEMO tenants use the same tenant-scoped authentication, authorization, repositories, and `tenantId` boundaries as STANDARD tenants. Demo data is not placed in a global/shared namespace, and demo users do not receive broader permissions simply because the tenant is a sandbox.

Platform administrators retain normal tenant-lifecycle capabilities, including creating, updating, and deleting DEMO tenants.

## Sandbox safeguards

`DemoTenantPolicy` is the central server-side decision point for behavior that must differ for DEMO tenants. New outbound integrations or tenant-structural operations should consult this policy rather than adding tenant-name checks or frontend-only restrictions.

The initial safeguards are:

- Account-activation emails are not sent for DEMO tenants.
- Password-reset emails are not sent for DEMO tenants.
- OAuth account linking is blocked for DEMO tenants before redirecting to an external identity provider.
- Tenant users cannot add jurisdictions to a DEMO tenant. Platform administrators can still manage the tenant lifecycle and its tenant type.

Normal in-tenant leave workflows and data changes remain available so the tenant can be used as an interactive sandbox. Existing leave-notification behavior that is currently log-only remains unchanged.

## Frontend metadata

`GET /auth/me` exposes:

- `tenantType`: `STANDARD` or `DEMO`.
- `demo`: convenience boolean derived from `tenantType`.

The frontend `CurrentUser` model includes both fields so screens can display a clear demo/sandbox indicator and conditionally hide actions that the server will reject. Server-side enforcement remains authoritative.

## Extending DEMO behavior

When adding a new email provider, webhook, third-party integration, billing action, or other external side effect, route the tenant decision through `DemoTenantPolicy.suppressOutboundSideEffects(...)`. For structural operations that should not be available to demo users, use `DemoTenantPolicy.requireStandardTenant(...)`.

Do not implement DEMO behavior by matching a specific tenant ID or tenant name; tenant type is the source of truth.
