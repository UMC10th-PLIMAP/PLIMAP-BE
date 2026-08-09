[CmdletBinding()]
param(
    [ValidatePattern("^[a-z][a-z0-9-]{4,28}[a-z0-9]$")]
    [string]$ProjectId = "plimap",
    [ValidatePattern("^[a-z]+-[a-z]+[0-9]$")]
    [string]$Region = "asia-northeast3",
    [ValidatePattern("^[a-z](?:[a-z0-9-]{0,47}[a-z0-9])?$")]
    [string]$ServiceName = "plimap-api-prod",
    [Parameter(Mandatory)]
    [ValidatePattern("^[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?$")]
    [string]$VpcNetwork,
    [Parameter(Mandatory)]
    [ValidatePattern("^[a-z](?:[a-z0-9-]{0,61}[a-z0-9])?$")]
    [string]$VpcSubnet,
    [string]$DeployerServiceAccount = "",
    [ValidatePattern("^[a-z0-9.-]+-docker\.pkg\.dev/[a-z0-9._/-]+(?::[a-zA-Z0-9._-]+)?$")]
    [string]$BootstrapImage = "us-docker.pkg.dev/cloudrun/container/hello:latest",
    [switch]$Apply
)

Set-StrictMode -Version Latest
$ErrorActionPreference = "Stop"

$runtimeServiceAccount = "plimap-api-prod@$ProjectId.iam.gserviceaccount.com"
if ([string]::IsNullOrWhiteSpace($DeployerServiceAccount)) {
    $DeployerServiceAccount = "plimap-github-prod-deployer@$ProjectId.iam.gserviceaccount.com"
}
$deployerPattern = "^[a-z][a-z0-9-]{4,28}[a-z0-9]@$([regex]::Escape($ProjectId))\.iam\.gserviceaccount\.com$"
if ($DeployerServiceAccount -notmatch $deployerPattern) {
    throw "DeployerServiceAccount must be a service account in project $ProjectId."
}

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

    if ($AllowMissing) {
        $serviceNames = Get-GcloudText -Arguments @(
            "run", "services", "list",
            "--project=$ProjectId",
            "--region=$Region",
            "--filter=metadata.name=$ServiceName",
            "--format=value(metadata.name)"
        )
        $matches = @($serviceNames -split "\r?\n" | Where-Object {
            -not [string]::IsNullOrWhiteSpace($_)
        })
        if ($matches.Count -eq 0) {
            return $null
        }
        if ($matches.Count -ne 1 -or $matches[0] -ne $ServiceName) {
            throw "Could not uniquely resolve the Prod Cloud Run bootstrap service."
        }
    }

    $serviceStateJson = @(& gcloud run services describe $ServiceName `
        --project=$ProjectId `
        --region=$Region `
        --format=json 2>$null)
    if ($LASTEXITCODE -ne 0) {
        throw "Could not describe Cloud Run service: $ServiceName"
    }
    if ($serviceStateJson.Count -eq 0) {
        throw "Cloud Run service describe returned an empty response: $ServiceName"
    }
    return ($serviceStateJson -join "`n") | ConvertFrom-Json
}

