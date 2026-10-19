<#
.SYNOPSIS
    Provisions and deploys the HavenPad API to Cloud Run.

.DESCRIPTION
    Run once with -Setup to create the infrastructure (Artifact Registry, Cloud SQL,
    uploads bucket, secrets, service account), then without flags for every release.

    Nothing here is destructive, and every step is idempotent, so re-running -Setup on an
    existing project reports "already exists" rather than replacing anything.

.EXAMPLE
    ./deploy/deploy-api.ps1 -Setup -DryRun
    Prints the provisioning commands without touching the project or billing.

.EXAMPLE
    ./deploy/deploy-api.ps1
    Builds the fat jar, builds the image with Cloud Build, and deploys it.
#>
[CmdletBinding()]
param(
    # Create the infrastructure the service depends on, then stop.
    [switch]$Setup,
    # Print commands instead of running them.
    [switch]$DryRun,
    # Image tag to build and deploy.
    [string]$Tag = "latest",
    # Reuse the jar already in server/build/libs.
    [switch]$SkipJar
)

$ErrorActionPreference = "Stop"
Set-StrictMode -Version Latest

$repoRoot = Split-Path -Parent $PSScriptRoot
$configPath = Join-Path $PSScriptRoot "release.config.ps1"

if (-not (Test-Path $configPath)) {
    throw "Missing $configPath. Copy release.config.ps1.example and fill in your project."
}
. $configPath

$sqlConnection = "${ProjectId}:${Region}:${SqlInstance}"
$imageRepo = "$Region-docker.pkg.dev/$ProjectId/$Repository/server"
$serviceAccount = "kejasafe-api@$ProjectId.iam.gserviceaccount.com"
$jwtSecretName = "kejasafe-jwt-secret"
$dbSecretName = "kejasafe-db-password"
$uploadMountPath = "/mnt/uploads"

function Invoke-Step {
    param(
        [Parameter(Mandatory)][string]$Description,
        [Parameter(Mandatory)][string[]]$Command,
        # Provisioning commands fail when the resource already exists; that is fine.
        [switch]$AllowFailure
    )

    # Keep generated credentials out of the console and any captured logs.
    $printable = $Command | ForEach-Object { $_ -replace '(--password=).*', '$1<redacted>' }

    Write-Host ""
    Write-Host "==> $Description" -ForegroundColor Cyan
    Write-Host "    $($printable -join ' ')" -ForegroundColor DarkGray

    if ($DryRun) { return }

    & $Command[0] @($Command[1..($Command.Length - 1)])
    if ($LASTEXITCODE -ne 0) {
        if ($AllowFailure) {
            Write-Host "    skipped (exit $LASTEXITCODE)" -ForegroundColor Yellow
            return
        }
        throw "$Description failed with exit code $LASTEXITCODE"
    }
}

function New-RandomSecret {
    param([int]$ByteCount = 48)

    $bytes = New-Object byte[] $ByteCount
    $rng = New-Object System.Security.Cryptography.RNGCryptoServiceProvider
    try { $rng.GetBytes($bytes) } finally { $rng.Dispose() }
    [Convert]::ToBase64String($bytes).TrimEnd("=").Replace("/", "_").Replace("+", "-")
}

function New-Secret {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value
    )

    # A temp file rather than a pipe: PowerShell appends a newline when piping, which
    # would become part of the secret.
    $file = [IO.Path]::GetTempFileName()
    try {
        [IO.File]::WriteAllText($file, $Value, (New-Object Text.UTF8Encoding $false))
        Invoke-Step -Description "Create secret $Name" -AllowFailure -Command @(
            "gcloud", "secrets", "create", $Name,
            "--project=$ProjectId",
            "--replication-policy=automatic",
            "--data-file=$file"
        )
    } finally {
        Remove-Item $file -Force -ErrorAction SilentlyContinue
    }
}

