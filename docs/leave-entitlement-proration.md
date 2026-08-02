# Leave entitlement proration

This document explains the proration logic used by `StaffService` when normalizing staff leave entitlements.

## When proration runs

Proration runs only when entitlement dates are not manually supplied:

- If both `from` and `to` are provided, they are validated (`from <= to`) and no automatic proration is performed.
- If only one of `from` or `to` is provided, the request is rejected.
- If both are missing, the service derives period and prorated value automatically.

## Automatic period resolution

When both dates are missing:

1. `joinDate` is mandatory.
2. The leave calendar covering `joinDate` is loaded using `LeaveCalendarService.getCalendarFor(joinDate)`.
   - If not found, the request fails.
3. Entitlement period is set to:
   - `from = calendar.start`
   - `to = min(calendar.end, termDate)` when `termDate` is present and earlier; otherwise `calendar.end`.

## Proration formula

Given:

- `fullPeriodEntitlement` = entitlement provided in payload (for full calendar period)
- `from/to` = resolved entitlement period
- `effectiveFrom = max(joinDate, from)`
- `effectiveTo = min(termDate, to)` when `termDate` exists, else `to`

Rules:

1. If the staff member is active for the whole period (`effectiveFrom == from` and `effectiveTo == to`), keep the original entitlement.
2. Otherwise prorate by inclusive day counts:
   - `totalPeriodDays = days_between(from, to) + 1`
   - `effectiveDays = days_between(effectiveFrom, effectiveTo) + 1`
   - `prorated = fullPeriodEntitlement * effectiveDays / totalPeriodDays`
3. If `effectiveDays <= 0`, entitlement becomes `0.00`.
4. Result is rounded to 2 decimal places using `HALF_UP`.

## Related validation failures

The service rejects entitlement normalization when:

- entitlement amount is missing
- leave type is missing or unknown
- period is invalid (`from > to`)
- only one period boundary is supplied
- `joinDate` is missing for automatic period mode
- no leave calendar can be found/generated for `joinDate`
