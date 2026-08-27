# Troubleshooting LeaveMaster

This guide covers common local, CI, Firebase, Cloud Run, authentication, transactional email, Terraform and AI-assistant failures.

## Frontend build or CI failures

### `npm ci` fails

Check:

- Node.js version is compatible with the repository workflow (Node 22+).
- `frontend/package-lock.json` matches `frontend/package.json`.
- The command is being run from `frontend/`.

Use `npm install` only when intentionally updating dependencies/lockfile. CI should continue using `npm ci`.

### TypeScript build fails but tests pass

The production build runs strict TypeScript compilation before Vite. Fix type errors rather than bypassing `npm run typecheck` or changing the build to skip `tsc -b`.

Run:

```bash
cd frontend
npm run typecheck
npm run build
```

### Coverage gate fails

Run:

```bash
cd frontend
npm run coverage
```

Add tests for changed behavior. Do not reduce coverage thresholds solely to make CI green.

## Local frontend cannot call backend

Expected local configuration:

```text
Frontend: http://localhost:5173
Backend:  http://localhost:8080
VITE_API_URL=http://localhost:8080
```

Confirm `frontend/.env.local` was created from `.env.example`, then restart Vite after changing environment variables.

If the browser reports CORS failure, confirm the backend allowed-origin configuration includes exactly `http://localhost:5173` and not an unrelated port/hostname.

## Login problems

### Newly created staff has no password

This is expected for staff provisioned through the current staff-creation flow. LeaveMaestro no longer generates a default password for new staff users.

From the login page:

1. enter the staff user's login name;
2. select **Continue**;
3. use **Send verification PIN** when the activation flow is offered;
4. verify the PIN sent to the staff email;
5. choose a permanent password.

Do not create/recover a default password as a workaround. See `docs/account-activation-and-email.md`.

### Activation PIN email is not delivered

Check:

- `EMAIL_PROVIDER=resend` is set in the deployed runtime;
- `EMAIL_FROM_ADDRESS` uses the intended development or verified-domain sender;
- Secret Manager contains an enabled version for the configured Resend secret;
- Cloud Run's runtime service account has `secretAccessor` on that secret;
- the Cloud Run revision has a secret-backed `RESEND_API_KEY` environment reference;
- Resend accepted the request / shows the expected test or delivery event.

Never print the API key or activation PIN while diagnosing delivery.

A provider delivery failure invalidates the generated PIN but retains request-throttling state. The user must request another PIN after the configured cooldown permits it.

### Resend rejects the sender/domain

For the current development/test rollout, use the supported `resend.dev` identity such as `onboarding@resend.dev`.

If using a custom sender, verify its domain/subdomain in Resend first and add the DNS records Resend provides. Buying a GoDaddy mailbox/email-hosting plan is not required merely for outbound Resend delivery.

### PIN is invalid or expired

The current defaults are:

- six-digit PIN;
- 15-minute expiry;
- maximum 5 failed verification attempts;
- 60-second resend cooldown;
- maximum 5 PIN requests per hour.

Requesting a new PIN replaces the prior active PIN. After successful password setup the activation is consumed and cannot be replayed.

Do not attempt to retrieve a plaintext PIN from the database or logs; only its hash is persisted.

### Correct credentials return a login error

Check the `app_user` row:

- exact login name;
- `active=true`;
- an activated account has a BCrypt password hash (usually starts with `$2`);
- required role assignments exist.

A pending staff account intentionally has no permanent password and should follow the activation flow instead of normal password authentication.

Spring Security form login posts to `/login` with `username` and `password` form parameters.

### Login appears successful but `/auth/me` returns `401` in production

This usually means the authenticated session did not survive the Firebase Hosting rewrite.

The Cloud Run profile must use:

```text
cookie name: __session
Secure: true
HttpOnly: true
SameSite: Lax
```

Firebase Hosting forwards the special `__session` cookie to the rewritten backend. A default `JSESSIONID` will not preserve the production session through the Hosting-to-Cloud-Run path.

Confirm the browser receives `__session` after login and sends it on later requests to the Firebase app origin.

### PlatformAdmin password is unknown

