# PlatformAdmin production password

LeaveMaster creates the default `PlatformAdmin` account only when no user is assigned to the `PLATFORM_ADMIN` role. The bootstrap password is encoded and stored in the database, so changing `PLATFORM_ADMIN_PASSWORD` later does not normally change an existing account.

Production should not rely on the local fallback password `changeme`.

## Secret Manager setup

Terraform creates the Secret Manager resource:

```text
leavemaster-platform-admin-password
```

Terraform intentionally does not manage the secret value. After the infrastructure PR is merged and the deployment has created the secret resource, add a strong password as the first version:

```bash
PROJECT_ID=leavemaster

printf '%s' 'YOUR_STRONG_PLATFORM_ADMIN_PASSWORD' | \
  gcloud secrets versions add leavemaster-platform-admin-password \
    --data-file=- \
    --project="${PROJECT_ID}"
```

If you are bootstrapping manually before Terraform has created the resource, create it with its first value instead:

```bash
printf '%s' 'YOUR_STRONG_PLATFORM_ADMIN_PASSWORD' | \
  gcloud secrets create leavemaster-platform-admin-password \
    --data-file=- \
    --replication-policy=automatic \
    --project="${PROJECT_ID}"
```

The Cloud Run runtime service account receives only `roles/secretmanager.secretAccessor` on this secret.

## Enable the secret-backed password

In GitHub **Settings → Environments → production → Environment variables**, set:

```text
ENABLE_PLATFORM_ADMIN_PASSWORD_SECRET=true
```

The next Cloud Run deployment injects the latest secret version as `PLATFORM_ADMIN_PASSWORD`.

This alone does not overwrite an existing PlatformAdmin password hash.

## Recover/reset the existing PlatformAdmin password

For one deployment only, also set:

```text
RESET_PLATFORM_ADMIN_PASSWORD=true
```

Run **Deploy to Cloud Run**. At application startup, LeaveMaster will reset the password only when all of the following are true:

- the explicit reset flag is `true`;
- the exact `PlatformAdmin` user exists; and
- that user is assigned to the `PLATFORM_ADMIN` role.

The application stores a newly encoded password hash; it never stores the plaintext secret in the database.

After the deployment succeeds and you can sign in with the new password, immediately change the GitHub production variable back to:

```text
RESET_PLATFORM_ADMIN_PASSWORD=false
```

Run the Cloud Run deployment again (or allow the next backend deployment to apply it). This prevents future cold starts or revisions from repeatedly resetting the password.

## Rotate the password later

Add a new Secret Manager version:

```bash
printf '%s' 'YOUR_NEW_PLATFORM_ADMIN_PASSWORD' | \
  gcloud secrets versions add leavemaster-platform-admin-password \
    --data-file=- \
    --project="${PROJECT_ID}"
```

Then temporarily set `RESET_PLATFORM_ADMIN_PASSWORD=true`, deploy once, verify login, and set it back to `false`.

## Safety notes

- Do not commit the password or place it in a `VITE_*` variable.
- Do not leave `RESET_PLATFORM_ADMIN_PASSWORD=true` after recovery.
- Do not grant Secret Manager administration to the Cloud Run runtime service account.
- `changeme` remains a local-development fallback only.
