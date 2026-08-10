output "cloud_run_url" {
  value = var.deploy_service ? google_cloud_run_v2_service.api[0].uri : null
}

output "container_image" {
  value = local.image_url
}

output "cloudbuild_source_bucket" {
  description = "Bucket used to stage source for Cloud Build"
  value       = google_storage_bucket.cloudbuild_source.name
}

output "attachments_bucket" {
  description = "Bucket used to store leave application attachments"
  value       = google_storage_bucket.attachments.name
}

output "public_app_url" {
  description = "Canonical public frontend origin used for OAuth return URLs and runtime configuration"
  value       = local.public_app_url
}

output "cors_allowed_origins" {
  description = "Exact browser origins allowed by backend CORS"
  value       = local.cors_allowed_origins
}

output "firebase_project_id" {
  description = "Google/Firebase project ID used by frontend deployment tooling"
  value       = var.enable_firebase_hosting ? google_firebase_project.frontend[0].project : null
}

output "firebase_hosting_site_id" {
  description = "Firebase Hosting site ID used by frontend deployment tooling"
  value       = var.enable_firebase_hosting ? google_firebase_hosting_site.frontend[0].site_id : null
}

output "firebase_hosting_default_url" {
  description = "Default Firebase Hosting URL for the frontend"
  value       = var.enable_firebase_hosting ? google_firebase_hosting_site.frontend[0].default_url : null
}

output "frontend_environment" {
  description = "Logical environment associated with the frontend hosting site"
  value       = var.frontend_environment
}
