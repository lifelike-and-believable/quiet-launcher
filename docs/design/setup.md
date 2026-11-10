---
id: design-setup
status: approved
derived-from:
  - story-setup-set-up: 6a69bd06742a
  - story-setup-restart: 1f1806140267
  - story-setup-recover: 734f151fb73e
  - story-setup-undo: 37a930b0c7c5
  - decision-home-takeover: d5aa5ee157da
  - persona-setup-person#goals: 1b5715ea18d9
  - persona-setup-person#skills-and-tools: 0ba05e70b88d
  - persona-setup-person#frustrations: 3e2f3fa89e9f
---

# Setup design

<!-- docs/design/setup.md. Setting the product up, recovering it, and undoing it, from the set-up
person's Windows PC. The user documentation (the written procedures) is written from this.
Revised 2026-11-09 after a persona walkthrough, a UX review, and an accessibility review. -->

## Personas

- persona-setup-person: one written procedure for each of set up and undo, each mostly one
  PowerShell script that checks its own work, so a change to the household's TV is deliberate,
  repeatable, and reversible. Recovery is the setup procedure again.

## Flow

**Before the first setup**, on the TV with the remote (the procedure lists the exact menu paths):
turn on developer options (Settings, System, About, press **Android TV OS build** seven times),
then **Wireless debugging** under Developer options.

**Set up** (also used to recover after an update or reset):

