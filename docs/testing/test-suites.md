# Automated test suites

LeaveMaestro separates fast pull-request smoke coverage from the broader business regression catalogue. The same deterministic scenario infrastructure is reused by both suites; tests are not copied merely to place them in another suite.

## PR smoke suite

The PR smoke suite runs from `.github/workflows/e2e.yml` for relevant pull requests. It preserves the normal backend and frontend unit/coverage gates, then runs a compact business subset.

Backend smoke scenarios are exposed by the Gradle `smokeTest` task. The task selects the high-value methods from `CoreBusinessScenarioRegressionTest`, covering:

- standard deterministic scenario/entitlement generation;
- representative annual-leave proration;
- employment-start boundary validation;
- assigned-manager approval plus unrelated-manager authorization denial;
- tenant-boundary validation.

Playwright smoke membership uses the `@smoke` marker in the test title. The current browser smoke set covers:

- password authentication and application shell rendering;
- Staff opening/submitting Apply Leave;
- employment-date limits on Apply Leave;
- Apply Leave blank-page regression protection;
- Staff apply -> assigned Manager approve -> Staff sees approved leave and reduced balance;
- unrelated Manager authorization denial;
- persisted multi-tenant Staff-list isolation;
- Staff/Manager/HR/Admin role-matrix sanity;
- Staff/Manager leave-calendar write-control denial;
- tenant-admin Staff-list isolation sanity.

Run locally:

```bash
cd backend
./gradlew smokeTest

cd ../frontend/e2e
npm install
npm run test:smoke
```

For the persisted-scenario Playwright tests, start the backend with the `e2e` profile first and expose it at `http://127.0.0.1:8080`, or set `E2E_BACKEND_URL` to the isolated E2E backend URL.

## Full regression suite

The full business regression runs from `.github/workflows/full-regression.yml`:

- nightly at 02:00 Singapore time;
- on manual `workflow_dispatch`;
- on pull requests that change the regression workflow or regression-test infrastructure, so workflow changes validate themselves before merge.

The backend `regressionTest` task runs the complete `CoreBusinessScenarioRegressionTest` catalogue, including approval/rejection variants, employment boundaries, weekend/public-holiday charging, tenant boundaries and multi-jurisdiction calendar selection.

The Playwright regression command runs the complete E2E catalogue, including tests without `@smoke`, so the broader #528 browser scenarios continue to run without maintaining duplicate test copies.

Run locally:

```bash
cd backend
./gradlew regressionTest

cd ../frontend/e2e
npm install
npm run test:regression
```

## Where a test belongs

Use these rules when adding coverage:

| Test type | Use when | Suite |
| --- | --- | --- |
| Unit/integration only | Fast rule, mapper, repository or service behavior is sufficiently proven below the browser boundary | Normal backend/frontend test suite |
| PR smoke | Failure would block a critical user journey or security boundary and feedback must arrive before merge | Add the Playwright `@smoke` marker or include the backend method in `smokeTest` |
| Full regression | Important variant/permutation that adds confidence but does not need to run on every application PR | Full Playwright catalogue or backend `regressionTest` |
| Both | A scenario is business-critical and should also remain part of the broader catalogue | Mark/select it for smoke; it is automatically included by the full suite |

Do not create a second copy of a test only to place it in another suite.

## Isolation and parallel execution

Persisted Playwright scenarios use the #526/#527 scenario factories and protected E2E bootstrap API. Each test receives a unique scenario identifier derived from the run, worker and test ID, and its fixture is deleted after the test. This allows Playwright's independent scenarios to run in parallel without sharing production-like mutable data.

Both workflows use the isolated `e2e` Spring profile and H2 database. Neither workflow deploys the application or points tests at production.

## Failure diagnostics

Both workflows fail when required tests fail and retain diagnostics for 14 days:

- Playwright HTML report;
- traces, screenshots and videos retained by Playwright on failures;
- Playwright `test-results` on failure;
- backend Gradle test reports;
- isolated E2E backend log on failure.

Scenario IDs are part of the deterministic fixture/test context and contain no credentials or secrets, allowing a failed scenario to be reproduced locally.
