---
id: docs-arranging
status: approved
derived-from:
  - design-arranging: 604d06415dcc
  - story-arranging-reorder: 9b4b345c6750
  - story-arranging-hide: 840d019d7142
  - story-arranging-guard: f90253694a7a
  - story-arranging-from-pc: b9b68bf58f66
---

# Arranging the apps

<!-- docs/user/arranging.md. For the set-up person (persona-setup-person). Written as if the
product exists. Specs cite sections by heading, for example docs-arranging#move-an-app. -->

You can put the apps on the quiet home screen in the order your household uses them, and hide the
ones nobody uses. Hidden apps stay installed. The arrangement is the same for everyone who uses the
TV, and it stays after a restart or a system update.

You can arrange the apps on the TV with the remote, or from your PC by editing a list in Notepad.
Both change the same arrangement.

## Start arranging on the TV

1. Press Home, then move to any app.
2. Hold select for 5 seconds. Nothing shows while you hold. If you let go sooner, the app opens.
3. The TV asks **Arrange apps?** Move right to **Arrange apps** and press select.

The top of the screen now says **Arranging apps**, with how many apps are hidden at the right. The
app you held is still selected. Below the apps is the **Hidden apps** row.

Arranging is behind the 5-second hold and the question so that nobody changes it by accident. The
question starts on **Cancel**, so pressing select again changes nothing.

## Move an app

1. Select the app and choose **Move**.
2. Use the arrows to carry it to its new place. Left and right move it one place, onto the next or
   previous row at the end of a row. Up and down move it one row. It stops at the first and last
   places.
3. Press select to put it down.

To change your mind while moving it, press Back: the app goes back where it was. **Cancel** in the
menu closes the menu without doing anything.

## Hide an app

Select the app and choose **Hide**. It moves to the end of the **Hidden apps** row, the line under
the heading says so, and the app that took its place is selected. It is no longer on the home
screen, but it is still installed, and it still opens from search and voice results.

You cannot hide the last app on the home screen.

## Show a hidden app

1. Move down to the **Hidden apps** row.
2. Select the app and choose **Show**.

It goes back on the home screen, at the end of the apps.

## Finish

Press Back or Home. The home screen shows your new arrangement, from the top. Every change was saved
as you made it, so there is nothing else to do.

If nobody presses a button for 2 minutes, arranging closes by itself. An app you were moving at the
time goes back where it was; everything else you changed is kept.

## Arrange the apps from your PC

You need the PC you set up the quiet home screen with (docs-setup), with the TV on and wireless
debugging on.

1. In the Quiet Launcher folder, double-click **quiet-arrange** (`quiet-arrange.cmd`). Or open
   PowerShell in that folder and run:

   ```
   powershell -ExecutionPolicy Bypass -File .\quiet-arrange.ps1
   ```

2. Notepad opens with your apps, one per line, in their order on the home screen. The apps below the
   **Hidden:** line are hidden.
3. Change the order by moving lines (cut and paste). Move a line below **Hidden:** to hide that app,
   or above it to show it again.
4. Save the file. Then go back to the script's window and press Enter. (To send nothing, type n and
   press Enter.)

The script sends the list to the TV and says, for example,
**Sent to the TV: 17 apps shown, 2 hidden. Press Home to see it.**

When you edit the list:

- Leave the **Hidden:** line where it is, on a line of its own. Lines starting with # are notes and
  are ignored.
- Names can be in any capitals. If two apps have the same name, the list shows the app's package in
  brackets after the name, such as `YouTube (com.google.android.youtube.tv)`; keep the brackets.
- If a line is not an app on the TV (a typo, say), the script skips it and names it. Fix it and run
  the script again.
- An app missing from the list stays on the home screen, at the end.
- Keep at least one app above **Hidden:**.

If someone arranged the apps on the TV while your list was open, the script asks before replacing
what they did.

## Put an arrangement back after setting up again

Each time the script sends a list, it keeps a dated copy in Documents\QuietLauncher. Removing the
quiet home screen (docs-setup#remove-it) also saves the TV's arrangement there first.

After you set the quiet home screen up again, run **quiet-arrange** as above. It sees that the apps
on the TV have not been arranged, and asks whether to put back the arrangement saved on the date it
shows. Type y and press Enter. New apps installed since then appear at the end.

To send a particular saved copy instead, drag the file onto **quiet-arrange** in Explorer.

## If something goes wrong

- If the app opened instead of the question, you let go of select before 5 seconds. Hold it longer.
- If the TV asked **Arrange apps?** and you did not mean it, press select (on **Cancel**) or Back.
  Nothing has changed.
- If the script cannot reach the TV, it says what to do, as setup does
  (docs-setup#if-something-goes-wrong). Usually wireless debugging is off after a restart: turn it
  back on and run the script again.
- If the script says the quiet home screen is not set up on this TV, run setup first.
- If an app you hid is gone from the list on the PC, it was uninstalled from the TV. If you install
  it again, it comes back at the end of the home screen, shown.
