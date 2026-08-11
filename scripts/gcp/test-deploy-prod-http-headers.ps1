$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Net.Http

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

$headerFunction = $scriptAst.Find({
    param($node)
    $node -is [System.Management.Automation.Language.FunctionDefinitionAst] -and
        $node.Name -eq "Get-HttpHeaderValues"
}, $true)
if ($null -eq $headerFunction) {
    throw "Get-HttpHeaderValues was not found in deploy-prod.ps1."
}
Invoke-Expression $headerFunction.Extent.Text

$httpResponse = [System.Net.Http.HttpResponseMessage]::new()
try {
    $httpResponse.Headers.TryAddWithoutValidation(
        "Set-Cookie",
        "XSRF-TOKEN=test; Path=/"
    ) | Out-Null
    $httpResponse.Headers.Location = [Uri]"https://accounts.google.com/o/oauth2/v2/auth"

    $cookieValues = @(Get-HttpHeaderValues -Response $httpResponse -Name "Set-Cookie")
    if (($cookieValues -join ";") -notmatch "XSRF-TOKEN=test") {
        throw "Set-Cookie was not read from HttpResponseHeaders."
    }

    $locationValues = @(Get-HttpHeaderValues -Response $httpResponse -Name "Location")
    if ($locationValues[0] -ne "https://accounts.google.com/o/oauth2/v2/auth") {
        throw "Location was not read from HttpResponseHeaders."
    }

    $missingValues = @(Get-HttpHeaderValues -Response $httpResponse -Name "Missing")
    if ($missingValues.Count -ne 0) {
        throw "A missing HttpResponseHeaders value must return an empty result."
    }
} finally {
    $httpResponse.Dispose()
}

$fallbackResponse = [pscustomobject]@{
    Headers = @{
        Location = "https://example.com/"
    }
}
$fallbackValues = @(Get-HttpHeaderValues -Response $fallbackResponse -Name "Location")
if ($fallbackValues[0] -ne "https://example.com/") {
    throw "Dictionary-style response headers were not read."
}

Write-Output "Prod deployment HTTP header tests passed."
