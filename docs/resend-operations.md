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

| GitHub variable | Terraform input | Production fallback |
| --- | --- | --- |
| `EMAIL_PROVIDER` | `email_provider` | `disabled` |
| `RESEND_API_KEY_SECRET_ID` | `resend_api_key_secret_id` | `leavemaster-resend-api-key` |
| `EMAIL_FROM_ADDRESS` | `email_from_address` | `contact@leavemaestro.com` |
| `EMAIL_FROM_NAME` | `email_from_name` | `LeaveMaestro` |

The Resend API key itself is **not** a GitHub variable. It remains in Google Secret Manager.

When `EMAIL_PROVIDER=resend`, Terraform gives the Cloud Run service account access to the configured secret and injects its latest enabled version as the secret-backed `RESEND_API_KEY` environment variable.

Cloud Run also receives:

```text
EMAIL_PROVIDER
EMAIL_FROM_ADDRESS
EMAIL_FROM_NAME
```

`backend/src/main/resources/application.yaml` explicitly maps those runtime variables into Spring configuration. Its `onboarding@resend.dev` fallback is retained for local/development use; the production workflow supplies the LeaveMaestro sender configuration.

The Java email adapters use the `app.email.*` properties. This explicit mapping is the configuration contract; do not rely on an implicit environment-variable naming convention.

## Required production setup

To enable Resend in production:

1. Add and verify `leavemaestro.com` in Resend.
2. Add the DNS records supplied by Resend to Cloudflare DNS. Preserve the existing Cloudflare Email Routing records and do not create conflicting SPF records.
3. Configure the GitHub Actions production variable `EMAIL_PROVIDER=resend`.
4. Keep the Resend API key in Google Secret Manager. The default secret ID is `leavemaster-resend-api-key`.
5. If a different secret ID is used, set `RESEND_API_KEY_SECRET_ID` to that **secret name**, not to the key value.
6. Remove any stale production override such as `EMAIL_FROM_ADDRESS=onboarding@resend.dev`, or set it explicitly to `contact@leavemaestro.com`.
7. Set `EMAIL_FROM_NAME=LeaveMaestro` if a production override exists.
8. Run the **Deploy to Cloud Run** workflow so Terraform creates a new revision with the updated configuration.

Changing a GitHub variable does not alter an already-running Cloud Run revision until the deployment workflow is run again.

## Cloudflare inbound vs Resend outbound

`contact@leavemaestro.com` serves two independent purposes:

```text
Inbound replies
Internet -> contact@leavemaestro.com -> Cloudflare Email Routing -> configured destination inbox

Outbound transactional email
LeaveMaestro -> Resend -> From: LeaveMaestro <contact@leavemaestro.com> -> recipient
```

Cloudflare Email Routing does not send LeaveMaestro transactional mail. Resend does not replace the inbound forwarding rule. The same visible address can be used for both once `leavemaestro.com` is verified in Resend.

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

## Diagnosing provider rejection

If the API returns `202` but delivery fails, inspect the Resend adapter log. Rejections include the HTTP status plus sanitized provider metadata where Resend returns structured JSON:

```text
Resend rejected account activation email with HTTP status 403 type=validation_error message=...
```

The adapter logs only selected provider fields (`type`/`name`/`code` and `message`), normalizes line breaks, and truncates long messages. It never logs the request body, Authorization header, API key, or PIN.

If Resend returns a non-JSON error body, the raw response body is deliberately not copied into logs. The log records a generic `unparseable` provider error instead.

A common 403 during testing is caused by using `onboarding@resend.dev` to send to recipients not permitted by Resend's testing-domain restrictions. Production should use the verified `contact@leavemaestro.com` sender instead.

The account activation service invalidates the generated activation record when email delivery fails. The public endpoint remains generic and the failed PIN cannot subsequently be verified.

## Common configuration mistakes

### Secret exists but provider remains disabled

Creating `leavemaster-resend-api-key` in Secret Manager is not sufficient by itself. Set:

```text
EMAIL_PROVIDER=resend
```

and redeploy.

### Stale sender override

The production workflow now falls back to `contact@leavemaestro.com`, but an existing GitHub production variable overrides that fallback. If `EMAIL_FROM_ADDRESS` is still set to `onboarding@resend.dev`, update or remove that variable before deployment.

### GitHub variable was changed but behavior did not change

GitHub Actions variables are consumed when the deployment workflow runs. Rerun **Deploy to Cloud Run** after changing email variables.

### Wrong secret ID

If the Google secret is not named `leavemaster-resend-api-key`, set:

```text
RESEND_API_KEY_SECRET_ID=<actual-secret-name>
```

Do not put the Resend API key value in that GitHub variable.

### Sender rejected by Resend

Confirm `leavemaestro.com` is verified in Resend and that the deployed revision receives:

```text
EMAIL_FROM_ADDRESS=contact@leavemaestro.com
EMAIL_FROM_NAME=LeaveMaestro
```

## Security rules

Never log or commit:

- `RESEND_API_KEY`;
- activation PINs;
- Authorization headers;
- Secret Manager payloads;
- complete provider request bodies.

Safe diagnostics may include provider name, HTTP status, provider error type/code, and a bounded sanitized provider error message.

## Related documentation

- [Account activation and transactional email](account-activation-and-email.md)
- [Cloud Run deployment](cloudrun-deployment.md)
- [Troubleshooting](troubleshooting.md)
