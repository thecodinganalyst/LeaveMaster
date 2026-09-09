mock_provider "google" {}
mock_provider "google-beta" {}

variables {
  project_id                      = "leavemaster"
  image_tag                       = "test"
  database_host                   = "db.example.com"
  database_username               = "leavemaster"
  github_actions_service_account  = "github-actions@example.iam.gserviceaccount.com"
  github_oauth_client_id          = "github-client-id"
  google_oauth_client_id          = "google-client-id"
  enable_firebase_hosting         = true
  frontend_environment            = "production"
  public_app_url                  = "https://app.leavemaestro.com"
}

run "includes_canonical_and_firebase_aliases" {
  command = plan

  assert {
    condition = output.cors_allowed_origins == [
      "https://app.leavemaestro.com",
      "https://leavemaster-production.web.app",
      "https://leavemaster-production.firebaseapp.com"
    ]
    error_message = "CORS origins must include the canonical app URL and both Firebase Hosting aliases."
  }
}

run "merges_explicit_additional_origins" {
  command = plan

  variables {
    allowed_frontend_origins = [
      "https://preview.example.com",
      "https://app.leavemaestro.com"
    ]
  }

  assert {
    condition = output.cors_allowed_origins == [
      "https://app.leavemaestro.com",
      "https://leavemaster-production.web.app",
      "https://leavemaster-production.firebaseapp.com",
      "https://preview.example.com"
    ]
    error_message = "Explicit origins must be merged with, rather than replace, canonical and Firebase origins."
  }
}

run "uses_explicit_hosting_site_id" {
  command = plan

  variables {
    firebase_hosting_site_id = "leave-demo"
  }

  assert {
    condition = output.cors_allowed_origins == [
      "https://app.leavemaestro.com",
      "https://leave-demo.web.app",
      "https://leave-demo.firebaseapp.com"
    ]
    error_message = "Firebase aliases must be derived from the configured Hosting site ID."
  }
}

run "rejects_wildcard_origin" {
  command = plan

  variables {
    allowed_frontend_origins = ["https://*.example.com"]
  }

  expect_failures = [var.allowed_frontend_origins]
}
