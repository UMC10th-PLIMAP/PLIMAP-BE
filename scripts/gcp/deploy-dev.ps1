[CmdletBinding()]
param(
    [string]$ProjectId = "plimap",
    [string]$Region = "asia-northeast3",
    [string]$ServiceName = "plimap-api-dev",
    [string]$Image = "asia-northeast3-docker.pkg.dev/plimap/plimap-docker/api:dev-initial",
    [string]$FrontendOrigin = "http://localhost:3000",
    [string]$FrontendRedirectUri = "http://localhost:3000/home"
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$runtimeServiceAccount = "plimap-api-dev@$ProjectId.iam.gserviceaccount.com"
$secretMap = [ordered]@{
    DB_URL                     = "plimap-dev-db-url"
    DB_USERNAME                = "plimap-dev-db-username"
    DB_PASSWORD                = "plimap-dev-db-password"
    REDIS_URL                  = "plimap-dev-redis-url"
    JWT_SECRET                 = "plimap-dev-jwt-secret"
    KAKAO_REST_API_KEY         = "plimap-dev-kakao-rest-api-key"
    KAKAO_REST_API_SECRET      = "plimap-dev-kakao-rest-api-secret"
    GOOGLE_CLIENT_ID           = "plimap-dev-google-client-id"
    GOOGLE_CLIENT_SECRET       = "plimap-dev-google-client-secret"
}

function Invoke-Gcloud {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & gcloud @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: gcloud $($Arguments -join ' ')"
    }
}

function ConvertTo-YamlSingleQuoted {
    param([Parameter(Mandatory)][string]$Value)

    return "'" + $Value.Replace("'", "''") + "'"
}

function Write-EnvironmentFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$CallbackBaseUrl
    )

    $lines = @(
        "SPRING_PROFILES_ACTIVE: 'dev'",
        "CORS_ALLOWED_ORIGINS: $(ConvertTo-YamlSingleQuoted $FrontendOrigin)",
        "OAUTH_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted $FrontendRedirectUri)",
        "KAKAO_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$CallbackBaseUrl/oauth/callback/kakao")",
        "GOOGLE_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$CallbackBaseUrl/oauth/callback/google")"
    )

    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, $lines, $utf8WithoutBom)
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud CLI was not found. Check the Google Cloud CLI installation and login."
}

foreach ($entry in $secretMap.GetEnumerator()) {
    $versionStates = @(& gcloud secrets versions list $entry.Value `
        --project=$ProjectId `
        --format="value(state)")

    if ($LASTEXITCODE -ne 0 -or $versionStates -notcontains "ENABLED") {
        throw "Secret has no enabled version: $($entry.Value) ($($entry.Key))"
    }
}

# Windows PowerShell 5.1 can promote native stderr to a terminating error when
# the service does not exist yet. That is expected on the first deploy.
$previousErrorActionPreference = $ErrorActionPreference
try {
    $ErrorActionPreference = "Continue"
    $serviceUrl = & gcloud run services describe $ServiceName `
        --project=$ProjectId `
        --region=$Region `
        --format="value(status.url)" 2>$null
    $serviceDescribeExitCode = $LASTEXITCODE
} finally {
    $ErrorActionPreference = $previousErrorActionPreference
}

if ($serviceDescribeExitCode -ne 0 -or [string]::IsNullOrWhiteSpace($serviceUrl)) {
    $callbackBaseUrl = "https://placeholder.invalid"
} else {
    $callbackBaseUrl = $serviceUrl.TrimEnd('/')
}

$environmentFile = New-TemporaryFile
try {
    Write-EnvironmentFile -Path $environmentFile.FullName -CallbackBaseUrl $callbackBaseUrl

    $secretBindings = ($secretMap.GetEnumerator() | ForEach-Object {
        "$($_.Key)=$($_.Value):latest"
    }) -join ","

    $deployArguments = @(
        "run", "deploy", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--image=$Image",
        "--service-account=$runtimeServiceAccount",
        "--execution-environment=gen2",
        "--port=8080",
        "--cpu=1",
        "--memory=512Mi",
        "--concurrency=40",
        "--timeout=60",
        "--min-instances=0",
        "--max-instances=2",
        "--ingress=all",
        "--allow-unauthenticated",
        "--cpu-boost",
        "--deploy-health-check",
        "--startup-probe=httpGet.path=/actuator/health/liveness,httpGet.port=8080,initialDelaySeconds=0,timeoutSeconds=3,periodSeconds=5,failureThreshold=24",
        "--liveness-probe=httpGet.path=/actuator/health/liveness,httpGet.port=8080,initialDelaySeconds=0,timeoutSeconds=3,periodSeconds=10,failureThreshold=3",
        "--readiness-probe=httpGet.path=/actuator/health/readiness,httpGet.port=8080,timeoutSeconds=3,periodSeconds=5,failureThreshold=3",
        "--env-vars-file=$($environmentFile.FullName)",
        "--set-secrets=$secretBindings",
        "--quiet"
    )
    Invoke-Gcloud -Arguments $deployArguments

    $deployedUrl = (& gcloud run services describe $ServiceName `
        --project=$ProjectId `
        --region=$Region `
        --format="value(status.url)").TrimEnd('/')
    if ($LASTEXITCODE -ne 0 -or [string]::IsNullOrWhiteSpace($deployedUrl)) {
        throw "Could not read the deployed Cloud Run URL."
    }

    if ($callbackBaseUrl -ne $deployedUrl) {
        Write-EnvironmentFile -Path $environmentFile.FullName -CallbackBaseUrl $deployedUrl
        Invoke-Gcloud -Arguments @(
            "run", "services", "update", $ServiceName,
            "--project=$ProjectId",
            "--region=$Region",
            "--env-vars-file=$($environmentFile.FullName)",
            "--quiet"
        )
    }

    foreach ($path in @(
        "/actuator/health/liveness",
        "/actuator/health/readiness",
        "/actuator/health",
        "/swagger-ui/index.html",
        "/v3/api-docs"
    )) {
        $response = Invoke-WebRequest -UseBasicParsing -Uri "$deployedUrl$path" -TimeoutSec 60
        if ($response.StatusCode -ne 200) {
            throw "Deployment verification failed: $path ($($response.StatusCode))"
        }
    }

    Write-Output "Cloud Run dev deployment and verification completed: $deployedUrl"
} finally {
    Remove-Item -LiteralPath $environmentFile.FullName -Force -ErrorAction SilentlyContinue
}
