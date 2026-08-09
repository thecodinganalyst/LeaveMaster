# Firebase Hosting infrastructure

LeaveMaster hosts the React/Vite frontend as a static single-page application on Firebase Hosting while the Spring Boot API remains on Cloud Run.

## What Terraform manages

When `enable_firebase_hosting=true`, `infra/terraform`:

1. enables the Firebase Management, Firebase Hosting, and Service Usage APIs;
2. associates the existing Google Cloud project with Firebase;
3. creates one Firebase Hosting site for the selected frontend environment; and
4. exposes the Firebase project ID, Hosting site ID, default Hosting URL, and logical frontend environment as Terraform outputs.

Firebase resources use the `google-beta` provider because Firebase Hosting Terraform support is currently beta. The existing Cloud Run, Artifact Registry, Cloud Build source bucket, attachment bucket, service account, and database secret remain managed by the existing Google provider/resources.

## Production variables

Firebase Hosting is intentionally disabled by default so merging infrastructure code does not unexpectedly change an existing environment.

For the GitHub `production` environment, configure:

| Variable | Example | Purpose |
| --- | --- | --- |
| `ENABLE_FIREBASE_HOSTING` | `true` | Enables Firebase resources during the production Terraform deployment. |
| `FRONTEND_ENVIRONMENT` | `production` | Used to derive an environment-specific site ID. |

By default the Hosting site ID is:

```text
<project_id>-<frontend_environment>
```

For example, project `leavemaster` and environment `production` produce `leavemaster-production`.

For a manual Terraform run, the equivalent arguments are:

```bash
terraform plan \
  -var="deploy_service=true" \
  -var="enable_firebase_hosting=true" \
  -var="frontend_environment=production"
```

Use `firebase_hosting_site_id` only when the derived globally unique site ID is unsuitable:

```bash
-var="firebase_hosting_site_id=my-unique-site"
```

Do not put OpenAI keys, database passwords, OAuth secrets, or other credentials into frontend variables. Firebase Hosting serves static browser assets, so anything built into the frontend must be treated as public.

## One-time Firebase prerequisite

The identity running Terraform must have sufficient IAM permissions to enable Firebase services and create Hosting resources. A human user may also need to have accepted the Firebase Terms of Service before Firebase can be added to an existing Google Cloud project.

If the Google Cloud project was already added to Firebase outside Terraform, import it before applying rather than attempting to recreate its Terraform state:

```bash
terraform import 'google_firebase_project.frontend[0]' leavemaster
```

If the intended Hosting site already exists, import it as well:

```bash
terraform import 'google_firebase_hosting_site.frontend[0]' 'leavemaster/leavemaster-production'
```

Substitute the actual project and site IDs.

## Validate before applying

From `infra/terraform`:

```bash
terraform fmt -check -recursive
terraform init
terraform validate
terraform plan \
  -var="deploy_service=true" \
  -var="enable_firebase_hosting=true" \
  -out=tfplan
./check-protected-plan.sh tfplan
```

The protected-plan check fails if Terraform proposes a delete/replacement action for the existing Cloud Run service or IAM binding, Cloud Run service account, Artifact Registry repository, build/attachment buckets, or database password secret.

Review the remaining plan before applying. For the first Firebase enablement, the intended infrastructure delta should be additive Firebase API/project/Hosting resources rather than backend replacement.

## Terraform outputs

After Firebase Hosting has been enabled and applied:

```bash
terraform output -raw firebase_project_id
terraform output -raw firebase_hosting_site_id
terraform output -raw firebase_hosting_default_url
terraform output -raw frontend_environment
```

Issue #118 can consume the project/site outputs when configuring the Firebase CLI deployment workflow.

## Firebase CLI SPA configuration

`frontend/firebase.json` serves `frontend/dist`, rewrites unknown routes to `index.html` for React Router, gives hashed JS/CSS assets long-lived immutable caching, and keeps `index.html` uncached so new releases are discovered promptly.

The Hosting site is environment-specific, so deployment automation should bind a target at runtime rather than committing a production project mapping. A CI job can run from `frontend/`:

```bash
firebase target:apply hosting leavemaster "$FIREBASE_HOSTING_SITE_ID" \
  --project "$FIREBASE_PROJECT_ID"

firebase deploy \
  --only hosting:leavemaster \
  --project "$FIREBASE_PROJECT_ID"
```

The broader build/deploy workflow is intentionally left to issue #118.

## Existing Cloud Run deployment safety

The Cloud Run workflow previously used `terraform apply -var="deploy_service=false"` as a bootstrap step. Against an existing state, that can make the counted Cloud Run resources absent from the desired configuration during that intermediate apply.

The workflow now targets only build/hosting prerequisite resources during bootstrap. It then builds the image, creates the full `deploy_service=true` plan, runs `check-protected-plan.sh`, and only then applies the complete plan.
