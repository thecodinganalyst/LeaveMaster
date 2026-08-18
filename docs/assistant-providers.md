# Ask LeaveMaestro AI providers

Ask LeaveMaestro is provider-neutral at the API and frontend layers. The backend selects one Spring AI chat provider at startup and reuses the same MCP tools, confirmation workflow, authorization checks, rate limits, timeout handling, circuit breaker and audit behavior for either provider.

For complete credential creation, Secret Manager, GitHub variable, deployment, verification, switching, rotation and troubleshooting steps, see [Set up Ask LeaveMaestro](assistant-setup.md).

## Runtime configuration

| Variable | Required | Description |
|---|---:|---|
| `ASSISTANT_ENABLED` | No | `true` enables the assistant. Default: `false`. |
| `ASSISTANT_PROVIDER` | When enabled | `openai` or `gemini`. Default provider: `openai`. |
| `ASSISTANT_MODEL` | When enabled | Provider-specific model ID, for example `gpt-5-mini` or `gemini-3.6-flash`. |
| `OPENAI_API_KEY` | Only for OpenAI | OpenAI credential. |
| `GEMINI_API_KEY` | Only for Gemini | Gemini Developer API credential. |

When the assistant is disabled, Spring AI chat auto-configuration is set to `none` and neither provider is initialized. When enabled, LeaveMaestro maps `openai` to Spring AI's OpenAI chat model and `gemini` to Spring AI's Google GenAI chat model.

Startup validation fails clearly if the provider is unsupported, the model is blank, or the credential for the selected provider is missing. The non-selected provider credential is not required.

## Local examples

OpenAI:

```bash
export ASSISTANT_ENABLED=true
export ASSISTANT_PROVIDER=openai
export ASSISTANT_MODEL=gpt-5-mini
export OPENAI_API_KEY='...'
./backend/gradlew bootRun
```

Gemini:

```bash
export ASSISTANT_ENABLED=true
export ASSISTANT_PROVIDER=gemini
export ASSISTANT_MODEL=gemini-3.6-flash
export GEMINI_API_KEY='...'
./backend/gradlew bootRun
```

Do not set provider API keys in frontend `VITE_*` variables, committed configuration files, Terraform state values, or GitHub Actions logs.

## Production GitHub variables

The Cloud Run workflow uses provider-neutral repository/environment variables:

```text
ENABLE_AI_ASSISTANT=false
AI_ASSISTANT_PROVIDER=openai
AI_ASSISTANT_MODEL=gpt-5-mini
OPENAI_API_KEY_SECRET_ID=leavemaster-openai-api-key
GEMINI_API_KEY_SECRET_ID=leavemaster-gemini-api-key
```

For Gemini production deployments, use `AI_ASSISTANT_PROVIDER=gemini` together with `AI_ASSISTANT_MODEL=gemini-3.6-flash`.

To switch providers, change `AI_ASSISTANT_PROVIDER` and `AI_ASSISTANT_MODEL`, ensure the corresponding Secret Manager secret exists, and redeploy. No frontend change is required.

## Secret Manager

Create only secret metadata through your normal infrastructure process and add API key material directly to Google Secret Manager. Terraform reads the selected existing secret but does not store API key values in variables.

For OpenAI, the default secret ID is:

```text
leavemaster-openai-api-key
```

For Gemini, the default secret ID is:

```text
leavemaster-gemini-api-key
```

At deployment, the Cloud Run service account receives `roles/secretmanager.secretAccessor` only for the selected provider secret, and only that secret is injected into the container as `OPENAI_API_KEY` or `GEMINI_API_KEY`.

To rotate a key, add a new secret version in Secret Manager and redeploy/restart the Cloud Run revision so `latest` resolves to the new version. Remove or disable the old version after the new revision is verified.

## Security behavior

Provider choice does not alter LeaveMaestro authorization. The server-authenticated user, tenant and permissions remain authoritative, and write tools continue to require the existing confirmation flow. Provider failures are returned through the same sanitized assistant error handling; credentials must never be included in errors, logs, diagnostics, prompts, tool output or audit records.
