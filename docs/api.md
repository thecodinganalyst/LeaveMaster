# API Documentation

This document lists all REST API endpoints exposed by LeaveMaster and describes what each one does.

All endpoints (except `/users/login`, OAuth2 login endpoints, Swagger/OpenAPI, and H2 console) require authentication and the matching RBAC permission.

---

## Tenants (`/tenants`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/tenants` | Retrieve a list of all tenants. |
| `GET` | `/tenants/{id}` | Retrieve a single tenant by ID. Returns `404` if not found. |
| `POST` | `/tenants` | Create a new tenant. Body: `{ "id": "...", "name": "...", "startDate": "YYYY-MM-DD", "endDate": "YYYY-MM-DD", "status": "ACTIVE" }`. `endDate` is optional. `status` must be one of `ACTIVE`, `DORMANT`, or `TERMINATED`. |
| `PUT` | `/tenants/{id}` | Update an existing tenant (name, dates, or status). Returns `404` if not found. |
| `DELETE` | `/tenants/{id}` | Delete a tenant. Returns `204` on success, `404` if not found. |

Required permissions: `TENANT_READ` for `GET`, `TENANT_WRITE` for `POST`/`PUT`/`DELETE`.

### Tenant Status Values

| Status | Meaning |
|--------|---------|
| `ACTIVE` | Tenant is currently active and operational. |
| `DORMANT` | Tenant exists but is temporarily inactive. |
| `TERMINATED` | Tenant has been permanently terminated. |

---

## Users (`/users`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/users` | Retrieve a list of all application users. |
| `GET` | `/users/{loginName}` | Retrieve a single user by their login name. Returns `404` if not found. |
| `POST` | `/users` | Create a new application user. Returns `409` if the login name is already taken, `400` for invalid input. |
| `PUT` | `/users/{loginName}` | Update the details of an existing user. Returns `404` if not found, `400` for invalid input. |
| `DELETE` | `/users/{loginName}` | Delete a user. Returns `204` on success, `404` if not found. |
| `PUT` | `/users/{loginName}/change-password` | Change the password of a user. Body: `{ "password": "..." }`. Returns `400` for invalid input. |
| `PUT` | `/users/{loginName}/activate` | Activate a previously deactivated user account. |
| `PUT` | `/users/{loginName}/deactivate` | Deactivate an active user account. |
| `POST` | `/users/login` | Authenticate a user. Body: `{ "loginName": "...", "password": "..." }`. Returns the user on success, `401` for invalid credentials, `403` if the account is inactive. |

Required permissions: `USER_READ` for `GET`, `USER_WRITE` for user management endpoints. `/users/login` is public.

---

## Roles (`/roles`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/roles` | Retrieve all roles with their permissions and active status. |
| `GET` | `/roles/{id}` | Retrieve one role by ID. Returns `404` if not found. |
| `GET` | `/roles/permissions` | Retrieve all available permission codes. |
| `POST` | `/roles` | Create a role. Body: `{ "id": "...", "description": "...", "active": true, "permissionCodes": ["TENANT_READ"] }`. Returns `400` for invalid role or permission codes. |
| `PUT` | `/roles/{id}` | Modify role description, active state, and permission codes. Returns `404` if not found, `400` for invalid input. |
| `PUT` | `/roles/{id}/disable` | Disable a role so it no longer grants permissions. Returns `404` if not found. |
| `PUT` | `/roles/{id}/enable` | Re-enable a disabled role. Returns `404` if not found. |
| `PUT` | `/roles/{id}/users/{loginName}` | Add a user to a role. Returns `404` if user/role is not found, `409` if role is disabled. |
| `DELETE` | `/roles/{id}/users/{loginName}` | Remove a user from a role. Returns `404` if user/role is not found. |

Required permissions: `ROLE_MANAGE` for all role endpoints.

---

