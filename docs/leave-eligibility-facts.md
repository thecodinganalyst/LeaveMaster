# Leave eligibility facts

LeaveMaster stores dependant and qualifying-event facts separately from jurisdiction policy definitions. The goal is to let multiple jurisdictions reuse the same employee facts without adding jurisdiction-specific branches to the core entitlement engine.

## Dependants

A `staff_dependant` record belongs to exactly one tenant and staff member. It can capture:

- a generic `relationshipCode` such as `CHILD`, `PARENT`, or another configured relationship
- date of birth
- citizenship and residency codes
- adoption date
- optional effective dates and active state

The core model does not assume that every dependant is a child and does not encode Singapore-specific relationship rules.

Tenant users access dependant data through the staff-scoped endpoints under `/api/staff/{staffId}/dependants`. The existing `STAFF_READ` and `STAFF_WRITE` permissions therefore apply. Tenant ownership is validated again in the service layer to prevent cross-tenant references even when IDs are known.

## Qualifying leave events

A `qualifying_leave_event` belongs to one tenant and staff member and can optionally reference one of that staff member's dependants. It stores:

- an extensible `eventTypeCode`, for example `BIRTH`, `ADOPTION`, `MARRIAGE`, `BEREAVEMENT`, or a jurisdiction-configured military/reservist event code
- event date and optional start/end dates
- optional external and supporting-document references
- a generic lifecycle status: `RECORDED`, `VERIFIED`, `REJECTED`, or `CANCELLED`

Event data is available through `/api/staff/{staffId}/qualifying-events`.

## Security and ownership

Tenant users can only read or mutate facts for staff in their own tenant. Platform administrators may inspect facts when they otherwise have staff read access, but the service deliberately rejects platform-admin mutations of tenant-owned eligibility facts. This avoids accidental cross-tenant administration while preserving platform-level diagnostics.

Deleting a dependant that is already referenced by a qualifying event is rejected so historical event provenance cannot be silently broken.

## How policies consume these facts

Jurisdiction templates and eligibility rules should reference reusable facts rather than introduce jurisdiction-specific service branches. For example, a future childcare rule can evaluate a dependant's date of birth and citizenship, while an event-based parental-leave rule can evaluate a verified `BIRTH` event. Singapore will be the first reference configuration, but the same fact records and APIs are intended for other jurisdictions.

This issue intentionally does not generate event-based entitlements. The entitlement lifecycle that consumes these facts is implemented separately in issue #299.
