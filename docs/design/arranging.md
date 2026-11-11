---
id: design-arranging
status: approved
derived-from:
  - story-arranging-reorder: 9b4b345c6750
  - story-arranging-hide: 840d019d7142
  - story-arranging-guard: f90253694a7a
  - story-arranging-from-pc: b9b68bf58f66
  - persona-setup-person#goals: 1b5715ea18d9
  - persona-viewer#frustrations: 8722d20eb95e
---

# Arranging the apps: design

<!-- docs/design/arranging.md. Ordering and hiding the household's apps, on the TV and from the PC.
The user documentation (docs/user/arranging.md) is written from this. Mock-ups:
docs/design/arranging/mockup.html (the source), rendered to PNG by render.sh, in the home screen's
look (design-home-screen). The project lead's choices, 2026-11-10: hold select for 5 seconds, then
confirm; hidden apps in a separate row; a text file in Notepad from the PC; all four stories in this
feature. Revised after a persona walkthrough the same day. -->

## Personas

- persona-setup-person: puts the household's apps in a sensible order and hides the ones nobody
  uses, on the TV with the remote for a small change, or from the PC for a large one. Changes are
  saved as they are made, so nothing is lost by stopping halfway, and the PC keeps dated copies, so
  removing and setting up the quiet home screen again does not lose the arrangement.
- persona-viewer: nothing they do by accident moves or hides an app. The only way in is a 5-second
  hold followed by a question whose first answer is **Cancel**; if they go past it by mistake, Back
  leaves with nothing changed, and arrange mode closes by itself if someone walks away from it.
  This guard stops accidents; someone who knows about the hold could still arrange the apps on
  purpose, which the project lead accepted (REQ-home-3 and 4, amended 2026-11-10, #41).

## Flow

On the TV:

1. The set-up person moves to any app on the quiet home screen and holds select. After 5 seconds the
   screen asks **Arrange apps?**, with **Cancel** selected. Letting go before 5 seconds opens the
   app, as a press does.
2. They choose **Arrange apps**. The top row is replaced by **Arranging apps**, a line of
   instructions, and how many apps are hidden. The apps are as they are on the home screen, with the
   app they held still selected; below them is the **Hidden apps** row.
3. To move an app: select it, choose **Move**, use the arrows to carry it to its new place, and press
   select to put it down there. Back while moving puts it back where it was.
4. To hide an app: select it and choose **Hide**. It moves to the end of the **Hidden apps** row, the
   status line says so, and the selection moves to the app that took its place.
5. To show a hidden app: move down to the **Hidden apps** row, select it, and choose **Show**. It goes
   back at the end of the apps, and the status line says so.
6. Press Back (or Home) when done. The quiet home screen shows the new arrangement, scrolled to the
   top. Each change was saved when it was made.

From the PC:

1. The set-up person double-clicks **Arrange apps** (`quiet-arrange.cmd`) in the Quiet Launcher
   folder, or runs `powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1` there, as for
   setup.
2. The script connects to the TV as setup does and reads the current arrangement.
   - If the apps on the TV have never been arranged (after undo and a new setup, say) and the PC has
     a saved arrangement for this TV, it offers to put the newest one back (y/n). Yes sends it and
     stops.
3. Otherwise it writes the arrangement to `arrangement-<serial>.txt` in Documents\QuietLauncher,
   opens it in Notepad, and waits.
4. The set-up person moves lines up or down, moves a line below **Hidden:** to hide that app or above
   it to show it again, and saves the file. Then they press Enter in the script's window (or type n
   and Enter to send nothing).
5. The script checks the file, sends it to the TV, checks that the TV has it, keeps a dated copy
   (`arrangement-<serial>-<date>.txt`), and says how many apps are shown and hidden. The next press
   of Home shows it.
6. Undo (design-setup) saves the TV's arrangement as a dated copy before it removes the app, so it is
   there to put back after setting up again.

## Mock-ups

Checked against the quiet home screen as built (2026-11-10) on a Chromecast with Google TV (Android
14). The apps are the reference device's, arranged as a set-up person might: Prime Video and CBC
News moved up, ExpressVPN and Tubi hidden. Tiles are placeholders for each app's banner.

| Screen or state | Mock-up | Notes |
|---|---|---|
| Asking first | ![Arrange apps?](arranging/confirm.png) | After holding select on Prime Video for 5 seconds. **Cancel** is selected |
| Arrange mode | ![Arrange mode](arranging/arrange.png) | Prime Video, the app held, is selected. The heading says how many apps are hidden, and where |
| Menu on an app | ![Move or Hide](arranging/menu.png) | Select on an app; **Move** is selected |
| Moving an app | ![Moving](arranging/moving.png) | Prime Video carried from fourth to second place; the status line says what the keys do |
| After hiding an app | ![Hidden](arranging/after-hide.png) | Tubi has gone to the **Hidden apps** row; the status line says so; Plex, which took its place, is selected |
| Menu on a hidden app | ![Show](arranging/hidden-menu.png) | The grid has scrolled to the **Hidden apps** row |
| No hidden apps | ![No hidden apps](arranging/no-hidden.png) | Scrolled to the end: the row says how to hide one |
| The last app on the home screen | ![Last app](arranging/last-app.png) | Its menu has no **Hide**; the status line says why |

