# Policy-driven leave entitlement generation

LeaveMaestro can generate and reconcile employee leave entitlements from configured leave entitlement policies. This guide explains how the policy catalogue, eligibility rules, policy resolution and entitlement generation fit together, and how administrators should operate the process safely.

## Overview

The end-to-end flow is:

```text
Leave type
   ↓
Leave entitlement policies
   ↓
Eligibility rules
   ↓
Policy resolution for staff + leave type + effective date
   ↓
Winning policy
   ↓
Entitlement calculation
   ├─ base entitlement
   ├─ proration / accrual
   ├─ carry-forward
   └─ preserved manual adjustment
   ↓
Generated leave_entitlement record
   ↓
Approved leave = used
Pending leave  = reserved
   ↓
Displayed leave balance
```

Generation is explicit. Administrators call the generation API for either one staff member or an entire tenant and supply the entitlement period to generate or reconcile.

## 1. Configure leave entitlement policies

A leave entitlement policy belongs to a tenant and a leave type. Policies define the amount and calculation behaviour used when creating employee entitlements.

Important policy fields include:

| Field | Purpose |
|---|---|
| `tenantId` | Tenant that owns the policy. |
| `leaveTypeId` | Tenant leave type to which the policy applies. |
| `priority` | Higher priority wins when more than one policy matches. |
| `entitlementUnit` | Currently `DAYS` is supported for generation. |
| `entitlementAmount` | Maximum/base entitlement amount defined by the policy. |
| `accrualMethod` | Annual or monthly calculation mode used by generation. |
| `accrualRate` | Rate used by monthly accrual when configured. |
| `prorationMethod` | `NONE`, `CALENDAR_DAYS`, or `MONTHS`. |
| `carryForwardAllowed` | Whether unused balance from the previous period may be brought forward. |
| `carryForwardLimit` | Optional maximum amount that may be carried forward. |
| `carryForwardExpiryMonths` | Optional maximum age of the previous entitlement used as carry-forward source. |
| `effectiveFrom` / `effectiveTo` | Policy effective period. |

Policy administration is protected by `LEAVE_ENTITLEMENT_POLICY_READ` and `LEAVE_ENTITLEMENT_POLICY_WRITE`.

## 2. Add eligibility rules

Eligibility rules narrow which staff can use a policy. LeaveMaestro currently evaluates rules using staff data it stores reliably:

- `LOCATION_ID`
- `JURISDICTION_CODE`
- `SERVICE_MONTHS`

Active rules on the same policy use **AND semantics**: every active rule must match for the policy to be eligible. Inactive rules are ignored.

Supported operators include `EQUALS`, `NOT_EQUALS`, `IN`, `NOT_IN`, `GREATER_THAN`, `GREATER_THAN_OR_EQUAL`, `LESS_THAN`, and `LESS_THAN_OR_EQUAL`, subject to criterion/operator validation.

## 3. Resolve the winning policy

Before entitlement generation, LeaveMaestro resolves the policy for:

```text
staff + leave type + effective date
```

For generation, the effective date passed to the resolver is the requested `periodStart`.

Resolution works as follows:

1. Find active policies for the tenant and leave type.
2. Keep only policies effective on `periodStart`.
3. Evaluate all active eligibility rules against the employee.
4. Keep policies whose rules all match.
5. Select the highest-priority policy.
6. If multiple matching policies share the same highest priority, resolution is ambiguous and no policy is selected.

The generation response reports `NO_MATCHING_POLICY` or `AMBIGUOUS_POLICY` instead of silently choosing a policy.

> A policy change part-way through a requested entitlement period is not split automatically. Generation resolves the policy at `periodStart`, so administrators should choose entitlement periods that align with their policy design.

## 4. Generate an entitlement

Generation can be run for one employee or all staff in a tenant.

### Generate for one staff member

```http
POST /api/leave-entitlement-generation/staff
Content-Type: application/json
```

```json
{
  "staffId": "staff-123",
  "periodStart": "2027-01-01",
  "periodEnd": "2027-12-31"
}
```

### Generate for a tenant

```http
POST /api/leave-entitlement-generation/tenant
Content-Type: application/json
```

```json
{
  "tenantId": "ACME",
  "periodStart": "2027-01-01",
  "periodEnd": "2027-12-31"
}
```

Both operations require the `LEAVE_ENTITLEMENT_GENERATE` permission. The default tenant HR and tenant Admin roles receive this permission, and Platform Admin can also generate entitlements. Tenant users cannot generate entitlements for another tenant.

The tenant endpoint evaluates every tenant staff member against every tenant leave type. A result is returned for each staff/leave-type combination.

## 5. How the amount is calculated

The generated entitlement stores several components separately for traceability:

```text
entitlementAmount = baseAmount + carriedForwardAmount + adjustmentAmount
```

The existing `entitlement` value remains the total balance basis used by the rest of LeaveMaestro.

### Annual entitlement

For annual policies without proration, `baseAmount` is the policy's configured entitlement amount.

### Join-date proration

Proration is applied when the employee joins after the requested period starts.

#### `NONE`

The employee receives the full configured amount.

#### `CALENDAR_DAYS`

The amount is proportional to eligible calendar days remaining in the requested period.

Conceptually:

```text
base = entitlementAmount × eligibleCalendarDays / totalCalendarDays
```

The generated amount is rounded to two decimal places using half-up rounding.

#### `MONTHS`

The amount is proportional to eligible calendar months in the requested period.

Conceptually:

```text
base = entitlementAmount × eligibleMonths / totalMonths
```

