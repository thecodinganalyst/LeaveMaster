# Environments, domains, CORS and runtime secrets

LeaveMaestro uses a same-origin production architecture: the React/Vite application is hosted by Firebase Hosting and browser-facing backend paths are rewritten by Firebase Hosting to the Spring Boot service on Cloud Run. This keeps session cookies and CSRF on one browser origin and avoids embedding the Cloud Run URL in the production bundle.

## Environment matrix

| Setting | Local development | Production |
|---|---|---|
| Frontend origin | `http://localhost:5173` | `https://leavemaster-production.firebaseapp.com` by default |
| Browser API base | `VITE_API_URL=http://localhost:8080` | leave `VITE_API_URL` unset |
| Backend | `http://localhost:8080` | Cloud Run behind Firebase Hosting rewrites |
| Session cookie | Spring default | `__session`, `Secure`, `HttpOnly`, `SameSite=Lax` |
| CORS | exact `http://localhost:5173` | exact configured production frontend origin(s) only |
| OAuth callback base | `http://localhost:8080` | canonical public app URL |
| Database/OpenAI/admin secrets | local environment only | Google Secret Manager / Cloud Run secret references |

## Frontend environment

Copy `frontend/.env.example` to `frontend/.env.local` for local development. Only non-secret browser configuration belongs in Vite variables.

```text
VITE_API_URL=http://localhost:8080
```

For production, do not set `VITE_API_URL`. `frontend/src/config/env.ts` then uses a relative API base and requests remain on the Firebase Hosting origin.

Never put database passwords, OpenAI keys, OAuth client secrets, admin passwords, or service-account credentials in `VITE_*` variables. Vite variables are compiled into browser assets.

## Firebase Hosting and Cloud Run

Production Hosting rewrites these backend paths before the SPA fallback:

- `/api/**`
- `/auth/**`
- `/oauth2/**`
- `/login/oauth2/**`
- `/login`
- `/logout`

Firebase Hosting forwards those requests to the `leavemaster-api` Cloud Run service in `asia-southeast1`. The browser therefore stays on the frontend origin even though Spring Boot handles the request.

The Cloud Run profile uses the `__session` cookie name because Firebase Hosting only forwards that specially named session cookie to dynamic backends. The cookie remains `Secure`, `HttpOnly`, and `SameSite=Lax`.

## CORS

Spring Security enables credentialed CORS with an exact origin allowlist. Wildcards are not permitted by production validation.

Local default:

```text
APP_CORS_ALLOWED_ORIGINS=http://localhost:5173
```

Terraform supplies production origins through `allowed_frontend_origins`. The GitHub production environment accepts the Terraform list as JSON in `ALLOWED_FRONTEND_ORIGINS`, for example:

```text
["https://leavemaster-production.firebaseapp.com"]
```

If `ALLOWED_FRONTEND_ORIGINS` is not set, Terraform allows only the canonical `public_app_url`.

Same-origin Firebase requests do not require CORS, but keeping an exact production allowlist makes direct browser calls to the backend fail closed rather than allowing arbitrary origins.

## Canonical public URL and custom domains

Terraform derives the default canonical application URL as:

```text
https://<project-id>-<frontend-environment>.firebaseapp.com
```

For the current production environment this is:

```text
https://leavemaster-production.firebaseapp.com
```

To move to a custom application domain such as `app.example.com`:

1. Add the domain to the Firebase Hosting site and complete Firebase's DNS ownership/TLS setup.
2. Add a GitHub production environment variable:

   ```text
   PUBLIC_APP_URL=https://app.example.com
   ```

3. Set the CORS list to the exact intended origins, for example:

   ```text
   ALLOWED_FRONTEND_ORIGINS=["https://app.example.com"]
   ```

4. Update each OAuth provider's registered callback URL to:

   ```text
   https://app.example.com/login/oauth2/code/<provider>
   ```

5. Deploy Cloud Run, then deploy Firebase Hosting.

A separate `api.example.com` domain is optional and is not recommended for the browser session flow. Keeping browser traffic under `app.example.com` and using Hosting rewrites avoids cross-site cookie/SameSite complexity. An API domain can still be introduced later for machine-to-machine clients with an authentication model designed for it.

DNS ownership remains with the domain/DNS provider. TLS for a Firebase Hosting custom domain is managed by Firebase after domain verification. Cloud Run remains the backend origin behind the Hosting rewrite.

## OAuth provider callbacks

LeaveMaestro now uses an explicit callback base rather than deriving production callbacks from whichever proxy hostname reached the backend.

