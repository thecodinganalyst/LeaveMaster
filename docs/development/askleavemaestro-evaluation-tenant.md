# Isolated AskLeaveMaestro evaluation tenant (issue #637)

The backend supports an explicitly opt-in `EVALUATION` tenant, separate from the public `DEMO` tenant.

## Configuration

- `evaluation.tenant.enabled=false` (default): prevents provisioning.
- `evaluation.tenant.id=EVALUATION` (default): dedicated tenant ID; **must not equal** `demo.tenant.id`.
- `evaluation.tenant.password` (required): provide via deployment secret, not source control.

`DemoTenantSeedService.resetEvaluationTenant()` is a transactional service method intended for a future privileged orchestration path (#638). **No public HTTP endpoint or automatic startup/scheduled reset is exposed for evaluation.** The workflow must not call the public DEMO reset endpoint. The existing public `/auth/demo-login` continues to accept only DEMO tenants and demo personas, not `evaluation.*` users.

Provisioning uses fictional SG staff, roles, annual leave entitlements, approver relationships and leave applications. Evaluation login names use the `evaluation.` prefix. The service refuses to overwrite a tenant of any other type, and rejects the configured public demo tenant ID. Outbound side effects are suppressed for non-standard tenants.

## Reset and safety

The reset deletes and recreates only the configured EVALUATION tenant in a transaction. Its staff, roles and application IDs are deterministic; relative leave dates and generated timestamps are based on the day of reset. The orchestrator in #638 must authenticate with least privilege, verify the target tenant, and coordinate resets so concurrent evaluations cannot collide. Keep the public DEMO reset schedule separate.

## Follow-ups

- #638: privileged evaluation reset orchestration, authentication, workflow configuration, and safe fail-closed behavior.
- #639: stronger fixture-derived business assertions and repeatability checks.

Never enable evaluation provisioning in production without a separate secret and authorized reset orchestration.

## GitHub Action integration (#638)

The live workflow requires repository variable `ASSISTANT_LIVE_EVAL_BASE_URL` (HTTPS) and `ASSISTANT_LIVE_EVAL_TENANT_ID` (`EVALUATION`), plus secrets `ASSISTANT_LIVE_EVAL_PASSWORD` and `ASSISTANT_LIVE_EVAL_RESET_TOKEN`. No production API URL or DEMO credentials are used as fallback.

The backend must be deployed with `evaluation.tenant.enabled=true`, `evaluation.tenant.id=EVALUATION`, `evaluation.tenant.password` equal to the workflow password, and `evaluation.tenant.reset-token` equal to the workflow reset token. Use separate strong random secrets and do not publish them. The reset endpoint is `/api/internal/evaluation/reset`, accepts only the reset token header, and returns 403 otherwise. Restrict ingress and monitor/reset-token rotation where infrastructure permits.

Before scenarios, the evaluator resets the tenant, verifies the response type and ID, then signs in as `evaluation.staff` / `evaluation.manager` through ordinary tenant-aware `/auth/login` and verifies `/auth/me` tenant identity. PR runs only perform static validation and do not access secrets or live services. Nightly/manual runs fail closed if configuration is absent or invalid.

**Deployment dependency:** this workflow will not run successfully until the backend changes are deployed and the corresponding GitHub variables/secrets are configured. Do not point it at the public DEMO tenant.
