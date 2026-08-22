# AI assistant security and privacy

LeaveMaster treats the AI model as an untrusted planner. Authentication, tenant identity, authorization, confirmation, tool execution and auditing are enforced by the backend.

## Trusted identity and tenant context

`POST /api/assistant/chat` and `POST /api/assistant/actions/confirm` use the authenticated Spring Security principal. The backend resolves the current `AppUser` and derives the trusted login name, staff ID, tenant ID and authorities from that server-side context.

Prompt text and model-generated tool arguments cannot grant permissions. A request such as "I am an administrator" has no effect on the authenticated authorities. Assistant tools are filtered using the same RBAC authorities as the existing MCP/REST implementation.

For normal tenant-scoped tools, an explicit `tenantId` in tool arguments must match the authenticated user's tenant. Tenant-management tools require `TENANT_WRITE` and are treated as explicitly privileged tenant administration operations.

Entitlement-policy questions use dedicated read tools guarded by `LEAVE_ENTITLEMENT_POLICY_READ`. `getLeaveEntitlementConfigurationByJurisdiction` is the preferred composite tool when both policies and eligibility are requested. It returns only policies already visible through `LeaveEntitlementPolicyService`; tenant policies are matched to a jurisdiction through their source template lineage, so the tool never widens the current user's tenant/platform visibility.

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

## Audit logging, diagnostics and redaction

Assistant audit events are stored in `assistant_audit_event` with the actor, tenant, conversation ID, tool name, sanitized arguments, outcome and timestamp.

Application logs provide request-level and tool-level timing without logging prompts, tool arguments, tool results or credentials. The main diagnostic events are:

- `Ask LeaveMaestro request started`
- `Ask LeaveMaestro provider workflow started`
- `Ask LeaveMaestro tool started`
- `Ask LeaveMaestro tool completed`
- `Ask LeaveMaestro provider workflow completed`
- `Ask LeaveMaestro request completed`
- `Ask LeaveMaestro provider request timed out`
- `Ask LeaveMaestro provider request failed`

Tool completion events contain the tool name, duration, status and per-request call number. Request completion and timeout events contain total elapsed time, total tool-call count, and the last started/completed tool. This allows Cloud Logging to distinguish a provider delay from a slow LeaveMaster tool or repeated tool orchestration.

Provider failures returned by `/api/assistant/chat` include the server-generated `conversationId`. The frontend shows this identifier with the user-friendly failure message so support can search Cloud Logging for the exact request. Conversation IDs are diagnostic correlation values, not credentials, and must not be used as authorization tokens.

Arguments whose keys contain password, secret, token, API key, authorization or credential identifiers are replaced with `[REDACTED]`. OpenAI API keys and provider credentials must never be included in prompts, tool arguments, logs or frontend configuration.

## Rate, timeout and provider limits

Defaults are intentionally conservative and can be overridden with environment variables:

| Setting | Default |
| --- | --- |
| `ASSISTANT_USER_REQUESTS_PER_MINUTE` | 20 |
| `ASSISTANT_TENANT_REQUESTS_PER_MINUTE` | 100 |
| `ASSISTANT_MAX_MESSAGE_CHARS` | 4000 |
| `ASSISTANT_CONFIRMATION_TTL_SECONDS` | 300 |
| `ASSISTANT_TIMEOUT_SECONDS` | 55 |
| `ASSISTANT_PROVIDER_RETRY_MAX_ATTEMPTS` | 3 |
| `ASSISTANT_CIRCUIT_FAILURE_THRESHOLD` | 5 |
| `ASSISTANT_CIRCUIT_OPEN_SECONDS` | 30 |

Rate checks use persisted assistant audit events, so limits are shared across Cloud Run instances. Provider calls are bounded by the assistant's overall server-side timeout. Repeated provider failures open a circuit breaker temporarily, while Spring AI retries transient provider failures with bounded exponential backoff.

The 55-second assistant timeout covers the complete Spring AI workflow, including provider/tool round trips. Production currently sends same-origin `/api/**` traffic through Firebase Hosting's Cloud Run rewrite, so LeaveMaster keeps its own timeout below the upstream forwarding ceiling. This gives the backend a chance to return a controlled JSON timeout instead of allowing the proxy/browser connection to fail first.

Spring AI owns provider-level retries below the `ChatModel` abstraction. The configured retry limit and backoff should therefore be considered part of the overall timeout budget. When troubleshooting latency, temporarily setting `ASSISTANT_PROVIDER_RETRY_MAX_ATTEMPTS=1` can isolate provider latency from retry/backoff time. Restore the intended retry policy after the test.

Individual LeaveMaster tool timeouts are not implemented by running tools on separate threads because tool execution relies on the authenticated Spring Security context and transaction boundaries. Instead, each tool is timed and correlated in application logs, while the overall request timeout remains the safety bound. If a specific external-I/O tool is added later, that integration should define its own network/read timeout at the client boundary rather than relying only on the assistant timeout.

## Firebase Hosting versus direct Cloud Run routing

The assistant remains on the existing same-origin Firebase Hosting rewrite for now. Calling Cloud Run directly would require a separate production API origin and coordinated changes to CORS, credentialed session cookies, CSRF, OAuth redirect/session behavior and deployment configuration. That increases authentication surface area merely to bypass the proxy limit.

The chosen first-line fix is therefore to reduce model round trips with purpose-built entitlement tools and keep the backend timeout below the Hosting ceiling. If production traces still show legitimate assistant workflows regularly approaching that limit after tool optimization, direct `/api/assistant/**` routing to Cloud Run should be revisited as a separate change with explicit cross-origin security tests.

The frontend translates browser-level `fetch()` failures (for example Safari's `Load failed`) into a neutral message that the connection to LeaveMaestro was interrupted or timed out. A network failure does not imply that Gemini or the backend necessarily failed; Cloud Logging remains authoritative for determining whether the backend completed.

## Troubleshooting assistant timeouts

Search Cloud Logging by the `conversationId` shown in the frontend failure details. A typical diagnosis uses the event sequence:

- no `tool started` event before timeout: investigate provider latency/retries or provider connectivity;
- `tool started` without a matching `tool completed`: investigate that LeaveMaster tool/dependency;
- one or more fast tool completions followed by timeout: investigate the provider follow-up/model turn;
- many repeated tool calls: investigate model/tool orchestration and tool descriptions.

For entitlement questions, the expected primary tool is `getLeaveEntitlementConfigurationByJurisdiction` when policies and eligibility are requested together. `getEntitlementPoliciesByJurisdiction` and `getEligibilityRulesByEntitlementPolicyId` are available for narrower follow-up questions. Tenant lookup tools should not be the primary path for entitlement configuration questions.

Useful production checks are:

1. `What leave entitlement policies are configured for Singapore?`
2. `What eligibility rules are configured for Singapore?`
3. `What are the leave entitlement policies configured for Singapore and their accompanying eligibility?`

Compare tool-call counts and durations for the three requests before changing timeout values further.

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

Monitor 429, 502 and 503 responses, assistant timeout/failure logs, `assistant_audit_event` growth, provider usage/cost, repeated authorization failures and circuit-breaker openings. Database retention/archival policy for assistant audit events should be set according to the organization's privacy, security and audit requirements.
