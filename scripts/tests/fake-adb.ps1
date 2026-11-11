# A fake ADB for testing quiet-setup.ps1 and quiet-undo.ps1 (docs/specs/setup-scripts.md).
# It keeps a pretend TV in the JSON file named by QUIET_FAKE_STATE and answers the ADB commands the
# scripts use, the way the reference device did in the spikes. Every call is recorded in "calls".
$ErrorActionPreference = 'Stop'
# Like the real adb.exe, independent of the calling script's strict mode.
Set-StrictMode -Off
$statePath = $env:QUIET_FAKE_STATE
$s = Get-Content -Raw $statePath | ConvertFrom-Json

$SERVICE = 'quiet.launcher/.HomeWatcher'
$LAUNCHERX = 'com.google.android.apps.tv.launcherx'

function Save { $s | ConvertTo-Json -Depth 6 | Set-Content -Encoding UTF8 $statePath }
function Get-Setting($k) { $v = $s.settings.$k; if ($null -eq $v) { 'null' } else { "$v" } }
function Set-Setting($k, $v) { $s.settings | Add-Member -Force -NotePropertyName $k -NotePropertyValue $v }
function Bound {
    $eas = Get-Setting 'enabled_accessibility_services'
    return ($null -ne $s.installed) -and ($eas -split ':' -contains $SERVICE) -and ((Get-Setting 'accessibility_enabled') -eq '1') -and $s.service_runs
}

# The app's arrangement receiver (docs/specs/arrange-receiver.md), from "apps" (each name and package,
# in the home screen's usual order) and "arrangement" (version, order, hidden; null if never arranged).
function B64([string]$text) { [Convert]::ToBase64String([Text.Encoding]::UTF8.GetBytes($text)) }
function Unb64([string]$text) { [Text.Encoding]::UTF8.GetString([Convert]::FromBase64String($text)) }
function Arranged {
    $apps = @($s.apps | Where-Object { $_ })
    $a = $s.arrangement
    $order = if ($a) { @($a.order) } else { @() }
    $hidden = if ($a) { @($a.hidden) } else { @() }
    $byPkg = @{}; foreach ($app in $apps) { $byPkg[$app.package] = $app }
    $shown = @($order | Where-Object { $byPkg.ContainsKey($_) -and $hidden -notcontains $_ } | ForEach-Object { $byPkg[$_] })
    $shown += @($apps | Where-Object { $order -notcontains $_.package -and $hidden -notcontains $_.package })
    $hid = @($hidden | Where-Object { $byPkg.ContainsKey($_) } | ForEach-Object { $byPkg[$_] })
    $list = @($shown | ForEach-Object { [ordered]@{ name = $_.name; package = $_.package; hidden = $false } }) +
            @($hid | ForEach-Object { [ordered]@{ name = $_.name; package = $_.package; hidden = $true } })
    return [ordered]@{ version = $(if ($a) { [long]$a.version } else { 0 }); arranged = [bool]$a; apps = $list }
}
function Answer([int]$result, $json) {
    return @('Broadcasting: Intent { flg=0x400000 cmp=quiet.launcher/.ArrangeReceiver }',
             "Broadcast completed: result=$result, data=""$(B64 ($json | ConvertTo-Json -Depth 5 -Compress))""")
}

# A real adb.exe receives text: make every argument a string, as PowerShell would for a program.
$argv = @($args | ForEach-Object { "$_" })
$s.calls += , ($argv -join ' ')
$device = $null
if ($argv.Count -ge 2 -and $argv[0] -eq '-s') { $device = $argv[1]; $argv = $argv[2..($argv.Count - 1)] }
$code = 0
$out = @()
$err = $null

