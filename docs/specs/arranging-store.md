---
status: implemented
tests: [app/src/test]
tests-commit: e590862d455cfbee044f4b1cd5181479d35155c6
---

# The arrangement: stored, and the list built from it

<!-- docs/specs/arranging-store.md. Arranging, delivery slice 1 of 4 (PLAN.md): the stored
arrangement and the home screen's list built from it. Arrange mode on the TV is slice 2, the PC
script slice 3, undo saving the arrangement slice 4. -->

## Problem

The set-up person's order and hidden apps have to be kept on the TV and applied every time the home
screen is drawn, the same for every profile, through restarts and updates
(story-arranging-reorder, story-arranging-hide; design-arranging#how-it-works). Until something is
arranged, the home screen must look exactly as it does now. This slice adds that, with nothing yet
to change the arrangement: arrange mode and the PC script build on it.

## User stories

- story-arranging-reorder: the order kept after a restart and an update; new apps at the end;
  removing an app keeps the others' order.
- story-arranging-hide: hidden apps left off the home screen, kept hidden after a restart.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | With no arrangement stored, the home screen's list is exactly as before: install order, the Play Store last (app-list#AC-2). | test: ArrangedListTest |
| AC-2 | With an arrangement, the apps it lists come first, in its order; installed apps it does not list (new ones) follow in their usual order. An app it lists that is no longer installed is skipped, and the others keep their order. | test: ArrangedListTest |
| AC-3 | Apps the arrangement hides are not in the home screen's list; they are returned as the hidden list, in the arrangement's order, for arrange mode. An app both listed and hidden is hidden. A hidden app that is no longer installed is left out. | test: ArrangedListTest |
| AC-4 | If none of the shown apps is installed any more but hidden ones are, the hidden ones are shown, so the home screen is never empty while apps are installed (design-arranging#states-and-errors). | test: ArrangedListTest |
| AC-5 | The arrangement is saved in one file in the app's own storage and read back the same by a new store (a restart). Each save increases its version by one, starting from 1. With no file, or a file that cannot be read, there is no arrangement, and nothing fails. A save never leaves a half-written file. | test: ArrangementStoreTest |
| AC-6 | The quiet home screen shows the arranged list, without the hidden apps, and picks up a changed arrangement the next time its list is rebuilt (each time it is shown). | test: ArrangedHomeTest |
| AC-7 | The app still has no internet permission (app-list#AC-9's test still passes), and the build passes. | command: `./gradlew :app:testDebugUnitTest` and `.\build.ps1` |

A test cites a criterion as `arranging-store#AC-1` in its name or a comment.

## Scope

- In: `Arrangement`, `ArrangementStore`, `AppCatalog.arranged`, and `HomeActivity` drawing the
  arranged list; their tests.
- Out: arrange mode (slice 2), the PC receiver and script (slice 3), undo saving the arrangement
  (slice 4). Checking on the reference device waits for slice 2, the first way to arrange the apps
  there.

## APIs and assumptions

- The file is JSON (`org.json`, part of Android), `arrangement.json` in the app's files directory:
  `{"version": 3, "order": ["pkg", ...], "hidden": ["pkg", ...]}`. Package names only: the activity
  is found from the installed apps, as now.
- Written to a temporary file in the same directory and renamed over the old one, so a power cut
  during a save leaves the old file or the new one.
- The arrangement is applied to the list `AppCatalog.visible` already gives (which apps, and their
  usual order), so the rules about which apps appear are unchanged.

## Delivery

- One pull request: the spec and failing tests first, then the implementation; merged with a merge
  commit.
