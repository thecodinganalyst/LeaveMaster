# Core business regression scenarios

Issue #528 defines the first scenario-driven business regression suite. The suite intentionally uses multiple test layers: backend tests for domain rules and Playwright for critical user-visible journeys.

| # | Business scenario | Automated coverage |
|---|---|---|
| 1 | Tenant -> jurisdiction -> staff -> entitlement | `CoreBusinessScenarioRegressionTest.standardScenarioContainsTenantStaffAndAnnualEntitlements` plus `scenario-bootstrap.e2e.ts` |
| 2 | Mid-year joiner receives prorated Annual Leave | `CoreBusinessScenarioRegressionTest.midYearJoinerStartsEntitlementOnJoinDateAndReceivesProratedAmount` |
| 3 | Cannot apply before employment start | `CoreBusinessScenarioRegressionTest.cannotApplyBeforeEmploymentStartDate` and existing Apply Leave browser date-boundary coverage |
| 4 | Cannot apply after termination | `CoreBusinessScenarioRegressionTest.cannotApplyAfterTerminationDate` and existing Apply Leave browser date-boundary coverage |
| 5 | Staff applies -> manager approves | `core-business-regression.e2e.ts` real persisted scenario journey, plus service approval tests |
| 6 | Staff applies -> manager rejects | `core-business-regression.e2e.ts` real persisted scenario journey, plus service rejection tests |
| 7 | Weekend/public holiday charging | `CoreBusinessScenarioRegressionTest.weekendAndPublicHolidayAreNotCharged` plus existing leave application service tests |
| 8 | Staff/Manager/HR/Admin role access | `core-business-regression.e2e.ts` role-matrix browser test; seeded E2E roles use production-equivalent permissions |
| 9 | Tenant A cannot see Tenant B data | `core-business-regression.e2e.ts` creates two persisted tenants and verifies Staff-list isolation; service tenant-boundary test covers leave types |
| 10 | Staff jurisdiction selects calendar/rules | `CoreBusinessScenarioRegressionTest.applicableStaffJurisdictionSelectsCalendarInMultiJurisdictionContext` plus existing jurisdiction calendar tests |

## Reliability rules

- Browser business tests use the profile-gated `/test/scenarios/**` bootstrap from #527 and automatically delete their generated scenario.
- Scenario identifiers are unique per Playwright worker/test and support parallel execution.
- The reusable `ScenarioDataFactory` is the source of staff, role, approver, entitlement and work-schedule fixtures.
- Browser tests use deterministic waits and assertions rather than sleeps.
- `installFailureGuards` fails successful journeys on uncaught page errors, console errors, failed network requests or unexpected HTTP errors.
- The E2E frontend is built against the isolated backend URL, never production.

## Expected PR gate

A change to the core business journeys is considered regression-safe only when backend tests and coverage, frontend unit tests and coverage, and Playwright E2E all pass.
