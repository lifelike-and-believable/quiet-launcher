<#
.SYNOPSIS
Arranges the apps on the quiet home screen from this PC: their order, and which are hidden.

.DESCRIPTION
Connects to the TV over ADB (pairing it with this PC if needed), saves the TV's arrangement to a
text file in Documents\QuietLauncher, and opens it in Notepad. Move lines to change the order, and
move a line below "Hidden:" to hide that app; save the file and press Enter here to send it. Every
arrangement sent is also kept as a dated copy. On a TV whose apps have never been arranged (after
setting the quiet home screen up again, say), it offers to put back the newest copy. Everything it
prints goes to a log in Documents\QuietLauncher. See docs/user/arranging.md.

.PARAMETER File
Send this arrangement file (a dated copy, say) without opening Notepad.

.PARAMETER Tv
The TV's address, for example 10.0.0.202:39423, when more than one device is connected to ADB.

.PARAMETER Pair
Pair again with the TV even if a pairing seems to exist, for example after the TV was reset.

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1 -File .\arrangement-17241HFDD2WLBM-2026-11-10-180000.txt

.EXAMPLE
powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1 -Tv 10.0.0.202:39423
#>
param(
    [string]$File,
    [string]$Tv,
    [switch]$Pair
)

$ErrorActionPreference = 'Stop'
Set-StrictMode -Version 2

$started = Get-Date

# ---- Output, log, and answers ----

$folder = if ($env:QUIET_FOLDER) { $env:QUIET_FOLDER } else { Join-Path ([Environment]::GetFolderPath('MyDocuments')) 'QuietLauncher' }
New-Item -ItemType Directory -Force $folder | Out-Null
$log = Join-Path $folder ('arrange-' + $started.ToString('yyyy-MM-dd-HHmmss') + '.log')
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

# ---- The app's arrangement receiver (docs/specs/arrange-receiver.md) ----

$RECEIVER = 'quiet.launcher/.ArrangeReceiver'

# Asks the receiver; returns the result code (0 if there is no receiver) and the answer's JSON.
function Receiver([string[]]$extra) {
    $out = Tv shell am broadcast -n $RECEIVER @extra
    if ($out -match 'result=(-?\d+), data="([^"]*)"') {
        $json = [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($Matches[2])) | ConvertFrom-Json
        return @{ code = [int]$Matches[1]; json = $json }
    }
    if ($out -match 'result=(-?\d+)') { return @{ code = [int]$Matches[1]; json = $null } }
    return @{ code = 0; json = $null }
}

function Get-Arrangement { return (Receiver @('-a', 'quiet.launcher.action.GET_ARRANGEMENT')) }

function Send-Arrangement($order, $hidden, [long]$version, [bool]$replace) {
    $json = [ordered]@{ order = @($order); hidden = @($hidden) } | ConvertTo-Json -Compress
    $b64 = [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($json))
    $extra = @('-a', 'quiet.launcher.action.SET_ARRANGEMENT', '--es', 'arrangement', $b64, '--el', 'version', "$version")
    if ($replace) { $extra += @('--ez', 'replace', 'true') }
    return (Receiver $extra)
}

# ---- The arrangement file (design-arranging, Mock-ups) ----

$HEADER = @(
    '# Quiet Launcher: the order of the apps on the home screen, one per line.',
    '# Move lines to change the order. Apps below "Hidden:" are not on the home screen.',
    "# Save the file, then press Enter in the script's window. Lines starting with # are ignored."
)

# Each app's line in the file: its name, with its package in brackets when another app has that name.
function Line-For($app, $apps) {
    $same = @($apps | Where-Object { $_.name.Trim().ToLower() -eq $app.name.Trim().ToLower() })
    if ($same.Count -gt 1) { return "$($app.name) ($($app.package))" }
    return $app.name
}

function Write-ArrangementFile([string]$path, $apps) {
    $lines = @($HEADER)
    $lines += @($apps | Where-Object { -not $_.hidden } | ForEach-Object { Line-For $_ $apps })
    $lines += @('', 'Hidden:')
    $lines += @($apps | Where-Object { $_.hidden } | ForEach-Object { Line-For $_ $apps })
    Set-Content -LiteralPath $path -Value $lines -Encoding UTF8
}

