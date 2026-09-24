# AskLeaveMaestro Quality dashboard

Platform Administrators can open **AskLeaveMaestro Quality** from the main navigation or go directly to `/platform/askleavemaestro-quality`.

The dashboard uses the privacy-safe telemetry introduced by issue #588. It shows request/success volume, average and p95 latency, retries, failure categories, tool usage, provider/model counts, feedback and recent correlation IDs. It intentionally does **not** display prompts, assistant response bodies, secrets or employee payloads.

The backend endpoint is `GET /api/platform/assistant-quality` and requires `PLATFORM_ADMIN`. Supported query parameters include `days` (1–90), `outcome`, `failureCategory`, `provider` and `model`. Tenant users are denied by method security.

Use correlation IDs to match dashboard rows with privacy-safe application logs when investigating failures.
