---
id: story-arranging-guard
status: draft
derived-from:
  - epic-arranging#outcome: 32b124ae0684
  - persona-viewer#frustrations: 8722d20eb95e
---

# A stray press never moves or hides an app

<!-- docs/product/stories/arranging/guard.md. Release: second (epic-arranging). REQ-home-3 and 4;
the project lead's choice of guard, 2026-11-10: hold select for 5 seconds, then confirm. -->

As a household viewer, I want the apps to stay where they were last time, so that I never have to
hunt for one or call the set-up person over because something moved.

## Acceptance criteria

- On the quiet home screen, pressing any button on the remote, or holding one for less than 5
  seconds, never moves, hides, or shows an app. Holding select on an app for less than 5 seconds
  opens it, as pressing select does.
- Arranging starts only after holding select on an app for 5 seconds and then choosing
  **Arrange apps** on a screen that asks first. On that screen **Cancel** is selected at first, so
  pressing select again changes nothing.
- If I get past the question by mistake, Back or Home leaves arrange mode with nothing changed.
  After 2 minutes with no button pressed, arrange mode closes by itself, so it is never left open
  for the next person.

[open] REQ-home-3 and 4 say nobody but the set-up person can arrange the apps "on purpose or by
accident". The hold and the question stop accidents; a guest who knows about the hold could still
arrange the apps on purpose. Either the requirements change to "by accident", or the guard needs
something only the set-up person knows.