The bootstrap initializer does not reset an existing PlatformAdmin password on every restart. A production database may therefore contain a valid BCrypt hash for an older password even when the local fallback is `changeme`.

Use the controlled recovery procedure in `docs/platform-admin-password.md`; do not manually store a plaintext password in PostgreSQL.

## CSRF errors

Unsafe requests obtain a CSRF token from `/auth/csrf` and send the returned header/token with credentials.

If a request is rejected:

1. confirm `/auth/csrf` returns successfully;
2. confirm the browser retains the session cookie;
3. inspect the request for the expected CSRF header;
4. avoid mixing direct Cloud Run and Firebase origins in the same browser session.

Production should use the Firebase origin and same-origin rewrites.

## OAuth/OIDC problems

### Provider reports redirect URI mismatch

The provider callback must match the canonical public app URL exactly:

```text
https://<app-origin>/login/oauth2/code/google
https://<app-origin>/login/oauth2/code/microsoft
https://<app-origin>/login/oauth2/code/github
https://<app-origin>/login/oauth2/code/facebook
```

If a custom domain is introduced, update both `PUBLIC_APP_URL` and the callback registrations at every enabled IdP.

### OAuth callback opens the SPA instead of Spring Security

Check Firebase Hosting rewrites include both:

```text
/oauth2/**
/login/oauth2/**
```

and that they appear before the catch-all SPA rewrite.

### OAuth login is rejected for a known person

LeaveMaster intentionally allows provider login only for an existing active application user mapped to the correct provider/subject. Verify the user's `oidcProvider`, `oidcSubject`, active state and roles.

## Firebase Hosting problems

### `Site Not Found`

Terraform provisioning a Hosting site does not upload frontend assets.

Check the `Frontend CI and Firebase Hosting` workflow has completed a production deployment after the site was provisioned. The deployment should upload the validated `frontend/dist` artifact.

### Frontend deploy workflow succeeds but old assets appear

Check:

- correct Firebase site ID/environment;
- the workflow built from the expected commit;
- `frontend/dist` artifact corresponds to that run;
- immutable asset caching is not being confused with the no-cache `index.html` entrypoint.

### API route returns `index.html`

A backend path is probably missing from `frontend/firebase.json` or is below the catch-all rewrite. Dynamic backend rewrites must be listed before:

```json
{ "source": "**", "destination": "/index.html" }
```

## Cloud Run deployment problems

### `Image ...:<sha> not found`

The Cloud Run Terraform plan deploys the image tagged with the current Git SHA. Cloud Build must push that exact tag first.

Do not derive the mutable image tag from Terraform state after a targeted prerequisite apply; targeted applies can leave state-backed outputs stale. The deployment workflow constructs the image URI using current workflow variables/Git SHA.

Check Cloud Build/Artifact Registry for the exact SHA shown in the Cloud Run error.

### Cloud Run revision fails startup

Review application logs for the first root exception. Common causes:

- database host/user/password unavailable;
- Flyway validation/migration error;
- invalid production `APP_PUBLIC_URL`;
- wildcard/non-HTTPS CORS setting;
- transactional email enabled but the Resend Secret Manager secret/version is missing;
- assistant enabled without the selected provider API key;
- Secret Manager secret version missing or runtime service account lacks accessor permission.

The `cloudrun` profile intentionally fails early for unsafe/missing runtime configuration.

### Cloud Run reaches Supabase but Flyway fails

Use Supabase direct/session-mode compatible connectivity on port 5432. Transaction pooler mode on port 6543 is not suitable for Flyway migration behavior.

Inspect `flyway_schema_history` before manually editing migration state. Migration filenames/versions already applied in production should not be casually renamed or reused.

## Terraform problems

### Terraform says resources already exist

A resource may have been created manually or by an earlier workflow but is not represented in the current Terraform state.

Do not delete production infrastructure just to make Terraform succeed. Determine whether the resource should be imported into the production state. Use the exact same remote GCS backend/prefix as the deployment workflow.

### `-target` warning appears

The Cloud Run deployment intentionally uses a targeted prerequisite phase so build-time resources can exist before a not-yet-built image is deployed.

Treat targeted output values carefully. A subsequent full plan/apply is still required to reconcile the complete configuration.

