[CmdletBinding()]
param(
    [string]$ProjectId = "plimap",
    [string]$Region = "asia-northeast3",
    [string]$ServiceName = "plimap-api-prod",
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$Image,
    [Parameter(Mandatory)]
    [ValidatePattern("^[0-9a-f]{40}$")]
    [string]$DeployCommit,
    [string]$PublicBaseUrl = "https://plimap.kr",
    [string]$FrontendRedirectUri = "",
    [string]$CorsAllowedOrigins = "",
    [string]$OAuthAllowedFrontendOrigins = "",
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$ProfileImageBucket,
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$VpcNetwork,
    [Parameter(Mandatory)]
    [ValidateNotNullOrEmpty()]
    [string]$VpcSubnet,
    [string]$ResultPath = ""
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$runtimeServiceAccount = "plimap-api-prod@$ProjectId.iam.gserviceaccount.com"
$secretMap = [ordered]@{
    DB_URL                = "plimap-prod-db-url"
    DB_USERNAME           = "plimap-prod-db-username"
    DB_PASSWORD           = "plimap-prod-db-password"
    REDIS_URL             = "plimap-prod-redis-url"
    JWT_SECRET            = "plimap-prod-jwt-secret"
    KAKAO_REST_API_KEY    = "plimap-prod-kakao-rest-api-key"
    KAKAO_REST_API_SECRET = "plimap-prod-kakao-rest-api-secret"
    GOOGLE_CLIENT_ID      = "plimap-prod-google-client-id"
    GOOGLE_CLIENT_SECRET  = "plimap-prod-google-client-secret"
    YOUTUBE_API_KEY       = "plimap-prod-youtube-api-key"
}

$result = [ordered]@{
    status                = "failed"
    deployCommit          = $DeployCommit
    image                 = $Image
    previousRevision      = ""
    candidateRevision     = ""
    candidateVerification = "not-started"
    trafficPromotion      = "not-started"
    serviceVerification   = "not-started"
    rollback              = "not-required"
    serviceUrl            = ""
    error                 = ""
}
$environmentFile = $null
$trafficPromotionAttempted = $false

function Invoke-Gcloud {
    param([Parameter(Mandatory)][string[]]$Arguments)

    & gcloud @Arguments
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: gcloud $($Arguments -join ' ')"
    }
}

function Get-GcloudText {
    param([Parameter(Mandatory)][string[]]$Arguments)

    $output = @(& gcloud @Arguments)
    if ($LASTEXITCODE -ne 0) {
        throw "gcloud command failed: gcloud $($Arguments -join ' ')"
    }
    return ($output -join "`n").Trim()
}

function Get-JsonProperty {
    param(
        [AllowNull()][object]$Object,
        [Parameter(Mandatory)][string]$Name
    )

    if ($null -eq $Object) {
        return $null
    }

    $property = $Object.PSObject.Properties[$Name]
    if ($null -eq $property) {
        return $null
    }

    return $property.Value
}

