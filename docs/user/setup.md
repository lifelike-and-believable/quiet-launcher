---
id: docs-setup
status: approved
derived-from:
  - design-setup: 9c53a9c2d0a7
  - story-setup-set-up: 6a69bd06742a
  - story-setup-restart: 1f1806140267
  - story-setup-recover: 734f151fb73e
  - story-setup-undo: 37a930b0c7c5
---

# Setting up, recovering, and removing the quiet home screen

<!-- docs/user/setup.md. For the set-up person (persona-setup-person): the project lead, at a
Windows PC with ADB. Written as if the product exists. Specs cite sections by heading, for example
docs-setup#set-it-up. -->

The quiet home screen is set up from a Windows PC over ADB, by one person. Nobody else in the
household needs to do anything. Setup takes under 15 minutes; undo takes under 15 minutes too.

## What you need

- A Windows PC on the same network as the TV, with ADB (platform-tools 30 or newer, for wireless
  pairing). If you do not have it: `winget install Google.PlatformTools`, then open a new
  PowerShell window.
- The Quiet Launcher folder: the app's install file, `quiet-launcher.apk`, and the scripts that
  set up and remove the quiet home screen: **quiet-setup** and **quiet-undo**, each a `.cmd` file
  to double-click and the `.ps1` script it runs. Build it from the repository: in a clone of
  github.com/lifelike-and-believable/quiet-launcher, run `.\build.ps1`, which writes the folder to
  `out\QuietLauncher`. The repository's README lists the build tools it needs. Nothing is
  published; this is the only way to get it.
- The TV's remote.

## Turn on wireless debugging

Do this once before the first setup, and again after a reset (a reset hides developer options and
turns wireless debugging off). On the TV, with the remote:

1. Open Settings, then System, then About.
2. Select **Android TV OS build** seven times. This turns on developer options.
3. Go back to System, open Developer options, and turn on **Wireless debugging**.

Leave the TV on. You pair it with the PC during setup.

## Set it up

1. On the PC, open the Quiet Launcher folder (`out\QuietLauncher`).
2. Double-click **quiet-setup** (`quiet-setup.cmd`). A window opens and the script runs in it. At
   the end the window says **Press any key to continue . . .**; it closes when you press a key. Or,
   if you prefer, open PowerShell in the folder and run:

   ```
   powershell -ExecutionPolicy Bypass -File .\quiet-setup.ps1 -Apk .\quiet-launcher.apk
   ```

3. If the TV is not yet paired with this PC, the script tells you what to open on the TV. On the TV,
   open Settings, System, Developer options, **Wireless debugging**, then **Pair device with
   pairing code**. Type the pairing address (the one shown under the code, not the one at the top
   of the screen) and the code into the script.
4. The script then works through its steps, one line each:
   - it saves a backup of the TV's accessibility settings and a picture of Google TV's home screen
     in `Documents\QuietLauncher`;
   - it installs the app and turns it on;
   - it checks that Google TV's own home app is installed and enabled (search needs it), that the
     quiet home screen's service is running, and that Google TV's home screen is covered when it
     opens.
5. When the script asks, check with the remote and answer y or n:
   - Press Home. The quiet home screen should appear, with no Google TV home screen first.
   - Search for the show the script names. Google's page for it should open.

A first setup looks like this (some lines shortened):

```
[1/6] ADB: found (platform-tools 36.0.0)
[2/6] TV: not paired with this PC
      ...
      Paired. Connected to 10.0.0.202:39423 (Chromecast with Google TV, Android 14)
[3/6] Backup: saved to Documents\QuietLauncher\backup-<serial>.json
[4/6] Install: installed quiet-launcher 1.0.0
[5/6] Turn on: accessibility service on
[6/6] Checks
      Google TV home app installed and enabled: yes
      Quiet Launcher service running: yes
      Google TV's home screen covered when it opens: yes (0.3 s)
...
Done in 3 min 12 s. Log: Documents\QuietLauncher\setup-<date>.log
```

Your y/n answers are in the log. If a check fails, see "If something goes wrong".

## After a restart or power cut

Nothing to do. When the TV starts, the quiet home screen comes up by itself. Google TV's home screen
may show for a moment first; that is expected.

## Put it back after a TV update or reset

Google TV's home screen can show for a moment (less than half a second) in normal use, for example
when someone leaves an app they opened with the remote's YouTube or Netflix button. If it stays,
and pressing Home does not bring the quiet home screen back, an update or reset has turned the
quiet home screen off. Run the setup steps again, exactly as above.

- After an update, step 5 says `Turn on: was off; turned it back on`.
- After a reset, turn on wireless debugging again first (see above); the script pairs again.
- If everything was already working, the script says `Nothing changed.` Running it does no harm.
- With a newer install file than the one on the TV, the script updates the app. The quiet home
  screen stops for a few seconds while it is replaced, and Google TV's home screen may show in that
  time.

