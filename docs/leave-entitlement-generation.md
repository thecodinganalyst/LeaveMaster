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

Generation is explicit. Administrators call the generation API for either one staff member or an entire tenant and supply the entitlement period to generate or reconcile. LeaveMaestro does not currently post leave automatically every month in a background scheduler.

## 1. Configure leave entitlement policies

A leave entitlement policy belongs to a tenant and a leave type. Policies define the amount and calculation behaviour used when creating employee entitlements.

Important policy fields include:

| Field | Purpose |
|---|---|
| `tenantId` | Tenant that owns the policy. |
| `leaveTypeId` | Tenant leave type to which the policy applies. |
| `priority` | Higher priority wins when more than one policy matches. |
| `entitlementUnit` | Currently `DAYS` is supported for generation. |
| `entitlementAmount` | Total entitlement amount defined by the policy for the entitlement period. |
| `accrualMethod` | Supported user-facing behaviours are **Front-loaded** (`NONE`) and **Monthly accrual** (`MONTHLY`). |
| `accrualRate` | Derived implementation value for monthly accrual. It is not manually configurable. |
| `prorationMethod` | `NONE`, `CALENDAR_DAYS`, or `MONTHS`. |
| `carryForwardAllowed` | Whether unused balance from the previous period may be brought forward. |
| `carryForwardLimit` | Optional maximum amount that may be carried forward. |
| `carryForwardExpiryMonths` | Optional maximum age of the previous entitlement used as carry-forward source. |
| `effectiveFrom` / `effectiveTo` | Policy effective period. |

Policy administration is protected by `LEAVE_ENTITLEMENT_POLICY_READ` and `LEAVE_ENTITLEMENT_POLICY_WRITE`.

### Accrual versus proration

Accrual and proration solve different problems:

- **Accrual** controls when entitlement becomes available during the entitlement period.
- **Proration** adjusts entitlement because an employee is eligible for only part of the entitlement period.

For a front-loaded policy, proration can reduce the configured entitlement for a joiner who becomes eligible part-way through the period.

For monthly accrual, the generation calculation already counts only eligible months. LeaveMaestro therefore does not apply an additional proration reduction after the monthly accrual calculation; this avoids reducing the same partial period twice.

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

### Front-loaded entitlement

The user-facing **Front-loaded** option is stored as `accrualMethod = NONE` for backward compatibility. It means entitlement is not earned progressively each month.

For a front-loaded policy without proration, `baseAmount` is the full configured `entitlementAmount` for the period. A periodic `accrualRate` is not applicable and is cleared by the backend.

### Join-date proration for front-loaded policies

Proration is applied when the employee joins after the requested period starts.

#### `NONE`

The employee receives the full configured amount.

#### `CALENDAR_DAYS`

The amount is proportional to eligible calendar days remaining in the requested period.

```text
base = entitlementAmount × eligibleCalendarDays / totalCalendarDays
```

The generated amount is rounded to two decimal places using half-up rounding.

#### `MONTHS`

The amount is proportional to eligible calendar months in the requested period.

```text
base = entitlementAmount × eligibleMonths / totalMonths
```

The employee's join month counts as an eligible month. If the employee joins after `periodEnd`, the base amount is zero.

## 6. Monthly accrual

For `accrualMethod = MONTHLY`, LeaveMaestro derives the rate automatically from the entitlement amount:

```text
monthlyAccrualRate = entitlementAmount / 12
```

For example, a 14-day entitlement produces a calculated rate of approximately `1.1667 days per month`.

The backend is authoritative: client-supplied manual accrual rates are ignored and replaced with the derived value. The persisted rate uses additional precision so intermediate rounding does not materially reduce a full-year entitlement.

During entitlement generation, LeaveMaestro counts eligible calendar months from the later of the employee's join date or the requested period start through the requested period end:

```text
base = monthlyAccrualRate × eligibleMonths
```

The result is capped at the policy's `entitlementAmount` and rounded to the generated entitlement precision.

Because eligible months are already part of this calculation, the normal join-date proration branch is not applied again for monthly accrual. This prevents double-proration.

This generation model calculates the entitlement represented by the supplied period. It does not currently schedule automatic month-by-month postings.

## 7. Legacy and unsupported accrual methods

The persisted enum still contains older values for backward compatibility, but they are not available for new configuration.

