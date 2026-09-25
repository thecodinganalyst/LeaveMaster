locals {
  github_actions_monitoring_roles = toset([
    "roles/logging.configWriter",
    "roles/monitoring.editor"
  ])
}

resource "google_project_iam_member" "github_actions_monitoring" {
  for_each = local.github_actions_monitoring_roles

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${var.github_actions_service_account}"
}
