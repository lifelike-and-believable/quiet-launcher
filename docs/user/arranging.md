---
id: docs-arranging
status: draft
derived-from:
  - design-arranging: f17ddef2fa26
  - story-arranging-reorder: 9b4b345c6750
  - story-arranging-hide: 02d0954dc7ee
  - story-arranging-guard: 205a70170649
  - story-arranging-from-pc: aca2d1c464f7
---

# Arranging the apps

<!-- docs/user/arranging.md. For the set-up person (persona-setup-person). Written as if the
product exists. Specs cite sections by heading, for example docs-arranging#move-an-app. -->

You can put the apps on the quiet home screen in the order your household uses them, and hide the
ones nobody uses. Hidden apps stay installed. The arrangement is the same for everyone who uses the
TV, and it stays after a restart or a system update.

You can arrange the apps on the TV with the remote, or from your PC by editing a list in Notepad.
Both change the same arrangement; whichever you changed last is what shows.

## Start arranging on the TV

1. Press Home, then move to any app.
2. Hold select for 5 seconds. If you let go sooner, the app opens instead.
3. The TV asks **Arrange apps?** Move right to **Arrange apps** and press select.

The top of the screen now says **Arranging apps**. Your apps are below, in their current order, and
below them is the **Hidden apps** row.

Arranging is behind the 5-second hold and the question so that nobody changes it by accident. The
question starts on **Cancel**, so pressing select again changes nothing.

## Move an app

1. Select the app and choose **Move**.
2. Use the arrows to carry it to its new place. Left and right move it one place; up and down move
   it one row.
3. Press select to put it down.

To change your mind while moving it, press Back: the app goes back where it was.

## Hide an app

Select the app and choose **Hide**. It moves to the **Hidden apps** row and is no longer on the
home screen. It is still installed, and it still opens from search and voice results.

You cannot hide the last app on the home screen.

## Show a hidden app

1. Move down to the **Hidden apps** row.
2. Select the app and choose **Show**.

It goes back on the home screen, at the end.

## Finish

Press Back or Home. The home screen shows your new arrangement. Every change was saved as you made
it, so there is nothing else to do.

If nobody presses a button for 2 minutes, arranging closes by itself. An app you were moving at the
time goes back where it was; everything else you changed is kept.

## Arrange the apps from your PC

You need the PC you set up the quiet home screen with (docs-setup), with the TV on and wireless
debugging on.

1. In the Quiet Launcher folder, double-click **Arrange apps** (`quiet-arrange.cmd`). Or, in
   PowerShell in that folder, run `.\quiet-arrange.ps1`.
2. Notepad opens with your apps, one per line, in their order on the home screen. The apps under
   **Hidden:** are hidden.
3. Change the order by moving lines (cut and paste). Move a line below **Hidden:** to hide that app,
   or above it to show it again.
4. Save, and close Notepad.

The script sends the list to the TV and says, for example,
**Sent to the TV: 17 apps shown, 2 hidden. Press Home to see it.**

- If a line is not an app on the TV (a typo, say), the script skips it and names it. Fix it and run
  the script again.
- An app that is missing from the list stays on the home screen, at the end.
- If you close Notepad without changing anything, the script says **Nothing changed.**

The list is saved as `arrangement-<serial>.txt` in Documents\QuietLauncher. Keep it: it is your copy
of the arrangement.

## Put a saved arrangement back

Removing the quiet home screen (docs-setup#remove-it) also removes its arrangement. After you set it
up again, send the list you saved:

```
.\quiet-arrange.ps1 -File "$HOME\Documents\QuietLauncher\arrangement-<serial>.txt"
```

New apps installed since you saved the list appear at the end.

## If something goes wrong

- If the app opened instead of the question, you let go of select before 5 seconds. Hold it longer.
- If the TV asked **Arrange apps?** and you did not mean it, press select (on **Cancel**) or Back.
  Nothing has changed.
- If the script cannot reach the TV, it says what to do, as setup does (docs-setup#if-something-goes-wrong).
  Usually wireless debugging is off after a restart: turn it back on and run the script again.
- If the script says the quiet home screen is not set up on this TV, run `quiet-setup` first.
- If an app you hid is gone from the list on the PC, it was uninstalled from the TV. If you install
  it again, it comes back at the end of the home screen, shown.
