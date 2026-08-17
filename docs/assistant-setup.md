# Set up Ask LeaveMaestro

Ask LeaveMaestro can use either **OpenAI** or **Gemini**. The frontend and MCP tool contract are provider-neutral; only the backend AI provider, model and provider credential change.

This guide covers the complete manual setup for local development and the production Cloud Run deployment.

## Before you start

Ask LeaveMaestro uses these provider-neutral settings:

| Setting | Meaning |
|---|---|
| `ASSISTANT_ENABLED` | Enables or disables the assistant at runtime. |
| `ASSISTANT_PROVIDER` | `openai` or `gemini`. |
| `ASSISTANT_MODEL` | Provider-specific model ID. |
| `OPENAI_API_KEY` | Backend-only OpenAI credential, required only when `openai` is selected. |
| `GEMINI_API_KEY` | Backend-only Gemini credential, required only when `gemini` is selected. |

For GitHub Actions / Terraform production deployment, the equivalent non-secret variables are:

```text
ENABLE_AI_ASSISTANT
AI_ASSISTANT_PROVIDER
AI_ASSISTANT_MODEL
OPENAI_API_KEY_SECRET_ID
GEMINI_API_KEY_SECRET_ID
```

The GitHub variables contain only configuration and **Secret Manager secret IDs**. Never put the actual provider API key value in GitHub Actions variables, Terraform variables/state, frontend `VITE_*` variables, source files, logs or screenshots.

When the assistant is disabled, Spring AI selects `none` and neither provider credential is required.

---

## Option 1: OpenAI / ChatGPT setup

### 1. Create an OpenAI API key

Sign in to the OpenAI developer platform and create an API key for the account/project that LeaveMaestro should use.

Official quickstart: <https://platform.openai.com/docs/quickstart/make-your-first-api-request>

Treat the key as a server-side secret. Do not expose it to the browser or commit it to the repository.

### 2. Create the Secret Manager secret

The default secret ID expected by LeaveMaestro is:

```text
leavemaster-openai-api-key
```

Using `gcloud`:

```bash
gcloud secrets create leavemaster-openai-api-key \
  --replication-policy=automatic
```

If the secret already exists, do not recreate it.

### 3. Add the OpenAI key as a secret version

Safest interactive approach:

```bash
printf '%s' "$OPENAI_API_KEY" | \
  gcloud secrets versions add leavemaster-openai-api-key --data-file=-
```

Verify that the secret has an enabled version:

```bash
gcloud secrets versions list leavemaster-openai-api-key
```

Do not print the secret value during verification.

### 4. Configure GitHub production variables

In the repository, open **Settings → Environments → production → Environment variables** (or the repository variables location used by the deployment workflow) and set:

```text
ENABLE_AI_ASSISTANT=true
AI_ASSISTANT_PROVIDER=openai
AI_ASSISTANT_MODEL=gpt-5-mini
OPENAI_API_KEY_SECRET_ID=leavemaster-openai-api-key
```

`GEMINI_API_KEY_SECRET_ID` can remain at its default. The non-selected provider secret is not mounted and its Secret Manager accessor grant is not created.

Choose another OpenAI model only if it is supported by the current Spring AI/OpenAI integration and available to the configured OpenAI account.

### 5. Deploy

Run or re-run `.github/workflows/deploy-cloud-run.yml`, or push a qualifying change to `main` after the variables are configured.

Terraform will:

1. read the selected OpenAI Secret Manager secret metadata;
2. grant the Cloud Run runtime service account `roles/secretmanager.secretAccessor` for that secret;
3. inject the selected secret into Cloud Run as `OPENAI_API_KEY`;
4. set `ASSISTANT_ENABLED`, `ASSISTANT_PROVIDER` and `ASSISTANT_MODEL` for the backend.

Only the selected provider secret is mounted.

### 6. Verify OpenAI startup

Check the Cloud Run revision logs. A successful configuration should log the selected provider and model, for example:

```text
Ask LeaveMaestro enabled with provider=openai model=gpt-5-mini
```

The API key must never appear in logs.

Then sign in to LeaveMaestro and test a read-only prompt, for example:

```text
What is my current leave balance?
```

Also verify an MCP-backed read request relevant to the signed-in user's permissions.

### 7. Run locally with OpenAI

From the repository root:

```bash
export ASSISTANT_ENABLED=true
export ASSISTANT_PROVIDER=openai
export ASSISTANT_MODEL=gpt-5-mini
export OPENAI_API_KEY='your-key-here'
./backend/gradlew bootRun
```

