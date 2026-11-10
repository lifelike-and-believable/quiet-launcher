<#
.SYNOPSIS
Builds Quiet Launcher into out\QuietLauncher (docs/user/setup.md, "What you need").

.DESCRIPTION
Builds the app with the Gradle wrapper and copies the install file, quiet-launcher.apk, and the
setup and undo scripts (once they exist) into out\QuietLauncher. Needs a JDK 17 and the Android SDK
(platform 34, build-tools 34.0.0). If JAVA_HOME or ANDROID_HOME is not set, it uses
%LOCALAPPDATA%\AndroidBuild\jdk and %LOCALAPPDATA%\AndroidBuild\sdk when they exist.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\build.ps1
#>
$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
$tools = Join-Path $env:LOCALAPPDATA 'AndroidBuild'

if (-not $env:JAVA_HOME -and (Test-Path (Join-Path $tools 'jdk'))) { $env:JAVA_HOME = Join-Path $tools 'jdk' }
if (-not $env:ANDROID_HOME -and (Test-Path (Join-Path $tools 'sdk'))) { $env:ANDROID_HOME = Join-Path $tools 'sdk' }
if (-not $env:JAVA_HOME) { Write-Error 'No JDK found: set JAVA_HOME to a JDK 17.'; exit 1 }
if (-not $env:ANDROID_HOME) { Write-Error 'No Android SDK found: set ANDROID_HOME.'; exit 1 }

& (Join-Path $root 'gradlew.bat') --no-daemon -p $root ':app:assembleRelease'
if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE }

$out = Join-Path $root 'out\QuietLauncher'
New-Item -ItemType Directory -Force $out | Out-Null
Copy-Item (Join-Path $root 'app\build\outputs\apk\release\app-release.apk') (Join-Path $out 'quiet-launcher.apk') -Force
foreach ($script in 'quiet-setup.ps1', 'quiet-undo.ps1') {
    $source = Join-Path $root "scripts\$script"
    if (Test-Path $source) { Copy-Item $source $out -Force }
}
Write-Host "Built $(Join-Path $out 'quiet-launcher.apk')"
