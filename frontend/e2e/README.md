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

## Run locally

From `frontend`:

```bash
npm ci
```

Then from `frontend/e2e`:

```bash
npm install
npx playwright install chromium
npm test
```

Playwright builds and starts the frontend preview server automatically on `http://127.0.0.1:4173`.

For interactive debugging:

```bash
npm run test:ui
```

## Failure artifacts

Playwright retains a trace, screenshot, and video when a test fails. CI uploads:

- `playwright-report`
- `playwright-test-results`

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
