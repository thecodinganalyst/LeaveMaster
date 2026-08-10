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

  firebase_required_apis = toset([
    "firebase.googleapis.com",
    "firebasehosting.googleapis.com",
    "serviceusage.googleapis.com"
  ])

  frontend_hosting_site_id = coalesce(
    var.firebase_hosting_site_id,
    "${var.project_id}-${var.frontend_environment}"
  )

  default_public_app_url = "https://${local.frontend_hosting_site_id}.firebaseapp.com"
  public_app_url         = trimsuffix(coalesce(var.public_app_url, local.default_public_app_url), "/")
  cors_allowed_origins = length(var.allowed_frontend_origins) > 0 ? var.allowed_frontend_origins : [
    local.public_app_url
  ]

  attachment_bucket_name = "${var.project_id}-leavemaster-attachments-${data.google_project.current.number}"
}

data "google_project" "current" {
  project_id = var.project_id
}

data "google_secret_manager_secret" "openai_api_key" {
  count = var.enable_openai_assistant ? 1 : 0

  project   = var.project_id
  secret_id = var.openai_api_key_secret_id
}

resource "google_project_service" "required" {
  for_each = local.required_apis

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_project_service" "firebase" {
  for_each = var.enable_firebase_hosting ? local.firebase_required_apis : toset([])

  project            = var.project_id
  service            = each.value
  disable_on_destroy = false
}

resource "google_firebase_project" "frontend" {
  count = var.enable_firebase_hosting ? 1 : 0

  provider = google-beta
  project  = var.project_id

  depends_on = [google_project_service.firebase]
}

resource "google_firebase_hosting_site" "frontend" {
  count = var.enable_firebase_hosting ? 1 : 0

  provider = google-beta
  project  = google_firebase_project.frontend[0].project
  site_id  = local.frontend_hosting_site_id

  depends_on = [google_project_service.firebase]
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

resource "google_storage_bucket" "attachments" {
  name     = local.attachment_bucket_name
  location = var.region

  storage_class               = "STANDARD"
  uniform_bucket_level_access = true
  public_access_prevention    = "enforced"

  depends_on = [
    google_project_service.required["storage.googleapis.com"]
  ]
}

resource "google_storage_bucket_iam_member" "cloud_run_attachments" {
  bucket = google_storage_bucket.attachments.name
  role   = "roles/storage.objectAdmin"
  member = "serviceAccount:${google_service_account.cloud_run.email}"
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

resource "google_secret_manager_secret" "platform_admin_password" {
  secret_id = "leavemaster-platform-admin-password"

  replication {
    auto {}
  }
}

resource "google_secret_manager_secret_iam_member" "cloud_run_platform_admin_password" {
  secret_id = google_secret_manager_secret.platform_admin_password.id
  role      = "roles/secretmanager.secretAccessor"
  member    = "serviceAccount:${google_service_account.cloud_run.email}"
}

resource "google_secret_manager_secret_iam_member" "cloud_run_openai_api_key" {
  count = var.enable_openai_assistant ? 1 : 0

  project   = var.project_id
  secret_id = data.google_secret_manager_secret.openai_api_key[0].secret_id
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
        name  = "APP_PUBLIC_URL"
        value = local.public_app_url
      }

      env {
        name  = "OAUTH_REDIRECT_BASE_URL"
        value = local.public_app_url
      }

      env {
        name  = "APP_CORS_ALLOWED_ORIGINS"
        value = join(",", local.cors_allowed_origins)
      }

      env {
        name  = "GCS_ATTACHMENT_BUCKET"
        value = local.attachment_bucket_name
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
        name  = "ASSISTANT_ENABLED"
        value = tostring(var.enable_openai_assistant)
      }

      env {
        name  = "SPRING_AI_MODEL_CHAT"
        value = var.enable_openai_assistant ? "openai" : "none"
      }

      env {
        name  = "OPENAI_MODEL"
        value = var.openai_model
      }

      env {
        name  = "PLATFORM_ADMIN_RESET_PASSWORD"
        value = tostring(var.reset_platform_admin_password)
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

      dynamic "env" {
        for_each = var.enable_openai_assistant ? [1] : []

        content {
          name = "OPENAI_API_KEY"

          value_source {
            secret_key_ref {
              secret  = data.google_secret_manager_secret.openai_api_key[0].secret_id
              version = "latest"
            }
          }
        }
      }

      dynamic "env" {
        for_each = var.enable_platform_admin_password_secret ? [1] : []

        content {
          name = "PLATFORM_ADMIN_PASSWORD"

          value_source {
            secret_key_ref {
              secret  = google_secret_manager_secret.platform_admin_password.secret_id
              version = "latest"
            }
          }
        }
      }
    }
  }

  depends_on = [
    google_project_service.required["run.googleapis.com"],
    google_secret_manager_secret_iam_member.cloud_run_database_password,
    google_secret_manager_secret_iam_member.cloud_run_platform_admin_password,
    google_secret_manager_secret_iam_member.cloud_run_openai_api_key
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