## Staff (`/staff`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/staff` | Retrieve a list of all staff records. |
| `GET` | `/staff/{id}` | Retrieve a single staff record by ID. Returns `404` if not found. |
| `POST` | `/staff` | Create a new staff record. Accepts an optional `location` object (`{ "id": "..." }`) to assign the staff member to a location. Returns `400` for invalid input. |
| `PUT` | `/staff/{id}` | Update an existing staff record, including the optional `location` assignment. Returns `404` if not found, `400` for invalid input. |
| `DELETE` | `/staff/{id}` | Delete a staff record. Returns `204` on success, `404` if not found, `409` if the staff member is referenced by other records. |
| `PUT` | `/staff/{id}/terminate` | Terminate a staff member on a given date (query param `termDate`). Cancels any approved future leave and removes pending leave applications. Returns `400` for invalid input. |

Required permissions: `STAFF_READ` for `GET`, `STAFF_WRITE` for `POST`/`PUT`/`DELETE`.

Staff write-side effects: [Staff change lifecycle](staff-change-lifecycle.md)

---

## Leave Types (`/leave-types`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/leave-types` | Retrieve a list of all leave types defined in the system (e.g. Annual Leave, Medical Leave). |
| `GET` | `/leave-types/{id}` | Retrieve a single leave type by ID. Returns `404` if not found. |
| `POST` | `/leave-types` | Create a new leave type. |
| `PUT` | `/leave-types/{id}` | Update an existing leave type. Returns `404` if not found. |
| `DELETE` | `/leave-types/{id}` | Delete a leave type. Returns `204` on success, `404` if not found, `409` if the leave type is in use by existing entitlements or applications. |

Required permissions: `LEAVE_TYPE_READ` for `GET`, `LEAVE_TYPE_WRITE` for `POST`/`PUT`/`DELETE`.

---

## Leave Approvers (`/leave-approvers`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/leave-approvers` | Retrieve a list of all leave-approver assignments. |
| `GET` | `/leave-approvers/{id}` | Retrieve a single leave-approver assignment by ID. Returns `404` if not found. |
| `GET` | `/leave-approvers/staff/{staffId}` | Retrieve all leave-approver assignments for a given staff member. Returns `404` if the staff member does not exist. |
| `POST` | `/leave-approvers` | Create a new leave-approver assignment, linking an approver to a staff member with optional effective dates. Returns `404` if either staff record is not found, `400` for invalid input. |
| `PUT` | `/leave-approvers/{id}` | Update an existing leave-approver assignment. Returns `404` if not found, `400` for invalid input. |
| `DELETE` | `/leave-approvers/{id}` | Delete a leave-approver assignment. Returns `204` on success, `404` if not found. |

Required permissions: `LEAVE_APPROVER_READ` for `GET`, `LEAVE_APPROVER_WRITE` for `POST`/`PUT`/`DELETE`.

---

## Leave Calendars (`/leave-calendars`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/leave-calendars` | Retrieve a list of all leave calendars. |
| `GET` | `/leave-calendars/current` | Retrieve the leave calendar that covers the given date (query param `date`, defaults to today). Returns `404` if no matching calendar is found. |
| `POST` | `/leave-calendars` | Create a new leave calendar for a specific year, including its list of public holidays. Each public holiday may include an optional `locationId` field to scope it to a specific location; omit `locationId` (or set it to `null`) for a holiday that applies globally to all locations. Returns `400` for invalid input, `409` if a calendar already exists for the same period. |

Required permissions: `LEAVE_CALENDAR_READ` for `GET`, `LEAVE_CALENDAR_WRITE` for `POST`/`PUT`/`DELETE`.

---

## Locations (`/locations`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/locations` | Retrieve a list of all locations. |
| `GET` | `/locations/{id}` | Retrieve a single location by ID. Returns `404` if not found. |
| `POST` | `/locations` | Create a new location. Body: `{ "id": "...", "locationName": "...", "country": "...", "state": "..." }`. `state` is optional (omit for country-level locations). |
| `PUT` | `/locations/{id}` | Update an existing location. Returns `404` if not found. |
| `DELETE` | `/locations/{id}` | Delete a location. Returns `204` on success, `404` if not found, `409` if the location is assigned to one or more staff members. |

Required permissions: `LOCATION_READ` for `GET`, `LOCATION_WRITE` for `POST`/`PUT`/`DELETE`.

---

## Leave Entitlement Generation (`/leave-entitlement-generation`)

