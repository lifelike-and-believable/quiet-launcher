---
status: draft
tests: [scripts/tests]
tests-commit: f7974b7f3ba10e1fdabf2f0a4465ff4e93782942
---

# Undo saves the arrangement

<!-- docs/specs/undo-arrangement.md. Arranging, delivery slice 4 of 4 (PLAN.md, step 11): undo keeps
the arrangement on the PC before it removes the app, so it can be put back after setting up again.
Builds on the setup scripts (#38) and quiet-arrange.ps1 (arrange-script, #50). -->

## Problem

Removing the app deletes its arrangement. The set-up person should not lose it by removing the
quiet home screen and setting it up again (story-arranging-from-pc: "undo saves the TV's arrangement
before it removes the app"; design-setup#flow; docs-setup#remove-it).

## User stories

- story-arranging-from-pc.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | When the apps have been arranged, undo saves the arrangement before it uninstalls the app, as `arrangement-<serial>-<date>.txt` in the Quiet Launcher folder, in the arrangement file's format (arrange-script#AC-2), and says under step 3 **Arrangement of the apps: saved to <path>**. | test: test_undo_arrangement |
| AC-2 | When the apps have never been arranged, undo saves nothing and says nothing about it. | test: test_undo_arrangement |
| AC-3 | When the app does not answer (already removed, or not working), undo carries on as before, with no error about the arrangement. | test: test_undo_arrangement |
| AC-4 | After undo and a new setup, quiet-arrange offers the arrangement undo saved (arrange-script#AC-8). | test: test_undo_arrangement |
| AC-5 | On the reference device, with the project lead: arrange the apps, run undo, set up again, and put the arrangement back with quiet-arrange. | command: on the TV with the project lead |

A test cites a criterion as `undo-arrangement#AC-1` in its name or a comment.

## Scope

- In: `scripts/quiet-undo.ps1` (step 3) and `scripts/tests/test_undo_arrangement.py`.
- Out: anything in the app; the setup and undo launchers.

## APIs and assumptions

- The arrangement is read through the app's receiver (arrange-receiver#AC-2), which answers whether
  the accessibility service is on or not, so undo can read it after restoring the settings in step 2.
- The file is written by the same rules as quiet-arrange.ps1's (the three `#` lines, names, a blank
  line, `Hidden:`, hidden names; duplicates with their package), copied into quiet-undo.ps1 because
  each script runs on its own.

## Delivery

- One pull request on top of #50: the spec and failing tests first, then the change; merged after
  #50, with a merge commit.
