# Cloud Run Deployment Guide

This guide explains how to deploy LeaveMaster to Google Cloud Run using a Supabase PostgreSQL database and a GitHub Actions CI/CD pipeline.

## Overview

The deployment pipeline:

1. A push to the `main` branch (or a manual trigger) fires the **Deploy to Cloud Run** GitHub Actions workflow.
2. GitHub Actions authenticates to Google Cloud via Workload Identity Federation (no long-lived service account keys).
3. Terraform provisions all required GCP resources (Artifact Registry, Cloud Build source bucket, Secret Manager secret, Cloud Run service).
4. Cloud Build builds the Docker image and pushes it to Artifact Registry.
5. Cloud Run runs the image and reads the database password from Secret Manager at runtime.

---

## Prerequisites

- A [Google Cloud](https://cloud.google.com/) account with billing enabled.
- A [Supabase](https://supabase.com/) account.
- A GitHub repository with this codebase.
- [Terraform](https://developer.hashicorp.com/terraform/install) ≥ 1.8 installed locally for the one-time bootstrap steps.
- [Google Cloud CLI (`gcloud`)](https://cloud.google.com/sdk/docs/install) installed and authenticated locally.

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

> All seven variables are required. The workflow will fail if any are missing.

---

## 4. First deployment

Push to `main` (or go to **Actions → Deploy to Cloud Run → Run workflow**) to trigger the pipeline. The workflow will:

1. Authenticate to GCP via Workload Identity Federation.
2. Run `terraform init` with the remote GCS backend.
3. Provision the Artifact Registry repository, Cloud Build source bucket, attachments bucket, Cloud Run service account, and Secret Manager secret (via `terraform apply`).
4. Build the Docker image with Cloud Build and push it to Artifact Registry.
5. Deploy the Cloud Run service.

After the workflow completes, the **Show service URL** step prints the public HTTPS URL of your Cloud Run service.

---

## 5. Subsequent deployments

Every push to `main` automatically rebuilds the image at the new Git SHA and updates the Cloud Run service. No manual steps are required.

### 5.1 Make the deployment inaccessible

If you need to remove public access from the production service, go to **Actions → Restrict Cloud Run Access → Run workflow**. The workflow removes the `allUsers` `roles/run.invoker` IAM binding from the `leavemaster-api` Cloud Run service.

> A later run of the **Deploy to Cloud Run** workflow will make the service public again by restoring that binding.

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

---

## 7. GCP resources created by Terraform

| Resource | Purpose |
|----------|---------|
| Artifact Registry repository | Stores Docker images |
| GCS bucket (Cloud Build source) | Stages source archives for Cloud Build; objects are auto-deleted after 7 days |
| GCS bucket (attachments) | Stores leave-application file attachments |
| Service account `leavemaster-cloud-run` | Identity under which Cloud Run runs |
| Secret Manager secret `leavemaster-db-password` | Holds the Supabase database password; injected into Cloud Run at runtime |

---

## Troubleshooting

| Symptom | Likely cause | Fix |
|---------|-------------|-----|
| Workflow fails at `Authenticate to Google Cloud` | WIF misconfiguration or wrong variable values | Double-check `WIF_PROVIDER` and `WIF_SERVICE_ACCOUNT`; verify the attribute condition matches your `GITHUB_ORG/GITHUB_REPO` |
| Terraform apply fails with *permission denied* | Service account missing IAM roles | Re-run step 2.4 and ensure all roles are bound |
| Cloud Run container crashes on startup | Database password secret has no version | Add the secret version (step 2.6) |
| Flyway migration error | Using the transaction-mode pooler (port 6543) | Use the direct connection or session-mode pooler (port 5432) in `SUPABASE_DB_HOST` |
| `SECRET_NOT_FOUND` | Secret was not created before `terraform apply` | Run step 2.6 before triggering the workflow |