function Assert-BootstrapServiceState {
    param([Parameter(Mandatory)][object]$ServiceState)

    $metadata = Get-JsonProperty -Object $ServiceState -Name "metadata"
    $labels = Get-JsonProperty -Object $metadata -Name "labels"
    $bootstrapLabel = [string](Get-JsonProperty -Object $labels -Name "plimap-bootstrap")
    if ($bootstrapLabel -ne "prod") {
        throw "Existing Cloud Run service is not the approved Prod bootstrap service."
    }

    $serviceAnnotations = Get-JsonProperty -Object $metadata -Name "annotations"
    $defaultUrlDisabled = [string](Get-JsonProperty `
        -Object $serviceAnnotations `
        -Name "run.googleapis.com/default-url-disabled")
    if ($defaultUrlDisabled -ne "true") {
        throw "Prod bootstrap service must have the default URL disabled."
    }
    $ingress = [string](Get-JsonProperty `
        -Object $serviceAnnotations `
        -Name "run.googleapis.com/ingress")
    if ($ingress -ne "internal-and-cloud-load-balancing") {
        throw "Prod bootstrap service ingress differs from the approved state."
    }

    $serviceSpec = Get-JsonProperty -Object $ServiceState -Name "spec"
    $template = Get-JsonProperty -Object $serviceSpec -Name "template"
    $templateMetadata = Get-JsonProperty -Object $template -Name "metadata"
    $templateAnnotations = Get-JsonProperty `
        -Object $templateMetadata `
        -Name "annotations"
    $executionEnvironment = [string](Get-JsonProperty `
        -Object $templateAnnotations `
        -Name "run.googleapis.com/execution-environment")
    if ($executionEnvironment -ne "gen2") {
        throw "Prod bootstrap service must use the Gen2 execution environment."
    }
    $maxScale = [string](Get-JsonProperty `
        -Object $serviceAnnotations `
        -Name "run.googleapis.com/maxScale")
    if ([string]::IsNullOrWhiteSpace($maxScale)) {
        $maxScale = [string](Get-JsonProperty `
            -Object $templateAnnotations `
            -Name "autoscaling.knative.dev/maxScale")
    }
    if ($maxScale -ne "1") {
        throw "Prod bootstrap service max scale differs from the approved state."
    }
    $minScale = [string](Get-JsonProperty `
        -Object $serviceAnnotations `
        -Name "run.googleapis.com/minScale")
    if ([string]::IsNullOrWhiteSpace($minScale)) {
        $minScale = [string](Get-JsonProperty `
            -Object $templateAnnotations `
            -Name "autoscaling.knative.dev/minScale")
    }
    if (-not [string]::IsNullOrWhiteSpace($minScale) -and $minScale -ne "0") {
        throw "Prod bootstrap service min scale differs from the approved state."
    }
    $vpcEgress = [string](Get-JsonProperty `
        -Object $templateAnnotations `
        -Name "run.googleapis.com/vpc-access-egress")
    if ($vpcEgress -ne "private-ranges-only") {
        throw "Prod bootstrap service VPC egress differs from the approved state."
    }

    $networkInterfacesJson = [string](Get-JsonProperty `
        -Object $templateAnnotations `
        -Name "run.googleapis.com/network-interfaces")
    if ([string]::IsNullOrWhiteSpace($networkInterfacesJson)) {
        throw "Prod bootstrap service network interface metadata is missing."
    }
    try {
        $parsedNetworkInterfaces = $networkInterfacesJson | ConvertFrom-Json
    } catch {
        throw "Prod bootstrap service network interface metadata is invalid."
    }
    $networkInterfaces = @(
        $parsedNetworkInterfaces | ForEach-Object { $_ }
    )
    if ($networkInterfaces.Count -ne 1) {
        throw "Prod bootstrap service must use exactly one network interface."
    }
    $actualNetwork = [string](Get-JsonProperty `
        -Object $networkInterfaces[0] `
        -Name "network")
    $actualSubnet = [string](Get-JsonProperty `
        -Object $networkInterfaces[0] `
        -Name "subnetwork")
    $networkTags = [string](Get-JsonProperty `
        -Object $networkInterfaces[0] `
        -Name "tags")
    if (($actualNetwork -split "/")[-1] -ne $VpcNetwork -or
        ($actualSubnet -split "/")[-1] -ne $VpcSubnet -or
        -not [string]::IsNullOrWhiteSpace($networkTags)) {
        throw "Prod bootstrap service network interface differs from the approved state."
    }

    $templateSpec = Get-JsonProperty -Object $template -Name "spec"
    $actualServiceAccount = [string](Get-JsonProperty `
        -Object $templateSpec `
        -Name "serviceAccountName")
    $containerConcurrency = Get-JsonProperty `
        -Object $templateSpec `
        -Name "containerConcurrency"
    $timeoutSeconds = Get-JsonProperty `
        -Object $templateSpec `
        -Name "timeoutSeconds"
    if ($actualServiceAccount -ne $runtimeServiceAccount -or
        [int]$containerConcurrency -ne 1 -or
        [int]$timeoutSeconds -ne 10) {
        throw "Prod bootstrap service runtime identity or request limits differ from the approved state."
    }

    $containers = @(Get-JsonProperty -Object $templateSpec -Name "containers")
    if ($containers.Count -ne 1) {
        throw "Prod bootstrap service must contain exactly one container."
    }
    $container = $containers[0]
    $actualImage = [string](Get-JsonProperty -Object $container -Name "image")
    $ports = @(Get-JsonProperty -Object $container -Name "ports")
    $resources = Get-JsonProperty -Object $container -Name "resources"
    $limits = Get-JsonProperty -Object $resources -Name "limits"
    $cpu = [string](Get-JsonProperty -Object $limits -Name "cpu")
    $memory = [string](Get-JsonProperty -Object $limits -Name "memory")
    $environmentEntries = Get-JsonProperty -Object $container -Name "env"
    $volumes = Get-JsonProperty -Object $templateSpec -Name "volumes"
    if ($actualImage -ne $BootstrapImage -or
        $ports.Count -ne 1 -or
        [int](Get-JsonProperty -Object $ports[0] -Name "containerPort") -ne 8080 -or
        $cpu -ne "1" -or
        $memory -ne "512Mi" -or
        ($null -ne $environmentEntries -and @($environmentEntries).Count -ne 0) -or
        ($null -ne $volumes -and @($volumes).Count -ne 0)) {
        throw "Prod bootstrap service container differs from the approved state."
    }

    $status = Get-JsonProperty -Object $ServiceState -Name "status"
    $serviceUrl = [string](Get-JsonProperty -Object $status -Name "url")
    if (-not [string]::IsNullOrWhiteSpace($serviceUrl) -and $serviceUrl -ne "None") {
        throw "Prod bootstrap service must keep the default URL disabled before the first application deployment."
    }

    $latestReadyRevision = [string](Get-JsonProperty `
        -Object $status `
        -Name "latestReadyRevisionName")
    $trafficTargets = @(Get-JsonProperty -Object $status -Name "traffic")
    $activeTraffic = @($trafficTargets | Where-Object {
        $percent = Get-JsonProperty -Object $_ -Name "percent"
        $null -ne $percent -and [int]$percent -gt 0
    })
    $bootstrapRevision = if ($activeTraffic.Count -eq 1) {
        [string](Get-JsonProperty -Object $activeTraffic[0] -Name "revisionName")
    } else {
        ""
    }
    if ($activeTraffic.Count -ne 1 -or
        [int](Get-JsonProperty -Object $activeTraffic[0] -Name "percent") -ne 100 -or
        $bootstrapRevision -ne $latestReadyRevision) {
        throw "Prod bootstrap sample revision must be the only 100% traffic target."
    }
}