The employee's join month counts as an eligible month.

If the employee joins after `periodEnd`, the base amount is zero.

## 6. Monthly accrual

When a policy uses monthly accrual and has an `accrualRate`, LeaveMaestro counts eligible calendar months from the later of the employee's join date or the requested period start through the requested period end.

Conceptually:

```text
base = accrualRate × eligibleMonths
```

The result is capped at the policy's `entitlementAmount`.

This generation model calculates the entitlement represented by the supplied period. It does not currently schedule automatic month-by-month postings.

## 7. Carry-forward

If `carryForwardAllowed` is false, the carried-forward amount is zero.

When carry-forward is enabled, LeaveMaestro finds the most recent earlier entitlement for the same employee and leave type whose `to` date is before the new period start.

Available balance from that previous entitlement is calculated as:

```text
previous entitlement
- approved leave in the previous period
- pending leave in the previous period
```

Negative values are floored at zero.

Then:

- `carryForwardLimit`, when configured, caps the amount carried into the new period;
- `carryForwardExpiryMonths`, when configured, prevents an entitlement older than the allowed expiry window from being used as the carry-forward source.

Pending leave is deducted as well as approved leave so a staff member cannot reserve leave in the previous period and then also carry the same amount into the next period.

## 8. Manual adjustments

Generated entitlements contain an `adjustmentAmount` field.

When LeaveMaestro reconciles an entitlement that was already generated for the same staff, leave type and period, the existing adjustment amount is preserved. This allows an administrator's manual correction to survive policy recalculation.

The total generated entitlement therefore becomes:

```text
recalculated base
+ recalculated carry-forward
+ existing manual adjustment
```

## 9. Used and reserved leave protection

LeaveMaestro does not persist a separate mutable "used balance" on `leave_entitlement`. Usage is derived from leave applications.

For entitlement reconciliation:

- `APPROVED` leave counts as **used**;
- `PENDING` leave counts as **reserved**;
- full-day leave counts as `1.0`;
- `AM` and `PM` leave each count as `0.5`.

Before saving a recalculated entitlement, LeaveMaestro checks:

```text
new entitlement >= used + reserved
```

If the new amount would be smaller than leave already used or reserved, generation fails rather than silently creating an invalid negative available balance.

## 10. Idempotent reconciliation

The database enforces one entitlement per:

```text
staff + leave type + period start + period end
```

Running generation again for the same combination therefore reconciles the existing generated record instead of creating a duplicate.

A successful result reports:

- `CREATED` when a new generated record is created;
- `UPDATED` when an existing policy-generated record is recalculated.

The generated record also stores its source `policyId` and a `generatedAt` timestamp for traceability.

## 11. Protected records

Generation deliberately avoids rewriting records that could represent historical or manually maintained state.

### `LEGACY_PROTECTED`

If an entitlement already exists for the same employee, leave type and period but has no source `policyId`, LeaveMaestro treats it as a legacy/manual entitlement and leaves it unchanged.

It is not silently converted to a policy-generated entitlement.

### `HISTORICAL_PROTECTED`

If an existing policy-generated entitlement's requested period has already ended, LeaveMaestro leaves that historical entitlement unchanged.

This prevents a later policy edit from retroactively rewriting historical leave balances through a normal generation run.

## 12. Generation result

Each staff/leave-type evaluation returns an `EntitlementGenerationResult` containing:

```json
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
```

Possible statuses are:

| Status | Meaning |
|---|---|
| `CREATED` | A new entitlement was generated. |
| `UPDATED` | An existing policy-generated entitlement was reconciled. |
| `NO_MATCHING_POLICY` | No effective/eligible policy was found. |
| `AMBIGUOUS_POLICY` | More than one matching policy shared the highest priority. |
| `LEGACY_PROTECTED` | An existing entitlement without a source policy was left unchanged. |
| `HISTORICAL_PROTECTED` | An existing generated historical entitlement was left unchanged. |

A batch request can therefore complete with a mixture of statuses. Administrators should review non-`CREATED`/`UPDATED` statuses instead of assuming every staff/leave-type combination generated a balance.

## 13. Current limitations

### Hour-based policies

`HOURS` entitlement policies cannot currently generate employee balances. LeaveMaestro's leave applications and balance calculations are based on full days and half days (`FULL`, `AM`, `PM`), so converting hour-based policy values without a work-hours model would be inaccurate.

Generation rejects these policies instead of performing an implicit conversion.

### Per-pay-period accrual

`PER_PAY_PERIOD` generation is not currently supported because LeaveMaestro does not yet have an authoritative payroll-period schedule. Generation rejects these policies rather than assuming a pay frequency.

## 14. Recommended operating procedure

For a normal annual entitlement cycle:

1. Configure leave types and entitlement policies.
2. Configure eligibility rules where required.
3. Resolve or review policy matching for representative staff before running a tenant-wide batch.
4. Generate the upcoming entitlement period.
5. Review any `NO_MATCHING_POLICY`, `AMBIGUOUS_POLICY`, `LEGACY_PROTECTED`, or `HISTORICAL_PROTECTED` results.
6. Correct policy/rule configuration where necessary.
7. Re-run the same period; matching generated records will reconcile instead of duplicating.
8. Avoid changing historical periods unless a separate controlled correction process is intended.

## Related documentation

- [API documentation](api.md)
- [Leave entitlement proration](leave-entitlement-proration.md)
- [Staff change lifecycle](staff-change-lifecycle.md)
- [Architecture](architecture.md)