Local callback URLs:

```text
http://localhost:8080/login/oauth2/code/google
http://localhost:8080/login/oauth2/code/microsoft
http://localhost:8080/login/oauth2/code/github
http://localhost:8080/login/oauth2/code/facebook
```

Production callback URLs use the canonical public app URL, for example:

```text
https://leavemaster-production.firebaseapp.com/login/oauth2/code/google
https://leavemaster-production.firebaseapp.com/login/oauth2/code/microsoft
https://leavemaster-production.firebaseapp.com/login/oauth2/code/github
https://leavemaster-production.firebaseapp.com/login/oauth2/code/facebook
```

Firebase rewrites the callback path to Cloud Run. After successful OAuth authentication, Spring redirects the browser to the canonical app root. Failed OAuth authentication returns the browser to `/login?oauthError=true` on the canonical app origin.

OAuth client IDs may be ordinary environment configuration, but OAuth client secrets must remain backend-only and must never be exposed through Vite.

## OpenAI runtime secret

The OpenAI assistant is disabled by default. The deployment expects an existing Secret Manager secret named:

```text
leavemaster-openai-api-key
```

Create it once if needed:

```bash
printf '%s' 'YOUR_OPENAI_API_KEY' | \
  gcloud secrets create leavemaster-openai-api-key \
    --data-file=- \
    --replication-policy=automatic \
    --project=leavemaster
```

If the secret already exists, rotate it by adding a version:

```bash
printf '%s' 'YOUR_NEW_OPENAI_API_KEY' | \
  gcloud secrets versions add leavemaster-openai-api-key \
    --data-file=- \
    --project=leavemaster
```

Then set these GitHub **production environment variables**:

```text
ENABLE_OPENAI_ASSISTANT=true
OPENAI_API_KEY_SECRET_ID=leavemaster-openai-api-key
OPENAI_MODEL=gpt-5-mini
```

Terraform grants only the Cloud Run runtime service account `roles/secretmanager.secretAccessor` on that secret and injects `OPENAI_API_KEY` as a secret-backed environment variable. The secret value is not placed in GitHub variables or Terraform state.

When `ENABLE_OPENAI_ASSISTANT=false`, Cloud Run receives no OpenAI secret and Spring AI chat remains disabled.

## GitHub production environment variables

Core deployment variables remain:

```text
GCP_PROJECT_ID
GCP_REGION
SUPABASE_DB_HOST
SUPABASE_DB_USERNAME
TF_STATE_BUCKET
WIF_PROVIDER
WIF_SERVICE_ACCOUNT
ENABLE_FIREBASE_HOSTING
FRONTEND_ENVIRONMENT
ENABLE_PLATFORM_ADMIN_PASSWORD_SECRET
RESET_PLATFORM_ADMIN_PASSWORD
```

Issue #119 adds these optional runtime variables:

```text
PUBLIC_APP_URL                 # omit to use the Firebase default
ALLOWED_FRONTEND_ORIGINS       # Terraform JSON list; omit to allow only PUBLIC_APP_URL/default
ENABLE_OPENAI_ASSISTANT        # default false
OPENAI_API_KEY_SECRET_ID       # default leavemaster-openai-api-key
OPENAI_MODEL                   # default gpt-5-mini
```

Secrets themselves belong in Google Secret Manager, not GitHub environment variables.

## Production startup validation

The `cloudrun` Spring profile fails startup clearly when:

- `APP_PUBLIC_URL` is missing or is not an HTTPS origin;
- a configured CORS origin contains a wildcard or is not an HTTPS origin;
- the OpenAI assistant is enabled without an `OPENAI_API_KEY` secret value.

Database placeholders are also required by the Cloud Run datasource configuration, so missing database runtime settings fail during application startup rather than silently falling back to H2.

## Verification checklist

After deployment:

1. Open the canonical production frontend URL.
2. Sign in and verify `/auth/me` returns `200` after login.
3. Verify the browser session cookie is named `__session` and is `Secure`/`HttpOnly`.
4. Confirm API requests use the Firebase frontend host rather than the `run.app` host.
5. Send a CORS preflight from an unapproved origin and confirm it is rejected.
6. If OAuth is configured, verify the provider receives the canonical `/login/oauth2/code/<provider>` callback and the successful flow returns to the app.
7. If the assistant is enabled, verify Cloud Run has a secret-backed `OPENAI_API_KEY` entry and that no `VITE_*` variable contains the key.
8. Check Terraform outputs `public_app_url` and `cors_allowed_origins` to confirm the deployed runtime contract.