The PC script's output (PowerShell; text in angle brackets varies):

```
PS> powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1
Quiet Launcher: arrange apps

[1/3] TV: connected to 10.0.0.202:34447
[2/3] Arrangement: opened in Notepad (Documents\QuietLauncher\arrangement-<serial>.txt)
      Move lines to change the order; move a line below "Hidden:" to hide that app.
      Save the file, then press Enter here to send it (or type n to send nothing): 
[3/3] Sent to the TV: 17 apps shown, 2 hidden. Press Home to see it.
      Copy kept: Documents\QuietLauncher\arrangement-<serial>-<date>.txt

Done. Log: Documents\QuietLauncher\arrange-<date>.log
```

After undo and a new setup:

```
[1/3] TV: connected to 10.0.0.202:34447
[2/3] Arrangement: the apps on this TV have not been arranged.
      Put back the arrangement saved on <date> (17 apps shown, 2 hidden)? (y/n): y
[3/3] Sent to the TV: 17 apps shown, 2 hidden. Press Home to see it.
```

Other results at step 3:

```
[3/3] Nothing changed.
```

```
[3/3] Skipped 1 line that is not an app on this TV: "Hulu"
      Sent to the TV: 17 apps shown, 2 hidden. Press Home to see it.
```

```
[3/3] The apps were arranged on the TV since this list was opened.
      Send your list anyway, replacing what was done on the TV? (y/n): n
      Nothing changed. Run this again to start from the TV's arrangement.
```

The file, as Notepad shows it:

```
# Quiet Launcher: the order of the apps on the home screen, one per line.
# Move lines to change the order. Apps below "Hidden:" are not on the home screen.
# Save the file, then press Enter in the script's window. Lines starting with # are ignored.
YouTube
Netflix
Disney+
Prime Video
CBC News
...
Google Play Store

Hidden:
ExpressVPN
Tubi
```

## Layout

- **Asking first**: a panel centred on the dimmed home screen, titled **Arrange apps?**, with
  **You can change the order of the apps and hide the ones nobody uses. The changes are for
  everyone who uses this TV.** and two buttons, **Cancel** (selected) and **Arrange apps**. Back
  is the same as **Cancel**.
- **Arrange mode** replaces the top row with the heading **Arranging apps**, the line **Select an
  app to move or hide it. Press Back when you are done; changes are saved as you go.**, and, at the
  right of the heading, **2 hidden apps, at the end** (or **No hidden apps**). The status line
  is under them, as on the home screen. The search box, **Voice search**, **Google TV**,
  **Settings**, and the clock are not shown.
- **Apps** are in the same grid as the home screen (five per row, the same tiles and selection
  look), followed by the **Hidden apps** label with **Not on the home screen. Select one to show it
  again.**, and then the hidden apps' row, their banners darkened and marked **Hidden**. With no
  hidden apps, the row says **No hidden apps. To hide one, select it and choose Hide.** Movement is
  the home screen's (no wrapping); Down from the last row of apps goes to the **Hidden apps** row.
- **Menu**: beside the selected app (to its right, or to its left for an app in the last column), a
  short list: **Move**, **Hide**, **Cancel** on an app; **Show**, **Cancel** on a hidden app. The
  first item is selected. Back closes it.
- **Moving**: the app is lifted (enlarged, with a dashed blue outline and arrows at its sides) and
  the status line says **Moving <app>. Use the arrows, then press select to put it here. Back puts it
  back.** Left and right move it one place in the list, across the end of a row to the next or
  previous row; up and down move it a row (five places), and go no further than the first and last
  places. The other apps shift to make room as it moves. It cannot be moved into the **Hidden apps**
  row.
- **Status line messages**: **<app> hidden. It is in the Hidden apps row.**, **<app> shown, at the
  end of the apps.**, **Keep at least one app on the home screen.**
