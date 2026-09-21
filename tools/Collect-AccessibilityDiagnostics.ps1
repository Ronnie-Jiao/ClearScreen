[CmdletBinding()]
param(
  [string]$PackageName = 'com.clearscreen.prototype',
  [string]$OutputDirectory = '',
  [switch]$ClearLogcat
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
$runDirectory = Join-Path $OutputDirectory "$stamp-$($PackageName.Replace('.', '-'))"
New-Item -ItemType Directory -Path $runDirectory -Force | Out-Null

function Save-AdbOutput {
  param(
    [string]$Name,
    [Parameter(Mandatory = $true)][string[]]$Arguments
  )
  $path = Join-Path $runDirectory "$Name.txt"
  @("# adb $($Arguments -join ' ')", "# collected $(Get-Date -Format o)", '') |
    Set-Content -LiteralPath $path -Encoding UTF8
  & $adb @Arguments 2>&1 | Add-Content -LiteralPath $path -Encoding UTF8
}

if ($ClearLogcat) {
  & $adb logcat -c
  if ($LASTEXITCODE -ne 0) { throw 'Unable to clear logcat.' }
  Write-Host 'Logcat cleared. On the phone, enable the target accessibility service, return to the list, then press Enter here.'
  [void](Read-Host)
}

Save-AdbOutput 'devices' @('devices', '-l')
Save-AdbOutput 'secure-enabled-accessibility-services' @('shell', 'settings', '--user', '0', 'get', 'secure', 'enabled_accessibility_services')
Save-AdbOutput 'package' @('shell', 'dumpsys', 'package', $PackageName)
Save-AdbOutput 'accessibility' @('shell', 'dumpsys', 'accessibility')
Save-AdbOutput 'package-path' @('shell', 'pm', 'path', $PackageName)
Save-AdbOutput 'logcat' @('logcat', '-d', '-v', 'threadtime')

$metadata = [ordered]@{
  collectedAt = (Get-Date).ToString('o')
  packageName = $PackageName
  outputDirectory = $runDirectory
  adb = $adb
}
$metadata | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $runDirectory 'metadata.json') -Encoding UTF8
Write-Host "Diagnostics saved to $runDirectory"
