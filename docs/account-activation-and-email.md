# Account activation and transactional email

This guide explains LeaveMaestro's first-time staff account activation flow and how transactional activation emails are delivered through Resend.

It covers the current development setup as well as the optional future step of using a verified custom sending domain.

## First-time staff account activation

New staff users no longer receive a generated default password.

When an administrator creates a staff member, the associated application user is created without a permanent password. The account remains eligible for first-time activation until the staff member completes the setup flow from the normal login page.

The user experience is:

1. Enter the LeaveMaestro login name and select **Continue**.
2. Existing users who already have a password continue to the normal password sign-in form.
3. A staff user without a password sees the account-setup flow.
4. The user explicitly selects **Send verification PIN**.
5. Only at that point does the backend generate a six-digit PIN and request transactional email delivery.
6. The user enters the PIN from the email.
7. After successful verification, the user chooses a permanent password.
8. The activation is consumed and subsequent logins use the normal password flow.

A PIN is therefore not generated when the staff record is created and is not left waiting for a staff member who may not yet be ready to activate the account.

## PIN lifecycle and security

The account activation service applies the following controls:

- PINs are generated with a cryptographically secure random generator.
- Only the hash of the PIN is persisted. Plaintext PINs are never written to the database.
- The current default PIN lifetime is 15 minutes.
- The current maximum failed verification attempts is 5.
- PIN requests are throttled with a default 60-second resend cooldown.
- The current hourly request limit is 5 requests per account.
- Requesting a new PIN replaces the previous active PIN.
- A successfully verified activation can be used only for the initial password setup.
- Completing password setup consumes the activation and clears the PIN hash.
- Disabled, terminated, future-start, cross-tenant or otherwise ineligible staff cannot complete activation.
- Activation logs must never include the plaintext PIN.

If the email provider fails after a PIN request is created, the PIN is invalidated while request-throttling state is retained. This prevents a provider outage from turning into an unlimited PIN-request bypass.

## Anti-enumeration behaviour

The unauthenticated activation flow intentionally avoids revealing unnecessary account details.

The PIN request endpoint returns a generic accepted response such as:

```text
If the account is eligible for activation, a verification PIN will be sent.
```

The frontend should not infer that a login name, tenant, email address or staff record exists merely from the request response.

The initial lookup distinguishes the normal password path from first-time activation for the login UX, but backend eligibility checks remain authoritative.

## Account activation API

The controller is available under both `/account-activation` and `/api/account-activation`. The `/api` form is recommended for frontend/API documentation examples.

### Lookup next step

```http
POST /api/account-activation/lookup
Content-Type: application/json

{
  "loginName": "alice"
}
```

Example response for a pending first-time account:

```json
{
  "nextStep": "ACTIVATION"
}
```

An existing account with a permanent password returns:

```json
{
  "nextStep": "PASSWORD"
}
```

### Request a verification PIN

```http
POST /api/account-activation/request
Content-Type: application/json

{
  "loginName": "alice"
}
```

The request returns `202 Accepted` with generic messaging. The API response never contains the PIN.

### Verify a PIN

```http
POST /api/account-activation/verify
Content-Type: application/json

{
  "loginName": "alice",
  "pin": "123456"
}
```

Successful verification returns `200 OK`. Invalid, expired, already-consumed or otherwise unusable PINs return a safe `400` response.

### Set the initial password

```http
POST /api/account-activation/set-password
Content-Type: application/json

{
  "loginName": "alice",
  "password": "a-new-password"
}
```

The endpoint succeeds only after a valid PIN has been verified and the verification is still usable. Successful setup returns `204 No Content` and the account then follows the normal password login path.

## Email architecture

Business/account-activation code does not call Resend directly.

```text
AccountActivationService
        |
        v
    EmailService
        |
        v
TransactionalEmailSender
        |
        +--> ResendTransactionalEmailSender
```

This keeps Resend as an infrastructure adapter and allows another transactional email provider to replace it without changing the account activation domain flow.

Automated tests use mocks/stubs or the disabled sender and must never send real email.

## Application configuration

The backend uses these runtime settings:

```text
EMAIL_PROVIDER
EMAIL_FROM_ADDRESS
EMAIL_FROM_NAME
RESEND_API_KEY
RESEND_BASE_URL
```

Current defaults are equivalent to:

```text
EMAIL_PROVIDER=disabled
EMAIL_FROM_ADDRESS=onboarding@resend.dev
EMAIL_FROM_NAME=LeaveMaster
RESEND_BASE_URL=https://api.resend.com
```

`RESEND_API_KEY` has no usable default and must never be committed to source control.

To enable Resend:

```text
EMAIL_PROVIDER=resend
EMAIL_FROM_ADDRESS=onboarding@resend.dev
EMAIL_FROM_NAME=LeaveMaster
RESEND_API_KEY=<injected secret>
```

`RESEND_BASE_URL` normally stays at its default and exists mainly to keep the adapter configurable/testable.

## Development and current rollout with `resend.dev`

A custom domain is not required to begin development/testing.

