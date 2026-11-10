---
id: design-setup
status: draft
derived-from:
  - story-setup-set-up:
  - story-setup-restart:
  - story-setup-recover:
  - story-setup-undo:
  - decision-home-takeover:
  - persona-setup-person#goals:
  - persona-setup-person#skills-and-tools:
  - persona-setup-person#frustrations:
---

# Setup design

<!-- docs/design/setup.md. Setting the product up, recovering it, and undoing it, from the set-up
person's Windows PC. The user documentation (the written procedures) is written from this. -->

## Personas

- persona-setup-person: one written procedure for each of set up and undo, each mostly one
  PowerShell script that checks its own work, so a change to the household's TV is deliberate,
  repeatable, and reversible. Recovery is the setup procedure again.

## Flow

**Set up** (also used to recover after an update or reset):

1. On the TV, with the remote: turn on developer options and **Wireless debugging**, and choose
   **Pair device with pairing code** (the procedure lists the exact menu path). Skip this if the TV
   is already paired with this PC.
2. On the PC, in PowerShell: `.\quiet-setup.ps1 -Apk <path to quiet-launcher.apk>`.
3. The script asks for the pairing address and code if the TV is not paired, then connects.
4. It saves a backup of the TV's accessibility settings and a screenshot of the current home screen,
   unless a backup from an earlier setup already exists.
5. It installs or updates the app, turns on its accessibility service (keeping any others that are
   on), and runs its checks.
6. It ends by asking the set-up person to do two checks with the remote: press Home, and search for
   a named show.

**Undo**: `.\quiet-undo.ps1`. It restores the saved accessibility settings, uninstalls the app,
returns to Google TV's home screen, and asks the set-up person to compare the TV with the saved
screenshot.

## Mock-ups

The interface is the scripts' output in PowerShell. Text in angle brackets varies.

**Set up, first time** (state: TV paired during the run):

```
PS> .\quiet-setup.ps1 -Apk .\quiet-launcher.apk
Quiet Launcher setup

[1/6] ADB ............................ found (platform-tools 36.0.0)
[2/6] TV ............................. not paired with this PC
      On the TV: Settings > System > Developer options > Wireless debugging >
      Pair device with pairing code.
      Pairing address shown on the TV (for example 10.0.0.202:41234): 10.0.0.202:41234
      Pairing code shown on the TV: 482913
      Paired. Connected to 10.0.0.202:39423 (Chromecast with Google TV, Android 14)
[3/6] Backup ......................... saved to Documents\QuietLauncher\backup-<serial>.json
      Screenshot of the current home screen: Documents\QuietLauncher\before-<serial>.png
[4/6] Install ........................ installed quiet-launcher 1.0.0
[5/6] Turn on ........................ accessibility service on
[6/6] Checks
      Google TV home app installed and enabled .... yes
      Quiet Launcher service running ................ yes
      Google TV home screen covered ................. yes (0.3 s)

Done in 3 min 12 s. Now check with the remote:
  1. Press Home: you should see the quiet home screen, never Google TV's.
  2. Search for "Severance": Google's page for it should open.
If either fails, run .\quiet-undo.ps1 to put the TV back.
```

**Set up again on a TV that is already set up** (state: recover, nothing to do):

```
PS> .\quiet-setup.ps1 -Apk .\quiet-launcher.apk
Quiet Launcher setup

[1/6] ADB ............................ found (platform-tools 36.0.0)
[2/6] TV ............................. connected to 10.0.0.202:39423
[3/6] Backup ......................... kept the one from <date> (not overwritten)
[4/6] Install ........................ already up to date (1.0.0)
[5/6] Turn on ........................ already on
[6/6] Checks
      ...
Nothing changed.
```

**Errors** (each stops the script, says what happened, and what to do; nothing on the TV has
changed unless it says so):

```
[1/6] ADB ............................ not found
      Install it with: winget install Google.PlatformTools
      then open a new PowerShell window and run this again.

[2/6] TV ............................. could not connect
      Check that the TV is on, on the same network, and that Wireless debugging is on.
      If the TV was reset, pair again: run this with -Pair.

[4/6] Install ........................ failed: <ADB's message>
      Nothing was changed on the TV apart from the backup on this PC. Run this again.

[6/6] Checks
      Google TV home screen covered ................. NO
      The service is on but did not cover Google TV's home screen. Run .\quiet-undo.ps1
      and report this with the file Documents\QuietLauncher\setup-<date>.log.
```

**Undo**:

```
PS> .\quiet-undo.ps1
Quiet Launcher undo

[1/4] TV ............................. connected to 10.0.0.202:39423
[2/4] Accessibility settings ......... restored from backup-<serial>.json
[3/4] Remove ......................... uninstalled quiet-launcher
[4/4] Checks
      Home opens Google TV's home screen ............ yes
      Google TV home app installed and enabled ...... yes

Done in 41 s. Compare the TV with Documents\QuietLauncher\before-<serial>.png: it should look
the same apart from the shows in its rows. Then check with the remote that search, your apps, and
Settings work.
```

If there is no backup (setup was never finished on this PC), undo turns off only this product's
service, leaves any other services as they are, and says so.

## Layout

- Two scripts, `quiet-setup.ps1` and `quiet-undo.ps1`, shipped beside the app's install file. They
  run in Windows PowerShell 5.1 or PowerShell 7 and need only ADB.
- Steps are numbered, each on one line with its result; anything that needs the set-up person is
  indented under the step. The same text goes to a log file,
  `Documents\QuietLauncher\setup-<date>.log` or `undo-<date>.log`.
- Options: `-Apk <path>` (required for setup), `-Tv <address>` (when there is more than one device),
  `-Pair` (pair again even if a pairing seems to exist).

## States and errors

- Already paired, not paired, paired but the address has changed (found again with ADB's network
  discovery), more than one device (asks for `-Tv`).
- First setup, setup again with nothing to do, setup again after an update turned the service off
  (turns it back on), setup after a reset (pairs, installs, backs up afresh).
- Each failure in the mock-ups above, with what to do next. A failure never leaves the TV without a
  working home screen: until the service is on, Google TV's home screen is still the home screen.
- Undo with a backup, without a backup, with the app already removed (restores settings only), and
  with the quiet home screen not working (undo needs only ADB).

## How it works

- **The backup**: setup reads `enabled_accessibility_services` and `accessibility_enabled` and
  writes them, with the TV's serial number and the date, to the backup file; it never overwrites an
  existing backup for the same TV. Undo writes them back. This is what makes "the accessibility
  settings are back to what they were" checkable.
- **Turning the service on**: setup adds this product's service to the existing list rather than
  replacing it, so another service that is on stays on. Spike: setting the list over ADB worked on
  Android 14 with no prompt on the TV.
- **After a restart**: Android keeps the enabled-services setting and starts the service again at
  start-up; the fallback in decision-home-takeover covers Google TV's home screen if it is the first
  screen. Nothing runs at the PC.
- **The checks**: "home app installed and enabled" from Android's package list; "service running"
  from Android's accessibility status; "home screen covered" by asking Android to open its home
  screen (Google TV's) and timing how long until the quiet home screen is in front. The Home key
  itself cannot be tested from the PC (keys sent over ADB do not reach accessibility services), so
  the remote check is part of the procedure.
- **Before screenshot**: taken over ADB before anything changes; undo's comparison is by eye.

## Accessibility

- Plain text output, no colour needed to read results; every result is a word ("yes", "NO",
  "failed"), not a symbol.
- Works with a screen reader in the terminal, since it is line-by-line text.
