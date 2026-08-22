# Event-based leave workflow

LeaveMaster models event-based leave separately from annual leave balances. An `EVENT_BASED` policy describes the amount granted for each qualifying event, the event type, whether verification is required, and the validity window around that event.

## Employee experience

Employees do not have to create a technical qualifying-event record before applying for leave. The normal leave application can supply event details inline. The backend either reuses a matching existing event or creates one as part of the same transactional request workflow, resolves the applicable policy, creates an event-specific entitlement, and links the leave application to that entitlement.

Examples include military/reservist call-up, birth, adoption, or other jurisdiction-defined events. The generic workflow contains no Singapore-specific branching.

Reusable dependant data, such as child details used by conditional annual childcare policies, remains separate from per-request event capture. A childcare application therefore does not require creation of a new event for every request.

## Event entitlements

An event entitlement is uniquely associated with a qualifying event and policy. It stores the staff member, leave type, validity period, granted amount, used/reserved amount and lifecycle status. Re-running generation for the same event and policy is idempotent.

Event entitlements do not participate in annual accrual, annual proration, or carry-forward. Leave requests linked to them consume only the matching event entitlement and never an unrelated annual balance.

## Verification

Policies can require event verification. A request-first application may then be recorded with `PENDING_VERIFICATION` while its event entitlement remains unusable. After HR verifies the qualifying event and event entitlement generation is refreshed, the entitlement becomes active, the pending applications move to normal `PENDING` approval, and their amounts are reserved once.

Standalone dependant/event CRUD remains available for HR/admin workflows, but is not a prerequisite for the employee journey.

## Policy configuration

Event-based policies use the following generic fields:

- `qualifyingEventTypeCode`: extensible event code such as `MILITARY_CALL_UP`, `BIRTH`, or `ADOPTION`.
- `eventRequiresVerification`: whether the event must be verified before leave can become usable.
- `eventValidityDaysBefore` / `eventValidityDaysAfter`: optional validity window around the event when the event itself does not provide start/end dates.
- `entitlementAmount`: amount granted per qualifying event. This is not an annual balance.

Tenant provisioning preserves the policy model and event configuration when platform templates are copied to tenant policies.
