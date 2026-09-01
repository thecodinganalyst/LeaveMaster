# Firebase Hosting backend routing

LeaveMaestro serves the production React SPA from Firebase Hosting and the Spring Boot API from Google Cloud Run. The browser intentionally talks to the Firebase origin for both frontend assets and application requests. Firebase Hosting rewrites selected URL paths to the `leavemaster-api` Cloud Run service.

This same-origin design is important for LeaveMaestro's session and CSRF model. In production the frontend leaves `VITE_API_URL` unset, so `frontend/src/config/env.ts` resolves the API base URL to an empty string. Requests such as `/auth/csrf`, `/api/staff`, `/leave-application-options/leave-types` and `/account-activation/lookup` therefore go to the current Firebase Hosting origin rather than directly to a Cloud Run URL. Firebase decides whether each path should be served as a frontend resource, forwarded to Cloud Run, or handled by the SPA fallback.

## How the routing works

The routing contract lives in `frontend/firebase.json`. Firebase evaluates rewrites in order. Backend routes must appear before the final SPA fallback.

A Cloud Run rewrite has this shape:

```json
{
  "source": "/account-activation/**",
  "run": {
    "serviceId": "leavemaster-api",
    "region": "asia-southeast1"
  }
}
```

When a request matches the source, Firebase Hosting proxies it to the named Cloud Run service. The browser remains on the Firebase origin and does not need to know the Cloud Run URL.

The final rewrite is intentionally:

```json
{
  "source": "**",
  "destination": "/index.html"
}
```

That rule supports client-side React routing. A browser navigation such as `/leave` does not correspond to a physical file, so Firebase returns `index.html` and React Router renders the page.

The catch-all must remain last. If a backend path is not matched before it, the request can be treated as an SPA navigation instead of an API call.

## Current backend path families

The production Hosting configuration routes these path families to `leavemaster-api` in `asia-southeast1`:

| Path | Purpose |
|---|---|
| `/api/**` | Main REST API, including staff, tenants, assistant and other `/api` resources |
| `/auth/**` | Session login, current-user and CSRF endpoints |
| `/account-activation/**` | First-login lookup, PIN request/verification and initial password setup |
| `/oauth2/**` | OAuth authorization entry points |
| `/login/oauth2/**` | OAuth callback/login handling |
| `/logout` | Session logout |
| `/leave-applications` | Legacy/non-`/api` leave application endpoint |
| `/leave-applications/**` | Legacy/non-`/api` leave application subpaths |
| `/leave-application-options/**` | Apply Leave option endpoints such as the active leave-type list |

Everything else eventually reaches the SPA fallback unless it is a static file.

## Why same-origin routing is used

LeaveMaestro uses server-side Spring Security sessions. Production session cookies use the Firebase-compatible `__session` cookie name. Keeping browser requests on the Firebase origin lets the browser send the session and CSRF context consistently while Firebase proxies backend requests to Cloud Run.

This also means frontend code must not assume that a successful HTTP response necessarily came from the backend. A missing Hosting rewrite can cause Firebase to return the SPA document instead. API clients should validate important response shapes, especially authentication, account-activation and leave-application option decisions.

## Incident example: missing account activation rewrite

Issue #443 was caused by `/account-activation/**` being omitted from `frontend/firebase.json`.

The observed sequence was:

1. A new tenant administrator was correctly provisioned with `active=true`, a valid email, `password=NULL`, and the `<tenant>_Admin` role.
2. The login page called `GET /auth/csrf`; that path matched `/auth/**`, so Cloud Run logged the request.
3. The login page then called `POST /account-activation/lookup`.
4. Firebase had no `/account-activation/**` backend rewrite, so the request did not reach Cloud Run.
5. The catch-all SPA rewrite handled the request instead.
6. Cloud Run therefore showed no `account-activation` request at all.
7. The frontend previously treated an unexpected lookup response as if the backend had selected password login, producing a misleading password screen.

The fix added the missing Hosting rewrite and made the activation API client reject malformed or unexpected responses instead of silently choosing password login.

## Incident example: Apply Leave options returned HTML with HTTP 200

Issue #498 was caused by `/leave-application-options/**` being omitted from `frontend/firebase.json`.

`ApplyLeavePage` requested `/leave-application-options/leave-types`. Because the path did not match a backend rewrite, Firebase served the SPA `index.html` through the catch-all rule with HTTP 200. The request therefore produced no Cloud Run error and could look successful in server logs, while the frontend received HTML instead of the expected leave-type JSON array.

The fix added `/leave-application-options/**` to the Cloud Run rewrites and extended the routing regression test. The frontend response guards introduced in #496 remain useful because they prevent a malformed successful response from crashing the route, but the Hosting rewrite is what ensures the production request reaches the backend.

