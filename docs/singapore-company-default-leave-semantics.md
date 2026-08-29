# Singapore company-default leave semantics

This document records the non-statutory Singapore company defaults revised by issue #429.

## Compassionate Leave

- Policy model: `EVENT_BASED`
- Default allowance: 2 days per qualifying `BEREAVEMENT` event
- Default event window: event date through 30 days after the event
- The allowance is tied to the qualifying event and does not renew as an annual balance.
- Re-processing the same qualifying event and policy remains idempotent through the event-entitlement uniqueness constraint.
- Tenants can replace or customise this company default.

## Marriage Leave

- Policy model: `EVENT_BASED`
- Default allowance: 2 days per qualifying `MARRIAGE` event
- Default event window: event date through 30 days after the event
- The allowance is tied to the qualifying event and does not renew as an annual balance.
- Tenants can replace or customise this company default.

## Generic Unpaid Leave

- Policy model: `REQUEST_BASED`
- Standard generated balance: none
- Default entitlement amount stored by the platform template: 0 days
- No qualifying event is required.
- Staff may submit an unpaid-leave request for the required duration through the normal approval workflow.
- Approved unpaid leave does not create or consume an Annual Leave entitlement.
- Tenants that want a contractual unpaid-leave limit can configure their own policy rather than inheriting an arbitrary 14-day allowance.

## Staff onboarding

Only `ANNUAL_ENTITLEMENT` and `CONDITIONAL_ANNUAL_ENTITLEMENT` policies produce conventional onboarding entitlement balances. `EVENT_BASED` and `REQUEST_BASED` policies are deliberately excluded from the staff entitlement proposal flow.

As a result, newly created Singapore staff no longer receive annual balances for Compassionate Leave, Marriage Leave, or generic Unpaid Leave.

## Existing tenants and audit history

Migration `V61__revise_singapore_company_default_leave_models.sql` updates the platform templates and tenant policies that retain `source_template_id` lineage to those defaults. Historical `leave_entitlement` and leave-application records are not deleted, preserving auditability. Explicit tenant policies that do not retain the platform-template lineage are not modified.

## AskLeaveMaestro guidance

Authoritative explanations should distinguish policy models:

- Compassionate and Marriage Leave are event-based company defaults, not recurring annual entitlements.
- Generic Unpaid Leave has no fixed standard annual allowance by default.
- Tenant-specific overrides take precedence when configured.
