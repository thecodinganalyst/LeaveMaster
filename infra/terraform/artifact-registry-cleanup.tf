# Repository-wide cleanup is initially dry-run only. Before enabling deletion,
# inventory Cloud Run revision image digests (including traffic-free rollback
# revisions), and explicitly protect any image older than this retention window.
# SHA-tagged releases are considered tagged images; untagged-only cleanup is
# insufficient for this repository.
variable "artifact_registry_cleanup_dry_run" {
  description = "Keep cleanup non-destructive until production and rollback digests have been audited."
  type        = bool
  default     = true
}

resource "google_artifact_registry_repository_cleanup_policy" "delete_old_images" {
  repository = google_artifact_registry_repository.docker.name
  location   = var.region
  project    = var.project_id
  name       = "delete-old-images"
  action     = "DELETE"

  condition {
    older_than = "2592000s" # 30 days
  }
}

resource "google_artifact_registry_repository_cleanup_policy" "keep_recent_images" {
  repository = google_artifact_registry_repository.docker.name
  location   = var.region
  project    = var.project_id
  name       = "keep-recent-images"
  action     = "KEEP"

  most_recent_versions {
    keep_count = 20
  }
}

# NOTE: The provider's repository cleanup policy resource does not itself
# expose a dry-run switch. The repository-level cleanup_policy_dry_run flag
# must be set on the repository resource (see main.tf).