### Protected-plan check fails

The deployment workflow refuses a plan that would delete/replace critical backend infrastructure. Inspect the Terraform plan rather than bypassing the safety check. Typical root causes are count/conditional changes, renamed addresses or state mismatch.

## Secret Manager problems

### Cloud Run says secret/version not found

Terraform can create or reference the Secret Manager resource without creating a secret **value**. Add an enabled secret version before enabling a Cloud Run secret reference.

### Resend key should be added or rotated

Use the existing/default secret ID `leavemaster-resend-api-key` unless deployment configuration specifies another one. Add a new Secret Manager version when rotating the value.

Do not place `RESEND_API_KEY` in GitHub variables, Terraform plaintext, Vite configuration or logs. Cloud Run should receive it through a secret-backed environment reference.

### PlatformAdmin secret was added but password did not change

Setting `PLATFORM_ADMIN_PASSWORD` alone does not reset an existing account. The controlled reset switch must be enabled for one deployment, then immediately disabled again. See `docs/platform-admin-password.md`.

### OpenAI key should be rotated

Add a new version to the existing Secret Manager secret rather than placing a key in GitHub/Vite/Terraform plaintext. Cloud Run references `latest` in the declarative assistant configuration.

## AI assistant problems

### `/api/assistant/chat` returns `503`

Check the configured assistant provider/model and its corresponding Secret Manager credential. The active provider must have an enabled secret version, runtime secret access and a secret-backed environment entry.

### Assistant returns `502`

This generally indicates provider/upstream failure. Inspect Cloud Run logs and provider status/credentials. LeaveMaster applies bounded retries, timeout and a circuit breaker, so repeated failures may temporarily trip the circuit.

### Assistant returns `429`

The configured per-user/per-tenant request limit or maximum input size may have been exceeded. See `docs/assistant-security.md` for defaults.

### User asks the AI to perform a write but nothing changes

That is expected before confirmation. Writes/approvals/destructive tools are stored as pending actions and return an opaque confirmation token. The business mutation occurs only through `POST /api/assistant/actions/confirm` after server-side actor/tenant/RBAC/expiry checks.

### Confirm button is clicked twice

The backend persists execution state and locks the pending action. An already executed token returns the stored result with `replayed=true` rather than executing the mutation again.

### Assistant exposes a tool the user should not have

Treat this as a security defect. Tool availability should be filtered by authenticated authorities and tool execution should still enforce server-side authorization. Capture the actor, authorities, requested tool and relevant audit event; do not solve it by hiding the frontend UI only.

## GitHub Actions problems

### A frontend-only PR triggers backend deployment

Check workflow `paths` filters. Frontend CI should own `frontend/**`; backend CI/deployment should be scoped to backend/infrastructure/container workflow paths.

### PR deploys production unexpectedly

Frontend PR runs should validate/build but not deploy Firebase Hosting. Production deployment should be restricted to the intended `main`/manual path and protected production environment.

### WIF authentication fails

Verify:

- `WIF_PROVIDER` points to the current workload identity provider resource;
- `WIF_SERVICE_ACCOUNT` is the intended GitHub Actions service account;
- provider attribute condition allows this repository;
- service account IAM allows the workload identity principal to impersonate it;
- workflow has `id-token: write`.

Do not fall back to committing a service-account JSON key.

## What to collect when opening an issue

Include:

- failing URL/operation;
- environment (local/production);
- Git commit SHA/workflow run;
- HTTP status code;
- first relevant Cloud Run/application exception;
- whether failure occurs through Firebase origin or direct Cloud Run origin;
- relevant non-secret config names/values;
- Terraform plan/resource address when infrastructure-related.

Never include database passwords, Resend/OpenAI/Gemini API keys, activation PINs, OAuth client secrets, session cookies, CSRF tokens, WIF credentials or Secret Manager payloads.

## Related guides

- `docs/account-activation-and-email.md`
- `docs/architecture.md`
- `docs/development-and-ci.md`
- `docs/environments-and-domains.md`
- `docs/cloudrun-deployment.md`
- `docs/platform-admin-password.md`
- `docs/assistant-security.md`
