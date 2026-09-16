# Deploying KejaSafe

The API runs on **Cloud Run**, backed by **Cloud SQL for PostgreSQL** for data and a
**Cloud Storage** bucket for uploads. The web app is a static bundle on **Firebase
Hosting**. Both live in one Google Cloud project.

Nothing here has been run yet: the project has no billing credits, so this describes the
release and lets you execute it when the subscription is active. Every script accepts
`-DryRun`, which prints the exact commands without calling Google Cloud.

```
Browser ──► Firebase Hosting (static Wasm bundle)
               │ window.KEJASAFE_API_BASE_URL
               ▼
Mobile  ──► Cloud Run (Ktor API) ──► Cloud SQL (Postgres)
                                └──► Cloud Storage (listing photos, KYC files)
```

## Why the architecture looks like this

**The database cannot stay on H2.** Local development writes to an H2 file under
`server/build/db`. Cloud Run instances have an ephemeral filesystem and are replaced
whenever it scales, so that file would vanish. `initDatabase()` now opens a pooled
connection through HikariCP and reads its URL, driver and credentials from the
environment, so the same binary runs on H2 locally and Postgres in production without a
code change.

**Uploads cannot stay on the local filesystem either.** Rather than rewriting
`ListingService` against a storage SDK, the deployment mounts a Cloud Storage bucket at
`/mnt/uploads` and points `KEJASAFE_UPLOAD_DIR` at it. The service keeps writing ordinary
files, and Cloud Storage FUSE persists them.

**The image is built from a prebuilt jar.** Compiling `:server` pulls in `:core`, which is
a Kotlin Multiplatform module and therefore needs the Android SDK. Installing that inside
the build container would be slow and brittle, so `./gradlew :server:buildFatJar` runs on
your machine and the container only copies `server-all.jar`.

**The web app must be told where the API is.** In development the browser assumes port
8080 on the host serving the page. On Hosting that guess is wrong, because the API is on a
different domain, so `deploy-web.ps1` writes the real URL into `config.js` in the release
bundle and `index.html` loads it before the app starts.

## One-time setup

