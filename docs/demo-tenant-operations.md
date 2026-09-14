# DEMO tenant seed and reset

LeaveMaestro can maintain a reusable public evaluation tenant whose data is restored to a known Singapore-oriented baseline. The reset is intentionally restricted to tenants classified as `DEMO`; an existing `STANDARD` tenant is never eligible for this operation.

## Baseline dataset

The default configured tenant ID is `DEMO`. A reset creates `Demo Company Pte Ltd` in Singapore and provisions the normal Singapore leave configuration before adding representative personas and workflow data:

| Persona | Login | Purpose |
|---|---|---|
| HR | `demo.hr` | HR administration and demo setup |
| Manager | `demo.manager` | Leave approver workflow |
| Staff | `demo.staff` | Employee leave workflow |
| Staff | `demo.staff2` | Additional employee history |

The baseline includes annual leave entitlements, reporting/approval relationships, and leave applications in `PENDING`, `APPROVED`, and `DENIED` states. Approved future leave is included so calendar/upcoming-leave views have meaningful data.

The demo password defaults to `Demo123!`. Override it in deployed environments with `demo.tenant.password` / `DEMO_TENANT_PASSWORD` configuration as appropriate for the deployment process.

## Production startup bootstrap

Cloud Run enables `demo.tenant.bootstrap-enabled` by default. When the application becomes ready, the bootstrap checks the configured demo tenant before public persona login is available:

- if the configured tenant does not exist, the baseline is created;
- if it exists as `DEMO` and all public persona accounts are present, no data is changed;
- if it exists as `DEMO` but a required public persona account is missing, the demo baseline is rebuilt;
- if the configured tenant ID exists as `STANDARD`, bootstrap refuses to modify it and application startup fails rather than deleting customer data.

Bootstrap is disabled by default outside the Cloud Run profile. It can be controlled with `demo.tenant.bootstrap-enabled` / `DEMO_TENANT_BOOTSTRAP_ENABLED`.

## Manual reset

A platform operator with `TENANT_WRITE` permission can reset the configured public demo tenant through:

```http
POST /api/tenants/demo/reset
```

The operation is idempotent from the user's perspective: each invocation removes the current DEMO tenant data and recreates the documented baseline. The tenant remains classified as `DEMO`.

The configured demo tenant can also be populated when it does not yet exist, allowing a clean database to create the public demo dataset through the same endpoint.

If the configured tenant ID already exists as `STANDARD`, reset fails with `403 Forbidden`; its data is left unchanged.

## Scheduled reset

Scheduled reset is disabled by default for non-Cloud Run profiles. The Cloud Run profile enables it so the public demo returns to a predictable baseline automatically.

```properties
demo.tenant.reset-enabled=true
demo.tenant.id=DEMO
demo.tenant.reset-cron=0 0 3 * * *
```

The default cron runs daily at 03:00 in the application JVM timezone. Override `DEMO_TENANT_RESET_CRON` when a different operational window is required.

## Local execution

1. Start the backend with a database containing the platform jurisdiction/template seed data.
2. Authenticate as a platform administrator with `TENANT_WRITE`.
3. Send `POST /api/tenants/demo/reset` with the normal CSRF/session requirements used by the application.
4. Sign in to tenant `DEMO` with one of the demo logins above and the configured demo password.

For automated local testing, run:

```bash
cd backend
./gradlew test --tests com.practical.leavemaster.tenant.DemoTenantSeedServiceTest --tests com.practical.leavemaster.tenant.DemoTenantBootstrapServiceTest
```

## Safety properties

- Bootstrap and reset check the current tenant classification before deleting any existing tenant.
- Existing `STANDARD` tenants are rejected before destructive work starts.
- Only the configured demo tenant may be created when absent; the existing-tenant reset path cannot create arbitrary tenant IDs.
- Startup bootstrap leaves a complete existing DEMO tenant untouched, so a deploy does not unnecessarily erase an active demo session.
- Re-provisioning goes through the normal tenant provisioning path, so Singapore leave types, policies and calendars stay aligned with production defaults.
- DEMO tenant outbound-side-effect suppression introduced for the demo sandbox remains active during normal use.
