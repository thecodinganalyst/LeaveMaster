# LeaveMaster

LeaveMaster is a Spring Boot REST API for managing employee leave. It handles leave applications, approvals, entitlements, public-holiday calendars, and email notifications — all exposed through a fully documented OpenAPI (Swagger) interface.

## Repository Layout

```text
LeaveMaster/
├── backend/         # Spring Boot API
├── frontend/        # React/Refine frontend
├── infra/terraform/ # Terraform infrastructure
├── docs/
└── .github/workflows/
```

## Features

- **Staff management** — create, update, and terminate staff records, each with a customisable weekly work schedule, leave entitlements, and an optional location assignment.
- **Leave types** — define the types of leave available in your organisation (e.g. Annual Leave, Medical Leave).
- **Leave entitlements** — assign leave quotas per staff member per leave type for a given date range.
- **Leave applications** — staff can apply for leave; applications can be approved, rejected, or cancelled by authorised approvers. Public holidays that match the staff member's location (or global holidays) are automatically excluded.
- **Leave balance** — query the remaining leave balance for a staff member across all leave types.
- **Leave approvers** — assign one or more approvers to a staff member with configurable effective dates.
- **Leave calendar** — define yearly leave calendars that contain public holidays. Each public holiday can be scoped to a specific location or left global (applying to all locations).
- **Locations** — maintain a list of locations (country, or country + state) used to scope public holidays and assign staff to the correct holiday set.
- **Tenants** — manage tenants that own all other resources; each tenant has a lifecycle status (`ACTIVE`, `DORMANT`, `TERMINATED`) and date range, and every record in the system is scoped to a tenant.
- **Email notifications** — send email alerts on leave-status changes.
- **Role-based access control (RBAC)** — enforce permission checks on every API endpoint. Roles are configurable, can be enabled/disabled, and users can be added to or removed from roles.
- **AI assistant backend** — authenticated server-side OpenAI integration that reuses LeaveMaster MCP tools; reads can execute under the user's RBAC permissions while writes are returned as pending actions for confirmation.
- **Swagger UI** — interactive API documentation available out of the box.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
| AI integration | Spring AI 2.0 / OpenAI |
| Persistence | Spring Data JPA / Hibernate |
| Database (dev) | H2 (in-memory) |
| Database (prod) | PostgreSQL 17 |
| Migrations | Flyway |
| API Docs | Springdoc OpenAPI 3 |
| Build | Gradle |
| Containerisation | Docker / Docker Compose |

## Getting Started

### Prerequisites

- Java 25+
- Gradle (or use the included `./backend/gradlew` wrapper)

### Run locally (H2 in-memory database)

```bash
./backend/gradlew bootRun
```

The application starts on **port 8080** and uses an H2 in-memory database that is pre-migrated by Flyway on startup.

### Enable the AI assistant locally

The AI assistant is **disabled by default**. To enable it, create an API key in the OpenAI Platform API key dashboard and provide it only to the backend process.

macOS / Linux:

```bash
export OPENAI_API_KEY="your-openai-api-key"
export ASSISTANT_ENABLED=true
export SPRING_AI_MODEL_CHAT=openai
export OPENAI_MODEL=gpt-5-mini   # optional
./backend/gradlew bootRun
```

PowerShell:

```powershell
$env:OPENAI_API_KEY="your-openai-api-key"
$env:ASSISTANT_ENABLED="true"
$env:SPRING_AI_MODEL_CHAT="openai"
$env:OPENAI_MODEL="gpt-5-mini"  # optional
./backend/gradlew bootRun
```

The authenticated assistant endpoint is:

```text
POST /api/assistant/chat
```

Example request body:

```json
{
  "message": "How much annual leave do I have?",
  "conversationId": null
}
```

