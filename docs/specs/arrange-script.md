---
status: draft
tests: [scripts/tests]
tests-commit: f31b3421331658c096b944544a0067d6b2ed39f8
---

# The arrangement from the PC: the script

<!-- docs/specs/arrange-script.md. Arranging, delivery slice 3b of 4 (PLAN.md, step 11):
quiet-arrange.ps1 and its launcher, as design-arranging and docs-arranging describe them. The app's
receiver is arrange-receiver (slice 3a). Builds on the setup scripts' fake ADB and tests (#38). -->

## Problem

The set-up person arranges the apps from the PC by editing a list in Notepad, sends it to the TV,
and keeps dated copies so an arrangement can be put back after the quiet home screen is set up again
(story-arranging-from-pc; docs-arranging#arrange-the-apps-from-your-pc).

## User stories

- story-arranging-from-pc.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Without ADB, the script stops at `[1/3] ADB: NOT FOUND` with the winget command. The TV step is setup's (`[1/3] TV: connected to <address>`, pairing, more than one device, could not connect). On a TV without the app it says **[1/3] TV: the quiet home screen is not set up on this TV. Run quiet-setup first.** All three exit non-zero with nothing sent. | test: test_arrange_errors |
| AC-2 | It writes the TV's arrangement to `arrangement-<serial>.txt` in the Quiet Launcher folder: the three `#` lines of design-arranging, each shown app's name on its own line in order, a blank line, `Hidden:`, and each hidden app's name. It opens the file in Notepad and asks **Save the file, then press Enter here to send it (or type n to send nothing):** | test: test_arrange_file |
| AC-3 | After Enter, it sends the file's order and hidden apps with the version it read, says **[3/3] Sent to the TV: <n> apps shown, <m> hidden. Press Home to see it.**, and keeps a copy, `arrangement-<serial>-<date>.txt`, naming it. A file that already is one of these copies (one put back, or given with `-File`) is the copy kept, and is not copied again. | test: test_arrange_send, test_arrange_copies |
| AC-4 | Names match whatever their capitals and surrounding spaces; `#` lines and blank lines are ignored. A line that is not an app on the TV is skipped and named (**Skipped 1 line that is not an app on this TV: "Hulu"**). Two apps with the same name are written with the package in brackets, and such a line matches by its package. An installed app missing from the file stays shown, at the end. | test: test_arrange_lines |
| AC-5 | A file with no `Hidden:` line, or with no app above it, is not sent, with the design's message. | test: test_arrange_lines |
| AC-6 | An unchanged file, or n, sends nothing: **[3/3] Nothing changed.** | test: test_arrange_send |
| AC-7 | If the arrangement changed on the TV while the file was open, it asks **Send your list anyway, replacing what was done on the TV? (y/n):**; n sends nothing and says to run it again; y replaces it. | test: test_arrange_send |
| AC-8 | On a TV whose apps have never been arranged, with a saved copy for its serial number, it offers the newest (**Put back the arrangement saved on <date> (<n> apps shown, <m> hidden)? (y/n):**); y sends it without opening Notepad; n carries on as usual. | test: test_arrange_put_back |
| AC-9 | `-File <path>` sends that file without opening Notepad. | test: test_arrange_put_back |
| AC-10 | Everything it prints, with the answers, goes to `arrange-<date>.log`; it exits 0 on success; `Get-Help` shows a synopsis, the options, and examples; it runs in Windows PowerShell 5.1 and PowerShell 7. | test: test_arrange_common |
| AC-11 | `quiet-arrange.cmd` runs the script with the execution policy bypassed for that run, passes a file dropped on it as `-File`, and waits for a key at the end so the result can be read. `build.ps1` copies both into `out\QuietLauncher`. | test: test_arrange_common; command: `.\build.ps1`, then list `out\QuietLauncher` |
| AC-12 | On the reference device, with the project lead: double-click **quiet-arrange**, move a line and hide an app in Notepad, press Enter; the home screen shows it. Then the TV is put back as it was. | command: on the TV with the project lead |

A test cites a criterion as `arrange-script#AC-1` in its name or a comment.

## Scope

- In: `scripts/quiet-arrange.ps1`, `scripts/quiet-arrange.cmd`, the fake ADB's answers for the
  receiver, `scripts/tests/test_arrange.py`, `build.ps1` copying both files.
- Out: undo saving the arrangement (slice 4); launchers for setup and undo (after the project lead
  confirms those scripts on the TV).

## APIs and assumptions

- The receiver (arrange-receiver): `adb shell am broadcast -n quiet.launcher/.ArrangeReceiver -a
  quiet.launcher.action.GET_ARRANGEMENT` or `SET_ARRANGEMENT --es arrangement <base64> --el version
  <n> [--ez replace true]`; result 1 done, 2 changed on the TV, 3 refused; data base64 JSON. A
  missing receiver answers `result=0` with no data.
- For tests, `QUIET_EDITOR` names a program run with the file instead of Notepad; `QUIET_ADB`,
  `QUIET_FOLDER`, and `QUIET_ANSWERS` as for setup. Set-up persons never need these.
- Notepad is started without waiting (Windows 11's Notepad can return at once), so the script waits
  for Enter instead (design-arranging#how-it-works).
- The file is UTF-8, so names with accents survive.

## Delivery

- One pull request on top of #38 (it needs #38's fake ADB and tests): the spec and failing tests
  first, then the implementation; then AC-12 with the project lead. Merged after #38.
