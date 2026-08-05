variable "project_id" {
  type = string
}

variable "region" {
  type    = string
  default = "asia-southeast1"
}

variable "service_name" {
  type    = string
  default = "leavemaster-api"
}

variable "artifact_repository" {
  type    = string
  default = "leavemaster"
}

variable "image_tag" {
  type = string
}

variable "database_host" {
  type = string
}

variable "database_username" {
  type = string
}

variable "database_name" {
  type    = string
  default = "postgres"
}

variable "deploy_service" {
  type    = bool
  default = false
}

variable "github_actions_service_account" {
  description = "Service account impersonated by GitHub Actions"
  type        = string
}
