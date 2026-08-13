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

$deployScriptContent = Get-Content -LiteralPath $deployScriptPath -Raw
if ($deployScriptContent -notmatch '\$csrfCode\s+-ne\s+"AUTH_CSRF_TOKEN_ISSUED_SUCCESS"') {
    throw "Prod CSRF smoke check must expect AUTH_CSRF_TOKEN_ISSUED_SUCCESS."
}

foreach ($functionName in @(
    "Get-HttpHeaderValues",
    "Test-HttpHeaderContainsToken"
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

$httpResponse = [System.Net.Http.HttpResponseMessage]::new()
try {
    $httpResponse.Headers.TryAddWithoutValidation(
        "Set-Cookie",
        "XSRF-TOKEN=test; Path=/"
    ) | Out-Null
    $httpResponse.Headers.Location = [Uri]"https://accounts.google.com/o/oauth2/v2/auth"
    $httpResponse.Headers.TryAddWithoutValidation(
        "Access-Control-Allow-Methods",
        "POST, GET"
    ) | Out-Null
    $httpResponse.Headers.TryAddWithoutValidation(
        "Access-Control-Allow-Headers",
        "X-XSRF-TOKEN, Content-Type"
    ) | Out-Null

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

    if (-not (Test-HttpHeaderContainsToken `
        -Response $httpResponse `
        -Name "Access-Control-Allow-Methods" `
        -ExpectedToken "GET")) {
        throw "GET was not found in Access-Control-Allow-Methods."
    }
    if (-not (Test-HttpHeaderContainsToken `
        -Response $httpResponse `
        -Name "Access-Control-Allow-Headers" `
        -ExpectedToken "content-type")) {
        throw "content-type was not found case-insensitively in Access-Control-Allow-Headers."
    }
    if (Test-HttpHeaderContainsToken `
        -Response $httpResponse `
        -Name "Access-Control-Allow-Methods" `
        -ExpectedToken "DELETE") {
        throw "A missing header token must not be reported as present."
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