function Get-ServiceState {
    param([switch]$AllowMissing)

    $serviceStateJson = @(& gcloud run services describe $ServiceName `
        --project=$ProjectId `
        --region=$Region `
        --format=json 2>$null)
    if ($LASTEXITCODE -ne 0) {
        if ($AllowMissing) {
            return $null
        }
        throw "Could not describe Cloud Run service: $ServiceName"
    }
    if ($serviceStateJson.Count -eq 0) {
        throw "Cloud Run service describe returned an empty response: $ServiceName"
    }

    return ($serviceStateJson -join "`n") | ConvertFrom-Json
}

function Get-ActiveTrafficAllocations {
    param([Parameter(Mandatory)][object]$ServiceState)

    $status = Get-JsonProperty -Object $ServiceState -Name "status"
    $latestReadyRevisionName = [string](Get-JsonProperty -Object $status -Name "latestReadyRevisionName")
    $trafficTargets = @(Get-JsonProperty -Object $status -Name "traffic")
    $allocations = @()

    foreach ($trafficTarget in $trafficTargets) {
        $percentValue = Get-JsonProperty -Object $trafficTarget -Name "percent"
        $percent = if ($null -eq $percentValue) { 0 } else { [int]$percentValue }
        if ($percent -le 0) {
            continue
        }

        $revisionName = [string](Get-JsonProperty -Object $trafficTarget -Name "revisionName")
        if ([string]::IsNullOrWhiteSpace($revisionName)) {
            $latestRevision = Get-JsonProperty -Object $trafficTarget -Name "latestRevision"
            if ($latestRevision -eq $true) {
                $revisionName = $latestReadyRevisionName
            }
        }
        if ([string]::IsNullOrWhiteSpace($revisionName)) {
            throw "Could not resolve the revision name for an active Cloud Run traffic target."
        }

        $allocations += [pscustomobject]@{
            revisionName = $revisionName
            percent      = $percent
        }
    }

    return $allocations
}

function Assert-RevisionReady {
    param([Parameter(Mandatory)][string]$RevisionName)

    $revisionStateJson = Get-GcloudText -Arguments @(
        "run", "revisions", "describe", $RevisionName,
        "--project=$ProjectId",
        "--region=$Region",
        "--format=json"
    )
    $revisionState = $revisionStateJson | ConvertFrom-Json
    $status = Get-JsonProperty -Object $revisionState -Name "status"
    $conditions = @(Get-JsonProperty -Object $status -Name "conditions")
    $readyConditions = @($conditions | Where-Object {
        (Get-JsonProperty -Object $_ -Name "type") -eq "Ready"
    })
    if ($readyConditions.Count -ne 1 -or
        (Get-JsonProperty -Object $readyConditions[0] -Name "status") -ne "True") {
        throw "Cloud Run candidate revision is not Ready: $RevisionName"
    }
}

function Wait-ForSingleRevisionTraffic {
    param(
        [Parameter(Mandatory)][string]$ExpectedRevision,
        [ValidateRange(1, 30)][int]$MaxAttempts = 15
    )

    $delaySeconds = 1
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $serviceState = Get-ServiceState
        $allocations = @(Get-ActiveTrafficAllocations -ServiceState $serviceState)
        if ($allocations.Count -eq 1 -and
            $allocations[0].revisionName -eq $ExpectedRevision -and
            [int]$allocations[0].percent -eq 100) {
            return
        }

        if ($attempt -eq $MaxAttempts) {
            $actualTraffic = if ($allocations.Count -eq 0) {
                "none"
            } else {
                ($allocations | ForEach-Object {
                    "$($_.revisionName)=$($_.percent)%"
                }) -join ","
            }
            throw "Cloud Run traffic did not converge to $ExpectedRevision=100% (actual=$actualTraffic)."
        }

        Write-Warning "Cloud Run traffic has not converged yet ($attempt/$MaxAttempts)."
        Start-Sleep -Seconds $delaySeconds
        $delaySeconds = [Math]::Min($delaySeconds * 2, 5)
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

function Get-WebOrigin {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value
    )

    try {
        $uri = [Uri]::new($Value.Trim(), [UriKind]::Absolute)
    } catch {
        throw "$Name contains an invalid Origin: $Value"
    }

    if ($uri.Scheme -ne "https" -or
        -not [string]::IsNullOrEmpty($uri.UserInfo) -or
        -not $uri.IsDefaultPort -or
        $uri.AbsolutePath -ne "/" -or
        -not [string]::IsNullOrEmpty($uri.Query) -or
        -not [string]::IsNullOrEmpty($uri.Fragment)) {
        throw "$Name must contain only HTTPS Origins without paths, query, fragment, credentials, or custom ports: $Value"
    }

    return $uri.GetLeftPart([UriPartial]::Authority)
}

function Get-AllowedOrigins {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value,
        [Parameter(Mandatory)][string]$RequiredOrigin
    )

    $origins = @($Value.Split(",") |
        ForEach-Object { $_.Trim() } |
        Where-Object { -not [string]::IsNullOrWhiteSpace($_) } |
        ForEach-Object { Get-WebOrigin -Name $Name -Value $_ } |
        Select-Object -Unique)

    if ($origins.Count -eq 0) {
        throw "$Name must contain at least one Origin."
    }
    if ($origins -notcontains $RequiredOrigin) {
        throw "$Name must include the public Origin ($RequiredOrigin)."
    }

    return $origins -join ","
}

function Get-HttpsUrl {
    param(
        [Parameter(Mandatory)][string]$Name,
        [Parameter(Mandatory)][string]$Value,
        [Parameter(Mandatory)][string]$ExpectedOrigin
    )

    $uri = [Uri]::new($Value, [UriKind]::Absolute)
    if ($uri.Scheme -ne "https" -or
        -not [string]::IsNullOrEmpty($uri.UserInfo) -or
        -not $uri.IsDefaultPort -or
        -not [string]::IsNullOrEmpty($uri.Fragment)) {
        throw "$Name must be an HTTPS URL without credentials, a custom port, or a fragment: $Value"
    }

    $actualOrigin = $uri.GetLeftPart([UriPartial]::Authority)
    if (-not [string]::Equals(
        $actualOrigin,
        $ExpectedOrigin,
        [StringComparison]::OrdinalIgnoreCase
    )) {
        throw "$Name must use the same origin as PublicBaseUrl ($ExpectedOrigin): $Value"
    }

    return $uri.AbsoluteUri
}

