# Public customer enquiries

LeaveMaestro exposes a deliberately narrow public endpoint for prospective-customer enquiries from the marketing site.

## Endpoint

`POST /api/public/contact`

Authentication and tenant membership are not required. No read/list endpoint is exposed publicly.

Example request:

```json
{
  "name": "Jane Doe",
  "company": "Example Pte Ltd",
  "email": "jane@example.com",
  "phone": "+65 6123 4567",
  "companySize": "21-100",
  "country": "Singapore",
  "enquiryType": "PRODUCT_DEMO",
  "message": "Please arrange a demo.",
  "website": ""
}
```

Supported `enquiryType` values are `LEAVE_MANAGEMENT`, `HR_PLATFORM`, `PRODUCT_DEMO`, `PRICING`, `PARTNERSHIP`, and `OTHER`.

Successful submissions return HTTP `201`. Validation failures return `400`, oversized submissions return `413`, and rate-limited submissions return `429`.

The `website` field is a honeypot used by the marketing form and should remain blank. Customer-provided message content is persisted as text and is not exposed through a public rendering endpoint.

## Persistence and isolation

Enquiries are stored in the dedicated `customer_enquiry` table with status `NEW`. They are not associated with a tenant and are not exposed by existing tenant APIs. This keeps lead data out of tenant-scoped application resources while allowing the public endpoint to operate without provisioning a tenant.

## Abuse controls

The backend applies:

- server-side required-field, email, and maximum-length validation;
- a 16 KiB request-size limit by default;
- an in-memory per-client rate limit of five submissions per 15 minutes by default;
- a marketing-form honeypot field;
- generic client-facing error messages rather than internal exception details.

The in-memory rate limiter is per application instance. Production deployments should retain Cloudflare/WAF-level abuse controls as an additional layer when traffic warrants it.

## Notification configuration

Persistence completes before the notification is attempted. A mail-delivery failure therefore does not discard the enquiry.

Configure the destination with:

```text
CUSTOMER_ENQUIRY_NOTIFICATION_RECIPIENT=sales@leavemaestro.com
CUSTOMER_ENQUIRY_NOTIFICATION_FROM=noreply@leavemaestro.com
```

Spring Boot mail configuration uses the standard `SPRING_MAIL_*` environment variables. At minimum a deployment normally supplies `SPRING_MAIL_HOST` plus any port, username, password, TLS, and authentication settings required by the selected SMTP provider.

If no recipient or mail transport is configured, the enquiry is still persisted and the backend logs that notification delivery was skipped.

Optional abuse-control settings:

```text
CUSTOMER_ENQUIRY_MAX_REQUEST_BYTES=16384
CUSTOMER_ENQUIRY_RATE_LIMIT_MAX_REQUESTS=5
CUSTOMER_ENQUIRY_RATE_LIMIT_WINDOW_SECONDS=900
```

## CORS and the marketing site

The static marketing site sends the request from the browser using `NEXT_PUBLIC_API_URL`. For local development it falls back to `http://localhost:8080`.

Production must include the marketing origin in `APP_CORS_ALLOWED_ORIGINS`, for example:

```text
APP_CORS_ALLOWED_ORIGINS=https://app.leavemaestro.com,https://leavemaestro.com,https://www.leavemaestro.com
```

Cloudflare preview domains should only be added when a preview genuinely needs to submit to the production API. Prefer a non-production API for preview testing.

## Marketing form

The contact form collects name, company, work email, optional phone/company size/country, enquiry type, and message. It displays submitting, success, and error states and only clears the form after a successful API response.

Run the marketing form logic tests with:

```bash
cd marketing
npm test
```
