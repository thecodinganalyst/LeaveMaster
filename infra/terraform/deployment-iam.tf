locals {
  github_actions_monitoring_roles = toset([
    "roles/logging.configWriter",
    "roles/monitoring.editor"
  ])
}

resource "google_project_iam_member" "github_actions_monitoring" {
  for_each = var.manage_deployment_project_iam ? local.github_actions_monitoring_roles : toset([])

  project = var.project_id
  role    = each.value
  member  = "serviceAccount:${var.github_actions_service_account}"
}
