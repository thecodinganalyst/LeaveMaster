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

variable "public_app_url" {
  description = "Optional public frontend URL. Defaults to the environment-specific Firebase Hosting URL."
  type        = string
  default     = null
  nullable    = true

  validation {
    condition = (
      var.public_app_url == null ||
      can(regex("^https://[^/]+/?$", var.public_app_url))
    )
    error_message = "public_app_url must be an HTTPS origin without a path, query, or fragment."
  }
}

variable "allowed_frontend_origins" {
  description = "Exact browser origins allowed by backend CORS. Empty defaults to the public app URL only."
  type        = list(string)
  default     = []

  validation {
    condition = alltrue([
      for origin in var.allowed_frontend_origins :
      can(regex("^https://[^/]+$", origin)) && !strcontains(origin, "*")
    ])
    error_message = "allowed_frontend_origins must contain exact HTTPS origins only; wildcards are not allowed."
  }
}

variable "enable_openai_assistant" {
  description = "Whether Cloud Run should enable the OpenAI assistant and read its API key from Secret Manager"
  type        = bool
  default     = false
}

variable "openai_api_key_secret_id" {
  description = "Existing Secret Manager secret containing the OpenAI API key"
  type        = string
  default     = "leavemaster-openai-api-key"
}

variable "openai_model" {
  description = "OpenAI model used by the optional assistant"
  type        = string
  default     = "gpt-5-mini"
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
