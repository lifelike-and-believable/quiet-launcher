<#
.SYNOPSIS
Removes the quiet home screen from the TV and puts the TV back as it was.

.DESCRIPTION
Connects to the TV over ADB (pairing it with this PC if needed), restores the accessibility settings
saved by quiet-setup.ps1, uninstalls the app, opens Google TV's home screen, and checks its work.
Then it asks you to check the TV with the remote. It works even if the quiet home screen is not
working or setup stopped half way. Everything it prints goes to a log in Documents\QuietLauncher.
See docs/user/setup.md.

.PARAMETER Tv
The TV's address, for example 10.0.0.202:39423, when more than one device is connected to ADB.

.PARAMETER Pair
Pair again with the TV even if a pairing seems to exist, for example after the TV was reset.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-undo.ps1

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-undo.ps1 -Tv 10.0.0.202:39423
#>
param(
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
$log = Join-Path $folder ('undo-' + $started.ToString('yyyy-MM-dd-HHmmss') + '.log')
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
    $found = Get-Command adb -ErrorAction SilentlyContinue
    if ($found) { return $found.Source }
    return $null
}

$script:adb = $null
$script:serial = $null
$script:adbExit = 0

function Adb {
    $output = & $script:adb @args 2>&1 | ForEach-Object { "$_" }
    $script:adbExit = $LASTEXITCODE
    return (@($output) -join "`n").Trim()
}

function Tv { return (Adb -s $script:serial @args) }

function Get-Devices {
    return @((Adb devices) -split "`n" | Where-Object { $_ -match "^(\S+)\s+device$" } | ForEach-Object { ($_ -split '\s+')[0] })
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
    $devices = @(Get-Devices)
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

# ---- 1. TV ----

Say 'Quiet Launcher undo'
Say ''
$script:adb = Find-Adb
if (-not $script:adb) {
    Say '[1/4] ADB: NOT FOUND'
    Say '      Install it with: winget install Google.PlatformTools'
    Say '      then open a new PowerShell window and run this again.'
    Stop-Setup
}
$script:serial = Connect-Tv '[1/4]'
$tvSerial = (Tv get-serialno).Trim()

# ---- 2. Accessibility settings ----

function Restore-Setting([string]$name, [string]$value) {
    if (-not $value -or $value -eq 'null') { Tv shell settings delete secure $name | Out-Null }
    else { Tv shell settings put secure $name $value | Out-Null }
}

$backupFile = Join-Path $folder "backup-$tvSerial.json"
$undoneName = $null
if (Test-Path $backupFile) {
    $backup = Get-Content -Raw $backupFile | ConvertFrom-Json
    Restore-Setting 'enabled_accessibility_services' "$($backup.enabled_accessibility_services)"
    Restore-Setting 'accessibility_enabled' "$($backup.accessibility_enabled)"
    $undoneName = "backup-$tvSerial-undone-$((Get-Date).ToString('yyyy-MM-dd-HHmm')).json"
    Rename-Item -Path $backupFile -NewName $undoneName
    Say "[2/4] Accessibility settings: restored from backup-$tvSerial.json"
} else {
    $services = Get-Setting 'enabled_accessibility_services'
    $others = Remove-Service $services
    Restore-Setting 'enabled_accessibility_services' $others
    if ($others -eq 'null') { Tv shell settings put secure accessibility_enabled 0 | Out-Null }
    Say "[2/4] Accessibility settings: No backup on this PC: turned off only Quiet Launcher's service and left any others as they were."
}

# ---- 3. Remove ----

Tv shell pm path quiet.launcher | Out-Null
if ($script:adbExit -ne 0) {
    Say '[3/4] Remove: already removed'
} else {
    $result = Tv uninstall quiet.launcher
    if ($script:adbExit -ne 0 -or $result -notmatch '(?m)^Success') {
        Say "[3/4] Remove: FAILED: $result"
        Say '      The accessibility settings were restored; run this again to remove the app.'
        Stop-Setup
    }
    Say '[3/4] Remove: uninstalled quiet-launcher'
}

# ---- 4. Checks ----

Say '[4/4] Checks'
Tv shell am start -a android.intent.action.MAIN -c android.intent.category.HOME | Out-Null
$failed = $false
$resolved = (Tv shell cmd package resolve-activity --brief -a android.intent.action.MAIN -c android.intent.category.HOME) -split "`n" | Select-Object -Last 1
if ($resolved -match "^$([regex]::Escape($LAUNCHERX))/") {
    Say "      Android's home screen is Google TV's: yes"
} else {
    Say "      Android's home screen is Google TV's: NO ($resolved)"
    $failed = $true
}
if ((Tv shell pm list packages -e $LAUNCHERX) -match "package:$([regex]::Escape($LAUNCHERX))") {
    Say '      Google TV home app installed and enabled: yes'
} else {
    Say '      Google TV home app installed and enabled: NO'
    Say "      Re-enable Google TV in the TV's Settings, Apps."
    $failed = $true
}
if ($failed) { Stop-Setup }

Say ''
Say 'Now check with the remote, then answer here:'
$homeAnswer = Ask '  Press Home. Did Google TV''s home screen appear? (y/n): '
$worksAnswer = Ask '  Do search, your apps, and Settings work? (y/n): '
$picture = Join-Path $folder "before-$tvSerial.png"
if (Test-Path $picture) {
    Say "Optional: compare the TV with $picture; it should look the"
    Say 'same apart from the shows in its rows.'
}
if ($homeAnswer -notmatch '^[yY]' -or $worksAnswer -notmatch '^[yY]') {
    Say 'Open an issue at github.com/lifelike-and-believable/quiet-launcher/issues and attach the log.'
    Stop-Setup
}
$minutes = [int][Math]::Floor(((Get-Date) - $started).TotalMinutes)
$seconds = [int]((Get-Date) - $started).Seconds
Say ''
$kept = if ($undoneName) { " The backup is kept as $undoneName." } else { '' }
Say "Done in $minutes min $seconds s.$kept Log: $log"
exit 0
