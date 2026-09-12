# LeaveMaestro end-to-end tests

This directory contains browser-level regression tests using Playwright.

## Scope

The initial suite covers:

- password login and authenticated application-shell rendering;
- Apply Leave rendering, submission, and the blank-page regression;
- Staff, Manager, HR, and Tenant Admin leave-calendar access;
- role-based leave-calendar write controls;
- shared failure guards for uncaught page errors, console errors, failed requests, and unexpected HTTP 4xx/5xx responses.

The browser tests use deterministic request fixtures instead of production accounts or production data. Backend unit/integration tests and JaCoCo verification run in the same CI job before Playwright, while the browser fixtures provide stable frontend/RBAC contract data.

## Reusable scenario data

`tests/scenario-data.ts` defines the browser representation of the standard Singapore test scenario used by the backend `ScenarioDataFactory`.

The standard scenario includes:

- one Tenant Admin and one HR user;
- two Managers;
- five Staff covering a normal employee, mid-year joiner, recent joiner, jurisdiction-override candidate, and missing-approver case;
- deterministic tenant-scoped IDs and Annual Leave identifiers;
- approver aliases for the positive workflow cases.

Use `createStandardSingaporeScenario('<unique-id>')` when a browser test only needs deterministic metadata. `mockAuthenticatedBackend(...)` consumes this scenario by default and accepts an optional scenario ID as its fourth argument.

## Persisted E2E scenarios

For tests that need actual backend state, the backend exposes scenario bootstrap endpoints only when the explicit `e2e` Spring profile is active:

```text
POST   /test/scenarios/standard-sg-company?scenarioId=<id>&referenceDate=2026-09-12
DELETE /test/scenarios/<id>
```

`tests/scenario-api.ts` wraps these endpoints and `tests/scenario-fixture.ts` provides a reusable Playwright fixture that creates a unique scenario and deletes it in teardown. The response includes the generated tenant ID, test-user aliases/login names, staff IDs, and the E2E password.

The controller, bootstrap service, and matching security chain are all annotated with `@Profile("e2e")`; they are not registered in the default or production runtime. Reset logic also refuses to target anything outside the reserved `E2E-*` tenant namespace.

CI starts the backend with the `e2e` profile against its own in-memory H2 database before Playwright runs. Do not enable this profile in production or point the E2E workflow at a production database.

## Run locally

From `frontend`:

```bash
npm ci
```

Start the backend in a separate terminal from `backend`:

```bash
SPRING_PROFILES_ACTIVE=e2e ./gradlew bootRun --args='--spring.profiles.active=e2e'
```

Then from `frontend/e2e`:

```bash
npm install
npx playwright install chromium
npm test
```

Playwright builds and starts the frontend preview server automatically on `http://127.0.0.1:4173`. The persisted scenario helper defaults to `http://127.0.0.1:8080` for the backend; override it with `E2E_BACKEND_URL` when necessary.

For interactive debugging:

```bash
npm run test:ui
```

## Failure artifacts

Playwright retains a trace, screenshot, and video when a test fails. CI uploads:

- `playwright-report`
- `playwright-test-results`
- `e2e-backend-log` when the workflow fails

Open the HTML report or trace locally to inspect browser actions, console output, and network activity around the failure.

## Adding a role fixture

1. Add the role and its authorities to `tests/support.ts`.
2. Use `mockAuthenticatedBackend(page, '<role>')` in the test.
3. Install `installFailureGuards(page)` before navigating.
4. Assert the role-specific page or control behavior.
5. Call the returned health assertion at the end of the test.

Do not add production credentials to E2E tests or CI variables.

## Network-failure policy

Tests fail on unexpected:

- uncaught `pageerror` events;
- browser console errors;
- failed requests;
- HTTP 4xx/5xx responses.

Tests that intentionally verify an authorization failure may explicitly allow the expected status when installing the failure guards.
