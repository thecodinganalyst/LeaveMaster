mock_provider "google" {}
mock_provider "google-beta" {}

variables {
  project_id                     = "test-project"
  region                         = "asia-southeast1"
  image_tag                      = "test"
  database_host                  = "db.example.com"
  database_username              = "postgres"
  github_actions_service_account = "github-actions@example.iam.gserviceaccount.com"
  github_oauth_client_id         = "github-client"
  google_oauth_client_id         = "google-client"
  enable_production_monitoring   = true
}

run "routine_deploy_does_not_manage_monitoring_resources" {
  command = plan

  assert {
    condition     = length(google_logging_metric.application_errors) == 0
    error_message = "Routine Terraform plans must not manage the production log metric."
  }

  assert {
    condition     = length(google_monitoring_alert_policy.cloud_run_5xx) == 0
    error_message = "Routine Terraform plans must not manage the Cloud Run 5xx alert policy."
  }

  assert {
    condition     = length(google_monitoring_alert_policy.application_errors) == 0
    error_message = "Routine Terraform plans must not manage the application error alert policy."
  }

  assert {
    condition     = length(google_monitoring_notification_channel.email) == 0
    error_message = "Routine Terraform plans must not manage monitoring notification channels."
  }
}

run "bootstrap_manages_monitoring_resources" {
  command = plan

  variables {
    manage_production_monitoring  = true
    monitoring_notification_email = "alerts@example.com"
  }

  assert {
    condition     = length(google_logging_metric.application_errors) == 1
    error_message = "Monitoring bootstrap must manage the production log metric."
  }

  assert {
    condition     = length(google_monitoring_alert_policy.cloud_run_5xx) == 1
    error_message = "Monitoring bootstrap must manage the Cloud Run 5xx alert policy."
  }

  assert {
    condition     = length(google_monitoring_alert_policy.application_errors) == 1
    error_message = "Monitoring bootstrap must manage the application error alert policy."
  }

  assert {
    condition     = length(google_monitoring_notification_channel.email) == 1
    error_message = "Monitoring bootstrap must manage the configured notification channel."
  }
}