- **Names**: **Arrange apps** (the question's button, and the PC launcher), **Arranging apps**,
  **Move**, **Hide**, **Show**, **Cancel**, **Hidden apps**, **Hidden**. The PC launcher is
  `quiet-arrange.cmd`, shown in Explorer as **quiet-arrange**; the script is `quiet-arrange.ps1`;
  its file is the arrangement file.

## States and errors

- **Holding select**: nothing shows during the 5 seconds [assumed]: with no hint, a viewer holding
  select out of habit only sees their app open, and nothing on the screen invites a guest to keep
  holding. The app opens when select is let go before 5 seconds, as it does now.
- **A viewer who chose Arrange apps by mistake**: nothing changes until an app is moved or hidden;
  Back or Home leaves. The viewer's documentation says so.
- **Leaving arrange mode**: Back (with no menu open and nothing being moved), Home, or 2 minutes with
  no button pressed [assumed]. If an app is being moved when it times out, it is put back where it
  was. The remote's YouTube, Netflix, and voice buttons work as they always do; opening another app
  leaves arrange mode, and Home then shows the quiet home screen as usual.
- **The last app on the home screen** cannot be hidden, because arrange mode is reached by holding
  select on an app: its menu has no **Hide**, and the status line says **Keep at least one app on
  the home screen.** If every app shown is uninstalled, the hidden ones are shown again, so the
  home screen is never left with nothing to hold select on while apps are installed.
- **An app is installed while arranging**: it appears at the end of the apps. **An app is removed**:
  it leaves the list, and the rest keep their order. If it was being moved, moving stops.
- **The PC sends an arrangement while arrange mode is open**: arrange mode closes and the home screen
  shows the new arrangement.

From the PC:

- **The TV is not reachable**, **more than one device**, **ADB not found**: the same messages as
  setup (design-setup, Errors), and nothing is changed.
- **The app is not installed**: **[1/3] TV: the quiet home screen is not set up on this TV. Run
  quiet-setup first.** and nothing is changed.
- **The arrangement on the TV changed while the file was open** (someone arranged on the TV): the
  script asks before replacing it, as in the mock-up; n sends nothing.
- **A line that is not an app on the TV** (a typo, or an app since removed): skipped, and named.
  Names match whatever their capitals and spaces. Two installed apps with the same name are told
  apart in the file by their package name in brackets after the name, for example
  `YouTube (com.google.android.youtube.tv)`; the script writes it that way, and a line with only the
  name then matches neither and is skipped.
- **An installed app missing from the file**: it stays shown, at the end.
- **The file has no "Hidden:" line**: **The file has no "Hidden:" line. Put it back (on a line of its
  own) and try again.** and nothing is sent.
- **The file has no apps above "Hidden:"**: **The file hides every app; keep at least one above
  "Hidden:".** and nothing is sent.
- **Unchanged, or n typed**: **Nothing changed.**

## How it works

- **What is stored**: one small file in the app's own storage on the TV: the arranged apps' package
  names in order, and the hidden ones. Nothing is stored per profile, so every profile sees the same
  arrangement. It survives restarts and updates of the app and of the TV's system; uninstalling the
  app (undo) or resetting the TV deletes it.
- **Building the list**: the installed apps (as now, design-home-screen, How it works) in the stored
  order, then any apps not in it (newly installed ones) in install order, with the hidden ones left
  out of the home screen. Before anything is arranged, the order is as now: install order, the Play
  Store last. Once arranged, the Play Store is wherever the set-up person put it, and new apps go
  after it.
- **Holding select**: the home screen times how long select has been held on an app tile. At 5
  seconds it shows the question and ignores the rest of that hold; letting go earlier opens the app,
  on release as now, so a press is no slower than today (REQ-home-10). The home screen does not use
  Android's own long press (about half a second).
- **From the PC**: the app has a receiver that only ADB's shell can call (it requires a permission
  that the shell has and that apps cannot be given, `android.permission.DUMP`) [to validate on the
  reference device]. The script asks it for the current arrangement (each app's name, package, and
  whether it is hidden, in order, and a number that changes with every change) with `adb shell am
  broadcast`, reading the reply from the broadcast's result [to validate], and sends the new one the
  same way, as base64 text so the names need no quoting. The receiver refuses a send whose number is
  out of date unless the script says to replace it; that is how the script knows the TV's
  arrangement changed while the file was open. The receiver saves it and tells the home screen,
  which redraws.
- **Waiting for the edit**: the script does not wait for Notepad to close, because Windows 11's
  Notepad can hand the file to a window that is already open and return at once; it waits for Enter
  in its own window instead.
- **Copies on the PC**: every send keeps `arrangement-<serial>-<date>.txt`; undo writes one before
  uninstalling, when the app still answers. The offer to put one back is made only when the TV has
  never been arranged (its stored file is missing), and offers the newest copy for that TV's serial
  number.
- **The launcher**: `quiet-arrange.cmd` runs the script with PowerShell's execution policy bypassed
  for that run only, and keeps the window open at the end so the result can be read. Dropping an
  arrangement file onto it sends that file (`-File`).

## Accessibility

- Everything works with the direction pad, select, and back. The hold is the only timed action, and
  the PC script does everything it does without one.
- Contrast: the heading and instructions are the home screen's text colours (8.8:1 and above); the
  status line's `#ffd28a` is 13:1; the moving outline `#8ab4f8` is 8.6:1 on the background, with a
  black edge outside it; hidden apps' names stay in the muted text colour (8.8:1) while their
  banners are darkened, and the **Hidden** badge is white on black.
- With TalkBack on:
  - Entering arrange mode is announced: "Arranging apps. Select an app to move or hide it. Press
    Back when you are done. 2 hidden apps, at the end."
  - Each app reads its name, "app", and its place ("3 of 17"); a hidden app reads "<name>, hidden
    app".
  - While moving, each step is announced: "Prime Video, 2 of 17". Putting it down: "Prime Video
    placed, 2 of 17". Back: "Prime Video put back, 4 of 17".
  - The status line's messages are announced as they appear.
  - Holding select with TalkBack on [to validate]: TalkBack may use the select key itself; the PC
    script is the way in if it does.
- With "remove animations" on, the moving app jumps from place to place and nothing slides.
