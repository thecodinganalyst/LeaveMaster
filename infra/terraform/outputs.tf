output "cloud_run_url" {
  value = var.deploy_service ? google_cloud_run_v2_service.api[0].uri : null
}

output "container_image" {
  value = local.image_url
}