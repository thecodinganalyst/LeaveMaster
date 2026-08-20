# Multi-jurisdiction tenant onboarding

LeaveMaster supports associating a tenant with multiple jurisdictions and optionally copying platform templates into tenant-owned configuration. The workflow is available during Platform Admin tenant creation and later to tenant-scoped administrators when the tenant expands into another jurisdiction.

## Data model

`tenant_jurisdiction` is the authoritative membership table for tenant-to-jurisdiction associations. A unique constraint on `(tenant_id, jurisdiction_id)` prevents the same jurisdiction from being associated with a tenant twice. Existing tenants are backfilled from the legacy `tenant.jurisdiction_id` value during migration.

The legacy `Tenant.jurisdictionId` field remains populated with the first selected jurisdiction for backward compatibility with code that still expects a primary jurisdiction. New features should use tenant-jurisdiction membership where multiple jurisdictions are relevant.

## Platform Admin tenant creation

`POST /api/tenants` and `POST /tenants` accept the normal tenant fields plus a `jurisdictions` array and optional top-level `calendarStart`/`calendarEnd` values.

Example:

```json
{
  "id": "ACME",
  "name": "ACME Pte Ltd",
  "startDate": "2026-08-21",
  "status": "ACTIVE",
  "calendarStart": "2026-01-01",
  "calendarEnd": "2026-12-31",
  "jurisdictions": [
    {
      "jurisdictionId": "SG",
      "includePublicHolidays": true,
      "includeLeaveConfiguration": true
    },
    {
      "jurisdictionId": "MY",
      "includePublicHolidays": true,
      "includeLeaveConfiguration": false
    }
  ]
}
```

For each selected jurisdiction:

- `includePublicHolidays=true` creates or reuses the tenant's jurisdiction-specific calendar for the selected period and copies applicable public holidays from the effective platform Public Holiday Template.
- `includeLeaveConfiguration=true` copies effective jurisdiction leave types, entitlement policy templates, and policy eligibility rules into tenant-owned records.
- Selecting neither option still creates the tenant-jurisdiction association.
- Template choices are independent between jurisdictions.

The Platform Admin UI defaults the initial calendar to January 1 through December 31 of the current year. The dates remain editable before submission. The backend applies the same current-year defaults if a holiday import is requested without explicit dates.

Tenant creation is transactional: a requested template import that cannot be completed causes the provisioning operation to fail rather than silently leaving ambiguous partial configuration.

## Tenant Admin add-jurisdiction API

Tenant-scoped administrators use the following endpoints. The tenant is derived from the authenticated user; callers cannot supply another tenant ID.

| Method | Path | Permission | Purpose |
|---|---|---|---|
| `GET` | `/api/tenant-jurisdictions` | `LEAVE_CALENDAR_READ` | List jurisdictions associated with the authenticated user's tenant. |
| `POST` | `/api/tenant-jurisdictions` | `LEAVE_CALENDAR_WRITE` | Add one jurisdiction and optionally import its templates. |

Equivalent non-`/api` paths under `/tenant-jurisdictions` are also supported.

Example request:

```json
{
  "jurisdictionId": "MY",
  "includePublicHolidays": true,
  "includeLeaveConfiguration": true,
  "calendarStart": "2026-01-01",
  "calendarEnd": "2026-12-31"
}
```

The Tenant Jurisdictions UI excludes jurisdictions already associated with the tenant. The backend independently rejects duplicate associations as a safety boundary.

Tenant Admin and HR roles receive `JURISDICTION_READ` so they can populate the jurisdiction picker, but they do not receive platform jurisdiction write access and cannot edit platform templates through this workflow.

## Template resolution and ownership

Provisioning follows the existing jurisdiction hierarchy. A more-specific jurisdiction template takes precedence over an inherited parent template for the same logical item.

Copied data is tenant-owned:

- leave types reference the source jurisdiction leave type where supported;
- entitlement policies retain `sourceTemplateId` and point to tenant leave types;
- copied eligibility rules point to the newly created tenant policy;
- tenant calendars use tenant scope, the selected jurisdiction, and source-template lineage.

Later edits to platform templates do not overwrite existing tenant configuration.

## Idempotency

The provisioning workflow is safe against retries:

- `(tenant_id, jurisdiction_id)` is unique;
- leave types and entitlement policies use existing source lineage checks;
- calendar provisioning uses tenant + jurisdiction + exact calendar date range as its logical key;
- public holidays are merged by holiday date and name so a repeated request does not create duplicates.

If public-holiday import is selected and no effective template overlaps the requested period, the request fails with a clear validation error. This makes missing requested template data deterministic rather than silently producing an empty calendar.

## Backward compatibility

Legacy tenant creation payloads that provide a single `jurisdictionId` and no `jurisdictions` array continue to use the existing one-jurisdiction bootstrap behavior. New Platform Admin UI submissions use the multi-jurisdiction request model.
