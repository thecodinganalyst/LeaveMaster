# Cloud Run Deployment Guide

This guide explains how to deploy LeaveMaster to Google Cloud Run using a Supabase PostgreSQL database and a GitHub Actions CI/CD pipeline.

## Overview

The deployment pipeline:

1. A push to the `main` branch (or a manual trigger) fires the **Deploy to Cloud Run** GitHub Actions workflow.
2. GitHub Actions authenticates to Google Cloud via Workload Identity Federation (no long-lived service account keys).
3. Terraform provisions all required GCP resources (Artifact Registry, Cloud Build source bucket, Secret Manager secret, Cloud Run service, and optional Firebase Hosting infrastructure).
4. Cloud Build builds the Docker image and pushes it to Artifact Registry.
5. Cloud Run runs the image and reads the database password from Secret Manager at runtime.

---

## Prerequisites

- A [Google Cloud](https://cloud.google.com/) account with billing enabled.
- A [Supabase](https://supabase.com/) account.
- A GitHub repository with this codebase.
- [Terraform](https://developer.hashicorp.com/terraform/install) ≥ 1.8 installed locally for the one-time bootstrap steps.
- [Google Cloud CLI (`gcloud`)](https://cloud.google.com/sdk/docs/install) installed and authenticated locally.
- An OpenAI Platform API key if you want to enable the optional LeaveMaster AI assistant.
- Access to the Firebase Console to accept Firebase terms once if Firebase has never been enabled for the Google account/project.

---

## 1. Supabase — create a database

1. Log in to [app.supabase.com](https://app.supabase.com/) and create a new project. Choose the AWS region closest to your intended Cloud Run region.
2. Wait for the project to finish provisioning.
3. Go to **Project Settings → Database → Connection pooling** and make sure **Session mode** (port 5432) is enabled.
4. Note the following values from the **Connection string** tab (Transaction mode, port 6543 is not compatible with Flyway migrations — use the direct or session-mode connection):
   - **Host** — e.g. `aws-0-ap-southeast-1.pooler.supabase.com`
   - **User** — e.g. `postgres.your-project-ref`
   - **Password** — the password you set when creating the project
   - **Database name** — `postgres` (Supabase default)
5. Keep these values handy for the steps below.

---

## 2. Google Cloud — one-time bootstrap

### 2.1 Create a GCP project

```bash
gcloud projects create YOUR_PROJECT_ID --name="LeaveMaster"
gcloud config set project YOUR_PROJECT_ID
```

Enable billing for the project in the [Billing console](https://console.cloud.google.com/billing).

### 2.2 Enable required APIs

```bash
gcloud services enable \
  iam.googleapis.com \
  iamcredentials.googleapis.com \
  cloudresourcemanager.googleapis.com \
  artifactregistry.googleapis.com \
  cloudbuild.googleapis.com \
  run.googleapis.com \
  secretmanager.googleapis.com \
  storage.googleapis.com
```

Firebase-specific APIs are enabled in step 2.8 when you are ready to provision the frontend hosting path.

### 2.3 Create a Terraform state bucket

Terraform stores its state in a GCS bucket. Create it once:

```bash
gcloud storage buckets create gs://YOUR_PROJECT_ID-tfstate \
  --location=YOUR_REGION \
  --uniform-bucket-level-access
```

Replace `YOUR_REGION` with your preferred region (e.g. `asia-southeast1`). This bucket name must be globally unique; appending your project ID is a safe convention.

### 2.4 Create a GitHub Actions service account

```bash
gcloud iam service-accounts create github-actions \
  --display-name="GitHub Actions"
```

Grant it the permissions required by the workflow:

```bash
PROJECT_ID=YOUR_PROJECT_ID
SA_EMAIL="github-actions@${PROJECT_ID}.iam.gserviceaccount.com"

# Terraform needs broad editor access to manage resources
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/editor"

# Additional roles for Terraform resource creation
gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountAdmin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/iam.serviceAccountUser"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/resourcemanager.projectIamAdmin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/secretmanager.admin"

# Allow the service account to read and write the Terraform state bucket
gcloud storage buckets add-iam-policy-binding "gs://${PROJECT_ID}-tfstate" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/storage.admin"
```

Firebase-specific roles for this same GitHub Actions/Terraform service account are added in step 2.8. Do not add Firebase administration roles to the Cloud Run runtime service account.

### 2.5 Set up Workload Identity Federation

Workload Identity Federation lets GitHub Actions impersonate the service account without a JSON key file.

```bash
PROJECT_ID=YOUR_PROJECT_ID
GITHUB_ORG=your-github-org-or-username
GITHUB_REPO=LeaveMaster

# Create a Workload Identity Pool
gcloud iam workload-identity-pools create "github-pool" \
  --location="global" \
  --display-name="GitHub Actions pool"

# Get the pool resource name
WIF_POOL=$(gcloud iam workload-identity-pools describe "github-pool" \
  --location="global" \
  --format="value(name)")

# Create an OIDC provider inside the pool
gcloud iam workload-identity-pools providers create-oidc "github-provider" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --display-name="GitHub Actions provider" \
  --issuer-uri="https://token.actions.githubusercontent.com" \
  --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository,attribute.ref=assertion.ref" \
  --attribute-condition="assertion.repository=='${GITHUB_ORG}/${GITHUB_REPO}'"

# Allow the GitHub repository to impersonate the service account
SA_EMAIL="github-actions@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud iam service-accounts add-iam-policy-binding "${SA_EMAIL}" \
  --project="${PROJECT_ID}" \
  --role="roles/iam.workloadIdentityUser" \
  --member="principalSet://iam.googleapis.com/${WIF_POOL}/attribute.repository/${GITHUB_ORG}/${GITHUB_REPO}"

# Print the provider resource name — you will need this for GitHub variables
gcloud iam workload-identity-pools providers describe "github-provider" \
  --location="global" \
  --workload-identity-pool="github-pool" \
  --format="value(name)"
```

Note the output (e.g. `projects/123456789/locations/global/workloadIdentityPools/github-pool/providers/github-provider`). This is your **WIF Provider** value.

### 2.6 Store the database password in Secret Manager

Create the secret and add the Supabase database password as its initial version:

```bash
echo -n "YOUR_SUPABASE_DB_PASSWORD" | \
  gcloud secrets create leavemaster-db-password \
    --data-file=- \
    --project=YOUR_PROJECT_ID
```

> **Note:** Terraform creates the secret *resource* but does not manage the secret *value*. You must add the value manually (as above) or via the GCP console before the first deployment.

### 2.7 Configure the OpenAI API key for the AI assistant

The assistant is optional and remains disabled unless you explicitly enable it. Create an API key in the OpenAI Platform API key dashboard and store the value in Google Secret Manager. Never commit the key to this repository or expose it through frontend configuration.

Set your project and create the secret:

```bash
PROJECT_ID=YOUR_PROJECT_ID
gcloud config set project "${PROJECT_ID}"

printf '%s' 'YOUR_OPENAI_API_KEY' | \
  gcloud secrets create leavemaster-openai-api-key \
    --data-file=- \
    --replication-policy=automatic \
    --project="${PROJECT_ID}"
```

If the secret resource already exists and you only need to rotate/update the key, add a new version instead:

```bash
printf '%s' 'YOUR_NEW_OPENAI_API_KEY' | \
  gcloud secrets versions add leavemaster-openai-api-key \
    --data-file=- \
    --project="${PROJECT_ID}"
```

Grant the Cloud Run runtime service account access to the secret:

```bash
CLOUD_RUN_SA="leavemaster-cloud-run@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud secrets add-iam-policy-binding leavemaster-openai-api-key \
  --member="serviceAccount:${CLOUD_RUN_SA}" \
  --role="roles/secretmanager.secretAccessor" \
  --project="${PROJECT_ID}"
```

After the normal Terraform/GitHub Actions deployment has created the `leavemaster-api` service, attach the secret and enable the assistant:

```bash
gcloud run services update leavemaster-api \
  --region=YOUR_REGION \
  --set-secrets=OPENAI_API_KEY=leavemaster-openai-api-key:latest \
  --update-env-vars=ASSISTANT_ENABLED=true,SPRING_AI_MODEL_CHAT=openai,OPENAI_MODEL=gpt-5-mini \
  --project="${PROJECT_ID}"
```

Verify the service configuration without printing the secret value:

```bash
gcloud run services describe leavemaster-api \
  --region=YOUR_REGION \
  --project="${PROJECT_ID}" \
  --format='yaml(spec.template.spec.containers[0].env)'
```

Expected backend variables are:

| Variable | Purpose |
|----------|---------|
| `OPENAI_API_KEY` | Secret-backed OpenAI credential |
| `ASSISTANT_ENABLED` | Must be `true` to enable `/api/assistant/chat` |
| `SPRING_AI_MODEL_CHAT` | Set to `openai` to activate the Spring AI OpenAI model |
| `OPENAI_MODEL` | Optional model override; defaults to `gpt-5-mini` |

> **Important:** The current Terraform Cloud Run resource does not yet manage these assistant-specific environment/secret bindings. A later Terraform deployment can therefore replace a manually updated Cloud Run revision. Until runtime secret/environment management is made declarative under the infrastructure work, re-run the `gcloud run services update` command above after a Terraform deployment if the assistant settings are removed.

### 2.8 Enable Firebase for frontend hosting

Firebase Hosting uses the same existing Google Cloud project. The Terraform added for frontend hosting can create the Firebase project association and Hosting site, but the Google/Firebase account may first need Firebase terms accepted once in the Firebase Console.

Set the project and enable the required APIs:

```bash
PROJECT_ID=YOUR_PROJECT_ID
gcloud config set project "${PROJECT_ID}"

gcloud services enable \
  firebase.googleapis.com \
  firebasehosting.googleapis.com \
  serviceusage.googleapis.com \
  --project="${PROJECT_ID}"
```

The actual Firebase association can then be managed by Terraform (`google_firebase_project`). If you prefer to initialize it manually instead, install the Firebase CLI and run:

```bash
npm install -g firebase-tools
firebase login
firebase projects:addfirebase
```

Choose the same existing Google Cloud project when prompted. Do not create a second GCP project for the frontend.

#### Grant Firebase permissions to the deployment service account

The identity that runs Terraform and later deploys Firebase Hosting is the **GitHub Actions/WIF service account**, not the Cloud Run runtime service account.

```bash
PROJECT_ID=YOUR_PROJECT_ID
SA_EMAIL="github-actions@${PROJECT_ID}.iam.gserviceaccount.com"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/firebase.admin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/firebasehosting.admin"

gcloud projects add-iam-policy-binding "${PROJECT_ID}" \
  --member="serviceAccount:${SA_EMAIL}" \
  --role="roles/serviceusage.serviceUsageAdmin"
```

These roles allow the deployment identity to associate the existing project with Firebase, enable/check required services, create/manage the Hosting site, and later deploy Hosting releases.

> **Do not grant `roles/firebase.admin`, `roles/firebasehosting.admin`, or `roles/serviceusage.serviceUsageAdmin` to `leavemaster-cloud-run@...`.** The Cloud Run service account only runs the backend application and does not need Firebase Hosting administration permissions.

After PR #117 is merged, enable the Terraform Firebase resources through GitHub production environment variables:

```text
ENABLE_FIREBASE_HOSTING=true
FRONTEND_ENVIRONMENT=production
```

Terraform then manages the Firebase project association and Hosting site. The frontend build/deploy workflow is handled separately by the frontend CI/CD work.

---

## 3. GitHub — repository configuration

### 3.1 Create a `production` environment

1. In your GitHub repository, go to **Settings → Environments → New environment**.
2. Name it `production`.
3. Optionally add required reviewers or deployment branch rules to protect it.

### 3.2 Add repository variables

Go to **Settings → Environments → production → Environment variables** and add:

| Variable | Value |
|----------|-------|
| `GCP_PROJECT_ID` | Your GCP project ID (e.g. `my-leavemaster-project`) |
| `GCP_REGION` | The GCP region (e.g. `asia-southeast1`) |
| `SUPABASE_DB_HOST` | Supabase connection-pooler host (e.g. `aws-0-ap-southeast-1.pooler.supabase.com`) |
| `SUPABASE_DB_USERNAME` | Supabase database user (e.g. `postgres.your-project-ref`) |
| `TF_STATE_BUCKET` | Name of the Terraform state GCS bucket (e.g. `YOUR_PROJECT_ID-tfstate`) |
| `WIF_PROVIDER` | Workload Identity provider resource name from step 2.5 |
| `WIF_SERVICE_ACCOUNT` | `github-actions@YOUR_PROJECT_ID.iam.gserviceaccount.com` |
| `ENABLE_FIREBASE_HOSTING` | Set to `true` when you want Terraform to provision Firebase Hosting |
| `FRONTEND_ENVIRONMENT` | Frontend environment name, e.g. `production` |

The original seven variables remain required for the Cloud Run workflow. Firebase variables are only required when enabling the frontend hosting infrastructure.

Do **not** add `OPENAI_API_KEY` as a GitHub repository/environment variable for the running application. Store it in Google Secret Manager as described in step 2.7 so Cloud Run receives it directly at runtime. Likewise, do not expose backend credentials through frontend `VITE_*` variables.

---

## 4. First deployment

Push to `main` (or go to **Actions → Deploy to Cloud Run → Run workflow**) to trigger the pipeline. The workflow will:

1. Authenticate to GCP via Workload Identity Federation.
2. Run `terraform init` with the remote GCS backend.
3. Provision the prerequisite infrastructure without temporarily destroying the existing Cloud Run resources.
4. Build the Docker image with Cloud Build and push it to Artifact Registry.
5. Generate the full Terraform plan with `deploy_service=true`.
6. Reject the plan if it would delete or replace protected backend resources.
7. Apply the safe plan and deploy/update the Cloud Run service.
8. Provision the Firebase project/Hosting site as part of the same Terraform state when Firebase Hosting is enabled.

After the workflow completes, the **Show service URL** step prints the public HTTPS URL of your Cloud Run service.

If you want the AI assistant enabled, complete step 2.7 after the service exists. If you want Firebase Hosting provisioned, complete step 2.8 and set the Firebase production variables before running the deployment.

---

## 5. Subsequent deployments

Every push to `main` automatically rebuilds the image at the new Git SHA and updates the Cloud Run service. No manual steps are required for the normal backend deployment.

If you manually enabled the assistant using step 2.7, verify the OpenAI secret/environment binding after deployments until it is managed declaratively by Terraform.

### 5.1 Make the deployment inaccessible

If you need to remove public access from the production service, go to **Actions → Restrict Cloud Run Access → Run workflow**. The workflow removes the `allUsers` `roles/run.invoker` IAM binding from the `leavemaster-api` Cloud Run service.

### 5.2 Make the deployment accessible again

To restore public access, either:

1. Go to **Actions → Deploy to Cloud Run → Run workflow** and run the deployment workflow manually, or
2. Push a new commit to `main` so the deployment workflow runs automatically.

That workflow recreates the public `allUsers` `roles/run.invoker` binding for the `leavemaster-api` Cloud Run service.

---

## 6. Terraform variables reference

The Terraform configuration lives in `infra/terraform/`. An example variable file is provided at `infra/terraform/terraform.tfvars.example`.

| Variable | Default | Description |
|----------|---------|-------------|
| `project_id` | *(required)* | GCP project ID |
| `region` | `asia-southeast1` | GCP region for all resources |
| `service_name` | `leavemaster-api` | Cloud Run service name |
| `artifact_repository` | `leavemaster` | Artifact Registry repository ID |
| `image_tag` | *(required)* | Docker image tag (set to `github.sha` by the workflow) |
| `database_host` | *(required)* | Supabase database host |
| `database_username` | *(required)* | Supabase database user |
| `database_name` | `postgres` | Database name |
| `deploy_service` | `false` | Set to `true` to deploy the Cloud Run service |
| `github_actions_service_account` | *(required)* | Service account email used by GitHub Actions |
| `enable_firebase_hosting` | `false` | Whether Terraform should associate the project with Firebase and create Hosting |
| `frontend_environment` | `production` | Environment suffix used for the Hosting site |
| `firebase_hosting_site_id` | `null` | Optional explicit globally unique Hosting site ID; otherwise Terraform derives one |

---

## 7. GCP resources created by Terraform

| Resource | Purpose |
|----------|---------|
| Artifact Registry repository | Stores Docker images |
| GCS bucket (Cloud Build source) | Stages source archives for Cloud Build; objects are auto-deleted after 7 days |
| GCS bucket (attachments) | Stores leave-application file attachments |
| Service account `leavemaster-cloud-run` | Identity under which Cloud Run runs |
| Secret Manager secret `leavemaster-db-password` | Holds the Supabase database password; injected into Cloud Run at runtime |
| Firebase project association *(optional)* | Enables Firebase services on the existing GCP project |
| Firebase Hosting site *(optional)* | Hosts the static React/Vite frontend |

The optional `leavemaster-openai-api-key` secret described in step 2.7 is currently created manually and is not yet part of the Terraform resource list.

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Workflow fails at `Authenticate to Google Cloud` | WIF misconfiguration or wrong variable values | Double-check `WIF_PROVIDER` and `WIF_SERVICE_ACCOUNT`; verify the attribute condition matches your `GITHUB_ORG/GITHUB_REPO` |
| Terraform apply fails with *permission denied* | Service account missing IAM roles | Re-run step 2.4; for Firebase failures also apply the roles in step 2.8 |
| Firebase Terraform resource fails with permission/API errors | Firebase APIs are disabled or GitHub Actions service account lacks Firebase roles | Enable `firebase.googleapis.com`, `firebasehosting.googleapis.com`, and `serviceusage.googleapis.com`; grant the deployment service account `roles/firebase.admin`, `roles/firebasehosting.admin`, and `roles/serviceusage.serviceUsageAdmin` |
| Firebase project creation reports terms/setup required | Firebase terms have not yet been accepted for the account/project | Open the Firebase Console once, accept the required terms, then rerun Terraform |
| Firebase Hosting site already exists | The project/site was previously initialized manually | Import the existing Firebase resources into Terraform as documented in `docs/firebase-hosting.md` rather than creating duplicates |
| Cloud Run container crashes on startup | Database password secret has no version | Add the secret version (step 2.6) |
| AI assistant returns unavailable | Assistant is disabled, OpenAI model is not activated, or secret binding is missing | Verify `ASSISTANT_ENABLED=true`, `SPRING_AI_MODEL_CHAT=openai`, and the `OPENAI_API_KEY` Secret Manager binding from step 2.7 |
| OpenAI authentication fails | API key is invalid, revoked, or an old secret version is active | Add a fresh secret version, ensure `:latest` is used, then deploy a new Cloud Run revision |
| Flyway migration error | Using the transaction-mode pooler (port 6543) | Use the direct connection or session-mode pooler (port 5432) in `SUPABASE_DB_HOST` |
| `SECRET_NOT_FOUND` | Secret was not created before `terraform apply` | Run step 2.6 before triggering the workflow |
