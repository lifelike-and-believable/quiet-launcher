---
status: draft
tests: [app/src/test]
tests-commit: 4e1959b7f336d21c209d4f79b91047de1a4218e4
unlocks:
  - app/src/test/java/quiet/launcher/ArrangeScreenTest.kt: Robolectric does not move focus when a direction key is dispatched to the activity (the window does that on a device; MovementTest uses focusSearch for the same reason), so AC-5's Up and Down inside the menu use focusSearch; no assertion changed
  - app/src/test/java/quiet/launcher/HoldTest.kt: as above, AC-3's Left, Right, and Up on the question, and AC-8's Right to Arrange apps, use focusSearch; no assertion changed
---

# Arrange mode on the TV

<!-- docs/specs/arrange-mode.md. Arranging, delivery slice 2 of 4 (PLAN.md, step 11): the hold, the
question, and arrange mode, with Move, Hide, and Show. Builds on arranging-store (slice 1). -->

## Problem

The set-up person arranges the apps with the remote (story-arranging-reorder, story-arranging-hide),
behind a step no viewer takes by accident (story-arranging-guard). design-arranging gives the flow,
the screens, and their wording; docs-arranging tells the set-up person how to use them.

## User stories

- story-arranging-reorder, story-arranging-hide, story-arranging-guard.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | On the home screen, pressing select on an app, or holding it for less than 5 seconds, opens the app as before, and no question appears. | test: HoldTest |
| AC-2 | Holding select on an app for 5 seconds shows **Arrange apps?** with its text (design-arranging#layout), **Cancel** selected, over the dimmed home screen. Letting go then does not open the app. Holding select on a top-row control does not ask. | test: HoldTest |
| AC-3 | On the question, **Cancel** or Back closes it, with nothing changed and the app still selected. Left and right move only between **Cancel** and **Arrange apps**. **Arrange apps** opens arrange mode. | test: HoldTest |
| AC-4 | Arrange mode shows **Arranging apps**, the instructions, and the hidden count (**2 hidden apps, at the end** or **No hidden apps**), with no top-row controls. The apps are in the arranged order, followed by **Hidden apps** and the hidden apps marked **Hidden**, or **No hidden apps. To hide one, select it and choose Hide.** The app that was held is selected. | test: ArrangeScreenTest |
| AC-5 | Select on an app opens a menu with **Move**, **Hide**, **Cancel**, **Move** selected; on a hidden app, **Show**, **Cancel**. On the last app on the home screen, the menu has no **Hide** and the status line says **Keep at least one app on the home screen.** **Cancel** or Back closes the menu with nothing changed. Up and down stay inside the menu. | test: ArrangeScreenTest |
| AC-6 | **Move** lifts the app and the status line says **Moving <app>. Use the arrows, then press select to put it here. Back puts it back.** Left and right move it one place, across rows; up and down five places; it stops at the first and last places on the home screen and never enters the hidden apps. Select puts it down and saves; Back puts it back where it was, and nothing is saved. Either way the app is no longer lifted: no dashed outline, and the selected app's size. | test: ArrangeSessionTest, ArrangeScreenTest, ArrangePutDownTest |
| AC-7 | **Hide** moves the app to the end of the hidden apps, says **<app> hidden. It is in the Hidden apps row.**, selects the app that took its place, and saves. **Show** moves a hidden app to the end of the apps, says **<app> shown, at the end of the apps.**, and saves. | test: ArrangeSessionTest, ArrangeScreenTest |
| AC-8 | Back (with no menu open and nothing moving) or Home leaves arrange mode; the home screen then shows the new arrangement from the top, with the first app selected, straight away: it does not draw the old order first, and the apps do not slide into their new places. After 2 minutes with no button pressed, arrange mode closes by itself, putting back an app that was being moved. | test: ArrangeScreenTest, HoldTest, ArrangeReturnTest |
| AC-9 | With TalkBack: entering arrange mode is announced with the instructions and the hidden count; a hidden app reads "<name>, hidden app"; status line messages are announced; each step of a move is announced as "<name>, <place> of <count>". | test: ArrangeScreenTest |
| AC-10 | On the reference device, as a test install: holding select on the remote for 5 seconds asks; **Arrange apps**, a move, and a hide work; the new order and the hidden app are still there after leaving arrange mode and after the app's process restarts. The TV is then put back as it was. | command: on the TV with the project lead, who holds select on the remote |

A test cites a criterion as `arrange-mode#AC-1` in its name or a comment.

## Scope

- In: the hold and the question in `HomeActivity`; `ArrangeActivity` (arrange mode) and
  `ArrangeSession` (its rules); the tiles' hidden and moving looks; their tests.
- Out: the PC script and its receiver, including closing arrange mode when the PC sends (slice 3);
  undo saving the arrangement (slice 4).

## APIs and assumptions

- The hold is timed from select's first key-down on an app tile; repeats are ignored; 5 seconds
  later the question shows and that hold's key-up is swallowed. A key-up before then lets the tile
  open the app on release, as now.
- Arrange mode is its own activity in the home screen's task. The home screen is `singleTask`, so a
  Home press (the service starts the home screen) closes arrange mode.
- Each change is saved with `ArrangementStore.save` at once: the arrangement's order is every app on
  the home screen in order, and its hidden list the hidden apps in order.
- Keys sent over ADB do not reach the accessibility service but do reach the focused window, so the
  on-device check can use ADB for everything except, perhaps, the 5-second hold
  (`input keyevent --longpress` is shorter); the project lead holds select on the remote for that.

## Delivery

- One pull request: the spec and failing tests first, then the implementation; then AC-10 on the
  TV; merged with a merge commit.