function Assert-HttpStatus {
    param(
        [Parameter(Mandatory)][string]$Uri,
        [Parameter(Mandatory)][int]$ExpectedStatus,
        [ValidateRange(1, 10)][int]$MaxAttempts = 5,
        [ValidateRange(1, 60)][int]$RequestTimeoutSeconds = 20,
        [ValidateRange(0, 60)][int]$InitialDelaySeconds = 2
    )

    $delaySeconds = $InitialDelaySeconds
    for ($attempt = 1; $attempt -le $MaxAttempts; $attempt++) {
        $webResponse = $null
        $actualStatus = $null
        $lastErrorMessage = $null

        try {
            $webResponse = Invoke-WebRequest `
                -UseBasicParsing `
                -Uri $Uri `
                -TimeoutSec $RequestTimeoutSeconds
            $actualStatus = [int]$webResponse.StatusCode
        } catch {
            $responseProperty = $_.Exception.PSObject.Properties["Response"]
            $errorResponse = if ($null -ne $responseProperty) {
                $responseProperty.Value
            } else {
                $null
            }
            if ($null -ne $errorResponse) {
                $actualStatus = [int]$errorResponse.StatusCode
            } else {
                $lastErrorMessage = $_.Exception.Message
            }
        }

        if ($actualStatus -eq $ExpectedStatus) {
            return $webResponse
        }

        if ($attempt -eq $MaxAttempts) {
            if ($null -ne $actualStatus) {
                throw "Endpoint verification failed after $MaxAttempts attempts: $Uri (expected=$ExpectedStatus, actual=$actualStatus)"
            }
            throw "Endpoint verification failed after $MaxAttempts attempts: $Uri (expected=$ExpectedStatus, error=$lastErrorMessage)"
        }

        $failureReason = if ($null -ne $actualStatus) {
            "expected=$ExpectedStatus, actual=$actualStatus"
        } else {
            "error=$lastErrorMessage"
        }
        Write-Warning "Endpoint verification attempt $attempt/$MaxAttempts failed: $Uri ($failureReason). Retrying in $delaySeconds seconds."
        Start-Sleep -Seconds $delaySeconds
        $delaySeconds = [Math]::Min($delaySeconds * 2, 10)
    }
}

function Assert-ProdEndpoints {
    param([Parameter(Mandatory)][string]$BaseUrl)

    foreach ($healthPath in @(
        "/actuator/health/liveness",
        "/actuator/health/readiness",
        "/actuator/health"
    )) {
        Assert-HttpStatus -Uri "$BaseUrl$healthPath" -ExpectedStatus 200 | Out-Null
    }

    foreach ($documentationPath in @(
        "/swagger-ui/index.html",
        "/v3/api-docs"
    )) {
        Assert-HttpStatus -Uri "$BaseUrl$documentationPath" -ExpectedStatus 404 | Out-Null
    }
}

function Write-EnvironmentFile {
    param(
        [Parameter(Mandatory)][string]$Path,
        [Parameter(Mandatory)][string]$PublicOrigin,
        [Parameter(Mandatory)][string]$FrontendRedirectUri,
        [Parameter(Mandatory)][string]$CorsAllowedOrigins,
        [Parameter(Mandatory)][string]$OAuthAllowedFrontendOrigins,
        [Parameter(Mandatory)][string]$ProfileImageBucket
    )

    $lines = @(
        "SPRING_PROFILES_ACTIVE: 'prod'",
        "PUBLIC_BASE_URL: $(ConvertTo-YamlSingleQuoted $PublicOrigin)",
        "CORS_ALLOWED_ORIGINS: $(ConvertTo-YamlSingleQuoted $CorsAllowedOrigins)",
        "OAUTH_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted $FrontendRedirectUri)",
        "OAUTH_ALLOWED_FRONTEND_ORIGINS: $(ConvertTo-YamlSingleQuoted $OAuthAllowedFrontendOrigins)",
        "KAKAO_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$PublicOrigin/oauth/callback/kakao")",
        "GOOGLE_REDIRECT_URI: $(ConvertTo-YamlSingleQuoted "$PublicOrigin/oauth/callback/google")",
        "PROFILE_IMAGE_BUCKET: $(ConvertTo-YamlSingleQuoted $ProfileImageBucket)",
        "PROFILE_IMAGE_PUBLIC_BASE_URL: 'https://storage.googleapis.com'"
    )

    [System.IO.File]::WriteAllLines(
        $Path,
        $lines,
        [System.Text.UTF8Encoding]::new($false)
    )
}

function Write-ResultFile {
    if ([string]::IsNullOrWhiteSpace($ResultPath)) {
        return
    }

    $resultDirectory = Split-Path -Parent $ResultPath
    if (-not [string]::IsNullOrWhiteSpace($resultDirectory)) {
        [System.IO.Directory]::CreateDirectory($resultDirectory) | Out-Null
    }
    $resultJson = $result | ConvertTo-Json -Depth 4
    [System.IO.File]::WriteAllText(
        $ResultPath,
        $resultJson,
        [System.Text.UTF8Encoding]::new($false)
    )
}

try {
    if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
        throw "gcloud CLI was not found. Check the Google Cloud CLI installation and login."
    }

    $publicOrigin = Get-HttpsOrigin -Value $PublicBaseUrl
    if ([string]::IsNullOrWhiteSpace($FrontendRedirectUri)) {
        $FrontendRedirectUri = "$publicOrigin/app/oauth/callback"
    }
    $frontendRedirectUrl = Get-HttpsUrl `
        -Name "FrontendRedirectUri" `
        -Value $FrontendRedirectUri `
        -ExpectedOrigin $publicOrigin

    if ([string]::IsNullOrWhiteSpace($CorsAllowedOrigins)) {
        $CorsAllowedOrigins = $publicOrigin
    }
    $corsOrigins = Get-AllowedOrigins `
        -Name "CorsAllowedOrigins" `
        -Value $CorsAllowedOrigins `
        -RequiredOrigin $publicOrigin

    if ([string]::IsNullOrWhiteSpace($OAuthAllowedFrontendOrigins)) {
        $OAuthAllowedFrontendOrigins = $publicOrigin
    }
    $oauthFrontendOrigins = Get-AllowedOrigins `
        -Name "OAuthAllowedFrontendOrigins" `
        -Value $OAuthAllowedFrontendOrigins `
        -RequiredOrigin $publicOrigin

    Invoke-Gcloud -Arguments @(
        "iam", "service-accounts", "describe", $runtimeServiceAccount,
        "--project=$ProjectId",
        "--quiet"
    )
    Invoke-Gcloud -Arguments @(
        "compute", "networks", "describe", $VpcNetwork,
        "--project=$ProjectId",
        "--quiet"
    )
    Invoke-Gcloud -Arguments @(
        "compute", "networks", "subnets", "describe", $VpcSubnet,
        "--project=$ProjectId",
        "--region=$Region",
        "--quiet"
    )
    Invoke-Gcloud -Arguments @(
        "storage", "buckets", "describe", "gs://$ProfileImageBucket",
        "--project=$ProjectId",
        "--quiet"
    )

    foreach ($entry in $secretMap.GetEnumerator()) {
        $versionStates = @(Get-GcloudText -Arguments @(
            "secrets", "versions", "list", $entry.Value,
            "--project=$ProjectId",
            "--format=value(state)"
        ) -split "`n")
        if ($versionStates -notcontains "ENABLED") {
            throw "Secret has no enabled version: $($entry.Value) ($($entry.Key))"
        }
    }

    $serviceState = Get-ServiceState -AllowMissing
    if ($null -ne $serviceState) {
        $activeTraffic = @(Get-ActiveTrafficAllocations -ServiceState $serviceState)
        if ($activeTraffic.Count -ne 1 -or [int]$activeTraffic[0].percent -ne 100) {
            throw "Existing Cloud Run service must have exactly one revision serving 100% traffic."
        }
        $result.previousRevision = [string]$activeTraffic[0].revisionName
    }

    $environmentFile = New-TemporaryFile
    Write-EnvironmentFile `
        -Path $environmentFile.FullName `
        -PublicOrigin $publicOrigin `
        -FrontendRedirectUri $frontendRedirectUrl `
        -CorsAllowedOrigins $corsOrigins `
        -OAuthAllowedFrontendOrigins $oauthFrontendOrigins `
        -ProfileImageBucket $ProfileImageBucket

    $secretBindings = ($secretMap.GetEnumerator() | ForEach-Object {
        "$($_.Key)=$($_.Value):latest"
    }) -join ","

    $shortCommit = $DeployCommit.Substring(0, 12)
    $deploymentTimestamp = Get-Date -Format "yyyyMMddHHmmss"
    $revisionSuffix = "$shortCommit-$deploymentTimestamp"
    $result.candidateRevision = "$ServiceName-$revisionSuffix"

    $deployArguments = @(
        "run", "deploy", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--image=$Image",
        "--revision-suffix=$revisionSuffix",
        "--no-traffic",
        "--service-account=$runtimeServiceAccount",
        "--execution-environment=gen2",
        "--port=8080",
        "--cpu=1",
        "--memory=1Gi",
        "--concurrency=40",
        "--timeout=60",
        "--min=0",
        "--max=3",
        "--ingress=all",
        "--allow-unauthenticated",
        "--cpu-throttling",
        "--cpu-boost",
        "--network=$VpcNetwork",
        "--subnet=$VpcSubnet",
        "--vpc-egress=private-ranges-only",
        "--deploy-health-check",
        "--startup-probe=httpGet.path=/actuator/health/liveness,httpGet.port=8080,initialDelaySeconds=0,timeoutSeconds=3,periodSeconds=5,failureThreshold=24",
        "--liveness-probe=httpGet.path=/actuator/health/liveness,httpGet.port=8080,initialDelaySeconds=0,timeoutSeconds=3,periodSeconds=10,failureThreshold=3",
        "--readiness-probe=httpGet.path=/actuator/health/readiness,httpGet.port=8080,timeoutSeconds=3,periodSeconds=5,failureThreshold=3",
        "--env-vars-file=$($environmentFile.FullName)",
        "--set-secrets=$secretBindings",
        "--quiet"
    )
    Invoke-Gcloud -Arguments $deployArguments

    Assert-RevisionReady -RevisionName $result.candidateRevision
    $result.candidateVerification = "passed"

    $trafficPromotionAttempted = $true
    Invoke-Gcloud -Arguments @(
        "run", "services", "update-traffic", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--to-revisions=$($result.candidateRevision)=100",
        "--quiet"
    )
    Wait-ForSingleRevisionTraffic -ExpectedRevision $result.candidateRevision
    $result.trafficPromotion = "passed"

    $serviceUrl = Get-GcloudText -Arguments @(
        "run", "services", "describe", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--format=value(status.url)"
    )
    if ([string]::IsNullOrWhiteSpace($serviceUrl)) {
        throw "Could not read the Cloud Run service URL after traffic promotion."
    }
    $result.serviceUrl = $serviceUrl.TrimEnd('/')

    Assert-ProdEndpoints -BaseUrl $result.serviceUrl
    $result.serviceVerification = "passed"

    $result.status = "succeeded"

    Write-Output "Cloud Run prod deployment and verification completed: $($result.candidateRevision)"
} catch {
    $result.error = $_.Exception.Message

    $shouldRollback = $false
    if ($trafficPromotionAttempted) {
        try {
            $failedServiceState = Get-ServiceState
            $failedTraffic = @(Get-ActiveTrafficAllocations -ServiceState $failedServiceState)
            $shouldRollback = @($failedTraffic | Where-Object {
                $_.revisionName -eq $result.candidateRevision -and [int]$_.percent -gt 0
            }).Count -gt 0
        } catch {
            Write-Warning "Could not inspect traffic after deployment failure; rollback will be attempted conservatively."
            $shouldRollback = $true
        }
    }

    if ($shouldRollback) {
        if (-not [string]::IsNullOrWhiteSpace($result.previousRevision)) {
            try {
                Invoke-Gcloud -Arguments @(
                    "run", "services", "update-traffic", $ServiceName,
                    "--project=$ProjectId",
                    "--region=$Region",
                    "--to-revisions=$($result.previousRevision)=100",
                    "--quiet"
                )
                Wait-ForSingleRevisionTraffic -ExpectedRevision $result.previousRevision
                if (-not [string]::IsNullOrWhiteSpace($result.serviceUrl)) {
                    Assert-ProdEndpoints -BaseUrl $result.serviceUrl
                }
                $result.rollback = "succeeded"
            } catch {
                $result.rollback = "failed: $($_.Exception.Message)"
            }
        } else {
            $result.rollback = "unavailable-first-deployment"
        }
    }

    throw
} finally {
    if ($null -ne $environmentFile) {
        Remove-Item -LiteralPath $environmentFile.FullName -Force -ErrorAction SilentlyContinue
    }
    Write-ResultFile
}