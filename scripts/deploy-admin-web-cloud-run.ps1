param(
    [string]$ProjectId = "drivememaps",
    [string]$Region = "europe-west1",
    [string]$ServiceName = "driveme-admin-web",
    [Parameter(Mandatory = $true)]
    [string]$BackendUrl
)

$ErrorActionPreference = "Stop"

$gcloud = "gcloud"
if (-not (Get-Command $gcloud -ErrorAction SilentlyContinue)) {
    $candidate = "$env:LOCALAPPDATA\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
    if (Test-Path $candidate) {
        $gcloud = $candidate
    }
}

Push-Location "$PSScriptRoot\..\admin-web"
try {
    & $gcloud builds submit `
        --project $ProjectId `
        --config cloudbuild.yaml `
        --substitutions "_IMAGE=gcr.io/$ProjectId/$ServiceName,_VITE_API_ORIGIN=$BackendUrl" `
        --suppress-logs `
        .
    if (-not $? -or $LASTEXITCODE -ne 0) {
        throw "Cloud Build failed."
    }

    & $gcloud run deploy $ServiceName `
        --project $ProjectId `
        --image "gcr.io/$ProjectId/$ServiceName" `
        --region $Region `
        --allow-unauthenticated
    if (-not $? -or $LASTEXITCODE -ne 0) {
        throw "Cloud Run deploy failed."
    }
} finally {
    Pop-Location
}
