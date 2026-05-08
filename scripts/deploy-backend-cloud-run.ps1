param(
    [string]$ProjectId = "drivememaps",
    [string]$Region = "europe-west1",
    [string]$ServiceName = "driveme-backend",
    [Parameter(Mandatory = $true)]
    [string]$AdminWebOrigin,
    [Parameter(Mandatory = $true)]
    [string]$BackendUrl,
    [string]$MailEnabled = "false",
    [string]$MailFrom = "noreply@driveme.com"
)

$ErrorActionPreference = "Stop"

$gcloud = "gcloud"
if (-not (Get-Command $gcloud -ErrorAction SilentlyContinue)) {
    $candidate = "$env:LOCALAPPDATA\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
    if (Test-Path $candidate) {
        $gcloud = $candidate
    }
}

Push-Location "$PSScriptRoot\..\backend"
try {
    & $gcloud builds submit --project $ProjectId --tag "gcr.io/$ProjectId/$ServiceName" --suppress-logs
    if (-not $? -or $LASTEXITCODE -ne 0) {
        throw "Cloud Build failed."
    }

    & $gcloud run deploy $ServiceName `
        --project $ProjectId `
        --image "gcr.io/$ProjectId/$ServiceName" `
        --region $Region `
        --allow-unauthenticated `
        --set-env-vars "SPRING_PROFILES_ACTIVE=prod,SPRING_FLYWAY_VALIDATE_ON_MIGRATE=false,SPRING_FLYWAY_REPAIR_ON_MIGRATE=true,APP_PUBLIC_BASE_URL=$BackendUrl,APP_CORS_ALLOWED_ORIGINS=$AdminWebOrigin,APP_WEBSOCKET_ALLOWED_ORIGIN_PATTERNS=$AdminWebOrigin,MAIL_ENABLED=$MailEnabled,MAIL_FROM=$MailFrom" `
        --set-secrets "SPRING_DATASOURCE_URL=SPRING_DATASOURCE_URL:latest,SPRING_DATASOURCE_USERNAME=SPRING_DATASOURCE_USERNAME:latest,SPRING_DATASOURCE_PASSWORD=SPRING_DATASOURCE_PASSWORD:latest,JWT_SECRET=JWT_SECRET:latest,ADMIN_SIGNUP_SECRET=ADMIN_SIGNUP_SECRET:latest,MAIL_USERNAME=MAIL_USERNAME:latest,MAIL_PASSWORD=MAIL_PASSWORD:latest,GOOGLE_MAPS_API_KEY=GOOGLE_MAPS_API_KEY:latest,WEATHER_API_KEY=WEATHER_API_KEY:latest"
    if (-not $? -or $LASTEXITCODE -ne 0) {
        throw "Cloud Run deploy failed."
    }
} finally {
    Pop-Location
}
