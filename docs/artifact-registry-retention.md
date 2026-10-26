# Artifact Registry retention (issue #656)

The production Cloud Build workflow pushes `leavemaster-api:<git SHA>` before Terraform deploy. Failed deploys can leave tagged images. The `leavemaster` registry held 32.9 GB in October 2026.

## Initial policy

Terraform configures a repository-wide DELETE condition for images older than 30 days, and a KEEP rule for the 20 most recent versions per package. **Cleanup is in dry-run mode**; it must not delete anything until the checks below pass. Tags are not equivalent to Cloud Run revision references.

## Before enabling deletion

1. Inventory all Cloud Run revisions (including zero-traffic revisions), their resolved image digests, traffic splits and desired rollback candidates. Confirm every required digest is protected; if not, add explicit KEEP conditions or lengthen retention.
2. Inspect the dry-run audit logs for deletion candidates, and ensure none matches the protected digest inventory.
3. Verify the release and rollback process, and check Terraform provider support for the configured cleanup fields.
4. Apply using the appropriately privileged infrastructure identity. Do not run production deploys from an unreviewed PR.
5. Only after approval, change `cleanup_policy_dry_run` to `false` in a separate reviewed PR; rerun plan and validation.
6. Compare Artifact Registry billed storage and costs after cleanup (allow for delayed billing updates). Virtual image sizes include shared layers and cannot be summed as billed bytes.

Do not delete the repository or assume that the most recent tags include every Cloud Run rollback revision.