Avoid placing the key in committed `.env`, YAML or shell-script files.

---

## Option 2: Gemini setup

### 1. Create a Gemini API key

Open Google AI Studio and use the Google Cloud project that should own the Gemini API credential.

Official API-key guide: <https://ai.google.dev/gemini-api/docs/api-key>

If your Cloud project is not visible in AI Studio, import it from the AI Studio Projects page first. Google currently states that newly created AI Studio keys are created as **auth keys**; standard keys are being phased out. Prefer a newly created auth key for LeaveMaestro.

### 2. Choose the Gemini model

The model currently configured and tested for LeaveMaestro is:

```text
gemini-2.5-flash
```

Official model page: <https://ai.google.dev/gemini-api/docs/models/gemini-2.5-flash>

Use a different model only after confirming that the model is available through the Gemini Developer API and supports the function/tool-calling behavior needed by Ask LeaveMaestro.

### 3. Create the Secret Manager secret

The default Gemini secret ID is:

```text
leavemaster-gemini-api-key
```

Using `gcloud`:

```bash
gcloud secrets create leavemaster-gemini-api-key \
  --replication-policy=automatic
```

If the secret already exists, do not recreate it.

### 4. Add the Gemini API key as a secret version

```bash
printf '%s' "$GEMINI_API_KEY" | \
  gcloud secrets versions add leavemaster-gemini-api-key --data-file=-
```

Verify that an enabled version exists:

```bash
gcloud secrets versions list leavemaster-gemini-api-key
```

Do not print the key itself.

### 5. Configure GitHub production variables

Set:

```text
ENABLE_AI_ASSISTANT=true
AI_ASSISTANT_PROVIDER=gemini
AI_ASSISTANT_MODEL=gemini-2.5-flash
GEMINI_API_KEY_SECRET_ID=leavemaster-gemini-api-key
```

`OPENAI_API_KEY_SECRET_ID` can remain at its default. The OpenAI secret is not mounted and no OpenAI-secret accessor grant is created while Gemini is selected.

### 6. Deploy

Run or re-run `.github/workflows/deploy-cloud-run.yml`, or push a qualifying change to `main` after the variables are configured.

Terraform will:

1. read the selected Gemini Secret Manager secret metadata;
2. grant the Cloud Run runtime service account `roles/secretmanager.secretAccessor` for that secret;
3. inject it into Cloud Run as `GEMINI_API_KEY`;
4. set the provider-neutral assistant settings.

### 7. Verify Gemini startup

Check the Cloud Run logs for a line similar to:

```text
Ask LeaveMaestro enabled with provider=gemini model=gemini-2.5-flash
```

No credential value should be logged.

Then test Ask LeaveMaestro with:

1. a simple read-only prompt; and
2. an MCP-backed read request that the current user is authorized to perform.

Provider selection does not bypass LeaveMaestro authorization. Tenant isolation and permissions continue to come from the authenticated backend session.

### 8. Run locally with Gemini

```bash
export ASSISTANT_ENABLED=true
export ASSISTANT_PROVIDER=gemini
export ASSISTANT_MODEL=gemini-2.5-flash
export GEMINI_API_KEY='your-key-here'
./backend/gradlew bootRun
```

---

## Switch providers without code changes

### OpenAI → Gemini

1. Ensure `leavemaster-gemini-api-key` exists and has an enabled version.
2. Set:

   ```text
   AI_ASSISTANT_PROVIDER=gemini
   AI_ASSISTANT_MODEL=gemini-2.5-flash
   ```

3. Ensure `GEMINI_API_KEY_SECRET_ID` points to the correct secret ID.
4. Redeploy Cloud Run.
5. Verify the startup log identifies Gemini and the intended model.
6. Run a read-only assistant smoke test.

### Gemini → OpenAI

1. Ensure `leavemaster-openai-api-key` exists and has an enabled version.
2. Set:

   ```text
   AI_ASSISTANT_PROVIDER=openai
   AI_ASSISTANT_MODEL=gpt-5-mini
   ```

3. Ensure `OPENAI_API_KEY_SECRET_ID` points to the correct secret ID.
4. Redeploy Cloud Run.
5. Verify the startup log identifies OpenAI and the intended model.
6. Run a read-only assistant smoke test.

Changing GitHub variables alone does not update an already-running Cloud Run revision. A deployment is required.

---

## Disable Ask LeaveMaestro

For production:

```text
ENABLE_AI_ASSISTANT=false
```

Redeploy after changing the variable.

For local development:

```bash
export ASSISTANT_ENABLED=false
./backend/gradlew bootRun
```

