[CmdletBinding()]
param(
  [int]$TargetSdk = 36,
  [string]$ApplicationId = 'com.clearscreen.app'
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $repoRoot 'android'
$gradle = Join-Path $androidRoot 'gradlew.bat'
$keystoreProperties = Join-Path $androidRoot 'keystore.properties'

if (-not (Test-Path -LiteralPath $keystoreProperties)) {
  throw "Missing $keystoreProperties. Run tools/New-ClearScreenSigning.ps1 first."
}

Push-Location $androidRoot
try {
  & $gradle `
    ':app:assembleDiagnostic' `
    ':app:assembleLttCompat' `
    ':probe:assembleRelease' `
    "-PclearscreenApplicationId=$ApplicationId" `
    "-PclearscreenTargetSdk=$TargetSdk" `
    "-PprobeTargetSdk=$TargetSdk" `
    '--no-daemon'
  if ($LASTEXITCODE -ne 0) {
    throw "Gradle diagnostics build failed with exit code $LASTEXITCODE."
  }
} finally {
  Pop-Location
}

Write-Host 'Diagnostic APKs:'
Get-ChildItem -LiteralPath (Join-Path $androidRoot 'app\build\outputs\apk') -Recurse -Filter '*.apk' |
  Where-Object { $_.FullName -match 'diagnostic|lttCompat' } |
  Select-Object -ExpandProperty FullName
Get-ChildItem -LiteralPath (Join-Path $androidRoot 'probe\build\outputs\apk') -Recurse -Filter '*.apk' |
  Select-Object -ExpandProperty FullName
