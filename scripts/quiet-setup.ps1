<#
.SYNOPSIS
Sets up the quiet home screen on the TV, or puts it back after a TV update or reset.

.DESCRIPTION
Connects to the TV over ADB (pairing it with this PC if needed), saves a backup of the TV's
accessibility settings and a picture of Google TV's home screen, installs the app, turns on its
accessibility service, and checks its work. Then it asks you to check two things with the remote.
Running it again on a TV that is already set up changes nothing. Everything it prints goes to a log
in Documents\QuietLauncher. See docs/user/setup.md.

.PARAMETER Apk
The app's install file, quiet-launcher.apk (in the Quiet Launcher folder). Required.

.PARAMETER Tv
The TV's address, for example 10.0.0.202:39423, when more than one device is connected to ADB.

.PARAMETER Pair
Pair again with the TV even if a pairing seems to exist, for example after the TV was reset.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk -Tv 10.0.0.202:39423

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk -Pair
#>
param(
    [string]$Apk,
    [string]$Tv,
    [switch]$Pair
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2

$SERVICE = 'quiet.launcher/.HomeWatcher'
$LAUNCHERX = 'com.google.android.apps.tv.launcherx'
$started = Get-Date

# ---- Output, log, and answers ----

$folder = if ($env:QUIET_FOLDER) { $env:QUIET_FOLDER } else { Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'QuietLauncher' }
New-Item -ItemType Directory -Force $folder | Out-Null
$log = Join-Path $folder ('setup-' + $started.ToString('yyyy-MM-dd-HHmmss') + '.log')
$answers = @(if ($env:QUIET_ANSWERS) { $env:QUIET_ANSWERS -split '\|' })
$answerIndex = 0
$fast = [bool]$env:QUIET_FAST

function Say([string]$text) {
    Write-Host $text
    Add-Content -Path $log -Value $text -Encoding UTF8
}

function Ask([string]$prompt) {
    if ($script:answerIndex -lt $script:answers.Count) {
        $answer = $script:answers[$script:answerIndex]
        $script:answerIndex++
        Write-Host "$prompt$answer"
    } else {
        $answer = Read-Host -Prompt $prompt.TrimEnd(': ').TrimEnd()
    }
    Add-Content -Path $log -Value "$prompt$answer" -Encoding UTF8
    return $answer.Trim()
}

function Stop-Setup([int]$code = 1) {
    Say "Log: $log"
    exit $code
}

function Wait-Seconds([double]$seconds) { if (-not $fast) { Start-Sleep -Milliseconds ([int]($seconds * 1000)) } }

# ---- ADB ----

function Find-Adb {
    if ($env:QUIET_ADB) { if (Test-Path $env:QUIET_ADB) { return $env:QUIET_ADB } else { return $null } }
    # A program only: the Adb function below would come first and has no path.
    $found = Get-Command adb -CommandType Application -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($found) { return $found.Source }
    # Installed after this window opened: look where winget and Android Studio put it, and in the
    # PATH Windows has now (this window keeps the one it started with).
    $dirs = @()
    foreach ($scope in 'User', 'Machine') {
        $p = [Environment]::GetEnvironmentVariable('Path', $scope)
        if ($p) { $dirs += $p -split ';' | Where-Object { $_ } }
    }
    if ($env:LOCALAPPDATA) {
        $dirs += @(Get-ChildItem (Join-Path $env:LOCALAPPDATA 'Microsoft\WinGet\Packages') -Directory -Filter 'Google.PlatformTools*' -ErrorAction SilentlyContinue |
            ForEach-Object { Join-Path $_.FullName 'platform-tools' })
        $dirs += Join-Path $env:LOCALAPPDATA 'Android\Sdk\platform-tools'
    }
    foreach ($d in $dirs) {
        $exe = Join-Path ([Environment]::ExpandEnvironmentVariables($d)) 'adb.exe'
        if (Test-Path $exe) { return $exe }
    }
    return $null
}

$script:adb = $null
$script:serial = $null
$script:adbExit = 0

function Adb {
    # adb writes warnings to stderr; Windows PowerShell 5.1 would stop on them here.
    $ErrorActionPreference = 'Continue'
    $output = & $script:adb @args 2>&1 | ForEach-Object { "$_" }
    $script:adbExit = $LASTEXITCODE
    return (@($output) -join "`n").Trim()
}

function Tv { return (Adb -s $script:serial @args) }

function Get-Devices {
    return @((Adb devices) -split "`n" | Where-Object { $_ -match "^(\S+)\s+device$" } | ForEach-Object { ($_ -split '\s+')[0] })
}

# ADB can list one TV twice, by its address and by its mDNS name. One entry for each serial number,
# the address when there is one.
function Get-Tvs {
    $bySerial = [ordered]@{}
    foreach ($d in @(Get-Devices)) {
        $serial = "$(Adb -s $d shell getprop ro.serialno)".Trim()
        if (-not $serial) { $serial = $d }
        if (-not $bySerial.Contains($serial) -or $d -match '^\d+\.\d+\.\d+\.\d+:\d+$') { $bySerial[$serial] = $d }
    }
    return @($bySerial.Values)
}

function Get-MdnsAddress {
    foreach ($line in ((Adb mdns services) -split "`n")) {
        if ($line -match '_adb-tls-connect\._tcp\s+(\S+)') { return $Matches[1] }
    }
    return $null
}

function Describe-Tv([string]$address) {
    $model = Adb -s $address shell getprop ro.product.model
    $release = Adb -s $address shell getprop ro.build.version.release
    return "$model, Android $release"
}

function Could-Not-Connect([string]$step) {
    Say "$step TV: COULD NOT CONNECT"
    Say '      Check that the TV is on (press a button on the remote), on the same network,'
    Say '      and that Wireless debugging is on. If the TV was reset, run this again with -Pair.'
    Stop-Setup
}

# The TV step, shared with quiet-undo.ps1 (design-setup: TV states).
function Connect-Tv([string]$step) {
    $devices = @(Get-Tvs)
    if ($Tv) {
        if ($devices -notcontains $Tv) {
            Adb connect $Tv | Out-Null
            if ((Get-Devices) -notcontains $Tv) { Could-Not-Connect $step }
        }
        Say "$step TV: connected to $Tv"
        return $Tv
    }
    if (-not $Pair) {
        if ($devices.Count -gt 1) {
            Say "$step TV: MORE THAN ONE DEVICE"
            $named = $devices | ForEach-Object { "$_ ($(Adb -s $_ shell getprop ro.product.model))" }
            Say ('      Found: ' + ($named -join ', '))
            Say "      Run this again with -Tv, for example: -Tv $($devices[0])"
            Stop-Setup
        }
        if ($devices.Count -eq 1) {
            Say "$step TV: connected to $($devices[0])"
            return $devices[0]
        }
        $address = Get-MdnsAddress
        if ($address) {
            Adb connect $address | Out-Null
            if ((Get-Devices) -contains $address) {
                Say "$step TV: connected to $address"
                return $address
            }
        }
    }

    Say "$step TV: not paired with this PC"
    Say '      On the TV: Settings, System, Developer options, Wireless debugging,'
    Say '      Pair device with pairing code. A code and an address appear.'
    for ($try = 1; $try -le 3; $try++) {
        $pairingAddress = Ask '      Pairing address shown on the TV (for example 10.0.0.202:41234): '
        $code = Ask '      Pairing code shown on the TV: '
        $result = Adb pair $pairingAddress $code
        if ($script:adbExit -eq 0 -and $result -match 'Successfully paired') { break }
        Say "$step TV: PAIRING FAILED (wrong or expired code, or wrong address)"
        Say '      On the TV, open Pair device with pairing code again; a new code appears.'
        Say '      Use the address shown under the code, not the one at the top of the screen.'
        if ($try -eq 3) { Stop-Setup }
    }
    for ($i = 0; $i -lt 10; $i++) {
        $address = Get-MdnsAddress
        if ($address) { break }
        Wait-Seconds 1
    }
    if ($address) { Adb connect $address | Out-Null }
    if (-not $address -or (Get-Devices) -notcontains $address) { Could-Not-Connect $step }
    Say "      Paired. Connected to $address ($(Describe-Tv $address))"
    return $address
}

function Get-Setting([string]$name) { return (Tv shell settings get secure $name) }

function Remove-Service([string]$list) {
    if (-not $list -or $list -eq 'null') { return 'null' }
    $kept = @($list -split ':' | Where-Object { $_ -and $_ -ne $SERVICE -and $_ -ne 'quiet.launcher/quiet.launcher.HomeWatcher' })
    if ($kept.Count -eq 0) { return 'null' }
    return ($kept -join ':')
}

function Has-Service([string]$list) {
    return ($list -split ':') -contains $SERVICE -or ($list -split ':') -contains 'quiet.launcher/quiet.launcher.HomeWatcher'
}

# ---- 1. ADB ----

Say 'Quiet Launcher setup'
Say ''
if (-not $Apk) {
    Say "Give the app's install file with -Apk, for example: -Apk .\quiet-launcher.apk"
    Stop-Setup
}
if (-not (Test-Path $Apk)) {
    Say "The install file was not found: $Apk"
    Stop-Setup
}
$Apk = (Resolve-Path $Apk).Path

$script:adb = Find-Adb
if (-not $script:adb) {
    Say '[1/6] ADB: NOT FOUND'
    Say '      Install it with: winget install Google.PlatformTools'
    Say '      then open a new PowerShell window and run this again.'
    Stop-Setup
}
$version = ((Adb version) -split "`n" | Where-Object { $_ -match '^Version (\S+)' } | ForEach-Object { ($_ -split '\s+')[1] -replace '-.*$', '' }) | Select-Object -First 1
Say "[1/6] ADB: found (platform-tools $version)"

# ---- 2. TV ----

$script:serial = Connect-Tv '[2/6]'
$changed = $false

# ---- 3. Backup ----

$tvSerial = (Tv shell getprop ro.serialno).Trim()
# A sleeping TV shows no home screen to picture or check.
Tv shell input keyevent KEYCODE_WAKEUP | Out-Null
$backupFile = Join-Path $folder "backup-$tvSerial.json"
$hadBackup = Test-Path $backupFile
if ($hadBackup) {
    $backup = Get-Content -Raw $backupFile | ConvertFrom-Json
    Say "[3/6] Backup: kept the one from $($backup.date)"
} else {
    $services = Get-Setting 'enabled_accessibility_services'
    $enabled = Get-Setting 'accessibility_enabled'
    $alreadyOn = Has-Service $services
    $recorded = Remove-Service $services
    if ($alreadyOn -and $recorded -eq 'null') { $enabled = '0' }
    $backup = [ordered]@{
        serial = $tvSerial
        date = (Get-Date).ToString('yyyy-MM-dd HH:mm')
        enabled_accessibility_services = $recorded
        accessibility_enabled = $enabled
    }
    $backup | ConvertTo-Json | Set-Content -Path $backupFile -Encoding UTF8
    Say "[3/6] Backup: saved to $backupFile"
    if ($alreadyOn) {
        Say '      Picture skipped: the quiet home screen is already on.'
    } else {
        $picture = Join-Path $folder "before-$tvSerial.png"
        Tv shell am start -a android.intent.action.MAIN -c android.intent.category.HOME | Out-Null
        Wait-Seconds 3
        Tv shell screencap -p /sdcard/quiet-before.png | Out-Null
        Tv pull /sdcard/quiet-before.png $picture | Out-Null
        Tv shell rm /sdcard/quiet-before.png | Out-Null
        if (Test-Path $picture) { Say "      Picture of Google TV's home screen: $picture" }
    }
}

# ---- 4. Install ----

function Installed-Version {
    $dump = Tv shell dumpsys package quiet.launcher
    if ($dump -match 'versionName=(\S+)') { return $Matches[1] }
    return $null
}

$localHash = (Get-FileHash -Algorithm SHA256 $Apk).Hash.ToLower()
$path = (Tv shell pm path quiet.launcher) -replace '^package:', ''
$remoteHash = $null
if ($script:adbExit -eq 0 -and $path) { $remoteHash = ((Tv shell sha256sum $path) -split '\s+')[0] }
if ($remoteHash -and $remoteHash -eq $localHash) {
    Say "[4/6] Install: already installed ($(Installed-Version)); skipped"
} else {
    $result = Tv install -r $Apk
    if ($script:adbExit -ne 0 -or $result -notmatch '(?m)^Success') {
        $reason = ($result -split "`n" | Where-Object { $_ -match 'Failure|failed' } | Select-Object -Last 1)
        if ($reason -match '(Failure \[.*\])') { $reason = $Matches[1] }
        Say "[4/6] Install: FAILED: $reason"
        Say '      Nothing on the TV was changed. A backup was saved on this PC. Run this again.'
        Stop-Setup
    }
    Say "[4/6] Install: installed quiet-launcher $(Installed-Version)"
    $changed = $true
}

# ---- 5. Turn on ----

$services = Get-Setting 'enabled_accessibility_services'
$enabled = Get-Setting 'accessibility_enabled'
if ((Has-Service $services) -and $enabled -eq '1') {
    Say '[5/6] Turn on: already on'
} else {
    $others = Remove-Service $services
    $list = if ($others -eq 'null') { $SERVICE } else { "${others}:$SERVICE" }
    $put1 = Tv shell settings put secure enabled_accessibility_services $list
    $exit1 = $script:adbExit
    $put2 = Tv shell settings put secure accessibility_enabled 1
    if ($exit1 -ne 0 -or $script:adbExit -ne 0 -or -not (Has-Service (Get-Setting 'enabled_accessibility_services'))) {
        Say "[5/6] Turn on: FAILED: $(@($put1, $put2) -join ' ')".TrimEnd()
        Say "      The app is installed but not turned on; the TV still uses Google TV's home screen."
        Say '      Run this again, or run quiet-undo.ps1 to remove it.'
        Stop-Setup
    }
    if ($hadBackup) { Say '[5/6] Turn on: was off; turned it back on' } else { Say '[5/6] Turn on: accessibility service on' }
    $changed = $true
}

# ---- 6. Checks ----

Say '[6/6] Checks'
$failed = $false
if ((Tv shell pm list packages -e $LAUNCHERX) -match "package:$([regex]::Escape($LAUNCHERX))") {
    Say '      Google TV home app installed and enabled: yes'
} else {
    Say '      Google TV home app installed and enabled: NO'
    Say "      Google TV's home app is disabled or missing, so search results cannot open. Re-enable"
    Say "      Google TV in the TV's Settings, Apps, then run this again."
    $failed = $true
}

$running = $false
for ($i = 0; $i -lt 10 -and -not $running; $i++) {
    $running = (Tv shell dumpsys accessibility) -match 'Bound services:\{Service\[label=Quiet Launcher'
    if (-not $running) { Wait-Seconds 0.5 }
}
if ($running) {
    Say '      Quiet Launcher service running: yes'
} else {
    Say '      Quiet Launcher service running: NO'
    Say '      The service is turned on but Android has not started it. Restart the TV, then run this'
    Say '      again; if it still says NO, run quiet-undo.ps1 and open an issue with the log.'
    $failed = $true
}

if ($running) {
    # Google TV's home screen already in front opens no window when asked for again, so the
    # service would have nothing to cover: show the TV's Settings first. (Keys sent over ADB do
    # not reach the service; only the remote's do. docs/product/notes.md)
    Tv shell am start -a android.settings.SETTINGS | Out-Null
    Wait-Seconds 1.5
    $clock = [Diagnostics.Stopwatch]::StartNew()
    Tv shell am start -a android.intent.action.MAIN -c android.intent.category.HOME | Out-Null
    $covered = $false
    for ($i = 0; $i -lt 30 -and -not $covered; $i++) {
        $covered = (Tv shell dumpsys activity activities) -match 'topResumedActivity=.*quiet\.launcher/'
        if (-not $covered) { Wait-Seconds 0.1 }
    }
    $clock.Stop()
    if ($covered) {
        Say ("      Google TV's home screen covered when it opens: yes ({0:0.0} s)" -f $clock.Elapsed.TotalSeconds)
    } else {
        Say "      Google TV's home screen covered when it opens: FAILED"
        Say "      The service is on but did not cover Google TV's home screen. Run quiet-undo.ps1, then"
        Say '      open an issue at github.com/lifelike-and-believable/quiet-launcher/issues and attach'
        Say "      $log (it records the window the TV showed)."
        Add-Content -Path $log -Value (Tv shell dumpsys activity activities) -Encoding UTF8
        $failed = $true
    }
}
if ($failed) { Stop-Setup }

# ---- Remote checks ----

$minutes = [int][Math]::Floor(((Get-Date) - $started).TotalMinutes)
$seconds = [int]((Get-Date) - $started).Seconds
if (-not $changed) {
    Say ''
    Say "Nothing changed. Log: $log"
    exit 0
}
Say ''
Say 'Now check with the remote, then answer here:'
$homeAnswer = Ask '  Press Home. Did the quiet home screen appear, with no Google TV home screen first? (y/n): '
$searchAnswer = Ask '  Search for "Severance". Did Google''s page for it open? (y/n): '
if ($homeAnswer -notmatch '^[yY]') {
    Say 'Run quiet-undo.ps1 and open an issue with the log.'
    Stop-Setup
}
if ($searchAnswer -notmatch '^[yY]') {
    Say "Check the TV's internet connection; if it is fine, run quiet-undo.ps1 and open an issue with the log."
    Stop-Setup
}
Say ''
Say "Done in $minutes min $seconds s. Log: $log"
exit 0