This is an important diagnostic pattern: if a frontend operation appears to succeed or changes UI state but there is no corresponding Cloud Run request, inspect Firebase Hosting rewrites before debugging database or service logic.

## Adding a new backend route

Prefer adding new application REST endpoints below `/api/**` where practical because that family is already routed to Cloud Run. Some authentication and protocol endpoints intentionally live outside `/api`.

When adding a frontend-used backend endpoint outside an already covered family:

1. Identify the browser-visible path, for example `/example/**`.
2. Add a Cloud Run rewrite for that path in `frontend/firebase.json`.
3. Place it before the `** -> /index.html` fallback.
4. Route it to `leavemaster-api` in `asia-southeast1` unless the architecture has intentionally changed.
5. Extend `frontend/src/config/firebaseHosting.test.ts` so the routing contract explicitly covers the new family.
6. Add or update API-client tests for the endpoint.
7. For security-sensitive response decisions, validate the runtime response shape rather than relying only on TypeScript types.
8. Update this document if the production routing model changes.

Do not solve a missing rewrite by hard-coding the Cloud Run service URL into production frontend code. Doing so would bypass the intended same-origin session, CSRF and deployment model.

## Automated verification

Frontend CI runs Vitest as part of the normal quality gate. `firebaseHosting.test.ts` reads `frontend/firebase.json` and verifies that required backend path families:

- exist as Cloud Run rewrites;
- target `leavemaster-api`;
- target `asia-southeast1`; and
- appear before the SPA catch-all.

When introducing a new non-`/api` backend family, extend the routing-contract list in that test in the same change. Code review should treat the frontend API path and Firebase rewrite as one deployment contract.

The normal frontend checks are:

```bash
cd frontend
npm ci
npm run lint
npm run typecheck
npm test
npm run coverage
npm run build
```

A pull request validates the configuration but does not deploy production. A qualifying push to `main` runs the Firebase Hosting deployment workflow and deploys the validated `frontend/dist` artifact plus the current Hosting configuration.

## Verifying after deployment

After a production deployment, verify both the Hosting release and the backend request path.

For an API request that should reach Cloud Run:

1. Open the production Firebase application.
2. Perform the action that invokes the endpoint.
3. Check the browser Network panel when available and confirm the expected request path and status.
4. Check Cloud Run request logs for the same method and path.
5. For Apply Leave, opening the page should produce `GET /leave-application-options/leave-types` in Cloud Run logs and return JSON rather than the SPA document.
6. For account activation specifically, submitting tenant ID and login name should produce `POST /account-activation/lookup` in Cloud Run logs.
7. Requesting a PIN should then produce `POST /account-activation/request` and the corresponding account-activation/email service logs.

The Firebase deployment workflow log also identifies the exact Hosting site. Production currently resolves to `leavemaster-production` when no explicit `FIREBASE_HOSTING_SITE` override is configured.

## Troubleshooting

### `/auth/csrf` reaches Cloud Run but another request does not

This strongly suggests the missing route is not covered by a Hosting rewrite. Compare the request path with `frontend/firebase.json` and ensure its rule is before the SPA fallback.

### The API response contains HTML or looks like the app shell

This is a common sign that the SPA fallback handled an API request. Confirm the request path has a Cloud Run rewrite. Do not treat the response as a valid API result.

### The frontend displays an unexpected authentication step

Check the actual backend request first. For first-login activation, Cloud Run must receive `/account-activation/lookup`. If it does not, investigate Firebase routing. If it does, use `AccountActivationService` eligibility logs and the tenant-scoped user data to diagnose backend eligibility.

### GitHub Actions passed but production still behaves differently

Confirm the relevant deployment workflow, not only CI, ran for the expected commit. Frontend/Firebase and backend/Cloud Run deployments are separate path-aware workflows. Verify the commit SHA and the deployed Hosting site or Cloud Run revision as appropriate.

### Incognito/private browsing has the same problem

That makes ordinary browser cache less likely. Check Hosting deployment destination and rewrite behavior instead of repeatedly clearing browser state.

## Maintenance checklist

Whenever frontend code adds or changes a backend call:

- Is the endpoint under an already routed family such as `/api/**`?
- If not, was `frontend/firebase.json` updated?
- Is the backend rewrite before the `**` SPA fallback?
- Does `firebaseHosting.test.ts` cover the path family?
- Does the API client validate security-sensitive response shapes?
- Did frontend lint, typecheck, tests, coverage and build pass?
- After merge, did the Firebase production deployment run for the expected commit?
- Can the request be observed in Cloud Run logs after deployment?

Treat Firebase Hosting rewrites as part of the application API contract, not merely static-hosting configuration.
