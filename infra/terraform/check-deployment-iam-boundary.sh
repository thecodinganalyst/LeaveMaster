#!/usr/bin/env bash
set -euo pipefail

workflow="../../.github/workflows/deploy-cloud-run.yml"

if grep -q -- '-target=google_project_iam_member.github_actions_monitoring' "$workflow"; then
  echo "Routine Cloud Run deployment must not target project-level deployment IAM." >&2
  exit 1
fi

if ! grep -q 'TF_VAR_manage_deployment_project_iam: "false"' "$workflow"; then
  echo "Routine Cloud Run deployment must explicitly disable project-level deployment IAM." >&2
  exit 1
fi
