---
status: draft
tests: [app/src/test]
---

# The arrangement from the PC: the app's receiver

<!-- docs/specs/arrange-receiver.md. Arranging, delivery slice 3a of 4 (PLAN.md, step 11): the part
of "from the PC" that lives in the app. The script, quiet-arrange.ps1, and its launcher are slice
3b, after the setup scripts' tests (#38) are merged. Builds on arrange-mode (slice 2). -->

## Problem

The set-up person arranges the apps from the PC (story-arranging-from-pc). The script reads the TV's
arrangement and sends a new one over ADB; something in the app has to answer, and nothing else on
the TV may (design-arranging#how-it-works). If the arrangement changed on the TV while the file was
open, the script must be able to tell, and ask before replacing it.

## User stories

- story-arranging-from-pc.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The receiver is exported and requires `android.permission.DUMP`, which ADB's shell has and other apps cannot be given (checked on the reference device, 2026-11-10: the shell has it; its protection is signature, privileged, development). | test: ArrangeReceiverTest |
| AC-2 | Asked for the arrangement, it answers with every app the home screen would show, in order, then the hidden apps, in order, each with its name, package, and whether it is hidden; the arrangement's version (0 when nothing is stored); and whether the apps have ever been arranged. | test: ArrangeRequestsTest |
| AC-3 | Sent an arrangement with the version the script read, it saves the order and hidden apps and answers with the new version. | test: ArrangeRequestsTest |
| AC-4 | Sent an arrangement with an out-of-date version, it saves nothing and answers that the arrangement changed, with the current version; sent with "replace", it saves it anyway. | test: ArrangeRequestsTest |
| AC-5 | Sent something that cannot be read, or an arrangement that would hide every installed app, it saves nothing and answers with the reason. | test: ArrangeRequestsTest |
| AC-6 | After an arrangement is saved this way, arrange mode, if it is open, closes, and the home screen shows the new arrangement at once. | test: ArrangeReceiverTest |
| AC-7 | On the reference device, as a test install: `adb shell am broadcast` asks for the arrangement and gets it back; a send changes what the home screen shows; a send with an old version is refused. The TV is then put back as it was. | command: `adb shell am broadcast` against the TV |

A test cites a criterion as `arrange-receiver#AC-1` in its name or a comment.

## Scope

- In: `ArrangeReceiver`, `ArrangeRequests` (what it does), the in-app notice that closes arrange mode
  and redraws the home screen; their tests.
- Out: `quiet-arrange.ps1`, its launcher, its file, and its copies on the PC (slice 3b); undo saving
  the arrangement (slice 4).

## APIs and assumptions

- Called as `adb shell am broadcast -n quiet.launcher/.ArrangeReceiver -a <action> [extras]`, which
  waits and prints `Broadcast completed: result=<code>, data="<data>"`.
- Actions: `quiet.launcher.action.GET_ARRANGEMENT`; `quiet.launcher.action.SET_ARRANGEMENT` with
  `--es arrangement <base64>` (JSON `{"order": [...], "hidden": [...]}`), `--el version <n>`, and
  optionally `--ez replace true`.
- Result codes: 1 done, 2 changed on the TV (refused), 3 refused for another reason. Data is base64
  of JSON (for a get: `{"version": n, "arranged": bool, "apps": [{"name", "package", "hidden"}]}`;
  for a set: `{"version": n}` or `{"error": "<reason>"}`), so nothing in it needs quoting.
- The notice to the app's own screens is a broadcast limited to this package
  (`quiet.launcher.action.ARRANGEMENT_CHANGED`), registered not exported.

## Delivery

- One pull request on top of slice 2's: the spec and failing tests first, then the implementation;
  then AC-7 on the TV over ADB (no remote needed); merged after slice 2, with a merge commit.
