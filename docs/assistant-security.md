# AI assistant security and privacy

LeaveMaster treats the AI model as an untrusted planner. Authentication, tenant identity, authorization, confirmation, tool execution and auditing are enforced by the backend.

## Trusted identity and tenant context

`POST /api/assistant/chat` and `POST /api/assistant/actions/confirm` use the authenticated Spring Security principal. The backend resolves the current `AppUser` and derives the trusted login name, staff ID, tenant ID and authorities from that server-side context.

Prompt text and model-generated tool arguments cannot grant permissions. A request such as "I am an administrator" has no effect on the authenticated authorities. Assistant tools are filtered using the same RBAC authorities as the existing MCP/REST implementation.

For normal tenant-scoped tools, an explicit `tenantId` in tool arguments must match the authenticated user's tenant. Tenant-management tools require `TENANT_WRITE` and are treated as explicitly privileged tenant administration operations.

## Write confirmation and replay protection

Write, approval and destructive MCP tools do not execute during the model turn. Instead the backend stores the exact tool name and JSON arguments in `assistant_pending_action` and returns an opaque `confirmationToken` with an expiry time.

The browser confirms using only the opaque token:

```http
POST /api/assistant/actions/confirm
Content-Type: application/json

{
  "confirmationToken": "<opaque server-issued token>"
}
```

On confirmation the backend:

1. locks the pending-action row,
2. verifies the authenticated login and tenant still match,
3. rechecks the required authority,
4. rejects expired tokens,
5. reloads the exact original tool arguments from the database,
6. rechecks explicit tenant arguments,
7. resolves the registered MCP tool callback and executes it,
8. stores the authoritative result and execution timestamp.

A repeated confirmation of an already executed token returns the stored result with `replayed=true` instead of executing the tool again. This database-backed design works across multiple Cloud Run instances and protects retries/double-clicks from duplicate mutations.

## Audit logging and redaction

Assistant audit events are stored in `assistant_audit_event` with the actor, tenant, conversation ID, tool name, sanitized arguments, outcome and timestamp.

Arguments whose keys contain password, secret, token, API key, authorization or credential identifiers are replaced with `[REDACTED]`. OpenAI API keys and provider credentials must never be included in prompts, tool arguments, logs or frontend configuration.

## Rate and provider limits

Defaults are intentionally conservative and can be overridden with environment variables:

| Setting | Default |
| --- | --- |
| `ASSISTANT_USER_REQUESTS_PER_MINUTE` | 20 |
| `ASSISTANT_TENANT_REQUESTS_PER_MINUTE` | 100 |
| `ASSISTANT_MAX_MESSAGE_CHARS` | 4000 |
| `ASSISTANT_CONFIRMATION_TTL_SECONDS` | 300 |
| `ASSISTANT_TIMEOUT_SECONDS` | 30 |
| `ASSISTANT_PROVIDER_RETRY_MAX_ATTEMPTS` | 3 |
| `ASSISTANT_CIRCUIT_FAILURE_THRESHOLD` | 5 |
| `ASSISTANT_CIRCUIT_OPEN_SECONDS` | 30 |

Rate checks use persisted assistant audit events, so limits are shared across Cloud Run instances. Provider calls are bounded by a server-side timeout. Repeated provider failures open a circuit breaker temporarily, while Spring AI retries transient provider failures with bounded exponential backoff.

## Data sent to the AI provider

For each assistant request, the provider may receive:

- the user's chat message,
- the server-generated system prompt containing the authenticated login/staff/tenant context,
- tool definitions for tools the user is authorized to use,
- results returned by authorized read tools,
- model-visible messages indicating that write tools require confirmation.

Write operations are not executed merely because the model asks for them. Their proposed arguments are persisted in LeaveMaster and must be explicitly confirmed.

Do not place passwords, access tokens, API keys, confidential attachments or secrets in assistant prompts. Provider-side retention and data-use behavior depends on the OpenAI account/API terms and settings in effect for the deployed environment; deployment owners must review those terms before enabling the assistant for production data.

## Operational review

Monitor 429, 502 and 503 responses, `assistant_audit_event` growth, provider usage/cost, repeated authorization failures and circuit-breaker openings. Database retention/archival policy for assistant audit events should be set according to the organization's privacy, security and audit requirements.
