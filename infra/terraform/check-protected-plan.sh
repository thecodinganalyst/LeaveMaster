#!/usr/bin/env bash
set -euo pipefail

PLAN_FILE="${1:-tfplan}"

if ! command -v jq >/dev/null 2>&1; then
  echo "jq is required to inspect the Terraform plan." >&2
  exit 2
fi

protected_prefixes=(
  "google_cloud_run_v2_service.api"
  "google_cloud_run_v2_service_iam_member.public"
  "google_service_account.cloud_run"
  "google_artifact_registry_repository.docker"
  "google_storage_bucket.cloudbuild_source"
  "google_storage_bucket.attachments"
  "google_secret_manager_secret.database_password"
)

plan_json="$(terraform show -json "$PLAN_FILE")"
violations=0

for prefix in "${protected_prefixes[@]}"; do
  while IFS= read -r change; do
    [[ -z "$change" ]] && continue
    echo "Protected backend resource has a destructive plan: $change" >&2
    violations=$((violations + 1))
  done < <(
    jq -r --arg prefix "$prefix" '
      .resource_changes[]?
      | select(.address | startswith($prefix))
      | select(.change.actions | index("delete"))
      | "\(.address): \(.change.actions | join(" -> "))"
    ' <<<"$plan_json"
  )
done

if (( violations > 0 )); then
  echo "Refusing to continue: Terraform would delete or replace protected backend infrastructure." >&2
  exit 1
fi

echo "Protected backend infrastructure has no delete/replace actions."
