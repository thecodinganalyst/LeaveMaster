# AskLeaveMaestro seeded test personas

Issue #579 extends the shared `ScenarioDataFactory` used by backend and Playwright tests. The catalogue is deterministic, uses reserved `E2E-*` tenants, and is never based on mutable production or DEMO data.

## Fixed reference scenario

Tests should use a fixed reference date (the regression suite uses `2026-09-12`). A scenario ID produces a tenant named `E2E-<scenarioId>`. All users use the E2E-only password returned by the bootstrap endpoint/service.

| Alias | Purpose | Key expected value |
|---|---|---|
| admin | Tenant Admin | tenant-scoped admin role |
| hr | HR | tenant-scoped HR role |
| manager01 / manager02 | Managers | deterministic approver/direct-report relationships |
| staff001 | normal full-year employee + leave history | Annual Leave entitlement = 14.00 days; PENDING, APPROVED and CANCELLED examples |
| staff002 | mid-year joiner | joins 2026-07-01; Annual Leave = 7.00 days |
| staff003 | recent joiner | joins 14 days before reference date |
| staff004 | jurisdiction override baseline | SG baseline, reusable for jurisdiction tests |
| staff005 | missing approver edge case | deliberately has no approver |
| staff006 | low/near-balance employee | Annual Leave = 1.00 day |
| staff007 | terminating employee | termination = reference date + 30 days |
| staff008 | half-day employee | Friday = AM; approved AM leave example |
| staff009 | second-jurisdiction employee | AU-NSW |
| platform admin | platform-level security persona | use the existing platform-admin test authentication fixture; it intentionally does not belong to an E2E tenant |

## Deterministic policy and calendar

The fixture contains an active SG Annual Leave policy of 14.00 days, monthly proration, no carry-forward, and an explicit FULL_TIME eligibility rule. It also contains a tenant SG calendar with an `E2E Public Holiday` at reference date + 10 days.

Expected values are stored explicitly in the scenario's `expectedValues` map. Tests should assert those constants rather than recalculate the business rule they are trying to verify.

## Isolation and jurisdictions

Each scenario ID creates a distinct tenant. Creating two scenario IDs is the standard tenant-isolation fixture. The tenant advertises SG and AU-NSW coverage; `staff009` is the deterministic AU-NSW persona while the remaining baseline staff are SG.

## Reset / idempotency

Backend tests and E2E setup can call:

```java
bootstrapService.resetStandardSingaporeScenario(scenarioId, referenceDate);
```

Reset deletes only the reserved `E2E-<scenarioId>` tenant and recreates it from the shared factory. Repeating reset with the same ID/date restores the same logical users, employment dates, schedules, policies, expected values and leave scenarios.

Use unique scenario IDs for parallel Playwright workers. Delete scenarios after browser tests to avoid retaining test rows.
