[CmdletBinding()]
param(
    [string]$ProjectId = "plimap",
    [string]$Region = "asia-northeast3",
    [string]$ServiceName = "plimap-api-dev",
    [string]$Image = "asia-northeast3-docker.pkg.dev/plimap/plimap-docker/api:dev-initial",
    [string]$PublicBaseUrl = "https://dev.plimap.kr",
    [string]$FrontendRedirectUri = "https://dev.plimap.kr/home"
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
    YOUTUBE_API_KEY            = "plimap-dev-youtube-api-key"
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

function Get-HttpsOrigin {
    param([Parameter(Mandatory)][string]$Value)

    $uri = [Uri]::new($Value, [UriKind]::Absolute)
    if ($uri.Scheme -ne "https" -or
        -not [string]::IsNullOrEmpty($uri.UserInfo) -or
        -not $uri.IsDefaultPort -or
        $uri.AbsolutePath -ne "/" -or
        -not [string]::IsNullOrEmpty($uri.Query) -or
        -not [string]::IsNullOrEmpty($uri.Fragment)) {
        throw "PublicBaseUrl must be an HTTPS origin without a path, query, fragment, credentials, or custom port: $Value"
    }

    return $uri.GetLeftPart([UriPartial]::Authority)
}

function Get-HttpsUrl {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value
    )

    $uri = [Uri]::new($Value, [UriKind]::Absolute)
    if ($uri.Scheme -ne "https" -or
        -not [string]::IsNullOrEmpty($uri.UserInfo) -or
        -not $uri.IsDefaultPort -or
        -not [string]::IsNullOrEmpty($uri.Fragment)) {
        throw "$Name must be an HTTPS URL without credentials, a custom port, or a fragment: $Value"
    }

    return $uri.AbsoluteUri
}

function Write-EnvironmentFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$PublicOrigin,
        [Parameter(Mandatory)][string]$FrontendRedirectUri
    )

    $lines = @(
        "SPRING_PROFILES_ACTIVE: 'dev'",
        "CORS_ALLOWED_ORIGINS: $(ConvertTo-YamlSingleQuoted $PublicOrigin)",
        "OAUTH_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted $FrontendRedirectUri)",
        "KAKAO_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$PublicOrigin/oauth/callback/kakao")",
        "GOOGLE_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$PublicOrigin/oauth/callback/google")"
    )

    $utf8WithoutBom = New-Object System.Text.UTF8Encoding($false)
    [System.IO.File]::WriteAllLines($Path, $lines, $utf8WithoutBom)
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud CLI was not found. Check the Google Cloud CLI installation and login."
}

$publicOrigin = Get-HttpsOrigin -Value $PublicBaseUrl
$frontendRedirectUrl = Get-HttpsUrl -Name "FrontendRedirectUri" -Value $FrontendRedirectUri

foreach ($entry in $secretMap.GetEnumerator()) {
    $versionStates = @(& gcloud secrets versions list $entry.Value `
        --project=$ProjectId `
        --format="value(state)")

    if ($LASTEXITCODE -ne 0 -or $versionStates -notcontains "ENABLED") {
        throw "Secret has no enabled version: $($entry.Value) ($($entry.Key))"
    }
}

$environmentFile = New-TemporaryFile
try {
    Write-EnvironmentFile `
        -Path $environmentFile.FullName `
        -PublicOrigin $publicOrigin `
        -FrontendRedirectUri $frontendRedirectUrl

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

    Write-Output "Cloud Run dev deployment and verification completed: $deployedUrl (public base: $publicOrigin)"
} finally {
    Remove-Item -LiteralPath $environmentFile.FullName -Force -ErrorAction SilentlyContinue
}
