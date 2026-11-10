---
id: epic-arranging
status: approved
derived-from:
  - requirements-home#REQ-home-3: 75def69f683e
  - requirements-home#REQ-home-4: 1e148813bfdd
  - persona-setup-person#goals: 1b5715ea18d9
  - persona-viewer#frustrations: 8722d20eb95e
---

# Arranging the apps

<!-- docs/product/epics/arranging.md: the set-up person orders and hides the household's apps. -->

## Outcome

The set-up person puts the household's apps in the order they want and hides the ones nobody uses,
and nobody else can change either, so the home screen stays the same from day to day.

## Personas

- persona-setup-person: the home screen shows what the household uses, in a sensible order, and
  they are not called over because something moved.
- persona-viewer: things stay where they were last time, and a stray press never moves or hides an
  app.

## Proposed stories

- Reorder apps, kept after a restart (REQ-home-3).
- Hide an app and show it again, with the hidden apps always visible to the set-up person
  (REQ-home-4).
- Only the set-up person can do either on the TV: arranging and hiding sit behind a step a viewer
  would not take by accident, so a stray press never moves or hides an app (REQ-home-3, 4).
- From the computer: the set-up person can also set the order and the hidden apps from their PC,
  for changes too large to make with the remote (REQ-home-3, 4).

## Stories

- None written yet.

## Release groups

- Second release, after the first release of epic-quiet-home, epic-search, and epic-setup (the
  project lead's decision, 2026-11-09). Until then apps appear in install order.
- Reordering, hiding, and the guard together: arranging that a viewer can trigger by accident fails
  REQ-home-3 and 4, and the guard means nothing without something to guard.
- From the computer can ship before or after the on-TV stories.

## Dependencies

- epic-quiet-home: the app list being arranged.

## Open questions

- None. The project lead decided (2026-11-09): both on the TV, behind a step a viewer would not
  take by accident, and from the computer.

## Out of scope

- Different orders or hidden apps per person (REQ-home-6).
