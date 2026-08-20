# Staff change lifecycle

This document describes the side effects performed by the staff write endpoints (`POST /staff`, `PUT /staff/{id}`, `DELETE /staff/{id}`).

## Create staff (`POST /staff`)

`StaffService.save(...)` performs the following steps:

1. If `leaveEntitlements` is provided, each entitlement is normalized:
   - `leaveType.id` is required and must exist.
   - The referenced leave type is marked `used=true` (if not already).
   - `staff` and `tenantId` are attached to the entitlement row.
   - Period/proration rules are applied (see [Leave entitlement proration](leave-entitlement-proration.md)).
2. The staff record is saved. `jurisdictionId` identifies the jurisdiction used for calendars and jurisdiction-based eligibility rules.
3. An `app_user` row is created for the staff member:
   - `loginName` uses `staff.loginName` when provided, otherwise falls back to `staff.id`.
   - Initial password is set to the same value as the login name (stored encoded).
   - User `active` is `true` only when `joinDate <= today`.
   - Duplicate login names fail with `DuplicateLoginNameException`.
4. Tenant activity is updated via `tenantActivityService.touch(tenantId)`.

## Update staff (`PUT /staff/{id}`)

`StaffService.update(...)` performs the following steps:

1. Load existing staff by ID, otherwise fail with `StaffNotFoundException`.
2. Update mutable fields: `name`, `joinDate`, `termDate`, and `jurisdictionId`.
3. If `workSchedule` is present in request, replace it; if omitted, keep existing schedule.
4. If `leaveEntitlements` is present in request:
   - Normalize them using the same rules as create.
   - Clear existing entitlement rows and replace with normalized rows.
5. Save staff and touch tenant activity.

Notes:

- Updating staff does **not** update the linked `app_user` login name/password/active state.
- Proration is applied only to entitlement rows that are supplied in the update payload.

## Delete staff (`DELETE /staff/{id}`)

`StaffService.delete(...)` performs the following steps:

1. Load existing staff by ID, otherwise fail with `StaffNotFoundException`.
2. If the staff member is referenced by leave applications (as applicant or approver), fail with `StaffInUseException` (`409` from controller).
3. Delete the staff row.
4. Touch tenant activity.

Notes:

- Deleting staff does **not** delete the related `app_user` row.
- Other database references (for example unresolved leave-approver links) can still block deletion at database-constraint level.
