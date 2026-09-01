# Tenant Administrators

Tenant Administrators manage tenant-level staff and leave configuration. Platform-level jurisdiction/template administration is separate from tenant administration.

## Staff and users

Use **Staff** to create, review and update tenant staff records. Staff creation includes employment dates, jurisdiction, work schedule and entitlement review. Use **Roles** where available to review tenant role membership and permissions.

## Leave types

Use **Leave Types** to review leave types available to the tenant and the entitlement information shown for each leave type. Some source/reference information comes from the underlying jurisdiction configuration and may be read-only at tenant level.

## Leave calendars and public holidays

Use **Leave Calendar** to maintain the tenant's configured calendar where your role has write access. Calendar configuration affects which dates LeaveMaestro treats as working/non-working days when leave requests are submitted. Staff and managers can view calendar information but should not receive write controls unless their permissions explicitly allow it.

## Entitlement policies and eligibility

Use **Entitlement Policies** to define how configured leave entitlements are granted. Review the leave type, policy model, amount/accrual/proration/carry-forward settings and effective dates exposed by the form.

Eligibility rules determine who qualifies for a policy. Keep jurisdiction/service/dependant/event criteria aligned with the intended policy, and test changes before relying on them for staff entitlement generation.

## Leave approvers

Use **Leave Approvers** to define which staff member approves another staff member's leave and the effective dates of that assignment. Do not create self-approval or circular approval relationships.

## Roles and permissions

Roles control which pages/actions a user can access. Assign only the permissions needed for the person's responsibilities. A missing create/edit/approve button normally indicates that the signed-in user does not hold the required permission.

## Account and security administration

Tenant administrators can manage the staff/user information and roles available to them. Individual users link their own supported OAuth identity from **Security**; administrators should not attempt to link a personal Google/GitHub identity on someone else's behalf.

## Safe configuration practice

Before changing leave policy or calendar configuration, consider the effect on existing staff entitlements and future leave requests. For implementation-level behavior or migration details, use the [Technical Documentation](../../technical/index.md).

See also: [HR guide](../hr/index.md) and [Account & Security](../account-security/index.md).
