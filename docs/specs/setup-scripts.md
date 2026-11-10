---
status: draft
tests: [scripts/tests]
tests-commit: 8837b1b69c4c13b21cd6c9e227404b62d06d2b5b
unlocks:
  - scripts/tests/test_scripts.py: AC-1 (ADB found or not) was only tested with QUIET_ADB set, so the project lead's first real run stopped at ADB NOT FOUND (the scripts' Adb function hid adb.exe); adds a run option that puts an adb program on PATH instead, and a test that both scripts find it; AC-2 (more than one device): the project lead's TV was listed twice (its address and its mDNS name, one serial number), which setup took for two devices, so a test lists the same TV twice and expects it to connect, and the existing two-device test gives the emulator its own serial number, as a real second device has; nothing else changes; AC-10 and AC-6: the project lead's run in Windows PowerShell 5.1 stopped at adb's stderr warning from am start, and the TV was asleep, so tests run 5.1 with the fake as a separate program and set up a sleeping TV; nothing else changes
  - scripts/tests/fake-adb.ps1: AC-2 and AC-3: answers as the reference device did when checked on 2026-11-10, get-serialno with the connection's name (10.0.0.202:34447) and getprop ro.serialno with the TV's serial number (17241HFDD2WLBM), the second from an optional serials map per device, so two entries can be one TV or two devices; the fake had answered get-serialno with the serial number, which hid that backup-<serial>.json would have been named after the address; AC-6 and AC-10: as the reference device did on 2026-11-10, am start for the home screen writes adb's warning to stderr, and a sleeping TV (awake: false) has no resumed activity until KEYCODE_WAKEUP; AC-6: as the reference device did on 2026-11-10, opening the home screen while Google TV's home screen is already in front opens no window, so the service has nothing to cover (the project lead's run failed check 3 that way), and am start for android.settings.SETTINGS opens the TV's Settings
---

# The setup and undo scripts

<!-- docs/specs/setup-scripts.md. Delivery slice 5 (PLAN.md, step 10): quiet-setup.ps1 and
quiet-undo.ps1, as design-setup and docs-setup describe them. -->

## Problem

The set-up person installs, recovers, and removes the quiet home screen from a Windows PC, by one
written procedure each (story-setup-set-up, story-setup-restart, story-setup-recover,
story-setup-undo). The scripts do the work and check it, so a change to the household's TV is
deliberate, repeatable, and reversible (persona-setup-person#goals).

## User stories

- story-setup-set-up, story-setup-recover, story-setup-undo (story-setup-restart needs nothing from
  the scripts: the service comes back by itself).

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Without ADB, setup stops at `[1/6] ADB: NOT FOUND` with the winget command, and exits non-zero having changed nothing. Without `-Apk`, setup says **Give the app's install file with -Apk, for example: -Apk .\quiet-launcher.apk** and exits non-zero. | test: test_setup_errors |
| AC-2 | With the TV not paired, the script asks for the pairing address and code, pairs, and connects; a wrong code gives `TV: PAIRING FAILED (wrong or expired code, or wrong address)` and asks again, up to 3 times, then stops. An unreachable TV gives `TV: COULD NOT CONNECT`; more than one device gives `TV: MORE THAN ONE DEVICE` with the devices listed and the `-Tv` example. | test: test_setup_tv |
| AC-3 | First setup saves the TV's accessibility settings (`enabled_accessibility_services`, `accessibility_enabled`) to `backup-<serial>.json` and a picture of Google TV's home screen to `before-<serial>.png`, in the Quiet Launcher folder of the user's Documents. A later run keeps that backup (`Backup: kept the one from <date>`). If this product's service is already on when the first backup is taken, it is left out of the recorded value and the picture is skipped with **Picture skipped: the quiet home screen is already on.** | test: test_setup_backup |
| AC-4 | Setup installs the app, or skips it when the same install file is already installed (`Install: already installed (<version>); skipped`). An install that fails stops with `Install: FAILED: <ADB's message>` and nothing else changed on the TV. | test: test_setup_install |
| AC-5 | Setup turns the service on by adding it to the existing list, keeping any other service, and sets `accessibility_enabled` to 1; `Turn on: already on` when it is; `Turn on: was off; turned it back on` when a backup exists and the service is off (recovery). | test: test_setup_turn_on |
| AC-6 | Setup's checks print `Google TV home app installed and enabled: yes`, `Quiet Launcher service running: yes`, and `Google TV's home screen covered when it opens: yes (<seconds> s)`, or `NO` / `FAILED` with what to do (design-setup, Errors), exiting non-zero on a failure. | test: test_setup_checks |
| AC-7 | Setup asks the two remote checks (y/n) and records the answers in the log; an n gives the design's message (**Check the TV's internet connection; if it is fine, run quiet-undo.ps1 and open an issue with the log.** or **Run quiet-undo.ps1 and open an issue with the log.**) and a non-zero exit. A run where everything was already set up says `Nothing changed.` | test: test_setup_remote_checks |
| AC-8 | Undo restores the recorded accessibility settings (deleting a setting that was unset), renames the backup to `backup-<serial>-undone-<date>.json`, uninstalls the app (`already removed` if it is gone), checks `Android's home screen is Google TV's: yes` and the Google TV home app, and asks the two remote checks. | test: test_undo |
| AC-9 | Undo without a backup turns off only this product's service, leaving any others, and says **No backup on this PC: turned off only Quiet Launcher's service and left any others as they were.** | test: test_undo |
| AC-10 | Both scripts write everything they print, with the answers, to `setup-<date>.log` or `undo-<date>.log` in the same folder; exit 0 on success; and have comment-based help (`Get-Help`) with a synopsis, the options, and examples. They run in Windows PowerShell 5.1 and PowerShell 7. | test: test_common |
| AC-11 | `build.ps1` copies both scripts into `out\QuietLauncher` beside the install file. | command: `powershell -ExecutionPolicy Bypass -File .\build.ps1`, then list `out\QuietLauncher` |
| AC-12 | On the reference device: setup from the built folder pairs or connects, installs, turns on, passes its checks, and Home shows the quiet home screen; undo puts the TV back as it was (accessibility settings, Google TV's home screen). Each under 15 minutes. | command: run both scripts against the TV with the project lead (it changes the TV's home screen and settings) |

A test cites a criterion as `setup-scripts#AC-1` in its name or a comment.

## Scope

- In: `scripts/quiet-setup.ps1`, `scripts/quiet-undo.ps1`, the fake ADB and tests under
  `scripts/tests/`, and a CI step that runs them with PowerShell 7 on Linux.
- Out: building the app (build.ps1 already does it).

## APIs and assumptions

- ADB commands, all seen on the reference device in the spikes: `devices`, `mdns services`, `pair`,
  `connect`, `install -r`, `uninstall`, `shell settings get|put|delete secure ...`, `shell pm list
  packages -e`, `shell pm path`, `shell sha256sum`, `shell dumpsys package|accessibility|activity`,
  `shell am start`, `shell screencap`, `pull`, `shell getprop` (`ro.serialno` names the TV: `get-serialno` gives the connection, which changes).
- For tests: the scripts use the ADB named by `QUIET_ADB` if set, the folder named by
  `QUIET_FOLDER` if set (instead of Documents\QuietLauncher), and the answers in `QUIET_ANSWERS`
  (separated by `|`) before asking at the console. Set-up persons never need these.
- Assumption: comparing the installed APK's SHA-256 (`sha256sum` on the TV) with the install file's
  tells whether it is the same build.

## Delivery

- One pull request, merged with a merge commit; then AC-12 with the project lead.