# Reads a file into packages: order, hidden, the lines skipped, or the reason it cannot be sent.
function Read-ArrangementFile([string]$path, $apps) {
    $byName = @{}
    $byLine = @{}
    foreach ($app in $apps) {
        $name = $app.name.Trim().ToLower()
        if (-not $byName.ContainsKey($name)) { $byName[$name] = @() }
        $byName[$name] += $app.package
        $byLine["$name ($($app.package.ToLower()))"] = $app.package
    }
    $result = @{ order = @(); hidden = @(); skipped = @(); error = $null }
    $lines = @(Get-Content -LiteralPath $path -Encoding UTF8)
    $split = -1
    for ($i = 0; $i -lt $lines.Count; $i++) { if ($lines[$i].Trim() -ieq 'Hidden:') { $split = $i; break } }
    if ($split -lt 0) {
        $result.error = 'The file has no "Hidden:" line. Put it back (on a line of its own) and try again.'
        return $result
    }
    for ($i = 0; $i -lt $lines.Count; $i++) {
        if ($i -eq $split) { continue }
        $text = ($lines[$i] -replace '\s+', ' ').Trim()
        if (-not $text -or $text.StartsWith('#')) { continue }
        $key = $text.ToLower()
        $pkg = $null
        if ($byLine.ContainsKey($key)) { $pkg = $byLine[$key] }
        elseif ($byName.ContainsKey($key) -and @($byName[$key]).Count -eq 1) { $pkg = @($byName[$key])[0] }
        if (-not $pkg) { $result.skipped += $text; continue }
        if ($result.order -contains $pkg -or $result.hidden -contains $pkg) { continue }
        if ($i -lt $split) { $result.order += $pkg } else { $result.hidden += $pkg }
    }
    if ($result.order.Count -eq 0) { $result.error = 'The file hides every app; keep at least one above "Hidden:".' }
    return $result
}

# The arrangement the TV would show for [read]: listed apps first, then the others as they are now.
function Same-As-Now($read, $apps) {
    $nowShown = @($apps | Where-Object { -not $_.hidden } | ForEach-Object { $_.package })
    $nowHidden = @($apps | Where-Object { $_.hidden } | ForEach-Object { $_.package })
    $rest = @($apps | ForEach-Object { $_.package } | Where-Object { $read.order -notcontains $_ -and $read.hidden -notcontains $_ })
    $shown = @($read.order) + $rest
    return (($shown -join '|') -eq ($nowShown -join '|')) -and ((@($read.hidden) -join '|') -eq ($nowHidden -join '|'))
}

