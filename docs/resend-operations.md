# Resend production operations

This guide describes the production configuration path for LeaveMaestro transactional email and how to diagnose activation PIN delivery failures without exposing secrets.

## Configuration flow

Production email configuration crosses four layers:

```text
GitHub Actions variables
        |
        v
Terraform variables
        |
        v
Cloud Run environment variables / Secret Manager reference
        |
        v
Spring app.email.* properties
        |
        v
TransactionalEmailSender
```

The deployment workflow reads these GitHub Actions variables:

| GitHub variable | Terraform input | Default |
| --- | --- | --- |
| `EMAIL_PROVIDER` | `email_provider` | `disabled` |
| `RESEND_API_KEY_SECRET_ID` | `resend_api_key_secret_id` | `leavemaster-resend-api-key` |
| `EMAIL_FROM_ADDRESS` | `email_from_address` | `onboarding@resend.dev` |
| `EMAIL_FROM_NAME` | `email_from_name` | `LeaveMaster` |

The Resend API key itself is **not** a GitHub variable. It remains in Google Secret Manager.

When `EMAIL_PROVIDER=resend`, Terraform gives the Cloud Run service account access to the configured secret and injects its latest enabled version as the secret-backed `RESEND_API_KEY` environment variable.

Cloud Run also receives:

```text
EMAIL_PROVIDER
EMAIL_FROM_ADDRESS
EMAIL_FROM_NAME
```

`backend/src/main/resources/application.yaml` explicitly maps those runtime variables into Spring configuration:

```yaml
app:
  email:
    provider: ${EMAIL_PROVIDER:disabled}
    from-address: ${EMAIL_FROM_ADDRESS:onboarding@resend.dev}
    from-name: ${EMAIL_FROM_NAME:LeaveMaster}
    resend:
      api-key: ${RESEND_API_KEY:}
      base-url: ${RESEND_BASE_URL:https://api.resend.com}
```

The Java email adapters use the `app.email.*` properties. This explicit mapping is the configuration contract; do not rely on an implicit environment-variable naming convention.

## Required production setup

To enable Resend in production:

1. Add the GitHub Actions variable `EMAIL_PROVIDER=resend` to the production environment/repository variables used by the Cloud Run workflow.
2. Create the Google Secret Manager secret containing the Resend API key. The default secret ID is `leavemaster-resend-api-key`.
3. If a different secret ID is used, set `RESEND_API_KEY_SECRET_ID` to that **secret name**, not to the key value.
4. Optionally configure `EMAIL_FROM_ADDRESS` and `EMAIL_FROM_NAME`.
5. Run the **Deploy to Cloud Run** workflow so Terraform creates a new revision with the updated configuration.

Changing a GitHub variable does not alter an already-running Cloud Run revision until the deployment workflow is run again.

## Startup validation

At application startup the logs identify the selected provider without printing credentials:

```text
Transactional email provider: resend
```

or:

```text
Transactional email provider: disabled
```

An unsupported provider value fails startup rather than silently selecting an unintended adapter.

When `resend` is selected, `ResendTransactionalEmailSender` also requires a nonblank API key and sender address. Missing required configuration causes startup to fail clearly instead of allowing the service to run with a provider that cannot send.

## Activation request semantics

`POST /account-activation/request` intentionally returns `202 Accepted` using privacy-safe anti-enumeration behavior. A `202` therefore means the activation request was accepted for processing; it does **not** prove that Resend accepted the email.

Use backend/provider logs to confirm delivery.

For a successful activation email, expect this sequence:

```text
Transactional email provider: resend
Sending account activation email through Resend
Resend accepted account activation email
Account activation PIN delivery requested successfully
```

The PIN itself must never appear in application logs.

## Diagnosing "Resend shows no email"

If the API returns `202` but the Resend dashboard shows no email/request:

1. Check the Cloud Run startup log for `Transactional email provider: resend`.
2. If it says `disabled`, verify the GitHub Actions variable `EMAIL_PROVIDER=resend` and redeploy Cloud Run.
3. Confirm the deployment workflow passed `TF_VAR_email_provider=resend` to Terraform.
4. Confirm the deployed revision has `EMAIL_PROVIDER=resend`.
5. Confirm the revision has a secret-backed `RESEND_API_KEY` reference.
6. Confirm the Secret Manager secret has an enabled version and the Cloud Run service account has `secretAccessor`.
7. Look for `Sending account activation email through Resend`.

If that `Sending ... through Resend` message is absent, the failure occurred before an HTTP request was made to Resend. The Resend dashboard will therefore have nothing to show.

If the message is present, inspect the next safe log entry:

- `Resend accepted ...` — Resend accepted the request.
- `Resend rejected ... with HTTP status <code>` — provider rejected the request.
- `Resend ... delivery failed due to a transport error` — connection/transport failure.

The account activation service invalidates the generated activation record when email delivery fails. Its failure log now includes a safe failure category/reason or unexpected exception type, while excluding PINs, API keys and authorization headers.

## Common configuration mistakes

### Secret exists but provider remains disabled

Creating `leavemaster-resend-api-key` in Secret Manager is not sufficient by itself. `EMAIL_PROVIDER` defaults to `disabled` in the deployment workflow. Set:

```text
EMAIL_PROVIDER=resend
```

and redeploy.

### GitHub variable was changed but behavior did not change

GitHub Actions variables are consumed when the deployment workflow runs. Rerun **Deploy to Cloud Run** after changing email variables.

### Wrong secret ID

If the Google secret is not named `leavemaster-resend-api-key`, set:

```text
RESEND_API_KEY_SECRET_ID=<actual-secret-name>
```

Do not put the Resend API key value in that GitHub variable.

### Sender rejected by Resend

`onboarding@resend.dev` is useful for supported testing. For normal production delivery, configure a sender on a domain verified in Resend and set `EMAIL_FROM_ADDRESS` accordingly.

## Security rules

Never log or commit:

- `RESEND_API_KEY`;
- activation PINs;
- Authorization headers;
- Secret Manager payloads.

Safe diagnostics may include provider name, HTTP status, exception class/category and non-secret configuration names.

## Related documentation

- [Account activation and transactional email](account-activation-and-email.md)
- [Cloud Run deployment](cloudrun-deployment.md)
- [Troubleshooting](troubleshooting.md)
