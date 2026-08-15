# LeaveMaster

LeaveMaster is a multi-tenant employee leave management application with a Spring Boot backend, a React/Refine frontend, an embedded AI assistant, and infrastructure-as-code for Google Cloud Run and Firebase Hosting.

The repository is a monorepo. Backend, frontend, infrastructure, deployment workflows, and operational documentation evolve together.

## What LeaveMaster provides

- Staff, tenants, locations, leave types, entitlements, calendars, approvers, applications and leave balances.
- Policy-driven leave entitlement generation with eligibility rules, deterministic policy resolution, proration, accrual, carry-forward and safe reconciliation.
- Configurable RBAC enforced on the backend for every protected operation.
- Session-based authentication plus optional Google, Microsoft, GitHub and Facebook OAuth/OIDC login for pre-provisioned users.
- A React 18 frontend built with Refine, Ant Design and Vite.
- An embedded **Ask LeaveMaster** assistant backed by Spring AI/OpenAI and the same authorized MCP tool contract used by the backend.
- Explicit confirmation, authorization re-checks, idempotency and audit logging for AI-proposed writes.
- PostgreSQL/Supabase production persistence, H2 local/test persistence and Flyway migrations.
- Google Cloud Run backend deployment, Firebase Hosting frontend deployment and Terraform-managed infrastructure.
- GitHub Actions quality gates and path-aware frontend/backend deployment workflows.

## Repository layout

```text
LeaveMaster/
├── backend/                    # Spring Boot API, security, MCP and AI assistant
├── frontend/                   # React + Refine + Ant Design SPA
├── infra/terraform/            # GCP/Firebase infrastructure as code
├── docs/                       # Architecture, deployment, security and operator guides
├── .github/workflows/          # CI/CD and Terraform validation
├── Dockerfile                  # Production backend container build
└── docker-compose.yml          # Local PostgreSQL/backend option
```

## Architecture

```mermaid
flowchart LR
    Browser[Browser]
    Firebase[Firebase Hosting\nReact / Vite SPA]
    CloudRun[Google Cloud Run\nSpring Boot API]
    Postgres[(Supabase PostgreSQL)]
    GCS[(GCS attachments)]
    OpenAI[OpenAI API]
    SecretManager[Google Secret Manager]

    Browser --> Firebase
    Firebase -->|/api /auth /login /oauth2 rewrites| CloudRun
    CloudRun --> Postgres
    CloudRun --> GCS
    SecretManager -->|runtime secrets| CloudRun
    CloudRun -. optional assistant .-> OpenAI
```

Production browser traffic is intentionally same-origin. Firebase Hosting serves the SPA and rewrites backend routes to Cloud Run. This preserves CSRF/session behavior and the Firebase-compatible `__session` cookie without exposing the Cloud Run URL in the frontend bundle.

See [Architecture](docs/architecture.md) for component, security, MCP/AI and deployment diagrams.

## Technology stack

| Area | Technology |
|---|---|
| Backend | Java 25, Spring Boot 4.1, Spring Security, Spring Data JPA |
| AI / MCP | Spring AI 2.0, OpenAI, Spring AI MCP server |
| Database | H2 locally/tests, PostgreSQL 17 in production |
| Migrations | Flyway |
| Frontend | React 18, TypeScript 5.9, Refine 4, Ant Design 5, Vite 7 |
| Frontend tests | Vitest, Testing Library, V8 coverage |
| Backend tests | JUnit, Spring Boot Test, Spring Security Test, JaCoCo |
| Infrastructure | Terraform, Google Cloud Run, Artifact Registry, Cloud Build, GCS, Secret Manager, Firebase Hosting |
| CI/CD | GitHub Actions + Google Workload Identity Federation |

## Quick start

### Prerequisites

- Java 25
- Node.js 22+
- npm
- Git

Gradle does not need to be installed globally; use the backend wrapper.

### 1. Start the backend

From the repository root:

```bash
./backend/gradlew bootRun
```

The local backend starts at `http://localhost:8080` using an in-memory H2 database initialized by Flyway.

Useful development endpoints:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- H2 console: `http://localhost:8080/h2-console`

Default local H2 connection:

```text
JDBC URL: jdbc:h2:mem:leavemaster
User: sa
Password: <empty>
```

### 2. Start the frontend

```bash
cd frontend
cp .env.example .env.local
npm ci
npm run dev
```

The frontend starts at `http://localhost:5173`. The example local environment points it at `http://localhost:8080`.