The generation controller is exposed under both `/leave-entitlement-generation` and `/api/leave-entitlement-generation`. The `/api` form is recommended for frontend and external API usage.

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/api/leave-entitlement-generation/staff` | Generate or reconcile entitlements for every leave type belonging to one staff member's tenant for the requested entitlement period. |
| `POST` | `/api/leave-entitlement-generation/tenant` | Generate or reconcile entitlements for every staff/leave-type combination in a tenant for the requested entitlement period. |

Required permission: `LEAVE_ENTITLEMENT_GENERATE` for both endpoints.

The default tenant HR and tenant Admin roles receive this permission. Platform Admin also receives it. Tenant-scoped users are rejected if they attempt to generate entitlements for a different tenant.

### Generate for one staff member

Request:

```json
{
  "staffId": "staff-123",
  "periodStart": "2027-01-01",
  "periodEnd": "2027-12-31"
}
```

A valid period requires both dates and `periodStart <= periodEnd`. An unknown staff ID or an invalid period is rejected.

### Generate for a tenant

Request:

```json
{
  "tenantId": "ACME",
  "periodStart": "2027-01-01",
  "periodEnd": "2027-12-31"
}
```

The tenant operation evaluates all tenant staff against all tenant leave types and returns one result per staff/leave-type combination.

### Generation response

Both endpoints return a JSON array of generation results. Example:

```json
[
  {
    "staffId": "staff-123",
    "leaveTypeId": "annual",
    "entitlementId": "entitlement-uuid",
    "policyId": "policy-uuid",
    "status": "CREATED",
    "baseAmount": 14.00,
    "carriedForwardAmount": 3.00,
    "adjustmentAmount": 1.00,
    "usedAmount": 2.00,
    "reservedAmount": 0.50,
    "entitlementAmount": 18.00,
    "reason": "Entitlement generated from policy"
  }
]
```

Result fields:

| Field | Meaning |
|---|---|
| `staffId` | Staff member evaluated. |
| `leaveTypeId` | Leave type evaluated. |
| `entitlementId` | Existing/new entitlement ID when an entitlement record is involved; otherwise `null`. |
| `policyId` | Winning/source policy ID when applicable. |
| `status` | Outcome for this staff/leave-type combination. |
| `baseAmount` | Calculated policy entitlement before carry-forward and adjustment. |
| `carriedForwardAmount` | Balance brought from a previous entitlement period. |
| `adjustmentAmount` | Preserved manual adjustment on an existing generated entitlement. |
| `usedAmount` | Approved leave derived from leave applications in the requested period. |
| `reservedAmount` | Pending leave derived from leave applications in the requested period. |
| `entitlementAmount` | Total generated entitlement: base + carry-forward + adjustment. |
| `reason` | Human-readable explanation of the result. |

Possible `status` values:

| Status | Meaning |
|---|---|
| `CREATED` | A new policy-generated entitlement was created. |
| `UPDATED` | An existing policy-generated entitlement for the same staff/type/period was reconciled. |
| `NO_MATCHING_POLICY` | No effective eligible policy was found. |
| `AMBIGUOUS_POLICY` | Multiple matching policies have the same highest priority; no policy is selected. |
| `LEGACY_PROTECTED` | An existing entitlement with no source policy is treated as legacy/manual and left unchanged. |
| `HISTORICAL_PROTECTED` | An existing policy-generated entitlement for a completed historical period is left unchanged. |

### Calculation and safety behaviour

- Policy resolution is performed at `periodStart`.
- Annual entitlement and join-date proration support `NONE`, `CALENDAR_DAYS`, and `MONTHS` proration.
- Monthly accrual uses `accrualRate` and is capped by the policy's configured entitlement amount.
- Carry-forward uses the most recent earlier entitlement for the same staff/leave type, deducts approved and pending leave from the source period, applies any carry-forward limit and respects configured expiry months.
- Existing manual `adjustmentAmount` is preserved when reconciling an already generated entitlement.
- `APPROVED` leave is counted as used and `PENDING` leave as reserved; recalculation is rejected if the new entitlement would be below used + reserved leave.
- Re-running the same staff + leave type + period reconciles the same entitlement rather than creating a duplicate.
- `HOURS` policies are currently rejected because LeaveMaestro's leave consumption model is day/half-day based.
- `PER_PAY_PERIOD` accrual is currently rejected because LeaveMaestro does not yet have an authoritative payroll-period schedule.

For the full policy → eligibility → resolution → generation workflow and operational examples, see [Policy-driven leave entitlement generation](leave-entitlement-generation.md).

---

## Leave Applications (`/leave-applications`)

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/leave-applications?staffId={staffId}` | Retrieve all leave applications visible to a staff member (their own applications plus applications from their direct reports that they can approve). Returns `404` if the staff member does not exist. |
| `GET` | `/leave-applications/{id}` | Retrieve a single leave application by ID. Returns `404` if not found. |
| `GET` | `/leave-applications/staff/{staffId}` | Retrieve all leave applications for a specific staff member, optionally filtered by calendar year of a given date (query param `date`, defaults to today). Returns `404` if the staff member or calendar is not found. |
| `GET` | `/leave-applications/staff/{staffId}/balance` | Retrieve the remaining leave balance for a staff member across all leave types. Returns `404` if the staff member does not exist. |
| `GET` | `/leave-applications/approver/{approverId}` | Retrieve all leave applications with status `PENDING` that are awaiting action by the specified approver. Returns `404` if the approver staff record does not exist. |
| `POST` | `/leave-applications` | Submit a new leave application on behalf of a staff member. Accepts `multipart/form-data` (parts: `request` as JSON, optional `file` for the attachment) or `application/json` (without attachment). A separate `LeaveApplication` record is created for each working day in the requested date range, excluding public holidays. Returns `400` for invalid input. |
| `POST` | `/leave-applications/{id}/attachment` | Upload or replace the attachment for a leave application. Accepts `multipart/form-data` with a `file` part. The file is stored in cloud storage and a storage key is saved in the database. Returns `404` if the leave application is not found. |
| `GET` | `/leave-applications/{id}/attachment` | Download the attachment for a leave application. Streams the file directly (local/docker profiles) or redirects to a short-lived signed URL (production). Returns `404` if the leave application has no attachment. |
| `PUT` | `/leave-applications/{id}` | Update a leave application (e.g. change remarks or status while still in `DRAFT`). Returns `404` if not found. |
| `DELETE` | `/leave-applications/{id}` | Delete a leave application. Only applications in `DRAFT` or `PENDING` status may be deleted. Returns `400` if the application is in a non-deletable status, `404` if not found. |
| `PUT` | `/leave-applications/{id}/approve` | Approve a `PENDING` leave application. Query param `approverId` must identify a valid approver for the applicant. Returns `400` if the application is not in an approvable state or the approver is not authorised. |
| `PUT` | `/leave-applications/{id}/reject` | Reject a `PENDING` leave application. Query param `approverId` must identify a valid approver for the applicant. Returns `400` if the application is not in a rejectable state or the approver is not authorised. |
| `PUT` | `/leave-applications/{id}/approve-cancellation` | Approve a cancellation request (`CANCEL_REQUESTED`) for a previously approved leave application, moving it to `CANCELLED`. Returns `400` if the application is not awaiting cancellation approval. |
| `PUT` | `/leave-applications/{id}/reject-cancellation` | Reject a cancellation request, returning the application to `APPROVED` status. Returns `400` if the application is not awaiting cancellation approval. |

Required permissions:

- `LEAVE_APPLICATION_READ` for `GET` endpoints
- `LEAVE_APPLICATION_WRITE` for `POST`/general `PUT`/`DELETE`
- `LEAVE_APPLICATION_APPROVE` for `PUT /leave-applications/{id}/approve`, `/reject`, `/approve-cancellation`, and `/reject-cancellation`

---

## Leave Application Status Flow

```
DRAFT → PENDING → APPROVED → CANCEL_REQUESTED → CANCELLED
                ↘ DENIED
```

| Status | Meaning |
|--------|---------|
| `DRAFT` | Application saved but not yet submitted. |
| `PENDING` | Submitted and awaiting approval. |
| `APPROVED` | Approved by an authorised approver. |
| `DENIED` | Rejected by an approver. |
| `CANCEL_REQUESTED` | Cancellation requested on an approved leave. |
| `CANCELLED` | Cancellation approved; leave is restored to the staff member's balance. |