### Legacy `ANNUAL`

Historically, `ANNUAL` behaved effectively like a front-loaded policy. New policies cannot select it. When a legacy annual policy is edited it is normalized to `NONE`/Front-loaded, and legacy annual templates copied into newly provisioned tenants are also normalized to Front-loaded.

### `PER_PAY_PERIOD`

`PER_PAY_PERIOD` cannot be newly configured because LeaveMaestro does not yet have an authoritative payroll/pay-period schedule. Existing historical data is not silently converted to another accrual model because that could change its meaning. Entitlement generation continues to reject `PER_PAY_PERIOD` until payroll schedules are implemented.

## 8. Carry-forward

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

## 9. Manual adjustments

Generated entitlements contain an `adjustmentAmount` field.

When LeaveMaestro reconciles an entitlement that was already generated for the same staff, leave type and period, the existing adjustment amount is preserved. This allows an administrator's manual correction to survive policy recalculation.

The total generated entitlement therefore becomes:

```text
recalculated base
+ recalculated carry-forward
+ existing manual adjustment
```

## 10. Used and reserved leave protection

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

## 11. Idempotent reconciliation

The database enforces one entitlement per:

```text
staff + leave type + period start + period end
```

Running generation again for the same combination therefore reconciles the existing generated record instead of creating a duplicate.

A successful result reports:

- `CREATED` when a new generated record is created;
- `UPDATED` when an existing policy-generated record is recalculated.

The generated record also stores its source `policyId` and a `generatedAt` timestamp for traceability.

## 12. Protected records

Generation deliberately avoids rewriting records that could represent historical or manually maintained state.

### `LEGACY_PROTECTED`

If an entitlement already exists for the same employee, leave type and period but has no source `policyId`, LeaveMaestro treats it as a legacy/manual entitlement and leaves it unchanged.

### `HISTORICAL_PROTECTED`

If an existing policy-generated entitlement's requested period has already ended, LeaveMaestro leaves that historical entitlement unchanged.

## 13. Generation result

Each staff/leave-type evaluation returns an `EntitlementGenerationResult` containing the generated components, usage/reservation amounts, total entitlement, source policy, status and reason.

Possible statuses are:

| Status | Meaning |
|---|---|
| `CREATED` | A new entitlement was generated. |
| `UPDATED` | An existing policy-generated entitlement was reconciled. |
| `NO_MATCHING_POLICY` | No effective/eligible policy was found. |
| `AMBIGUOUS_POLICY` | More than one matching policy shared the highest priority. |
| `LEGACY_PROTECTED` | An existing entitlement without a source policy was left unchanged. |
| `HISTORICAL_PROTECTED` | An existing generated historical entitlement was left unchanged. |

## 14. Current limitations

### Hour-based policies

`HOURS` entitlement policies cannot currently generate employee balances. LeaveMaestro's leave applications and balance calculations are based on full days and half days (`FULL`, `AM`, `PM`), so converting hour-based policy values without a work-hours model would be inaccurate.

### Per-pay-period accrual

`PER_PAY_PERIOD` generation is not currently supported because LeaveMaestro does not yet have an authoritative payroll-period schedule.

### Automatic accrual scheduling

Monthly accrual is evaluated when entitlement generation/reconciliation is invoked. There is no automatic monthly scheduler yet. If automatic reconciliation is required, it should be implemented as a separate scheduling capability rather than changing the policy semantics described here.

## 15. Recommended operating procedure

For a normal annual entitlement cycle:

1. Configure leave types and entitlement policies.
2. Choose **Front-loaded** or **Monthly accrual**.
3. Configure eligibility rules where required.
4. Resolve or review policy matching for representative staff before running a tenant-wide batch.
5. Generate or reconcile the entitlement period.
6. Review any `NO_MATCHING_POLICY`, `AMBIGUOUS_POLICY`, `LEGACY_PROTECTED`, or `HISTORICAL_PROTECTED` results.
7. Correct policy/rule configuration where necessary and rerun the same period.
8. Avoid changing historical periods unless a separate controlled correction process is intended.

## Related documentation

- [API documentation](api.md)
- [Leave entitlement proration](leave-entitlement-proration.md)
- [Staff change lifecycle](staff-change-lifecycle.md)
- [Architecture](architecture.md)
