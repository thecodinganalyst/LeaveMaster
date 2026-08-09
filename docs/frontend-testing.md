# Frontend testing and coverage

LeaveMaster uses Vitest with the jsdom environment and React Testing Library for frontend unit/component tests. Tests are deterministic and mock LeaveMaster network calls at the HTTP/API boundary; they must not depend on Cloud Run, Supabase, Firebase, or OpenAI being available.

## Commands

Run from `frontend/`:

```bash
npm test
npm run coverage
```

`npm test` runs the frontend test suite once. `npm run coverage` runs the same suite with V8 coverage and fails when the configured global threshold is not met.

Coverage output is written to `frontend/coverage/` in text, HTML, LCOV, and JSON-summary formats. GitHub Actions uploads this directory as the `frontend-coverage` artifact even when the quality gate fails, making regressions diagnosable.

## Initial quality gate

The initial global minimum is:

| Metric | Minimum |
| --- | ---: |
| Statements | 60% |
| Branches | 50% |
| Functions | 60% |
| Lines | 60% |

The gate covers application logic under `src/api`, `src/auth`, `src/providers`, and `src/features`. Test/spec files, declaration files, type-only modules, and barrel `index.ts` files are excluded. Bootstrap/routing-only code is intentionally outside the initial gate because behavior there is better validated by build/routing checks than by artificial coverage tests.

The threshold is a floor, not a target. Raise it gradually as meaningful tests are added; do not lower it merely to make a pull request pass. When coverage falls, prefer tests around user-visible behavior, authorization decisions, validation, API mapping, and destructive/write flows.

## Required critical-path coverage

Changes to authentication or RBAC should cover successful sessions, expired/unauthenticated sessions, forbidden actions, and server-issued authorities. Leave workflow changes should cover application/cancellation and manager approval decisions without live backend dependencies. Assistant write changes should cover the server-issued confirmation token flow, explicit Confirm/Cancel behavior, authoritative execution results, error handling, and duplicate/replay behavior.

Avoid tests that assert implementation details such as component internals or large snapshots. Prefer queries and interactions a user would recognize, with network calls mocked at `apiFetch`/`fetch` boundaries.
