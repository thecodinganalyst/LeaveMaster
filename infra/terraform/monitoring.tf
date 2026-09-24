locals {
  monitoring_notification_channels = var.monitoring_notification_email == null ? [] : [
    google_monitoring_notification_channel.email[0].name
  ]
}

resource "google_monitoring_notification_channel" "email" {
  count = var.enable_production_monitoring && var.monitoring_notification_email != null ? 1 : 0

  project      = var.project_id
  display_name = "LeaveMaestro production incidents"
  type         = "email"

  labels = {
    email_address = var.monitoring_notification_email
  }

  depends_on = [google_project_service.required["monitoring.googleapis.com"]]
}

resource "google_logging_metric" "application_errors" {
  count = var.enable_production_monitoring ? 1 : 0

  project = var.project_id
  name    = "leavemaestro_application_errors"
  filter  = <<-EOT
    resource.type="cloud_run_revision"
    resource.labels.service_name="${var.service_name}"
    severity>=ERROR
  EOT

  metric_descriptor {
    metric_kind  = "DELTA"
    value_type   = "INT64"
    unit         = "1"
    display_name = "LeaveMaestro application errors"
  }

  depends_on = [google_project_service.required["logging.googleapis.com"]]
}

resource "google_monitoring_alert_policy" "cloud_run_5xx" {
  count = var.enable_production_monitoring ? 1 : 0

  project      = var.project_id
  display_name = "LeaveMaestro Cloud Run 5xx errors"
  combiner     = "OR"

  conditions {
    display_name = "Cloud Run returned a 5xx response"

    condition_threshold {
      filter = <<-EOT
        resource.type = "cloud_run_revision"
        AND resource.label."service_name" = "${var.service_name}"
        AND metric.type = "run.googleapis.com/request_count"
        AND metric.label."response_code_class" = "5xx"
      EOT
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"

      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_SUM"
      }
    }
  }

  notification_channels = local.monitoring_notification_channels

  documentation {
    content   = "LeaveMaestro Cloud Run returned one or more HTTP 5xx responses. Inspect Cloud Logging for the production API around the incident time."
    mime_type = "text/markdown"
  }

  alert_strategy {
    auto_close = "1800s"
  }

  depends_on = [google_project_service.required["monitoring.googleapis.com"]]
}

resource "google_monitoring_alert_policy" "application_errors" {
  count = var.enable_production_monitoring ? 1 : 0

  project      = var.project_id
  display_name = "LeaveMaestro application ERROR logs"
  combiner     = "OR"

  conditions {
    display_name = "Application emitted ERROR log entries"

    condition_threshold {
      filter          = "resource.type = \"cloud_run_revision\" AND metric.type = \"logging.googleapis.com/user/${google_logging_metric.application_errors[0].name}\""
      comparison      = "COMPARISON_GT"
      threshold_value = 0
      duration        = "0s"

      aggregations {
        alignment_period   = "60s"
        per_series_aligner = "ALIGN_SUM"
      }
    }
  }

  notification_channels = local.monitoring_notification_channels

  documentation {
    content   = "LeaveMaestro emitted an ERROR-or-higher Cloud Run log entry. Inspect Cloud Logging for the production API and incident time window."
    mime_type = "text/markdown"
  }

  alert_strategy {
    auto_close = "1800s"
  }

  depends_on = [
    google_logging_metric.application_errors,
    google_project_service.required["monitoring.googleapis.com"]
  ]
}