function Get-ServiceIamPolicy {
    $policyJson = Get-GcloudText -Arguments @(
        "run", "services", "get-iam-policy", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--format=json"
    )
    return $policyJson | ConvertFrom-Json
}

function Test-IamBinding {
    param(
        [Parameter(Mandatory)][object]$Policy,
        [Parameter(Mandatory)][string]$Role,
        [Parameter(Mandatory)][string]$Member
    )

    $bindings = @(Get-JsonProperty -Object $Policy -Name "bindings")
    return @($bindings | Where-Object {
        [string](Get-JsonProperty -Object $_ -Name "role") -eq $Role -and
        @(Get-JsonProperty -Object $_ -Name "members") -contains $Member
    }).Count -eq 1
}

if (-not (Get-Command gcloud -ErrorAction SilentlyContinue)) {
    throw "gcloud CLI was not found. Check the Google Cloud CLI installation and login."
}

Invoke-Gcloud -Arguments @(
    "iam", "service-accounts", "describe", $runtimeServiceAccount,
    "--project=$ProjectId",
    "--quiet"
)
Invoke-Gcloud -Arguments @(
    "iam", "service-accounts", "describe", $DeployerServiceAccount,
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

$serviceState = Get-ServiceState -AllowMissing
if ($null -ne $serviceState) {
    Assert-BootstrapServiceState -ServiceState $serviceState
}

if (-not $Apply) {
    $serviceStatus = if ($null -eq $serviceState) { "missing" } else { "approved-bootstrap" }
    Write-Output "Plan only: service=$ServiceName status=$serviceStatus"
    Write-Output "Apply will create only an unreachable sample bootstrap revision and service-level IAM."
    return
}

if ($null -eq $serviceState) {
    Invoke-Gcloud -Arguments @(
        "run", "deploy", $ServiceName,
        "--project=$ProjectId",
        "--region=$Region",
        "--image=$BootstrapImage",
        "--revision-suffix=bootstrap",

        "--no-default-url",
        "--no-allow-unauthenticated",
        "--service-account=$runtimeServiceAccount",
        "--execution-environment=gen2",
        "--port=8080",
        "--cpu=1",
        "--memory=512Mi",
        "--concurrency=1",
        "--timeout=10",
        "--min=0",
        "--max=1",
        "--ingress=internal-and-cloud-load-balancing",
        "--cpu-throttling",
        "--no-cpu-boost",
        "--network=$VpcNetwork",
        "--subnet=$VpcSubnet",
        "--vpc-egress=private-ranges-only",
        "--deploy-health-check",
        "--labels=plimap-bootstrap=prod",
        "--quiet"
    )
    $serviceState = Get-ServiceState
    Assert-BootstrapServiceState -ServiceState $serviceState
}

Invoke-Gcloud -Arguments @(
    "run", "services", "add-iam-policy-binding", $ServiceName,
    "--project=$ProjectId",
    "--region=$Region",
    "--member=allUsers",
    "--role=roles/run.invoker",
    "--quiet"
)
Invoke-Gcloud -Arguments @(
    "run", "services", "add-iam-policy-binding", $ServiceName,
    "--project=$ProjectId",
    "--region=$Region",
    "--member=serviceAccount:$DeployerServiceAccount",
    "--role=roles/run.developer",
    "--quiet"
)

$finalState = Get-ServiceState
Assert-BootstrapServiceState -ServiceState $finalState
$iamPolicy = Get-ServiceIamPolicy
$publicInvokerExists = Test-IamBinding `
    -Policy $iamPolicy `
    -Role "roles/run.invoker" `
    -Member "allUsers"
if (-not $publicInvokerExists) {
    throw "Prod Cloud Run service is missing allUsers roles/run.invoker."
}
$deployerBindingExists = Test-IamBinding `
    -Policy $iamPolicy `
    -Role "roles/run.developer" `
    -Member "serviceAccount:$DeployerServiceAccount"
if (-not $deployerBindingExists) {
    throw "Prod deployer is missing service-level roles/run.developer."
}

Write-Output "Prod Cloud Run bootstrap completed with the default URL disabled: $ServiceName"