1. On the PC, in PowerShell, in the folder with the scripts and the app's install file:
   `powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk`. (This
   form runs the script without changing Windows' script policy, and works on downloaded files.)
2. If the TV is not paired with this PC, the script says what to open on the TV (**Pair device with
   pairing code**) and asks for the pairing address and code.
3. It saves a backup of the TV's accessibility settings and a picture of Google TV's home screen,
   unless this PC already has a backup for this TV.
4. It installs the app (or skips it if the same version is there), turns on its accessibility
   service (keeping any other services that are on), and runs its checks.
5. It asks the set-up person to do two checks with the remote, press Home and search for a named
   show, and to answer y or n for each; the answers go in the log.

**Undo**: `powershell -ExecutionPolicy Bypass -File .\quiet-undo.ps1`. It connects to the TV the
same way (pairing if needed), restores the saved accessibility settings, uninstalls the app, opens
Google TV's home screen, runs its checks, and asks the set-up person to press Home and check
search, apps, and Settings with the remote.

## Mock-ups

The interface is the scripts' output in PowerShell. Text in angle brackets varies. Results are
words, separated from the step by a colon, so a screen reader reads them cleanly.

**Set up, first time** (state: the TV is paired during the run):

```
PS> powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk
Quiet Launcher setup

[1/6] ADB: found (platform-tools 36.0.0)
[2/6] TV: not paired with this PC
      On the TV: Settings, System, Developer options, Wireless debugging,
      Pair device with pairing code. A code and an address appear.
      Pairing address shown on the TV (for example 10.0.0.202:41234): 10.0.0.202:41234
      Pairing code shown on the TV: 482913
      Paired. Connected to 10.0.0.202:39423 (Chromecast with Google TV, Android 14)
[3/6] Backup: saved to Documents\QuietLauncher\backup-<serial>.json
      Picture of Google TV's home screen: Documents\QuietLauncher\before-<serial>.png
[4/6] Install: installed quiet-launcher 1.0.0
[5/6] Turn on: accessibility service on
[6/6] Checks
      Google TV home app installed and enabled: yes
      Quiet Launcher service running: yes
      Google TV's home screen covered when it opens: yes (0.3 s)

Now check with the remote, then answer here:
  Press Home. Did the quiet home screen appear, with no Google TV home screen first? (y/n): y
  Search for "Severance". Did Google's page for it open? (y/n): y

Done in 3 min 12 s. Log: Documents\QuietLauncher\setup-<date>.log
```

**Set up again on a TV that is already set up** (state: recover, nothing to do):

```
[1/6] ADB: found (platform-tools 36.0.0)
[2/6] TV: connected to 10.0.0.202:39423
[3/6] Backup: kept the one from <date>
[4/6] Install: already installed (1.0.0); skipped
[5/6] Turn on: already on
[6/6] Checks
      ...
Nothing changed. Log: Documents\QuietLauncher\setup-<date>.log
```

**Set up after an update turned the service off** (state: recover):

```
[3/6] Backup: kept the one from <date>
[4/6] Install: already installed (1.0.0); skipped
[5/6] Turn on: was off; turned it back on
```

**Errors.** Each stops the script with a non-zero exit code, says what happened, and says what to do;
nothing on the TV has changed unless it says so.

```
[1/6] ADB: NOT FOUND
      Install it with: winget install Google.PlatformTools
      then open a new PowerShell window and run this again.

[2/6] TV: PAIRING FAILED (wrong or expired code)
      On the TV, open Pair device with pairing code again; a new code appears.
      Pairing address shown on the TV: _          (asks up to 3 times, then stops)

[2/6] TV: COULD NOT CONNECT
      Check that the TV is on (press a button on the remote), on the same network,
      and that Wireless debugging is on. If the TV was reset, run this again with -Pair.

[2/6] TV: MORE THAN ONE DEVICE
      Found: 10.0.0.202:39423 (Chromecast with Google TV), emulator-5554 (Android SDK)
      Run this again with -Tv, for example: -Tv 10.0.0.202:39423

[4/6] Install: FAILED: <ADB's message>
      Nothing on the TV was changed. A backup was saved on this PC. Run this again.

[5/6] Turn on: FAILED: <ADB's message>
      The app is installed but not turned on; the TV still uses Google TV's home screen.
      Run this again, or run quiet-undo.ps1 to remove it.

[6/6] Checks
      Google TV's home screen covered when it opens: FAILED
      The service is on but did not cover Google TV's home screen. Run quiet-undo.ps1, then
      open an issue at github.com/lifelike-and-believable/quiet-launcher/issues and attach
      Documents\QuietLauncher\setup-<date>.log (it records the window the TV showed).
```

A missing `-Apk`: **Give the app's install file with -Apk, for example: -Apk .\quiet-launcher.apk**.
`Get-Help .\quiet-setup.ps1` shows a synopsis, the options, and examples, for both scripts.

**Undo**:

```
PS> powershell -ExecutionPolicy Bypass -File .\quiet-undo.ps1
Quiet Launcher undo

[1/4] TV: connected to 10.0.0.202:39423
[2/4] Accessibility settings: restored from backup-<serial>.json
[3/4] Remove: uninstalled quiet-launcher
[4/4] Checks
      Android's home screen is Google TV's: yes
      Google TV home app installed and enabled: yes

Now check with the remote, then answer here:
  Press Home. Did Google TV's home screen appear? (y/n): y
  Do search, your apps, and Settings work? (y/n): y
Optional: compare the TV with Documents\QuietLauncher\before-<serial>.png; it should look the
same apart from the shows in its rows.

Done in 41 s. The backup is kept as backup-<serial>-undone-<date>.json. Log: Documents\QuietLauncher\undo-<date>.log
```

Undo uses the same TV step as setup, with the same pairing prompt, errors, and `-Tv` and `-Pair`
options. Without a backup (setup was never finished on this PC), step 2 says **No backup on this
PC: turned off only Quiet Launcher's service and left any others as they were.** With the app
already removed, step 3 says **already removed**.

## Layout

- Two scripts, `quiet-setup.ps1` and `quiet-undo.ps1`, shipped beside the app's install file. They
  run in Windows PowerShell 5.1 or PowerShell 7 and need only ADB.
- Steps are numbered, each on one line as "step: result"; anything that needs the set-up person is
  indented under the step. Failures are in capitals (FAILED, NOT FOUND) so they stand out without
  colour. The same text goes to a log file, `Documents\QuietLauncher\setup-<date>.log` or
  `undo-<date>.log`, with the y/n answers.
- Options: `-Apk <path>` (required for setup), `-Tv <address>` (when there is more than one
  device), `-Pair` (pair again even if a pairing seems to exist). Exit code 0 on success, non-zero
  on any stop.

## States and errors

- TV: already paired; not paired; paired but its address has changed (found again with ADB's
  network discovery); asleep (connecting wakes it); more than one device; pairing failed;
  unreachable.
- Setup: first time; again with nothing to do; again after an update turned the service off; again
  after a reset (pairs, installs, turns on, keeps the existing backup); again with a newer install
  file (updates the app: the quiet home screen stops for a few seconds while it is replaced, and
  Google TV's home screen may show in that time).
- Each failure in the mock-ups, with what to do next. A failure never leaves the TV without a
  working home screen: until the service is on, Google TV's home screen is the home screen.
- Undo: with a backup; without one; with the app already removed; with the quiet home screen not
  working (undo needs only ADB); with the TV not paired (pairs first).

## How it works

- **The backup** records what the TV's accessibility settings were before this product:
  - Setup reads `enabled_accessibility_services` and `accessibility_enabled` and writes them, with
    the TV's serial number and the date, to the backup file, only if there is no backup for this
    TV on this PC. If this product's service is already in the list (setup run from a second PC,
    say), it is removed from the recorded value, and the picture is skipped with **Picture
    skipped: the quiet home screen is already on.**
  - The existing backup is kept on every later run, including after a reset, because it describes
    the household's settings before the product.
  - Undo writes the values back; a setting that was unset (as on the reference device, where both
    were `null` and `0`) is deleted rather than written as text. It then renames the backup to
    `backup-<serial>-undone-<date>.json`, so a later setup takes a fresh one.
- **The picture**: before changing anything, setup opens Google TV's home screen, waits three
  seconds, and takes a screenshot over ADB, so the picture is of the home screen and not of an app
  or the screensaver. Undo's comparison with it is optional; its checks do not depend on it.
- **Turning the service on**: setup adds this product's service to the existing list rather than
  replacing it. Spike: setting the list over ADB worked on Android 14 with no prompt on the TV.
- **After a restart**: Android keeps the enabled-services setting and starts the service again at
  start-up; the fallback in decision-home-takeover covers Google TV's home screen if it is the
  first screen. Nothing runs at the PC. [to validate] in the first build: whether the service is
  running before Google TV's home screen first appears.
- **The checks**: "home app installed and enabled" from Android's package list; "service running"
  from Android's accessibility status; "home screen covered when it opens" by asking Android to
  open its home screen (Google TV's) and timing how long until the quiet home screen is in front.
  That check tests only the fallback: the Home key cannot be tested from the PC (keys sent over ADB
  do not reach accessibility services), which is why the procedure asks for the remote check.

## Accessibility

- Plain text, line by line, with no colour needed: every result is a word ("yes", "FAILED",
  "NOT FOUND"), and steps and results are separated by a colon rather than dot leaders.
- Every check the scripts make is in text; the picture comparison is optional.
