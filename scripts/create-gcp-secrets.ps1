param(
    [string]$ProjectId = "drivememaps"
)

$ErrorActionPreference = "Stop"

$gcloud = "gcloud"
if (-not (Get-Command $gcloud -ErrorAction SilentlyContinue)) {
    $candidate = "$env:LOCALAPPDATA\Google\Cloud SDK\google-cloud-sdk\bin\gcloud.cmd"
    if (Test-Path $candidate) {
        $gcloud = $candidate
    }
}

function ConvertFrom-SecureText {
    param([securestring]$Secure)
    $ptr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($Secure)
    try {
        [Runtime.InteropServices.Marshal]::PtrToStringBSTR($ptr)
    } finally {
        [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($ptr)
    }
}

function Set-SecretValue {
    param(
        [string]$Name,
        [string]$Value
    )

    $exists = & $gcloud secrets list --project $ProjectId --filter="name:$Name" --format="value(name)"
    if (-not $exists) {
        & $gcloud secrets create $Name --project $ProjectId --replication-policy="automatic" | Out-Null
    }

    $tmp = New-TemporaryFile
    try {
        Set-Content -LiteralPath $tmp -Value $Value -NoNewline
        & $gcloud secrets versions add $Name --project $ProjectId --data-file=$tmp | Out-Null
        Write-Host "Updated secret: $Name"
    } finally {
        Remove-Item -LiteralPath $tmp -Force
    }
}

function Read-Plain {
    param(
        [string]$Prompt,
        [string]$Default = ""
    )
    if ($Default) {
        $value = Read-Host "$Prompt [$Default]"
        if ([string]::IsNullOrWhiteSpace($value)) { return $Default }
        return $value
    }
    return Read-Host $Prompt
}

function Read-Secret {
    param([string]$Prompt)
    ConvertFrom-SecureText (Read-Host $Prompt -AsSecureString)
}

function New-Base64Secret {
    $bytes = New-Object byte[] 64
    $rng = [Security.Cryptography.RandomNumberGenerator]::Create()
    try {
        $rng.GetBytes($bytes)
        [Convert]::ToBase64String($bytes)
    } finally {
        $rng.Dispose()
    }
}

Write-Host "Creating/updating DriveMe secrets in project: $ProjectId"
Write-Host "Values typed here are sent to Google Secret Manager and are not written to the repo."

Set-SecretValue "JWT_SECRET" (New-Base64Secret)
Set-SecretValue "ADMIN_SIGNUP_SECRET" (New-Base64Secret)

Set-SecretValue "SPRING_DATASOURCE_URL" (Read-Plain "Postgres JDBC URL")
Set-SecretValue "SPRING_DATASOURCE_USERNAME" (Read-Plain "Postgres username")
Set-SecretValue "SPRING_DATASOURCE_PASSWORD" (Read-Secret "Postgres password")
Set-SecretValue "GOOGLE_MAPS_API_KEY" (Read-Secret "Google Maps API key")
Set-SecretValue "WEATHER_API_KEY" (Read-Secret "OpenWeather API key")

$mailEnabled = Read-Plain "Enable real email? true/false" "false"
if ($mailEnabled -eq "true") {
    Set-SecretValue "MAIL_USERNAME" (Read-Plain "SMTP username/email")
    Set-SecretValue "MAIL_PASSWORD" (Read-Secret "SMTP app password")
} else {
    Set-SecretValue "MAIL_USERNAME" "disabled"
    Set-SecretValue "MAIL_PASSWORD" "disabled"
}

Write-Host "Done."