Install the [Google Cloud CLI](https://cloud.google.com/sdk/docs/install) and the
[Firebase CLI](https://firebase.google.com/docs/cli), then sign in:

```powershell
gcloud auth login
firebase login
```

Create the release configuration, which is git-ignored because it names your project:

```powershell
Copy-Item deploy/release.config.ps1.example deploy/release.config.ps1
```

Fill in `deploy/release.config.ps1`: the project id, the region (`africa-south1` is
Johannesburg, the closest to Kenya), and the Cloud SQL, bucket and hosting names.

Review the provisioning, then run it:

```powershell
./deploy/deploy-api.ps1 -Setup -DryRun
./deploy/deploy-api.ps1 -Setup
```

That enables the APIs, creates the Artifact Registry repository, a dedicated runtime
service account, the uploads bucket, a `db-f1-micro` Postgres instance with its database
and user, and stores the database password and JWT signing secret in Secret Manager. It
grants the service account exactly three things: read the two secrets, connect to Cloud
SQL, and write to the uploads bucket. Creating the Postgres instance takes several
minutes. Re-running is safe — existing resources are reported and skipped.

## Releasing the API

```powershell
./deploy/deploy-api.ps1
```

This builds the fat jar, has Cloud Build turn it into an image, and deploys to Cloud Run
with the full configuration: Postgres over the Cloud SQL connector, secrets injected as
environment variables, the bucket mounted at `/mnt/uploads`, and CORS restricted to your
hosting domains. It scales to zero when idle, so an unused service costs nothing beyond
storage and the Cloud SQL instance.

The script prints the service URL when it finishes. Check `/health` and `/swagger`.

Configuration the container reads, all defined in
[`ServerConfig.kt`](../server/src/main/kotlin/com/rotd/kejasafe/config/ServerConfig.kt):

| Variable | Deployed value |
| --- | --- |
| `PORT` | set by Cloud Run |
| `KEJASAFE_DB_URL` | `jdbc:postgresql:///kejasafe?cloudSqlInstance=…&socketFactory=…` |
| `KEJASAFE_DB_DRIVER` | `org.postgresql.Driver` |
| `KEJASAFE_DB_USER` / `KEJASAFE_DB_PASSWORD` | user from config, password from Secret Manager |
| `KEJASAFE_DB_POOL_SIZE` | defaults to 5 per instance |
| `KEJASAFE_JWT_SECRET` | from Secret Manager |
| `KEJASAFE_UPLOAD_DIR` | `/mnt/uploads` (the mounted bucket) |
| `KEJASAFE_ALLOWED_ORIGINS` | your hosting domains |

Keep `KEJASAFE_DB_POOL_SIZE` × max instances below the Postgres connection limit;
`db-f1-micro` allows roughly 25, and the defaults of 5 × 4 instances fit.

Once the service exists, a release only has to replace the image; the environment
variables, secrets, Cloud SQL connection and bucket mount stay as they were. That is what
[`deploy/cloudbuild.yaml`](../deploy/cloudbuild.yaml) does, and it is how the
`Deploy API` workflow ships every later change:

```powershell
./gradlew :server:buildFatJar
gcloud builds submit --config deploy/cloudbuild.yaml --substitutions=_TAG=1.0.1
```

## Releasing the web app

```powershell
./deploy/deploy-web.ps1
```

It resolves the API URL (from `ApiBaseUrl`, or by asking Cloud Run), builds
`:app:webApp:wasmJsBrowserDistribution`, writes `config.js`, and deploys to Hosting. For a
throwaway URL that leaves the live site alone:

```powershell
./deploy/deploy-web.ps1 -PreviewChannel pr-42
```

The Wasm payloads are the bulk of the download and their filenames are content-hashed, so
Hosting serves them as immutable for a year. `index.html`, `webApp.js`, `styles.css` and
`config.js` keep their names between builds, so those are sent with `Cache-Control:
no-cache` and revalidate on every load.

## Continuous integration and delivery

Three workflows live in [`.github/workflows`](../.github/workflows):

| Workflow | Trigger | What it does |
| --- | --- | --- |
| [`ci.yml`](../.github/workflows/ci.yml) | every pull request and push to `master_dev`/`main` | runs the JVM tests, assembles the debug APK, and builds the fat jar and container image |
| [`deploy-web.yml`](../.github/workflows/deploy-web.yml) | push to `master_dev`/`main`, and pull requests | publishes the web app to Hosting, or to a preview URL for a pull request |
| [`deploy-api.yml`](../.github/workflows/deploy-api.yml) | push to `master_dev`/`main` touching the server, or run manually | builds a new image and rolls it out to the existing Cloud Run service |

Every runner needs a JDK 21 and the Android SDK, because `:core` and `:app:shared` declare
Android targets and Gradle cannot configure the build without it. The Wasm compile takes
around ten minutes, which dominates the web release.

`ci.yml` needs no credentials. It runs `:server:test`, `:core:jvmTest` and
`:app:shared:jvmTest`, and it also builds `server-all.jar` and the Docker image so a
release never fails for something a pull request could have caught. The debug APK and the
test reports are uploaded as artifacts. There is no signing configuration in the
repository, so nothing produces a release APK or AAB yet; adding one means committing a
signing config and storing the keystore plus its passwords as secrets.

The deploy workflows only run when something that affects their output changes, and a
newer commit cancels an in-flight web build. `deploy-api.yml` runs under the `production`
environment, so you can require an approval before anything bills under **Settings →
Environments → production**.

### Secrets and variables

All of these live under **Settings → Secrets and variables → Actions**. Only one is a
secret: the rest are names, not credentials, so they go in the *Variables* tab where they
stay readable in logs and diffs.

| Name | Kind | Used by | Value |
| --- | --- | --- | --- |
| `FIREBASE_SERVICE_ACCOUNT` | secret | web | the full JSON key of a service account allowed to deploy Hosting; `FIREBASE_SERVICE_ACCOUNT_KEJASAFE` also works |
| `KEJASAFE_API_BASE_URL` | variable | web | the Cloud Run URL, for example `https://kejasafe-api-xxxxx.a.run.app` |
| `GCP_PROJECT_ID` | variable | api, web | `kejasafe` (the web workflow falls back to this name if unset) |
| `GCP_REGION` | variable | api | `africa-south1` |
| `GCP_SERVICE_NAME` | variable | api | `kejasafe-api` |
| `GCP_WORKLOAD_IDENTITY_PROVIDER` | variable | api | `projects/<project-number>/locations/global/workloadIdentityPools/github/providers/github` |
| `GCP_DEPLOY_SERVICE_ACCOUNT` | variable | api | `github-deploy@kejasafe.iam.gserviceaccount.com` |

Until the API is deployed, leave `KEJASAFE_API_BASE_URL` unset. The web workflow still
publishes the site and logs a warning; the pages render, and anything that reads from the
API shows its empty state until you set the variable and re-run.

### The Firebase Hosting secret

The Firebase CLI can do all of this, and it is the better option because the key never
touches your disk:

```powershell
firebase init hosting:github
```

It asks for the repository (`JoshuaMusyoki/KejaSafe`), creates a service account with the
right roles, and writes the key straight into GitHub as a secret. Two things to know:

- It names the secret `FIREBASE_SERVICE_ACCOUNT_KEJASAFE` rather than
  `FIREBASE_SERVICE_ACCOUNT`, and GitHub will not show you the value again so you cannot
  copy it under the other name. `deploy-web.yml` therefore accepts either spelling.
- It offers to write its own workflow files. Decline, or delete the generated
  `firebase-hosting-pull-request.yml` and `firebase-hosting-merge.yml` afterwards, because
  `deploy-web.yml` already covers both cases and builds the Wasm bundle first.

To create the account by hand instead, in the [Cloud Console service accounts
page](https://console.cloud.google.com/iam-admin/serviceaccounts), grant it these four
roles and download a JSON key:

| Role | Why |
| --- | --- |
| `roles/firebasehosting.admin` | deploy to the live site and to preview channels |
| `roles/firebaseauth.admin` | add each preview URL to the authorised domains for sign-in |
| `roles/serviceusage.apiKeysViewer` | required by CLI-based deploys |
| `roles/run.viewer` | only needed if Hosting ever rewrites to Cloud Run |

Then store it and delete the local copy, which is a permanent credential:

```powershell
Get-Content hosting-key.json -Raw | gh secret set FIREBASE_SERVICE_ACCOUNT --repo JoshuaMusyoki/KejaSafe
Remove-Item hosting-key.json
```

The equivalent with `gcloud`, once it is installed:

```powershell
gcloud iam service-accounts create github-hosting `
    --display-name="GitHub Actions (Firebase Hosting)" --project=kejasafe
foreach ($role in @(
    "roles/firebasehosting.admin",
    "roles/firebaseauth.admin",
    "roles/serviceusage.apiKeysViewer",
    "roles/run.viewer"
)) {
    gcloud projects add-iam-policy-binding kejasafe `
        --member=serviceAccount:github-hosting@kejasafe.iam.gserviceaccount.com --role=$role
}
gcloud iam service-accounts keys create hosting-key.json `
    --iam-account=github-hosting@kejasafe.iam.gserviceaccount.com
```

### Keyless authentication for the API

`deploy-api.yml` avoids a stored key. Workload Identity Federation lets GitHub's own OIDC
token be exchanged for a short-lived Google Cloud token, scoped to this one repository, so
there is nothing to leak or rotate. Run this once, replacing `<owner>` with your GitHub
account:

```powershell
$ProjectId = "kejasafe"
$Repo = "<owner>/KejaSafe"
$ProjectNumber = gcloud projects describe $ProjectId --format="value(projectNumber)"
$DeploySa = "github-deploy@$ProjectId.iam.gserviceaccount.com"

gcloud services enable iamcredentials.googleapis.com --project=$ProjectId

gcloud iam service-accounts create github-deploy `
    --display-name="GitHub Actions (Cloud Run)" --project=$ProjectId

gcloud iam workload-identity-pools create github --location=global `
    --display-name="GitHub Actions" --project=$ProjectId

# The attribute condition is what restricts the pool to this repository. Without it, any
# repository on GitHub could request a token.
gcloud iam workload-identity-pools providers create-oidc github `
    --location=global --workload-identity-pool=github `
    --issuer-uri="https://token.actions.githubusercontent.com" `
    --attribute-mapping="google.subject=assertion.sub,attribute.repository=assertion.repository" `
    --attribute-condition="assertion.repository=='$Repo'" `
    --project=$ProjectId

gcloud iam service-accounts add-iam-policy-binding $DeploySa `
    --role=roles/iam.workloadIdentityUser `
    --member="principalSet://iam.googleapis.com/projects/$ProjectNumber/locations/global/workloadIdentityPools/github/attributes.repository/$Repo" `
    --project=$ProjectId
```

The workflow submits a Cloud Build and reads the service afterwards, so grant it:

```powershell
foreach ($role in @(
    "roles/cloudbuild.builds.editor",  # submit the build
    "roles/storage.admin",             # upload the build's source archive
    "roles/run.viewer",                # read the service URL and check it exists
    "roles/iam.serviceAccountUser"     # act as the build's service account
)) {
    gcloud projects add-iam-policy-binding $ProjectId `
        --member="serviceAccount:$DeploySa" --role=$role
}
```

The rollout itself happens inside the build, under Cloud Build's own service account, so
that one needs permission to update Cloud Run and to act as the runtime service account:

```powershell
$BuildSa = "$ProjectNumber@cloudbuild.gserviceaccount.com"

gcloud projects add-iam-policy-binding $ProjectId `
    --member="serviceAccount:$BuildSa" --role=roles/run.admin
gcloud iam service-accounts add-iam-policy-binding "kejasafe-api@$ProjectId.iam.gserviceaccount.com" `
    --member="serviceAccount:$BuildSa" --role=roles/iam.serviceAccountUser --project=$ProjectId
```

Then set `GCP_WORKLOAD_IDENTITY_PROVIDER` to
`projects/<project-number>/locations/global/workloadIdentityPools/github/providers/github`
and `GCP_DEPLOY_SERVICE_ACCOUNT` to the `github-deploy` address.

If federation is more setup than you want for now, swap the two
`workload_identity_provider` and `service_account` inputs in the authentication step for
`credentials_json: ${{ secrets.GCP_SERVICE_ACCOUNT_KEY }}` and store a JSON key for the
same service account instead. It works identically, at the cost of a credential that never
expires.

Note that `deploy-api.yml` deliberately refuses to run before the first manual release:
an image-only rollout against a service that does not exist would create one with no
database, no secrets and no uploads volume. Run `./deploy/deploy-api.ps1` once from a
workstation, and CI takes over from there.

## After the first deploy

Point the mobile apps at the deployed API instead of a dev machine by setting
`ApiConfig.DEV_API_BASE_URL` in
[`ApiConfig.kt`](../core/src/commonMain/kotlin/com/rotd/kejasafe/network/ApiConfig.kt) to
the Cloud Run URL. It is HTTPS, so no cleartext exception is needed in
`network_security_config.xml`.

If you add a custom domain, do it in three places or browser calls will fail CORS: map the
domain in Cloud Run or Hosting, set `ApiBaseUrl`, and add the new origin to
`AllowedOrigins` before redeploying the API.

## Costs to expect

Cloud Run scales to zero and has a free tier, so idle traffic is close to free. The
ongoing charges are the Cloud SQL instance, which bills whether or not anyone connects,
Cloud Storage for uploads, and Artifact Registry for stored images. If the subscription is
delayed, the cheapest stopgap is to keep Cloud Run and swap `KEJASAFE_DB_URL` for a free
hosted Postgres — the connection is plain JDBC, so only that variable and
`KEJASAFE_DB_DRIVER` change.
