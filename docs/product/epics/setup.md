---
id: epic-setup
status: approved
derived-from:
  - requirements-setup#REQ-setup-1: 9e7f07f7fc74
  - requirements-setup#REQ-setup-2: e721df90f64b
  - requirements-setup#REQ-setup-3: a1be1780ea5f
  - requirements-setup#REQ-setup-4: b4cabd0a86d6
  - requirements-setup#REQ-setup-5: fa43db2e9563
  - persona-setup-person#goals: 1b5715ea18d9
  - persona-setup-person#context-of-use: 93f08fded3f5
---

# Set up and keep it

<!-- docs/product/epics/setup.md: installing the product, keeping it the home screen, and removing
it, by the set-up person from their computer. -->

## Outcome

The set-up person makes the product the TV's home screen by following one written procedure from
their computer, it stays that way through restarts, running the procedure again recovers it after a
Google TV update or reset, and one written undo puts the TV back as it was.

## Personas

- persona-setup-person: each change to the household's TV is deliberate, written down, and
  reversible, and they only go to the PC when something undoes the setup.

## Proposed stories

- Set up: install the app and turn on its accessibility service over ADB, following the written
  procedure; Google TV's home app stays installed and working (REQ-setup-1, 2).
- Survive a restart or power cut (REQ-setup-3).
- Recover after an update or reset by running setup again, keeping the order and hidden apps after
  an update (REQ-setup-4).
- Undo: one written procedure removes the product and returns the TV to Google TV's home screen as
  it was (REQ-setup-5).

## Stories

- None written yet.

## Release groups

- First release (with epic-quiet-home and epic-search).
- Set up, survive a restart, and undo: a setup without a tested undo, or one that a power cut
  undoes, is not something to put on the household's TV.
- Recovery after an update or reset can ship afterwards, once an update has been seen.

## Dependencies

- epic-quiet-home: there must be a home screen to install.
- The spike's finding that an accessibility service catching the Home key takes over Home on the
  reference device while search keeps working.

## Open questions

- None.

## Out of scope

- Setup by anyone other than the project lead, or from the TV alone (REQ-setup-1).
- Root access, or publishing on the Play Store (concept, Non-goals).
