[CmdletBinding()]
param(
  [string[]]$PackageName = @('com.clearscreen.prototype', 'hello.litiaotiao.app'),
  [string]$OutputDirectory = ''
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
if ([string]::IsNullOrWhiteSpace($OutputDirectory)) {
  $OutputDirectory = Join-Path $repoRoot 'diagnostics'
}

$adbCandidates = @()
if ($env:LOCALAPPDATA) { $adbCandidates += Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools\adb.exe' }
if ($env:ANDROID_HOME) { $adbCandidates += Join-Path $env:ANDROID_HOME 'platform-tools\adb.exe' }
if ($env:ANDROID_SDK_ROOT) { $adbCandidates += Join-Path $env:ANDROID_SDK_ROOT 'platform-tools\adb.exe' }
$adbCommand = Get-Command adb.exe -ErrorAction SilentlyContinue
$adb = if ($adbCommand) { $adbCommand.Source } else { $adbCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1 }
if (-not $adb) {
  throw 'adb.exe was not found.'
}

New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'
$runDirectory = Join-Path $OutputDirectory "$stamp-package-compare"
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

foreach ($package in $PackageName) {
  $safeName = $package.Replace('.', '-')
  & $adb shell dumpsys package $package 2>&1 |
    Set-Content -LiteralPath (Join-Path $runDirectory "$safeName-dumpsys-package.txt") -Encoding UTF8
  & $adb shell pm path $package 2>&1 |
    Set-Content -LiteralPath (Join-Path $runDirectory "$safeName-path.txt") -Encoding UTF8
}

& $adb shell settings --user 0 get secure enabled_accessibility_services 2>&1 |
  Set-Content -LiteralPath (Join-Path $runDirectory 'secure-enabled-accessibility-services.txt') -Encoding UTF8
Write-Host "Package comparison saved to $runDirectory"