if ($Setup) {
    Write-Host "Provisioning $ProjectId in $Region" -ForegroundColor Green

    Invoke-Step -Description "Enable the APIs the service needs" -Command @(
        "gcloud", "services", "enable",
        "run.googleapis.com",
        "cloudbuild.googleapis.com",
        "artifactregistry.googleapis.com",
        "sqladmin.googleapis.com",
        "secretmanager.googleapis.com",
        "storage.googleapis.com",
        "--project=$ProjectId"
    )

    Invoke-Step -Description "Create the Docker repository" -AllowFailure -Command @(
        "gcloud", "artifacts", "repositories", "create", $Repository,
        "--repository-format=docker",
        "--location=$Region",
        "--description=HavenPad server images",
        "--project=$ProjectId"
    )

    Invoke-Step -Description "Create the runtime service account" -AllowFailure -Command @(
        "gcloud", "iam", "service-accounts", "create", "havenpad-api",
        "--display-name=HavenPad API (Cloud Run)",
        "--project=$ProjectId"
    )

    Invoke-Step -Description "Create the uploads bucket" -AllowFailure -Command @(
        "gcloud", "storage", "buckets", "create", "gs://$UploadsBucket",
        "--location=$Region",
        "--uniform-bucket-level-access",
        "--project=$ProjectId"
    )

    Invoke-Step -Description "Let the service read and write uploads" -Command @(
        "gcloud", "storage", "buckets", "add-iam-policy-binding", "gs://$UploadsBucket",
        "--member=serviceAccount:$serviceAccount",
        "--role=roles/storage.objectAdmin",
        "--project=$ProjectId"
    )

    # db-f1-micro is the cheapest tier; resize once real traffic arrives.
    Invoke-Step -Description "Create the Postgres instance (takes several minutes)" -AllowFailure -Command @(
        "gcloud", "sql", "instances", "create", $SqlInstance,
        "--database-version=POSTGRES_16",
        "--tier=db-f1-micro",
        "--region=$Region",
        "--storage-size=10GB",
        "--storage-auto-increase",
        "--project=$ProjectId"
    )

    Invoke-Step -Description "Create the database" -AllowFailure -Command @(
        "gcloud", "sql", "databases", "create", $SqlDatabase,
        "--instance=$SqlInstance",
        "--project=$ProjectId"
    )

    $dbPassword = New-RandomSecret -ByteCount 32
    Invoke-Step -Description "Create the database user" -AllowFailure -Command @(
        "gcloud", "sql", "users", "create", $SqlUser,
        "--instance=$SqlInstance",
        "--password=$dbPassword",
        "--project=$ProjectId"
    )
    New-Secret -Name $dbSecretName -Value $dbPassword
    New-Secret -Name $jwtSecretName -Value (New-RandomSecret)

    foreach ($secret in @($dbSecretName, $jwtSecretName)) {
        Invoke-Step -Description "Let the service read $secret" -Command @(
            "gcloud", "secrets", "add-iam-policy-binding", $secret,
            "--member=serviceAccount:$serviceAccount",
            "--role=roles/secretmanager.secretAccessor",
            "--project=$ProjectId"
        )
    }

    Invoke-Step -Description "Let the service open Cloud SQL connections" -Command @(
        "gcloud", "projects", "add-iam-policy-binding", $ProjectId,
        "--member=serviceAccount:$serviceAccount",
        "--role=roles/cloudsql.client"
    )

    Write-Host ""
    Write-Host "Provisioning done. Deploy with: ./deploy/deploy-api.ps1" -ForegroundColor Green
    if (-not $DryRun -and $dbPassword) {
        Write-Host "The database password is stored in Secret Manager as $dbSecretName." -ForegroundColor Yellow
    }
    return
}

if (-not $SkipJar) {
    Invoke-Step -Description "Build the server fat jar" -Command @(
        (Join-Path $repoRoot "gradlew.bat"), ":server:buildFatJar", "--console=plain"
    )
}

$jarPath = Join-Path $repoRoot "server/build/libs/server-all.jar"
if (-not $DryRun -and -not (Test-Path $jarPath)) {
    throw "Expected the fat jar at $jarPath. Run ./gradlew :server:buildFatJar first."
}

Invoke-Step -Description "Build and push the image with Cloud Build" -Command @(
    "gcloud", "builds", "submit",
    "--project=$ProjectId",
    "--config=deploy/cloudbuild.yaml",
    "--substitutions=_REGION=$Region,_REPOSITORY=$Repository,_SERVICE=$ServiceName,_TAG=$Tag",
    $repoRoot
)

# The Cloud SQL Java connector dials the instance over a Unix socket that Cloud Run
# provides; "lazy" refresh suits instances that scale to zero.
$dbUrl = "jdbc:postgresql:///${SqlDatabase}?cloudSqlInstance=${sqlConnection}" +
    "&socketFactory=com.google.cloud.sql.postgres.SocketFactory" +
    "&cloudSqlRefreshStrategy=lazy"

# A YAML file rather than --set-env-vars, because the allowed origins list contains the
# commas that gcloud would otherwise read as value separators.
$envFile = [IO.Path]::GetTempFileName()
$envYaml = @(
    "KEJASAFE_HOST: `"0.0.0.0`"",
    "KEJASAFE_DB_URL: `"$dbUrl`"",
    "KEJASAFE_DB_DRIVER: `"org.postgresql.Driver`"",
    "KEJASAFE_DB_USER: `"$SqlUser`"",
    "KEJASAFE_UPLOAD_DIR: `"$uploadMountPath`"",
    "KEJASAFE_ALLOWED_ORIGINS: `"$AllowedOrigins`""
) -join "`n"
[IO.File]::WriteAllText($envFile, "$envYaml`n", (New-Object Text.UTF8Encoding $false))

try {
    Invoke-Step -Description "Deploy to Cloud Run" -Command @(
        "gcloud", "run", "deploy", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--image=${imageRepo}:$Tag",
        "--service-account=$serviceAccount",
        "--allow-unauthenticated",
        "--port=8080",
        "--cpu=1",
        "--memory=1Gi",
        "--concurrency=40",
        "--timeout=300",
        "--min-instances=0",
        "--max-instances=4",
        # gen2 is required for Cloud Storage volume mounts.
        "--execution-environment=gen2",
        "--add-cloudsql-instances=$sqlConnection",
        "--add-volume=name=uploads,type=cloud-storage,bucket=$UploadsBucket",
        "--add-volume-mount=volume=uploads,mount-path=$uploadMountPath",
        "--env-vars-file=$envFile",
        "--set-secrets=KEJASAFE_JWT_SECRET=${jwtSecretName}:latest,KEJASAFE_DB_PASSWORD=${dbSecretName}:latest"
    )
} finally {
    Remove-Item $envFile -Force -ErrorAction SilentlyContinue
}

if ($DryRun) { return }

$url = & gcloud run services describe $ServiceName --project=$ProjectId --region=$Region --format="value(status.url)"
Write-Host ""
Write-Host "API deployed: $url" -ForegroundColor Green
Write-Host "Health:  $url/health"
Write-Host "Swagger: $url/swagger"
Write-Host ""
Write-Host "If this is a new URL, set ApiBaseUrl in deploy/release.config.ps1 and run" -ForegroundColor Yellow
Write-Host "./deploy/deploy-web.ps1 so the web app calls the right host." -ForegroundColor Yellow