Only non-secret browser configuration belongs in `VITE_*` variables. Never put database passwords, OpenAI keys, OAuth client secrets or service-account credentials in Vite configuration.

### 3. Sign in locally

A default `PlatformAdmin` is bootstrapped when no user currently holds the `PLATFORM_ADMIN` role. Local development defaults to the password `changeme` unless `PLATFORM_ADMIN_PASSWORD` is supplied.

Production uses Secret Manager and a controlled reset flow; see [Platform Admin password management](docs/platform-admin-password.md).

## Development commands

### Backend

Run from the repository root:

```bash
./backend/gradlew test
./backend/gradlew build
./backend/gradlew bootJar
```

`build` includes the configured JaCoCo verification gate. Backend code lives entirely under `backend/`.

### Frontend

Run from `frontend/`:

```bash
npm ci
npm run dev
npm run lint
npm run typecheck
npm test
npm run coverage
npm run build
npm run preview
```

The production build is emitted to `frontend/dist`.

See [Development and CI](docs/development-and-ci.md) for the complete local/test workflow.

## Authentication and authorization

LeaveMaster uses Spring Security as the security authority. The frontend can hide or show navigation/actions based on the current user's permissions, but frontend access control is only a usability layer; every protected backend endpoint and MCP tool is still authorized server-side.

Authentication options:

- Username/password form login.
- Google OAuth/OIDC.
- Microsoft Entra ID OAuth/OIDC.
- GitHub OAuth.
- Facebook Login.

OAuth login is restricted to existing active LeaveMaster users whose provider identity has been mapped in the application. Provider-specific setup is documented under [`docs/idp/`](docs/idp/).

Production sessions use a `Secure`, `HttpOnly`, `SameSite=Lax` cookie named `__session` because Firebase Hosting forwards this special cookie name to Cloud Run rewrites.

See [Environments, domains, CORS and runtime secrets](docs/environments-and-domains.md).

## MCP and the embedded AI assistant

The backend exposes a Spring AI MCP server that wraps LeaveMaster business capabilities as tools. Tool authorization is not delegated to the model: authenticated identity, tenant and authorities come from Spring Security, and existing service/method authorization remains authoritative.

The embedded **Ask LeaveMaster** experience calls:

```text
POST /api/assistant/chat
POST /api/assistant/actions/confirm
```

The assistant reuses the MCP tool callbacks rather than maintaining a second set of business operations.

### Read operations

Authorized read tools may execute during the model turn. Selected business results are also returned as structured data so the frontend can render authoritative cards separately from model-generated prose.

### Write operations

AI-proposed writes never mutate business data during the model turn. Instead:

```mermaid
sequenceDiagram
    participant U as User
    participant UI as Frontend
    participant A as Assistant backend
    participant DB as PostgreSQL
    participant T as Authorized MCP tool

    U->>UI: Ask for a write operation
    UI->>A: POST /api/assistant/chat
    A->>DB: Store exact tool + arguments
    A-->>UI: Opaque confirmationToken
    U->>UI: Confirm
    UI->>A: POST /api/assistant/actions/confirm
    A->>DB: Lock action + recheck actor/tenant/RBAC/expiry
    A->>T: Execute stored tool arguments
    T-->>A: Authoritative result
    A->>DB: Store result + audit event
    A-->>UI: Result, including replay status
```

Assistant controls include confirmation expiry, persisted idempotency, actor/tenant/RBAC revalidation, sanitized audit events, rate limits, provider timeout/retries and a circuit breaker.

See [AI assistant security and privacy](docs/assistant-security.md).

### Direct ChatGPT-to-MCP integration

Direct external ChatGPT access to LeaveMaster MCP is **not required** for the embedded assistant. It remains a separate optional/future integration tracked by issue **#104**. The embedded assistant runs entirely through the LeaveMaster backend and does not depend on #104.

## Production deployment

Production uses:

- Firebase Hosting for the frontend SPA.
- Cloud Run for the Spring Boot backend.
- Artifact Registry for backend images.
- Cloud Build for container builds.
- Supabase PostgreSQL for application data.
- GCS for attachments and Cloud Build staging.
- Secret Manager for database, PlatformAdmin and optional OpenAI credentials.
- Terraform state in a GCS backend.
- GitHub Actions authentication to Google Cloud through Workload Identity Federation; no long-lived service-account JSON key is required.

See:

- [Cloud Run deployment](docs/cloudrun-deployment.md)
- [Environments, domains, CORS and runtime secrets](docs/environments-and-domains.md)
- [Platform Admin password management](docs/platform-admin-password.md)

