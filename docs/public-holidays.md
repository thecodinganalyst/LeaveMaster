# Platform public holidays

## Purpose

LeaveMaestro separates **platform public-holiday reference data** from **tenant leave calendars**.

- **Public holidays** are jurisdiction/year seed data maintained centrally by `PLATFORM_ADMIN`.
- **Leave calendars** are tenant-owned operational configuration used for leave calculations and tenant customisation.

Platform Admin therefore manages public holidays through the dedicated **Public Holiday Templates** menu and API. It does not require tenant `LEAVE_CALENDAR_READ` or `LEAVE_CALENDAR_WRITE` permissions.

Jurisdiction is the geographic source of truth for public holidays. The former Location module and `locationId` holiday applicability have been removed.

## Seed data

The current seed migration provides Singapore public holidays for **2026 and 2027**. This feature does not add additional jurisdictions.

The seeded records remain stored using the existing `leave_calendar` + `public_holiday` persistence model. Platform template calendar rows are an internal backing store for jurisdiction holiday data; they are not exposed as tenant leave-calendar resources to Platform Admin.

## Permissions

| Permission | Purpose | Default platform assignment |
|---|---|---|
| `PUBLIC_HOLIDAY_READ` | List and view platform public-holiday reference data | `PLATFORM_ADMIN` |
| `PUBLIC_HOLIDAY_WRITE` | Create, edit and delete platform public-holiday reference data | `PLATFORM_ADMIN` |
| `LEAVE_CALENDAR_READ` | Read tenant leave calendars | Tenant-scoped roles only |
| `LEAVE_CALENDAR_WRITE` | Create/update/delete tenant leave calendars | Tenant-scoped roles only |

Granting `PUBLIC_HOLIDAY_READ` or `PUBLIC_HOLIDAY_WRITE` does not grant access to tenant leave calendars.

## Admin UI

Platform Admin users with `PUBLIC_HOLIDAY_READ` see a **Public Holiday Templates** menu item.

The resource supports list/search, view, create, edit, delete, jurisdiction selection through the existing jurisdiction selector, and date/year display.

The **Leave Calendars** menu remains controlled independently by `LEAVE_CALENDAR_READ`.

## API

The platform public-holiday API is available under both the root and `/api` route aliases used by the application. Frontend calls use `/api/public-holidays`.

### List

```http
GET /api/public-holidays
GET /api/public-holidays?jurisdictionId=SG&year=2026
```

Requires `PUBLIC_HOLIDAY_READ`.

### Get one

```http
GET /api/public-holidays/{id}
```

Requires `PUBLIC_HOLIDAY_READ`.

The `id` is an opaque encoded identifier generated from the backing platform template and the holiday's date/name. Clients must treat it as opaque.

### Create

```http
POST /api/public-holidays
Content-Type: application/json

{
  "jurisdictionId": "SG",
  "holidayDate": "2026-12-25",
  "holidayName": "Christmas Day"
}
```

Requires `PUBLIC_HOLIDAY_WRITE`.

If no backing platform template exists for the jurisdiction/year, the service creates one internally for that calendar year.

### Update

```http
PUT /api/public-holidays/{id}
Content-Type: application/json

{
  "jurisdictionId": "SG",
  "holidayDate": "2026-12-25",
  "holidayName": "Christmas Day"
}
```

Requires `PUBLIC_HOLIDAY_WRITE`.

Changing the jurisdiction, date or name may result in a new opaque resource ID. Clients should use the ID returned in the update response.

### Delete

```http
DELETE /api/public-holidays/{id}
```

Requires `PUBLIC_HOLIDAY_WRITE`.

## Validation and ownership rules

- Platform public holidays must have a jurisdiction, holiday date and holiday name.
- Duplicate holiday date/name combinations within the same backing jurisdiction template are rejected.
- Only platform-template records with `tenant_id IS NULL` are exposed by the platform public-holiday service.
- Tenant calendar records are excluded from this API even when they contain public holidays.
- Tenant users do not receive the platform public-holiday permissions by default.

## Tenant provisioning

Tenant provisioning copies applicable jurisdiction public holidays into a tenant-owned leave calendar. Once copied, the tenant calendar is independent: changing platform public-holiday reference data does not silently overwrite tenant customisations.
