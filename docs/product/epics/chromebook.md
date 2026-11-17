---
id: epic-chromebook
status: approved
derived-from:
  - requirements-chromebook#REQ-chromebook-1: 28894598d5df
  - requirements-chromebook#REQ-chromebook-2: b95df4d33199
  - requirements-chromebook#REQ-chromebook-3: ccc620e173b8
  - requirements-chromebook#REQ-chromebook-4: c9e62cf62621
  - requirements-chromebook#REQ-chromebook-5: 0803033b5a70
  - requirements-chromebook#REQ-chromebook-6: 6de348bc8ebb
  - requirements-chromebook#REQ-chromebook-7: 692aae0e30fa
  - requirements-chromebook#REQ-chromebook-8: 077b320dba35
  - requirements-chromebook#REQ-chromebook-9: b874979dd6d0
  - requirements-chromebook#REQ-chromebook-10: 4bed4a55da39
  - requirements-chromebook#REQ-chromebook-11: f40779b811e8
  - requirements-chromebook#REQ-chromebook-12: 0831b2aacc81
  - persona-chromebook-viewer#goals: 322e3fb02f13
  - persona-chromebook-viewer#frustrations: 574438d89687
---

# The Chromebook launcher

<!-- docs/product/epics/chromebook.md: the quiet launcher on the project lead's Chromebook, as a
Chrome extension. -->

## Outcome

On their Chromebook, the project lead presses one shortcut and has every streaming service they use
in front of them, full screen and in the TV's familiar interface; they go back and forth between
the launcher and the services without the browser showing and without a video playing on behind
them, find which service has a title, and go back to ordinary browsing when they are done.

## Personas

- persona-chromebook-viewer: one place for every service, full screen with nothing of the browser
  showing, a video that stops when they leave it, and a search that says where a title streams,
  while Chrome stays a browser.

## Proposed stories

- Bring up the launcher with a shortcut, full screen, in its own window, from anywhere in Chrome
  (REQ-chromebook-1, 5, 10).
- Open a service in its own full-screen window, or go back to it where it was left
  (REQ-chromebook-2, 5, 7).
- Go back to the launcher, which pauses the video, or straight back to the last service; by
  shortcut, or by touch in tablet mode (REQ-chromebook-6, 7, 12).
- Search JustWatch for a title and go to the service that has it (REQ-chromebook-8).
- Add, remove, arrange, and hide services (REQ-chromebook-3, 4, 10).
- Install the extension from the repository, change its shortcuts, and remove it cleanly
  (REQ-chromebook-9, 11).

## Stories

- story-chromebook-launcher
- story-chromebook-open-service
- story-chromebook-go-back
- story-chromebook-search
- story-chromebook-services
- story-chromebook-install

## Release groups

- First release, together: install, launcher, open a service, go back, and services. Without
  any one of them the launcher cannot be installed, reached, filled, or left.
- Search can ship in the same release or just after; the launcher is useful without it.

## Dependencies

- None on the TV's epics: the Chromebook launcher shares their look and names, not their code or
  data (REQ-chromebook-4: its own list and order).

## Open questions

- None. The project lead's decisions of 2026-11-16 and 17 settled the shortcut, full screen, a
  window per service, pausing, JustWatch, and that the new tab page stays Chrome's.

## Out of scope

- Play, pause, or other controls for what plays, beyond the pause when going back (concept,
  Non-goals).
- Anything on the TV, or controlling one device from the other.
