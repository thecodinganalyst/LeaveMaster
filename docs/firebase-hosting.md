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

For the GitHub `production` environment, configure:

| Variable | Example | Purpose |
| --- | --- | --- |
| `ENABLE_FIREBASE_HOSTING` | `true` | Enables Firebase resources during the production Terraform deployment. |
| `FRONTEND_ENVIRONMENT` | `production` | Used to derive an environment-specific site ID. |
| `FIREBASE_HOSTING_SITE` | `leavemaster-production` | Optional explicit Hosting site for frontend deployment. If omitted, CI derives `<GCP_PROJECT_ID>-<FRONTEND_ENVIRONMENT>`. |

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

The identity running Terraform and the Hosting deployment must have sufficient Firebase/Service Usage IAM permissions. A human user may also need to accept the Firebase Terms of Service before Firebase can be added to an existing Google Cloud project.

The production workflow authenticates with the existing GitHub Actions service account through Workload Identity Federation. It does not use a committed service-account key or legacy `FIREBASE_TOKEN`.

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

The CI workflow uses the equivalent GitHub production environment variables so it does not need Terraform state access just to publish static assets.

## Frontend CI and production deployment

`.github/workflows/frontend-quality.yml` is the frontend quality and Firebase Hosting workflow.

For pull requests that touch `frontend/**`, it runs:

```text
npm ci
npm run lint
npm run typecheck
npm test
npm run coverage
npm run build
```

Coverage and `frontend/dist` are uploaded as GitHub Actions artifacts. Pull requests do not deploy production.

For a push to `main` that changes the frontend (or a manual workflow dispatch), the production deployment job runs only after the quality job succeeds. It:

1. downloads the exact `frontend/dist` artifact produced by the successful quality job;
2. authenticates to Google Cloud with the existing WIF service account;
3. installs the pinned Firebase CLI;
4. injects the environment-specific Hosting site ID into a temporary `firebase.ci.json`; and
5. runs `firebase deploy --only hosting` against the production project/site.

Firebase CLI uses Application Default Credentials exported by `google-github-actions/auth`, so no long-lived Firebase token or service-account JSON key is needed.

Production deploys use the `production` GitHub environment and a dedicated concurrency group so overlapping live Hosting releases do not run at the same time.

## Same-origin Cloud Run API

`frontend/firebase.json` serves `frontend/dist` and sends backend routes to the existing `leavemaster-api` Cloud Run service in `asia-southeast1` before the React Router fallback:

```text
/api/**   -> Cloud Run
/auth/**  -> Cloud Run
/login    -> Cloud Run
/logout   -> Cloud Run
**        -> /index.html
```

This lets the production browser use the Firebase Hosting origin for application API/session requests. `frontend/src/config/env.ts` therefore defaults to an empty API base in production and continues to use `http://localhost:8080` during local development. `VITE_API_URL` remains available as an explicit non-secret override.

The Cloud Run rewrite also avoids embedding the Cloud Run URL or any credentials in the static bundle. OpenAI credentials remain backend-only.

## Path-aware monorepo workflows

Frontend-only changes do not trigger Java CI or Cloud Run deployment. Backend Java CI is scoped to `backend/**`; Cloud Run deployment is scoped to backend/infrastructure/container workflow changes. Frontend CI/deployment is scoped to `frontend/**` and its workflow file.

Infrastructure changes continue to be validated by the Terraform workflow and can still trigger Cloud Run/Terraform deployment when merged to `main`.

## Existing Cloud Run deployment safety

The Cloud Run workflow previously used `terraform apply -var="deploy_service=false"` as a bootstrap step. Against an existing state, that can make the counted Cloud Run resources absent from the desired configuration during that intermediate apply.

The workflow now targets only build/hosting prerequisite resources during bootstrap. It then builds the image, creates the full `deploy_service=true` plan, runs `check-protected-plan.sh`, and only then applies the complete plan.
