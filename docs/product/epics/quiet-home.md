---
id: epic-quiet-home
status: approved
derived-from:
  - requirements-home#REQ-home-1: 85ea81eef156
  - requirements-home#REQ-home-2: 1de9a9580c82
  - requirements-home#REQ-home-5: 7bee291a8d2a
  - requirements-home#REQ-home-6: 65404500f700
  - requirements-home#REQ-home-7: 77a504a39c5e
  - requirements-home#REQ-home-8: adbd1ec631c8
  - requirements-home#REQ-home-9: 78773327028c
  - requirements-home#REQ-home-10: 494817827b0d
  - requirements-home#REQ-home-11: e333ddea998b
  - persona-viewer#goals: 24b5a84b1f16
  - persona-viewer#frustrations: 8722d20eb95e
---

# Quiet home

<!-- docs/product/epics/quiet-home.md: what a viewer sees and does when they press Home. -->

## Outcome

A viewer presses Home and sees the household's apps, each looking as it does elsewhere, and nothing
promoting content, not even for a moment. They open an app, the TV's settings, or see the time, all
with the remote.

## Personas

- persona-viewer: their apps are no longer buried among promoted rows, the screen stays the same
  from day to day, and a guest recognises the apps without being shown.

## Proposed stories

- Home opens this screen: pressing Home from any app shows it within 1 second, with no Google TV
  home screen first, and a stray Google TV home screen is covered within 0.5 seconds (REQ-home-1, 2,
  10).
- The apps: every installed app (except hidden ones) with its own artwork and name, opened with
  select (REQ-home-2, 6, 9, 11).
- New and removed apps: a new app appears at the end, an uninstalled one disappears (REQ-home-5).
- TV settings from the home screen (REQ-home-7).
- A clock that follows the TV's time zone and 12- or 24-hour setting (REQ-home-8).
- Google TV's own home screen on request, for the watchlist, Play purchases, and live TV, with Home
  or sleep bringing the quiet home screen back (REQ-home-12; added 2026-11-09).

## Stories

- story-quiet-home-apps
- story-quiet-home-clock
- story-quiet-home-google-tv
- story-quiet-home-home-key
- story-quiet-home-new-and-removed-apps
- story-quiet-home-tv-settings

## Release groups

- First release (with epic-search and epic-setup).
- Home opens this screen, and the apps: a home screen that cannot open apps is not a release, and an
  app list nobody reaches with Home is not a home screen.
- New and removed apps, TV settings, and the clock can each ship separately, after those two.

## Dependencies

- epic-setup: the screen can only take over Home once setup has installed it and turned on its
  accessibility service.
- epic-search ships the search box on this screen; REQ-home-10's 1-second budget covers the search
  box too.

## Open questions

- None.

## Out of scope

- Arranging and hiding apps (epic-arranging). Until that ships, apps appear in install order.
- Search (epic-search).
- Watchlist, Play purchases, and live TV (concept, Non-goals).