**Do not commit the API key** to this repository, put it in `application.yaml`, expose it through a frontend `VITE_*` variable, or send it to the browser. The key belongs only in server-side environment/secret configuration. See [Cloud Run deployment](docs/cloudrun-deployment.md#27-configure-the-openai-api-key-for-the-ai-assistant) for production setup using Google Secret Manager.

### Run with Docker Compose (PostgreSQL)

```bash
./backend/gradlew bootJar
POSTGRES_PASSWORD=secret docker compose up --build
```

This builds the backend JAR from `backend/`, then starts a PostgreSQL 17 container and the application container together. The application is available on **port 8080**.

Optional environment variables (all have defaults except `POSTGRES_PASSWORD`):

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | *(required)* | PostgreSQL password |
| `POSTGRES_DB` | `leavemaster` | Database name |
| `POSTGRES_USER` | `leavemaster` | Database user |
| `PLATFORM_ADMIN_PASSWORD` | `changeme` | Password for the default `PlatformAdmin` user (see [Platform Admin](#platform-admin) below) |
| `ASSISTANT_ENABLED` | `false` | Enables `/api/assistant/chat` |
| `SPRING_AI_MODEL_CHAT` | `none` | Set to `openai` when the assistant is enabled |
| `OPENAI_API_KEY` | *(none)* | OpenAI API key; required only when the OpenAI assistant is enabled |
| `OPENAI_MODEL` | `gpt-5-mini` | OpenAI model used by the assistant |

### Platform Admin

On every startup, LeaveMaster automatically ensures a `PLATFORM_ADMIN` role and a default `PlatformAdmin` user exist:

- The `PLATFORM_ADMIN` role is created with `TENANT_READ` and `TENANT_WRITE` permissions if it does not already exist.
- If no user is currently assigned to `PLATFORM_ADMIN`, a `PlatformAdmin` user is created with the password from the `PLATFORM_ADMIN_PASSWORD` environment variable.

> **Important:** Set `PLATFORM_ADMIN_PASSWORD` to a strong, secret value before deploying. The default `changeme` is only suitable for local development.

The initializer is idempotent — restarting the application has no side effects once the role and user are in place.

### OIDC / Social Login Providers

LeaveMaster supports OAuth2/OIDC login flows for Google, Microsoft Entra ID, GitHub, and Facebook.

OIDC login is restricted to existing active application users. Admins must map each user to an IdP identity using:

- `oidcProvider` (for example: `google`, `microsoft`, `github`, `facebook`)
- `oidcSubject` (provider user identifier, usually `sub` or `id`)

Set these environment variables with credentials from each provider app registration:

| Variable | Description |
|----------|-------------|
| `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` | Google OAuth app credentials |
| `MICROSOFT_CLIENT_ID` / `MICROSOFT_CLIENT_SECRET` | Microsoft Entra ID app credentials |
| `MICROSOFT_TENANT_ID` | Entra tenant ID (or `common`) |
| `GITHUB_CLIENT_ID` / `GITHUB_CLIENT_SECRET` | GitHub OAuth app credentials |
| `FACEBOOK_CLIENT_ID` / `FACEBOOK_CLIENT_SECRET` | Facebook app credentials |

Provider login endpoints:

- `/oauth2/authorization/google`
- `/oauth2/authorization/microsoft`
- `/oauth2/authorization/github`
- `/oauth2/authorization/facebook`

Provider setup guides:

- [Google OIDC setup](docs/idp/google.md)
- [Microsoft Entra ID OIDC setup](docs/idp/microsoft.md)
- [GitHub OAuth app setup](docs/idp/github.md)
- [Facebook Login setup](docs/idp/facebook.md)

### Deploy to Google Cloud Run

See the [Cloud Run deployment guide](docs/cloudrun-deployment.md) for step-by-step instructions covering Supabase, Google Cloud, GitHub Actions, and OpenAI assistant secret setup.

### Build

```bash
./backend/gradlew build
```

### Run tests

```bash
./backend/gradlew test
```

## API Documentation (Swagger)

Once the application is running, open the interactive Swagger UI in your browser:

```
http://localhost:8080/swagger-ui.html
```

The raw OpenAPI specification is available at:

```
http://localhost:8080/api-docs
```

## H2 Console (development only)

When running locally, the H2 web console is enabled at:

```
http://localhost:8080/h2-console
```

Use JDBC URL `jdbc:h2:mem:leavemaster`, username `sa`, and an empty password.

## API Overview

For detailed endpoint documentation, see [docs/api.md](docs/api.md).

| Resource | Base Path | Description |
|----------|-----------|-------------|
| Tenants | `/tenants` | Manage tenants and their lifecycle status |
| Staff | `/staff` | Manage staff records and work schedules |
| Leave Types | `/leave-types` | Define available leave types |
| Leave Applications | `/leave-applications` | Apply for, approve, reject, and cancel leave |
| Leave Approvers | `/leave-approvers` | Assign approvers to staff members |
| Leave Calendars | `/leave-calendars` | Manage yearly calendars with public holidays |
| Locations | `/locations` | Manage locations for public holiday scoping |
| Roles | `/roles` | Manage roles, permissions, and user-to-role assignments |
| AI Assistant | `/api/assistant/chat` | Authenticated conversational interface over authorized LeaveMaster MCP tools |

### Leave Application Status Flow

```
DRAFT → PENDING → APPROVED → CANCEL_REQUESTED → CANCELLED
                ↘ DENIED
```

| Status | Meaning |
|--------|---------|
| `DRAFT` | Application saved but not yet submitted |
| `PENDING` | Submitted and awaiting approval |
| `APPROVED` | Approved by an authorised approver |
| `DENIED` | Rejected by an approver |
| `CANCEL_REQUESTED` | Cancellation requested on an approved leave |
| `CANCELLED` | Cancellation approved |

## RBAC Permissions

Every protected API call requires a permission that is granted through one or more active roles assigned to the authenticated user.

Default permission codes:

- `TENANT_READ`, `TENANT_WRITE`
- `USER_READ`, `USER_WRITE`
- `ROLE_MANAGE`
- `STAFF_READ`, `STAFF_WRITE`
- `LEAVE_TYPE_READ`, `LEAVE_TYPE_WRITE`
- `LEAVE_APPROVER_READ`, `LEAVE_APPROVER_WRITE`
- `LEAVE_CALENDAR_READ`, `LEAVE_CALENDAR_WRITE`
- `LOCATION_READ`, `LOCATION_WRITE`
- `LEAVE_APPLICATION_READ`, `LEAVE_APPLICATION_WRITE`, `LEAVE_APPLICATION_APPROVE`
