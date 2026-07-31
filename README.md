# LeaveMaster

LeaveMaster is a Spring Boot REST API for managing employee leave. It handles leave applications, approvals, entitlements, public-holiday calendars, and email notifications — all exposed through a fully documented OpenAPI (Swagger) interface.

## Features

- **Staff management** — create, update, and terminate staff records, each with a customisable weekly work schedule and leave entitlements.
- **Leave types** — define the types of leave available in your organisation (e.g. Annual Leave, Medical Leave).
- **Leave entitlements** — assign leave quotas per staff member per leave type for a given date range.
- **Leave applications** — staff can apply for leave; applications can be approved, rejected, or cancelled by authorised approvers.
- **Leave balance** — query the remaining leave balance for a staff member across all leave types.
- **Leave approvers** — assign one or more approvers to a staff member with configurable effective dates.
- **Leave calendar** — define yearly leave calendars that contain public holidays, which are automatically excluded when calculating leave duration.
- **Email notifications** — send email alerts on leave-status changes.
- **Swagger UI** — interactive API documentation available out of the box.

## Technology Stack

| Layer | Technology |
|-------|-----------|
| Language | Java 25 |
| Framework | Spring Boot 4.1.0 |
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
- Gradle (or use the included `./gradlew` wrapper)

### Run locally (H2 in-memory database)

```bash
./gradlew bootRun
```

The application starts on **port 8080** and uses an H2 in-memory database that is pre-migrated by Flyway on startup.

### Run with Docker Compose (PostgreSQL)

```bash
POSTGRES_PASSWORD=secret docker compose up
```

This starts a PostgreSQL 17 container and the application container together. The application is available on **port 8080**.

Optional environment variables (all have defaults except `POSTGRES_PASSWORD`):

| Variable | Default | Description |
|----------|---------|-------------|
| `POSTGRES_PASSWORD` | *(required)* | PostgreSQL password |
| `POSTGRES_DB` | `leavemaster` | Database name |
| `POSTGRES_USER` | `leavemaster` | Database user |

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

### Build

```bash
./gradlew build
```

### Run tests

```bash
./gradlew test
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

| Resource | Base Path | Description |
|----------|-----------|-------------|
| Staff | `/staff` | Manage staff records and work schedules |
| Leave Types | `/leave-types` | Define available leave types |
| Leave Applications | `/leave-applications` | Apply for, approve, reject, and cancel leave |
| Leave Approvers | `/leave-approvers` | Assign approvers to staff members |
| Leave Calendars | `/leave-calendars` | Manage yearly calendars with public holidays |

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