switch -Regex ($argv -join ' ') {
    '^version$' { $out = 'Android Debug Bridge version 1.0.41', 'Version 36.0.0-13206524' }
    '^devices$' {
        $out = @('List of devices attached') + @($s.devices | ForEach-Object { "$_`tdevice" }) + @('')
    }
    '^mdns services$' {
        $out = @('List of discovered mdns services')
        if ($s.reachable -and $s.paired) { $out += "adb-$($s.serialno)-x`t_adb-tls-connect._tcp`t$($s.address)" }
    }
    '^pair ' {
        if ($s.reachable -and $argv[1] -eq $s.pairing_address -and $argv[2] -eq $s.pairing_code) {
            $s.paired = $true; $out = "Successfully paired to $($argv[1]) [guid=adb-$($s.serialno)]"
        } else { $out = 'Failed: Wrong password or connection was dropped.'; $code = 1 }
    }
    '^connect ' {
        if ($s.reachable -and $s.paired -and $argv[1] -eq $s.address) {
            if ($s.devices -notcontains $argv[1]) { $s.devices += $argv[1] }
            $out = "connected to $($argv[1])"
        } else { $out = "failed to connect to $($argv[1])" }
    }
    # As the reference device answered: get-serialno gives the connection's name, not the TV's.
    '^get-serialno$' { $out = if ($device) { $device } else { @($s.devices)[0] } }
    '^shell getprop ro\.serialno$' {
        # One TV can be listed twice (its address and its mDNS name); another device has its own.
        $own = if ($device -and $s.serials) { $s.serials.$device } else { $null }
        $out = if ($own) { $own } else { $s.serialno }
    }
    '^shell getprop ro\.product\.model$' { $out = $s.model }
    '^shell getprop ro\.build\.version\.release$' { $out = $s.release }
    '^shell settings get secure (\S+)$' { $out = Get-Setting $argv[4] }
    '^shell settings put secure (\S+) (\S+)$' {
        if ($s.settings_locked) { $out = 'Security exception: not allowed'; $code = 1 } else { Set-Setting $argv[4] $argv[5] }
    }
    '^shell settings delete secure (\S+)$' { $s.settings.PSObject.Properties.Remove($argv[4]); $out = 'Deleted 1 rows' }
    '^shell pm list packages -e (\S+)$' {
        if ($argv[5] -eq $LAUNCHERX -and $s.launcherx_enabled) { $out = "package:$LAUNCHERX" }
        elseif ($argv[5] -eq 'quiet.launcher' -and $null -ne $s.installed) { $out = 'package:quiet.launcher' }
    }
    '^shell pm path quiet\.launcher$' {
        if ($null -ne $s.installed) { $out = 'package:/data/app/~~x/quiet.launcher-1/base.apk' } else { $code = 1 }
    }
    '^shell sha256sum (\S+)$' { $out = "$($s.installed.sha)  $($argv[2])" }
    '^shell dumpsys package quiet\.launcher$' {
        if ($null -ne $s.installed) { $out = "    versionName=$($s.installed.version)" }
    }
    '^shell dumpsys accessibility$' {
        $out = "     Enabled services:{{$((Get-Setting 'enabled_accessibility_services') -replace ':', '}, {')}}}"
        if (Bound) { $out = @($out, '     Bound services:{Service[label=Quiet Launcher home button, feedbackType[FEEDBACK_GENERIC]]}') }
        else { $out = @($out, '     Bound services:{}') }
    }
    '^shell am start -a android\.intent\.action\.MAIN -c android\.intent\.category\.HOME$' {
        # Already in front, Google TV's home screen opens no window, so there is nothing to cover.
        $alreadyHome = $s.top -eq "$LAUNCHERX/.home.HomeActivity"
        $s.top = if ((Bound) -and $s.covers -and -not $alreadyHome) { 'quiet.launcher/.HomeActivity' } else { "$LAUNCHERX/.home.HomeActivity" }
        $out = 'Starting: Intent { act=android.intent.action.MAIN cat=[android.intent.category.HOME] }'
        # The reference device wrote this to stderr (Google TV's home was already running).
        $err = 'Warning: Activity not started, its current task has been brought to the front'
    }
    '^shell am start -a android\.settings\.SETTINGS$' {
        $s.top = 'com.android.tv.settings/.MainSettings'
        $out = 'Starting: Intent { act=android.settings.SETTINGS }'
    }
    '^shell am broadcast -n quiet\.launcher/\.ArrangeReceiver -a quiet\.launcher\.action\.GET_ARRANGEMENT$' {
        if ($null -eq $s.installed) { $out = 'Broadcasting: Intent { }', 'Broadcast completed: result=0' }
        else { $out = Answer 1 (Arranged) }
    }
    '^shell am broadcast -n quiet\.launcher/\.ArrangeReceiver -a quiet\.launcher\.action\.SET_ARRANGEMENT --es arrangement (\S+) --el version (\d+)( --ez replace true)?$' {
        if ($null -eq $s.installed) { $out = 'Broadcasting: Intent { }', 'Broadcast completed: result=0'; break }
        $sent = Unb64 $Matches[1] | ConvertFrom-Json
        $replace = [bool]$Matches[3]
        $current = if ($s.arrangement) { [long]$s.arrangement.version } else { 0 }
        $s | Add-Member -Force -NotePropertyName last_sent -NotePropertyValue ([pscustomobject]@{ order = @($sent.order); hidden = @($sent.hidden); version = [long]$Matches[2]; replace = $replace })
        if ($current -ne [long]$Matches[2] -and -not $replace) { $out = Answer 2 ([ordered]@{ version = $current }); break }
        $s | Add-Member -Force -NotePropertyName arrangement -NotePropertyValue ([pscustomobject]@{ version = $current + 1; order = @($sent.order); hidden = @($sent.hidden) })
        $out = Answer 1 ([ordered]@{ version = $current + 1 })
    }
    '^shell input keyevent KEYCODE_WAKEUP$' { $s | Add-Member -Force -NotePropertyName awake -NotePropertyValue $true }
    '^shell dumpsys activity activities$' {
        # A sleeping TV has no resumed activity.
        if ($s.awake -ne $false) { $out = "      topResumedActivity=ActivityRecord{a1 u0 $($s.top) t1}" }
    }
    '^shell cmd package resolve-activity --brief -a android\.intent\.action\.MAIN -c android\.intent\.category\.HOME$' {
        $out = 'priority=2 preferredOrder=0 match=0x108000 specificIndex=-1 isDefault=true', "$LAUNCHERX/.home.HomeActivity"
    }
    '^shell screencap -p (\S+)$' { $s.screen_on_sdcard = $true }
    '^pull (\S+) (.+)$' {
        if ($s.screen_on_sdcard) { [IO.File]::WriteAllBytes($argv[2], [byte[]](137, 80, 78, 71)); $out = "$($argv[1]): 1 file pulled" }
        else { $out = "adb: error: remote object '$($argv[1])' does not exist"; $code = 1 }
    }
    '^shell rm (\S+)$' { $s.screen_on_sdcard = $false }
    '^install -r (.+)$' {
        $out = @('Performing Streamed Install')
        if ($s.install_failure) { $out += "adb: failed to install $($argv[2]): $($s.install_failure)"; $code = 1 }
        else {
            $sha = (Get-FileHash -Algorithm SHA256 $argv[2]).Hash.ToLower()
            $s.installed = [pscustomobject]@{ version = $s.apk_version; sha = $sha }
            $out += 'Success'
        }
    }
    '^uninstall quiet\.launcher$' {
        if ($null -ne $s.installed) { $s.installed = $null; $out = 'Success' }
        else { $out = 'Failure [DELETE_FAILED_INTERNAL_ERROR]'; $code = 1 }
    }
    default { $out = "fake adb: unknown command: $($argv -join ' ')"; $code = 1 }
}

Save
$out | ForEach-Object { Write-Output $_ }
if ($err) { [Console]::Error.WriteLine($err) }
exit $code