The script keeps your first backup (`Backup: kept the one from <date>`), so undo still puts the TV
back as it was before you first set up the quiet home screen. If the quiet home screen is already
on when a backup is first taken (for example, setting up from a second PC), the script leaves this
product out of the backup and says **Picture skipped: the quiet home screen is already on.**

## Remove it

1. On the PC, open the Quiet Launcher folder.
2. Double-click **quiet-undo** (`quiet-undo.cmd`). Its window stays open at the end until you press
   a key, as setup's does. Or open PowerShell in the folder and run:

   ```
   powershell -ExecutionPolicy Bypass -File .\quiet-undo.ps1
   ```

3. If the TV is not paired with this PC, the script pairs it, as in setup.
4. The script restores the accessibility settings from the backup, uninstalls the app, opens Google
   TV's home screen, and checks that Android's home screen is Google TV's and that Google TV's home
   app is installed and enabled.
5. When the script asks, check with the remote and answer y or n:
   - Press Home. Google TV's home screen should appear.
   - Search, your apps, and Settings should all work.

If you like, compare the TV with the picture `Documents\QuietLauncher\before-<serial>.png`. It
should look the same apart from the shows in its rows.

Undo works even if the quiet home screen is not working, or setup stopped half way. If the app was
already removed, step 3 says it is already removed. The script keeps the backup, renamed
`backup-<serial>-undone-<date>.json`, so the next setup takes a fresh one.

If you have arranged the apps (docs-arranging), undo saves the arrangement to Documents\QuietLauncher
before it removes the app, and says so under step 3. After you set it up again, the arrangement
script offers to put it back (docs-arranging#put-an-arrangement-back-after-setting-up-again). If the
quiet home screen is not working, undo cannot read the arrangement; the copy saved the last time
you used the arrangement script is still there.

## Options

| Option | Use it when |
|---|---|
| `-Apk <file>` | Always, for setup: the app's install file |
| `-Tv <address>` | More than one device is connected to ADB (setup and undo) |
| `-Pair` | You want to pair again, for example after the TV was reset (setup and undo) |

`Get-Help .\quiet-setup.ps1` (or `.\quiet-undo.ps1`) lists them with examples. Each script exits
with code 0 when it finishes and a non-zero code when it stops.

## If something goes wrong

When a step fails, the script stops, says in capitals what happened, and says what to do. Nothing
on the TV has changed unless the message says so.

| You see | What it means | What to do |
|---|---|---|
| A red PowerShell error about running scripts | The script was started without `-ExecutionPolicy Bypass` | Use the command exactly as shown above |
| **Give the app's install file with -Apk, for example: -Apk .\quiet-launcher.apk** | `-Apk` is missing | Add it |
| `ADB: NOT FOUND` | ADB is not installed, or this window started before it was | `winget install Google.PlatformTools`, open a new PowerShell window, and run the script again |
| `TV: PAIRING FAILED (wrong or expired code, or wrong address)` | The code was mistyped or has expired, or the address is not the pairing one | On the TV, open **Pair device with pairing code** again for a new code, and use the address shown under the code; the script asks up to 3 times |
| `TV: COULD NOT CONNECT` | The TV is off, on another network, or wireless debugging is off | Wake the TV with the remote, check the network and **Wireless debugging**, and run again; after a reset, add `-Pair` |
| `TV: MORE THAN ONE DEVICE` | ADB sees another device too | Run again with `-Tv` and the TV's address, which the message lists |
| `Install: FAILED` | ADB could not install the app | Nothing on the TV changed. Run again; if it fails again, open an issue with the log (below) |
| `Turn on: FAILED` | The app is installed but not turned on | The TV still uses Google TV's home screen. Run again, or run undo to remove the app |
| `Google TV home app installed and enabled: NO` | Google TV's home app is disabled or missing, so search cannot open its results | Re-enable Google TV in the TV's Settings, Apps, then run setup again |
| `Quiet Launcher service running: NO` | The service is turned on but Android has not started it | Restart the TV and run setup again; if it still says NO, run undo and open an issue with the log |
| `Google TV's home screen covered when it opens: FAILED` | The quiet home screen did not cover Google TV's | Run undo, then open an issue with the log |
| **Check the TV's internet connection; if it is fine, run quiet-undo.ps1 and open an issue with the log.** | You answered n to the search check | A search needs the internet; the quiet home screen does not. If the internet is fine, run undo and open an issue |
| **Run quiet-undo.ps1 and open an issue with the log.** | You answered n to the Home check | Run undo, and open an issue |
| **No backup on this PC: turned off only Quiet Launcher's service and left any others as they were.** (undo) | Setup was never finished on this PC | Nothing else to do; check the TV with the remote |

To open an issue: github.com/lifelike-and-believable/quiet-launcher/issues, with the log file from
`Documents\QuietLauncher` attached.
