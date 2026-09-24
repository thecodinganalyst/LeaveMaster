# Production monitoring and incident notifications

LeaveMaestro provisions production observability through Terraform in `infra/terraform/monitoring.tf`.

## What is monitored

Two Cloud Monitoring alert policies are created by default:

- **LeaveMaestro Cloud Run 5xx errors** — opens an incident when the `leavemaster-api` Cloud Run service records one or more HTTP 5xx responses.
- **LeaveMaestro application ERROR logs** — uses a log-based metric scoped to the Cloud Run service and opens an incident when an `ERROR`-or-higher log entry is emitted.

Both policies auto-close after 30 minutes when the condition is no longer active. Application logs and stack traces remain available in Cloud Logging/Error Reporting for diagnosis.

## Enable email notifications

Monitoring is enabled by default. To receive incident emails, add this **GitHub production environment variable**:

```text
MONITORING_NOTIFICATION_EMAIL=your-address@example.com
```

Do not store a personal email address directly in Terraform source. The deployment workflow passes the GitHub variable to Terraform and creates a Google Cloud Monitoring email notification channel.

After the next eligible production deployment, Google Cloud may send a channel verification message. Complete verification if prompted.

If `MONITORING_NOTIFICATION_EMAIL` is not configured, Terraform still creates the log metric and alert policies, but they have no email notification channel. Incidents remain visible under **Google Cloud Console → Monitoring → Alerting → Incidents**.

To disable Terraform-managed production monitoring entirely:

```text
ENABLE_PRODUCTION_MONITORING=false
```

## Where to investigate an incident

For a notification or open incident:

1. Open **Monitoring → Alerting → Incidents** and select the incident.
2. Note the incident start time and condition.
3. Open **Logging → Logs Explorer**.
4. Filter to Cloud Run service `leavemaster-api` and the incident time range.
5. For application failures, inspect the exception/stack trace and correlate it with the HTTP request.
6. Open **Error Reporting** for grouped Java exceptions when available.

A useful Logs Explorer filter is:

```text
resource.type="cloud_run_revision"
resource.labels.service_name="leavemaster-api"
severity>=ERROR
```

## Validation

Infrastructure CI runs:

```bash
terraform fmt -check -recursive
terraform init -backend=false
terraform validate
terraform test
```

The production deployment performs a full Terraform plan and protected-resource check before applying.

After deployment, confirm both policies are enabled in **Monitoring → Alerting** and the `leavemaestro_application_errors` log-based metric exists in Cloud Logging.

### Controlled notification test

Prefer a controlled non-destructive test rather than deliberately breaking the production service. If an existing test endpoint or known safe request can produce a handled server-side ERROR/5xx in a test environment, use that and verify an incident is created. Do not introduce a permanent public endpoint whose only purpose is to throw production errors.
