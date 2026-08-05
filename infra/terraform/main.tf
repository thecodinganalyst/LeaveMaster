locals {
  image_url = "${var.region}-docker.pkg.dev/${var.project_id}/${var.artifact_repository}/${var.service_name}:${var.image_tag}"

  database_url = "jdbc:postgresql://${var.database_host}:5432/${var.database_name}?sslmode=require"

  cloudbuild_source_bucket_name = "${var.project_id}-cloudbuild-source-${data.google_project.current.number}"

  required_apis = toset([
    "artifactregistry.googleapis.com",
    "cloudbuild.googleapis.com",
    "run.googleapis.com",
    "secretmanager.googleapis.com",
    "storage.googleapis.com"
  ])
}

data "google_project" "current" {
  project_id = var.project_id
}

resource "google_project_service" "required" {
  for_each = local.required_apis

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_storage_bucket" "cloudbuild_source" {
  name     = local.cloudbuild_source_bucket_name
  location = var.region

  storage_class               = "STANDARD"
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  lifecycle_rule {
    condition {
      age = 7
    }

    action {
      type = "Delete"
    }
  }

  depends_on = [
    google_project_service.required["storage.googleapis.com"]
  ]
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

resource "google_storage_bucket_iam_member" "github_cloudbuild_source" {
  bucket = google_storage_bucket.cloudbuild_source.name
  role   = "roles/storage.admin"
  member = "serviceAccount:${var.github_actions_service_account}"
}

resource "google_cloud_run_v2_service" "api" {
  count = var.deploy_service ? 1 : 0

  name     = var.service_name
  location = var.region

  deletion_protection = false

  template {
    service_account = google_service_account.cloud_run.email
    timeout         = "300s"

    scaling {
      min_instance_count = 0
      max_instance_count = 1
    }

    containers {
      image = local.image_url

      ports {
        container_port = 8080
      }

      resources {
        limits = {
          cpu    = "1"
          memory = "1Gi"
        }

        cpu_idle = true
      }

      env {
        name  = "SPRING_PROFILES_ACTIVE"
        value = "cloudrun"
      }

      env {
        name  = "DATABASE_URL"
        value = local.database_url
      }

      env {
        name  = "DATABASE_USERNAME"
        value = var.database_username
      }

      env {
        name  = "DB_MAX_POOL_SIZE"
        value = "5"
      }

      env {
        name = "DATABASE_PASSWORD"

        value_source {
          secret_key_ref {
            secret  = google_secret_manager_secret.database_password.secret_id
            version = "latest"
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.required["run.googleapis.com"],
    google_secret_manager_secret_iam_member.cloud_run_database_password
  ]
}

resource "google_cloud_run_v2_service_iam_member" "public" {
  count = var.deploy_service ? 1 : 0

  project  = var.project_id
  location = var.region
  name     = google_cloud_run_v2_service.api[0].name
  role     = "roles/run.invoker"
  member   = "allUsers"
}

