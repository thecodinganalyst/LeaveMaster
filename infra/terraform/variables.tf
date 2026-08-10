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

variable "enable_platform_admin_password_secret" {
  description = "Whether Cloud Run should read PLATFORM_ADMIN_PASSWORD from Secret Manager"
  type        = bool
  default     = false
}

variable "reset_platform_admin_password" {
  description = "Explicit one-deployment switch that resets the existing default PlatformAdmin password at application startup"
  type        = bool
  default     = false
}

variable "enable_firebase_hosting" {
  description = "Whether to enable Firebase services and provision the frontend Hosting site"
  type        = bool
  default     = false
}

variable "frontend_environment" {
  description = "Logical frontend environment name used when deriving the Firebase Hosting site ID"
  type        = string
  default     = "production"

  validation {
    condition     = can(regex("^[a-z0-9-]+$", var.frontend_environment))
    error_message = "frontend_environment must contain only lowercase letters, digits, and hyphens."
  }
}

variable "firebase_hosting_site_id" {
  description = "Optional globally unique Firebase Hosting site ID. Defaults to <project_id>-<frontend_environment>."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition = (
      var.firebase_hosting_site_id == null ||
      can(regex("^[a-z0-9][a-z0-9-]*[a-z0-9]$", var.firebase_hosting_site_id))
    )
    error_message = "firebase_hosting_site_id must be a valid lowercase domain label when provided."
  }
}
