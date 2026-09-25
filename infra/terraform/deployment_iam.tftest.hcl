mock_provider "google" {}
mock_provider "google-beta" {}

variables {
  project_id                     = "leavemaster"
  image_tag                      = "test"
  database_host                  = "db.example.com"
  database_username              = "leavemaster"
  github_actions_service_account = "github-actions@example.iam.gserviceaccount.com"
  github_oauth_client_id         = "github-client-id"
  google_oauth_client_id         = "google-client-id"
}

run "grants_monitoring_resource_permissions_to_deployment_identity" {
  command = plan

  assert {
    condition = google_project_iam_member.github_actions_monitoring["roles/monitoring.editor"].role == "roles/monitoring.editor"
    error_message = "The deployment identity must be able to manage Cloud Monitoring resources."
  }

  assert {
    condition = google_project_iam_member.github_actions_monitoring["roles/logging.configWriter"].role == "roles/logging.configWriter"
    error_message = "The deployment identity must be able to manage logs-based metrics."
  }

  assert {
    condition = alltrue([
      for binding in google_project_iam_member.github_actions_monitoring :
      binding.member == "serviceAccount:github-actions@example.iam.gserviceaccount.com"
    ])
    error_message = "Monitoring IAM roles must be granted to the configured GitHub Actions service account."
  }
}
