# Platform Holiday Calendar Templates

LeaveMaster stores jurisdiction-level public-holiday seed data as platform-scoped `LeaveCalendar` records. These templates are maintained independently of tenant calendars and are intended to be consumed by a later tenant-provisioning workflow.

## Scope model

Platform holiday templates use:

- `scope = PLATFORM_TEMPLATE`
- `tenantId = null`
- a required `jurisdictionId`
- a date range identifying the calendar period
- embedded `publicHolidays` containing holiday name and gazetted date

Tenant calendars continue to use `scope = TENANT` and a non-null `tenantId`. This feature does not copy or synchronize platform templates into tenants.

## Platform Admin management

Platform Admin can use the existing Leave Calendars administration resource to create, edit, and delete platform templates. The `jurisdictionId` field uses the shared jurisdiction selector, and the backend forces Platform Admin-created calendars to platform-template scope.

The platform catalogue can also be queried explicitly by jurisdiction and year:

```http
GET /api/leave-calendars/templates?jurisdictionId=SG&year=2026
```

The endpoint requires `JURISDICTION_READ` and returns only platform templates for the requested jurisdiction that overlap the requested year. It does not create tenant records.

## Singapore seed data

Database migrations seed Singapore (`SG`) templates for 2026 and 2027 in both the H2 and PostgreSQL migration sets. The dates are based on Singapore Ministry of Manpower public-holiday announcements.

The seed records the gazetted holiday dates themselves. Singapore rules can make the following working day a paid holiday when a public holiday falls on an employee's rest day. That outcome depends on the employee's work/rest-day arrangement, so those conditional substitute days are not inserted as unconditional additional public-holiday records in the platform template.

If a tenant later needs an explicit substitute/observed day, tenant provisioning or tenant calendar management should derive or add that date according to the employee/work-schedule rules rather than treating every announced substitute date as universally applicable.

## Validation

Calendar validation enforces:

- start date is on or before end date;
- platform templates have no tenant id and have a jurisdiction id;
- tenant calendars have a tenant id and no jurisdiction id;
- public holidays have a date and name;
- holiday dates fall within the calendar range;
- the same holiday name/date pair cannot be repeated within one calendar;
- platform templates for the same jurisdiction cannot have overlapping date ranges.

The `public_holiday` primary key also protects against duplicate persisted holiday name/date combinations for the same calendar.

## Future tenant provisioning

Tenant creation/bootstrap is deliberately out of scope for issue #240. A follow-up feature can use the jurisdiction/year lookup to select effective platform templates and copy them into tenant-owned calendars. Existing tenant calendars must not be silently coupled to or overwritten by later platform-template changes.
