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

The live workflow requires repository variable `ASSISTANT_LIVE_EVAL_BASE_URL` (HTTPS) and `ASSISTANT_LIVE_EVAL_TENANT_ID` (`EVALUATION`), plus optional repository variables `ASSISTANT_LIVE_EVAL_PASSWORD_SECRET_ID` and `ASSISTANT_LIVE_EVAL_RESET_TOKEN_SECRET_ID` (defaults `leavemaestro-evaluation-password` and `leavemaestro-evaluation-reset-token`). Do not store the credential values as GitHub secrets. No production API URL or DEMO credentials are used as fallback.

Set GitHub variable `ENABLE_EVALUATION_TENANT=true` before running the Cloud Run deployment workflow. Terraform injects the backend environment variables and Secret Manager references automatically. Terraform creates both Secret Manager secret resources and grants secret-level accessor IAM to Cloud Run and the GitHub Actions service account. Add nonempty latest secret versions manually after the secret resources are provisioned, before deploying Cloud Run with evaluation enabled. The GitHub workload identity service account must have `roles/secretmanager.secretAccessor` on those two secrets for the evaluation workflow; the Cloud Run service account is granted access by Terraform. Use separate strong random secrets and do not publish them. The reset endpoint is `/api/internal/evaluation/reset`, accepts only the reset token header, and returns 403 otherwise. Restrict ingress and monitor/reset-token rotation where infrastructure permits.

Before scenarios, the evaluator resets the tenant, verifies the response type and ID, then signs in as `evaluation.staff` / `evaluation.manager` through ordinary tenant-aware `/auth/login` and verifies `/auth/me` tenant identity. PR runs only perform static validation and do not access secrets or live services. Nightly/manual runs fail closed if configuration is absent or invalid.

**Deployment dependency:** this workflow will not run successfully until the backend changes are deployed and the corresponding GitHub variables/secrets are configured. Do not point it at the public DEMO tenant.

### Setup checklist

1. Terraform creates `leavemaestro-evaluation-password` and `leavemaestro-evaluation-reset-token` and grants scoped accessor roles to both service accounts. The Terraform deployment identity must have permission to create secrets and manage their IAM policies.
2. Add separate strong values as latest secret versions using Google Cloud Secret Manager after the secret resources exist. Never place values in Terraform or GitHub variables.
3. Set GitHub Actions repository variables `ENABLE_EVALUATION_TENANT=true`, `ASSISTANT_LIVE_EVAL_TENANT_ID=EVALUATION`, and `ASSISTANT_LIVE_EVAL_BASE_URL` to the current direct HTTPS Cloud Run API URL. Optional `ASSISTANT_LIVE_EVAL_*_SECRET_ID` variables override the default secret IDs.
4. Deploy Terraform and backend, then manually dispatch the live evaluation workflow. The script fails closed on missing credentials, wrong tenant, or reset failure.

The scheduled job remains disabled by its configuration preconditions until the credentials and backend are provisioned. Terraform grants the GitHub WIF identity read-only Secret Manager access to these two secrets. Note the first deployment must provision the secret resources and their versions before Cloud Run can reference `latest`; if the deploy workflow performs both in one uninterrupted run, Cloud Run deployment will fail until versions have been added.
