$ErrorActionPreference = "Stop"

$deployScriptPath = Join-Path $PSScriptRoot "deploy-prod.ps1"
$tokens = $null
$parseErrors = $null
$scriptAst = [System.Management.Automation.Language.Parser]::ParseFile(
    $deployScriptPath,
    [ref]$tokens,
    [ref]$parseErrors
)
if ($parseErrors.Count -gt 0) {
    throw "deploy-prod.ps1 contains PowerShell parse errors."
}

foreach ($functionName in @(
    "Test-PrivateNetworkIpv4",
    "Get-WebOrigin",
    "Get-AllowedOrigins",
    "Get-ProdAllowedOrigins"
)) {
    $function = $scriptAst.Find({
        param($node)
        $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
            $node.Name -eq $functionName
    }, $true)
    if ($null -eq $function) {
        throw "$functionName was not found in deploy-prod.ps1."
    }
    Invoke-Expression $function.Extent.Text
}

$publicOrigin = "https://plimap.kr"
$adminOrigin = "https://admin.plimap.kr"

$defaultOrigins = Get-ProdAllowedOrigins `
    -Name "CorsAllowedOrigins" `
    -Value "" `
    -PublicOrigin $publicOrigin `
    -AdminFrontendOrigin $adminOrigin
if ($defaultOrigins -ne "$publicOrigin,$adminOrigin") {
    throw "Prod defaults must include the public and Admin Origins."
}

$explicitOrigins = Get-ProdAllowedOrigins `
    -Name "CorsAllowedOrigins" `
    -Value "$publicOrigin,https://ops.plimap.kr" `
    -PublicOrigin $publicOrigin `
    -AdminFrontendOrigin $adminOrigin
if ($explicitOrigins -ne "$publicOrigin,https://ops.plimap.kr,$adminOrigin") {
    throw "Explicit Prod Origins must be preserved while adding the Admin Origin."
}

$duplicateOrigins = Get-ProdAllowedOrigins `
    -Name "OAuthAllowedFrontendOrigins" `
    -Value "$publicOrigin,$adminOrigin" `
    -PublicOrigin $publicOrigin `
    -AdminFrontendOrigin $adminOrigin
if ($duplicateOrigins -ne "$publicOrigin,$adminOrigin") {
    throw "Required Prod Origins must not be duplicated."
}

$invalidAdminOriginRejected = $false
try {
    Get-ProdAllowedOrigins `
        -Name "OAuthAllowedFrontendOrigins" `
        -Value $publicOrigin `
        -PublicOrigin $publicOrigin `
        -AdminFrontendOrigin "https://admin.plimap.kr/path" | Out-Null
} catch {
    $invalidAdminOriginRejected = $true
}
if (-not $invalidAdminOriginRejected) {
    throw "An Admin Origin containing a path must be rejected."
}

Write-Output "Prod Admin Origin tests passed."