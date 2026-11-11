---
status: draft
tests: [scripts/tests]
tests-commit: aa7dea6e4117171ae8369cf14579f7014ec6f879
---

# Double-click launchers for setup and undo

<!-- docs/specs/setup-launchers.md. quiet-setup.cmd and quiet-undo.cmd, like quiet-arrange.cmd
(arrange-script#AC-11): the set-up person double-clicks them in the Quiet Launcher folder instead of
typing a PowerShell command. Asked for by the project lead (2026-11-10). Builds on the setup scripts
(setup-scripts, #38). -->

## Problem

Setup and undo are run by typing a long PowerShell command in the right folder
(docs-setup#set-it-up, docs-setup#remove-it). The arrangement script already has a launcher that is
double-clicked (design-arranging); setup and undo should start the same way, with the typed command
kept for anyone who prefers it.

## User stories

- story-setup-set-up, story-setup-restart, story-setup-undo.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | `quiet-setup.cmd` runs `quiet-setup.ps1` from its own folder with the execution policy bypassed for that run and `-Apk` set to the `quiet-launcher.apk` beside it, so double-clicking it in the Quiet Launcher folder sets up the TV, also from a folder whose path has spaces. | test: test_launchers |
| AC-2 | `quiet-undo.cmd` runs `quiet-undo.ps1` from its own folder the same way. | test: test_launchers |
| AC-3 | Both pass on any options given to them (`-Tv`, `-Pair`), wait for a key at the end so the result can be read, and exit with the script's exit code. | test: test_launchers |
| AC-4 | `build.ps1` copies both into `out\QuietLauncher`. | test: test_launchers; command: `.\build.ps1`, then list `out\QuietLauncher` |
| AC-5 | design-setup and docs-setup give double-clicking **quiet-setup** and **quiet-undo** as the way to run them, and keep the PowerShell command as the other way. | review: the project lead |
| AC-6 | On the reference device, with the project lead: double-click **quiet-setup**, then **quiet-undo**; each runs to **Done**, and the window stays open until a key is pressed. Then the TV is put back as it was. | command: on the TV with the project lead |

A test cites a criterion as `setup-launchers#AC-1` in its name or a comment.

## Scope

- In: `scripts/quiet-setup.cmd`, `scripts/quiet-undo.cmd`, `build.ps1` copying them,
  `scripts/tests/test_launchers.py`, and the amendments to design-setup and docs-setup.
- Out: any change to what the scripts do.

## APIs and assumptions

- `%~dp0` is the launcher's own folder, with a trailing backslash, so the launchers work wherever the
  folder is and whatever the current directory is when Explorer starts them.
- They use Windows PowerShell (`powershell`), which every Windows PC has; the scripts run in 5.1
  (setup-scripts#AC-10).

## Delivery

- One pull request from main: the spec and failing tests first, then the launchers and the
  amended design and documentation. It touches `docs/design/` and `docs/user/`, so it is merged
  only when the project lead says so.