1. Create or sign in to a Resend account.
2. Create an API key.
3. Store the key securely; do not commit it, paste it into Terraform variables, or expose it to the Vite frontend.
4. Configure `EMAIL_PROVIDER=resend`.
5. Keep `EMAIL_FROM_ADDRESS=onboarding@resend.dev` for the current development/test sender.
6. Use Resend-supported test recipients/events for delivery, bounce and complaint smoke tests where appropriate.

`resend.dev` is a development/testing identity. Do not treat it as unrestricted production delivery for real staff.

You do **not** need to buy a GoDaddy email/mailbox plan merely to send outbound transactional mail through Resend. Domain registration, mailbox hosting and outbound transactional sending are separate concerns.

## Google Secret Manager and Cloud Run

Terraform supports injecting the Resend API key into Cloud Run from an existing Secret Manager secret. The default secret ID is:

```text
leavemaster-resend-api-key
```

The secret value must be created/rotated outside source control, for example:

```bash
printf '%s' 'YOUR_RESEND_API_KEY' | \
  gcloud secrets create leavemaster-resend-api-key \
    --data-file=- \
    --replication-policy=automatic \
    --project=YOUR_PROJECT_ID
```

For an existing secret, add a new version instead:

```bash
printf '%s' 'YOUR_NEW_RESEND_API_KEY' | \
  gcloud secrets versions add leavemaster-resend-api-key \
    --data-file=- \
    --project=YOUR_PROJECT_ID
```

Do not put the actual key in GitHub variables, Terraform state, `application.yaml`, `.env` files committed to Git, logs, or frontend configuration.

The relevant Terraform/runtime configuration is:

```hcl
email_provider           = "resend"
resend_api_key_secret_id = "leavemaster-resend-api-key"
email_from_address       = "onboarding@resend.dev"
email_from_name          = "LeaveMaster"
```

Cloud Run receives the API key through a secret-backed `RESEND_API_KEY` environment reference.

## Optional future production custom domain

A verified custom sending domain is an upgrade path, not a prerequisite for the current implementation.

When LeaveMaestro is ready to send activation emails to real users using its own identity:

1. Add the LeaveMaestro domain or a sending subdomain in Resend.
2. Add the DNS records supplied by Resend at the DNS provider/registrar.
3. Wait for Resend to verify the domain.
4. Change `EMAIL_FROM_ADDRESS` to an address on the verified domain, for example `account@example.com`.
5. Restrict/rotate the Resend API key if appropriate for the production setup.
6. Deploy the configuration change; no Java/domain code change is needed.

A mailbox subscription from GoDaddy is still not required just to send through Resend.

SPF/DKIM and related DNS authentication improve sender authenticity/deliverability. DNS verification records are configuration records; they are not application secrets.

## Smoke testing

For development validation:

1. Set `EMAIL_PROVIDER=resend` and configure the secret-backed API key.
2. Use a staff/login setup whose delivery target is an appropriate Resend test recipient where supported.
3. Start the activation flow from the login page.
4. Select **Send verification PIN**.
5. Confirm the backend records a successful delivery attempt without logging the PIN.
6. Confirm the expected delivery/test event in Resend.
7. Exercise PIN verification and initial password setup.
8. Confirm the same account then follows the normal password login path.

Do not automate this smoke test with a real Resend call in unit/integration tests. CI must remain provider-independent.

## Troubleshooting

### Provider is disabled

Symptom: PIN requests cannot deliver email and logs indicate transactional email is not configured.

Check:

```text
EMAIL_PROVIDER=resend
```

### Invalid or missing API key

Check that:

- the Secret Manager secret has at least one enabled version;
- Cloud Run's runtime service account can access the secret;
- Terraform points to the correct secret ID;
- the deployed Cloud Run revision has a secret-backed `RESEND_API_KEY` reference.

Never print the key while diagnosing the problem.

### Resend rejects the sender/domain

If using a custom sender, verify that its domain is verified in Resend. For the current development setup, use the supported `resend.dev` testing identity rather than inventing an unverified sender address.

### Rate limit/provider rejection

The application has its own PIN request throttling in addition to any Resend provider limits. A provider error should appear as a sanitized delivery failure, while the API continues to preserve safe account-enumeration behaviour.

### PIN email failed to send

A failed provider call invalidates the generated PIN. The user must request another PIN after applicable cooldown/rate limits allow it. An undelivered PIN cannot later be verified.

### PIN expired or too many attempts

Request a new PIN after the resend cooldown. Do not extend or recover a plaintext PIN from logs/database; plaintext PINs are deliberately unavailable after generation.

## Security checklist

- Never commit or log `RESEND_API_KEY`.
- Never persist or log plaintext activation PINs.
- Keep deployed secrets in Secret Manager.
- Keep provider credentials out of `VITE_*` frontend variables.
- Use the application PIN throttling/attempt limits even if the provider also rate-limits.
- Treat `resend.dev` as development/test usage.
- Use a verified domain when production deliverability/branding becomes necessary.
- DNS verification records are not secrets; provider API keys are secrets.

## Related documentation

- [README](../README.md)
- [API documentation](api.md)
- [Cloud Run deployment](cloudrun-deployment.md)
- [Architecture](architecture.md)
- [Environments and domains](environments-and-domains.md)
- [Troubleshooting](troubleshooting.md)
- [Platform Admin password management](platform-admin-password.md)
