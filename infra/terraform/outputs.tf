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