function Open-Editor([string]$path) {
    if ($env:QUIET_EDITOR) { & $env:QUIET_EDITOR $path; return }
    # Not waited for: Windows 11's Notepad can hand the file to an open window and return at once.
    Start-Process -FilePath 'notepad.exe' -ArgumentList "`"$path`""
}

function Done {
    Say ''
    Say "Done. Log: $log"
    exit 0
}

# Sends what was read from [path]; says what happened; keeps a dated copy when sent.
function Send-File([string]$path, $read, [long]$version) {
    $prefix = '[3/3] '
    if ($read.skipped.Count -eq 1) {
        Say "[3/3] Skipped 1 line that is not an app on this TV: ""$($read.skipped[0])"""
        $prefix = '      '
    } elseif ($read.skipped.Count -gt 1) {
        Say ("[3/3] Skipped $($read.skipped.Count) lines that are not apps on this TV: " + (($read.skipped | ForEach-Object { """$_""" }) -join ', '))
        $prefix = '      '
    }
    $answer = Send-Arrangement $read.order $read.hidden $version $false
    if ($answer.code -eq 2) {
        Say '[3/3] The apps were arranged on the TV since this list was opened.'
        $replace = Ask '      Send your list anyway, replacing what was done on the TV? (y/n): '
        if ($replace -notmatch '^[yY]') {
            Say "      Nothing changed. Run this again to start from the TV's arrangement."
            Done
        }
        $answer = Send-Arrangement $read.order $read.hidden $version $true
    }
    if ($answer.code -ne 1) {
        $reason = if ($answer.json -and $answer.json.error) { $answer.json.error } else { 'no answer from the app' }
        Say "[3/3] NOT SENT: the TV refused the list ($reason)."
        Stop-Setup
    }
    # AC-3: a file that already is a dated copy (put back, or given with -File) is the copy kept;
    # copying it again could copy it onto itself, in the same second as its own name.
    $full = [IO.Path]::GetFullPath($path)
    $isCopy = ([IO.Path]::GetDirectoryName($full) -eq [IO.Path]::GetFullPath($folder).TrimEnd('\', '/')) -and
        ([IO.Path]::GetFileName($full) -match ('^arrangement-' + [regex]::Escape($tvSerial) + '-\d{4}-\d{2}-\d{2}-\d{6}\.txt$'))
    if ($isCopy) {
        $copy = $full
    } else {
        $copy = Join-Path $folder ("arrangement-$tvSerial-" + (Get-Date).ToString('yyyy-MM-dd-HHmmss') + '.txt')
        Copy-Item -LiteralPath $path -Destination $copy -Force
    }
    $now = @((Get-Arrangement).json.apps)
    $shown = @($now | Where-Object { -not $_.hidden }).Count
    $hidden = @($now | Where-Object { $_.hidden }).Count
    Say "${prefix}Sent to the TV: $shown apps shown, $hidden hidden. Press Home to see it."
    Say "      Copy kept: $copy"
    Done
}

# ---- 1. ADB and the TV ----

Say 'Quiet Launcher: arrange apps'
Say ''
$script:adb = Find-Adb
if (-not $script:adb) {
    Say '[1/3] ADB: NOT FOUND'
    Say '      Install it with: winget install Google.PlatformTools'
    Say '      then open a new PowerShell window and run this again.'
    Stop-Setup
}
$script:serial = Connect-Tv '[1/3]'
$tvSerial = (Tv shell getprop ro.serialno).Trim()
$current = Get-Arrangement
if ($current.code -ne 1 -or -not $current.json) {
    Say '[1/3] TV: the quiet home screen is not set up on this TV. Run quiet-setup first.'
    Stop-Setup
}
$apps = @($current.json.apps)
$version = [long]$current.json.version

# ---- 2. The arrangement ----

if ($File) {
    if (-not (Test-Path -LiteralPath $File)) {
        Say "[2/3] Arrangement: the file was not found: $File"
        Stop-Setup
    }
    $File = (Resolve-Path -LiteralPath $File).Path
    Say "[2/3] Arrangement: read from $File"
    $read = Read-ArrangementFile $File $apps
    if ($read.error) { Say "[3/3] $($read.error)"; Stop-Setup }
    if (Same-As-Now $read $apps) { Say '[3/3] Nothing changed.'; Done }
    Send-File $File $read $version
}

# AC-8: after undo and a new setup, offer the newest copy kept for this TV.
if (-not $current.json.arranged) {
    $saved = @(Get-ChildItem -LiteralPath $folder -Filter "arrangement-$tvSerial-*.txt" -ErrorAction SilentlyContinue | Sort-Object Name)
    if ($saved.Count -gt 0) {
        $newest = $saved[-1]
        $read = Read-ArrangementFile $newest.FullName $apps
        if (-not $read.error) {
            $stamp = $newest.BaseName.Substring("arrangement-$tvSerial-".Length)
            $when = if ($stamp -match '^(\d{4}-\d{2}-\d{2})-(\d{2})(\d{2})') { "$($Matches[1]) $($Matches[2]):$($Matches[3])" } else { $stamp }
            Say '[2/3] Arrangement: the apps on this TV have not been arranged.'
            $put = Ask "      Put back the arrangement saved on $when ($($read.order.Count) apps shown, $($read.hidden.Count) hidden)? (y/n): "
            if ($put -match '^[yY]') { Send-File $newest.FullName $read $version }
        }
    }
}

$path = Join-Path $folder "arrangement-$tvSerial.txt"
Write-ArrangementFile $path $apps
Say "[2/3] Arrangement: opened in Notepad ($path)"
Say '      Move lines to change the order; move a line below "Hidden:" to hide that app.'
Open-Editor $path
$go = Ask '      Save the file, then press Enter here to send it (or type n to send nothing): '
if ($go -match '^[nN]') { Say '[3/3] Nothing changed.'; Done }

# ---- 3. Send ----

$read = Read-ArrangementFile $path $apps
if ($read.error) { Say "[3/3] $($read.error)"; Stop-Setup }
if (Same-As-Now $read $apps) { Say '[3/3] Nothing changed.'; Done }
Send-File $path $read $version
