[CmdletBinding()]
param(
  [string]$Alias = 'clearscreen',
  [int]$ValidityDays = 10000,
  [switch]$Force
)

$ErrorActionPreference = 'Stop'
$repoRoot = Split-Path -Parent $PSScriptRoot
$androidRoot = Join-Path $repoRoot 'android'
$keystorePath = Join-Path $androidRoot 'clearscreen-release.keystore'
$propertiesPath = Join-Path $androidRoot 'keystore.properties'

if ((Test-Path -LiteralPath $keystorePath) -or (Test-Path -LiteralPath $propertiesPath)) {
  if (-not $Force) {
    throw 'Signing files already exist. Use -Force only when intentionally rotating the local test identity.'
  }
}

$keytool = Get-Command keytool.exe -ErrorAction SilentlyContinue
if (-not $keytool) {
  $keytoolCandidates = @(
    (Join-Path $env:JAVA_HOME 'bin\keytool.exe'),
    (Join-Path $env:USERPROFILE 'jdk\jdk-21.0.12.1+1\bin\keytool.exe'),
    (Join-Path $env:ProgramFiles 'Android\Android Studio\jbr\bin\keytool.exe')
  )
  $keytoolPath = $keytoolCandidates | Where-Object { $_ -and (Test-Path -LiteralPath $_) } | Select-Object -First 1
  if ($keytoolPath) {
    $keytool = Get-Item -LiteralPath $keytoolPath
  }
}
if (-not $keytool) {
  throw 'keytool.exe was not found. Install a JDK and make keytool available on PATH.'
}
$keytoolExecutable = if ($keytool.PSObject.Properties.Name -contains 'Source') {
  $keytool.Source
} else {
  $keytool.FullName
}

$randomBytes = New-Object byte[] 32
$randomGenerator = [System.Security.Cryptography.RandomNumberGenerator]::Create()
$randomGenerator.GetBytes($randomBytes)
$randomGenerator.Dispose()
$password = [Convert]::ToBase64String($randomBytes).Replace('+', '-').Replace('/', '_').TrimEnd('=')

if (Test-Path -LiteralPath $keystorePath) {
  Remove-Item -LiteralPath $keystorePath -Force
}

& $keytoolExecutable -genkeypair -v `
  -keystore $keystorePath `
  -storepass $password `
  -keypass $password `
  -alias $Alias `
  -keyalg RSA `
  -keysize 4096 `
  -validity $ValidityDays `
  -dname 'CN=ClearScreen, OU=Android, O=ClearScreen, L=Shanghai, ST=Shanghai, C=CN' `
  -noprompt

if ($LASTEXITCODE -ne 0) {
  throw "keytool failed with exit code $LASTEXITCODE."
}

$properties = @(
  "storeFile=clearscreen-release.keystore"
  "storePassword=$password"
  "keyAlias=$Alias"
  "keyPassword=$password"
)
Set-Content -LiteralPath $propertiesPath -Value $properties -Encoding ASCII

Write-Host "Created local release signing identity: $keystorePath"
Write-Host "Stored ignored Gradle properties: $propertiesPath"
Write-Host 'Back up both files securely. Never commit either file or print the password into a public log.'