## CI/CD

GitHub Actions are path-aware so frontend-only work does not rebuild the backend and backend-only work does not redeploy Firebase Hosting.

### Backend CI

Changes under `backend/**` run the Java/Gradle workflow. It builds/tests the application, enforces JaCoCo coverage and submits dependency information.

### Frontend CI and deployment

Changes under `frontend/**` run:

```text
npm ci
lint
strict TypeScript typecheck
Vitest tests
coverage gate
Vite production build
```

Pull requests validate and build but do not deploy production. A qualifying `main` push deploys the exact validated `frontend/dist` artifact to Firebase Hosting through WIF/Application Default Credentials.

### Cloud Run deployment

Backend/infrastructure/container changes on `main` trigger the Cloud Run workflow. It:

1. authenticates with WIF;
2. initializes the production Terraform GCS backend;
3. validates/formats Terraform;
4. provisions build/runtime prerequisites;
5. builds the backend JAR and Cloud Build container image tagged with the current Git SHA;
6. creates a full Terraform plan;
7. rejects destructive changes to protected backend resources;
8. applies the plan and updates Cloud Run.

### Terraform validation

Infrastructure changes also run a standalone Terraform validation workflow before deployment.

See [Development and CI](docs/development-and-ci.md) for workflow triggers, quality gates and deployment responsibilities.

## Configuration and secrets

Local defaults are development-friendly; production is fail-closed for important runtime configuration.

Common non-secret settings include:

- `VITE_API_URL` — local frontend backend URL; leave unset for same-origin production.
- `APP_PUBLIC_URL` — canonical production app origin.
- `APP_CORS_ALLOWED_ORIGINS` — exact allowed frontend origins.
- `ASSISTANT_ENABLED` / `SPRING_AI_MODEL_CHAT` / `OPENAI_MODEL` — assistant runtime selection.

Production secret values belong in Google Secret Manager, including:

- database password;
- PlatformAdmin bootstrap/recovery password;
- OpenAI API key when the assistant is enabled.

OAuth client secrets are also backend-only credentials and must never be compiled into frontend assets.

The Cloud Run profile validates production URLs/CORS and refuses to start an enabled assistant without its OpenAI key.

## API documentation

Once the backend is running:

```text
Swagger UI: http://localhost:8080/swagger-ui.html
OpenAPI:    http://localhost:8080/api-docs
```

For resource-level API notes, see [API documentation](docs/api.md). For the entitlement policy, eligibility, resolution and generation workflow, see [Policy-driven leave entitlement generation](docs/leave-entitlement-generation.md).

## Documentation map

| Guide | Use it for |
|---|---|
| [Architecture](docs/architecture.md) | Components, request flows, RBAC, MCP/AI and production diagrams |
| [Development and CI](docs/development-and-ci.md) | Local setup, scripts, test gates, GitHub Actions and deployments |
| [Policy-driven leave entitlement generation](docs/leave-entitlement-generation.md) | Policies, eligibility, resolution, proration, accrual, carry-forward, reconciliation and generation API examples |
| [Troubleshooting](docs/troubleshooting.md) | Build, authentication, Firebase, Cloud Run, Terraform and AI failures |
| [AI assistant security](docs/assistant-security.md) | Confirmation, audit, redaction, rate/provider limits and AI trust boundaries |
| [Environments and domains](docs/environments-and-domains.md) | CORS, cookies, OAuth callbacks, custom domains and secrets |
| [Cloud Run deployment](docs/cloudrun-deployment.md) | One-time GCP/Supabase/WIF/Terraform deployment setup |
| [Platform Admin password](docs/platform-admin-password.md) | Secure bootstrap, reset and rotation |
| [`docs/idp/`](docs/idp/) | OAuth/OIDC provider registration |

## Troubleshooting entry points

A few production-specific rules solve many common problems:

- A login that succeeds but `/auth/me` returns `401`: verify the Cloud Run session cookie is named `__session`.
- Firebase shows **Site Not Found**: provisioning the site is not the same as deploying `frontend/dist`; check the frontend deployment workflow.
- Cloud Run reports an image tag not found: verify Cloud Build pushed the current Git SHA and do not rely on stale state-backed image outputs after targeted Terraform applies.
- The assistant returns `503`: confirm it is enabled and the Secret Manager OpenAI key is attached to Cloud Run.
- CORS fails locally: use the documented `http://localhost:5173` frontend origin and `VITE_API_URL=http://localhost:8080`.

See [Troubleshooting](docs/troubleshooting.md) for detailed checks.