When disabled, Spring AI chat model selection is `none`; neither `OPENAI_API_KEY` nor `GEMINI_API_KEY` is required.

---

## Rotate a provider key

The application reads Secret Manager version `latest` when the Cloud Run revision is deployed.

1. Create a replacement key with the provider.
2. Add it as a new version of the existing Secret Manager secret.
3. Redeploy/restart the Cloud Run service so a new revision resolves the new `latest` version.
4. Run the assistant smoke tests.
5. Only after verification, disable/destroy the old secret version and revoke the old provider key.

Example:

```bash
printf '%s' "$NEW_GEMINI_API_KEY" | \
  gcloud secrets versions add leavemaster-gemini-api-key --data-file=-
```

Use the equivalent OpenAI secret ID for OpenAI rotation.

## Roll back

If a new provider/model/key fails:

1. Restore the previously working `AI_ASSISTANT_PROVIDER` and `AI_ASSISTANT_MODEL` values.
2. If the failure was a rotated secret, ensure the previously working secret version is enabled or add a valid replacement version.
3. Redeploy.
4. Verify the provider/model startup log and perform a read-only assistant smoke test.

If necessary, temporarily set `ENABLE_AI_ASSISTANT=false` and redeploy. Leave management functionality remains available without the assistant.

---

## Troubleshooting

### `ASSISTANT_PROVIDER must be one of: openai, gemini`

`AI_ASSISTANT_PROVIDER` / `ASSISTANT_PROVIDER` contains an unsupported value. Use exactly `openai` or `gemini`, then redeploy/restart.

### `ASSISTANT_ENABLED=true requires ASSISTANT_MODEL to be configured`

Set `AI_ASSISTANT_MODEL` in production or `ASSISTANT_MODEL` locally to a non-blank model ID.

### `ASSISTANT_PROVIDER=openai requires OPENAI_API_KEY`

Check that:

- the OpenAI Secret Manager secret exists;
- it has an enabled secret version;
- `OPENAI_API_KEY_SECRET_ID` is correct;
- the deployment selected `openai`;
- the Cloud Run runtime service account can access the selected secret.

### `ASSISTANT_PROVIDER=gemini requires GEMINI_API_KEY`

Perform the same checks for `GEMINI_API_KEY_SECRET_ID` and the Gemini secret.

### Secret exists but has no enabled version

List versions:

```bash
gcloud secrets versions list SECRET_ID
```

Add or enable a valid version, then redeploy.

### Cloud Run service account cannot access the selected secret

Confirm the deployment completed its Terraform apply. The selected provider secret should grant the Cloud Run runtime service account `roles/secretmanager.secretAccessor`.

Do not manually grant both provider secrets just to bypass a failed deployment; fix the Terraform/deployment configuration so least-privilege behavior remains intact.

### Provider rejects the key

The key may be invalid, expired, revoked, restricted incorrectly or associated with the wrong project/account. Create/repair the provider credential, add a new Secret Manager version and redeploy.

### Model unavailable or unsupported

Check the provider's current model documentation and account/project availability. Model names can change independently of LeaveMaestro. Update `AI_ASSISTANT_MODEL` and redeploy only after confirming compatibility.

### Billing, quota or rate-limit error

Check the selected provider's billing/quota console. LeaveMaestro's own rate limits and circuit breaker do not increase provider quota.

### Provider variables were changed but behavior did not change

Redeploy. GitHub variable changes are consumed by the deployment workflow; they do not mutate an existing Cloud Run revision automatically.

### Assistant returns an error after provider switching

Confirm all three settings are consistent:

```text
ENABLE_AI_ASSISTANT=true
AI_ASSISTANT_PROVIDER=<selected provider>
AI_ASSISTANT_MODEL=<model belonging to that provider>
```

Then confirm the selected provider's Secret Manager secret has an enabled version.

---

## Security invariants

Provider selection changes only the model provider. It does **not** change LeaveMaestro's trust boundary:

- provider credentials remain backend-only;
- frontend code never receives provider API keys;
- authenticated identity, tenant and permissions come from Spring Security;
- MCP tools continue to enforce backend authorization;
- write actions continue to require the existing confirmation flow and authorization re-check;
- audit logging, idempotency, rate limiting, timeouts, retries and circuit-breaker behavior remain provider-neutral;
- secrets must never be written to prompts, tool output, diagnostics, logs or audit records.

For the underlying provider-neutral configuration, see [Ask LeaveMaestro AI providers](assistant-providers.md). For the security model, see [AI assistant security and privacy](assistant-security.md).
