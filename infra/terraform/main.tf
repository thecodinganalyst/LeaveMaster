locals {
  image_url = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_repository}/${var.service_name}:${var.image_tag}"

  database_url = "jdbc:postgresql://${var.database_host}:5432/${var.database_name}?sslmode=require"
}

resource "google_project_service" "required" {
  for_each = local.required_apis

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_artifact_registry_repository" "docker" {
  location      = var.region
  repository_id = var.artifact_repository
  format        = "DOCKER"
}

resource "google_service_account" "cloud_run" {
  account_id   = "leavemaster-cloud-run"
  display_name = "LeaveMaster Cloud Run"
}

resource "google_secret_manager_secret" "database_password" {
  secret_id = "leavemaster-db-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_iam_member" "cloud_run_database_password" {
  secret_id = google_secret_manager_secret.database_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_cloud_run_v2_service" "api" {
  count = var.deploy_service ? 1 : 0

  name     = var.service_name
  location = var.region
}

resource "google_cloud_run_v2_service_iam_member" "public" {
  count = var.deploy_service ? 1 : 0

  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.api[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

