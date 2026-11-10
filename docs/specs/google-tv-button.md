---
status: draft
tests: [app/src/test]
tests-commit: d5276c07819cac9398f663b9c80112f3ad4e7a2f
---

# The Google TV button

<!-- docs/specs/google-tv-button.md. The project lead's request of 2026-11-09 (PLAN.md, step 10,
item 4): REQ-home-12, story-quiet-home-google-tv, design-home-screen (Google TV), and
docs-home-screen#open-google-tvs-own-home-screen. -->

## Problem

Some things only Google TV's own home screen has: the watchlist, Google Play purchases, live TV.
The household viewer needs a way to it from the quiet home screen, and a way back with Home, without
weakening the rule that Google TV's home screen is otherwise always covered (REQ-home-2, REQ-home-12).

## User stories

- story-quiet-home-google-tv: as a household viewer, I want to open Google TV's own home screen when
  I need something only it has, so that I am never stuck without it.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The top row has a **Google TV** button, between **Voice search** and **Settings**, which TalkBack reads as "Google TV". | test: GoogleTvButtonTest |
| AC-2 | Selecting **Google TV** asks the service, inside the app, to open Google TV's home screen; nothing outside the app can send that request. | test: GoogleTvButtonTest |
| AC-3 | On that request the service stops covering Google TV's home screen, then opens it (`MAIN` + `HOME` to `com.google.android.apps.tv.launcherx`), so it is never covered on its way up. | test: HomeWatcherGoogleTvTest |
| AC-4 | While it is open this way, Google TV's home screen and its pages are not covered. | test: HomeWatcherGoogleTvTest |
| AC-5 | The next Home press brings the quiet home screen back, and from then on Google TV's home screen is covered again. | test: HomeWatcherGoogleTvTest |
| AC-6 | When the screen turns off while Google TV's home screen is open this way, the service covers again and brings the quiet home screen forward, so the TV wakes to it. | test: HomeWatcherGoogleTvTest |
| AC-7 | Nothing is stored: a service that starts afresh (after a restart) covers Google TV's home screen. | test: HomeWatcherGoogleTvTest |
| AC-8 | On the reference device: **Google TV** opens Google TV's home screen and it stays while used (including its own pages); Home returns to the quiet home screen; after sleep and wake, the quiet home screen shows; afterwards Google TV's home screen opened directly is covered again. | command: install the build and turn on the service (standing go-ahead), the project lead presses Home, check with screenshots and the logs, restore the TV |

A test cites a criterion as `google-tv-button#AC-1` in its name or a comment.

`MovementTest` (top-row-and-search#AC-10, whose spec is implemented and so no longer locked) is
amended in this change: Right from **Voice search** now lands on **Google TV**, and Right from
**Google TV** on **Settings**.

## Scope

- In: the **Google TV** button in `HomeActivity`; in `HomeWatcher`, a receiver for the in-app
  request and for `ACTION_SCREEN_OFF`, and the "Google TV allowed" state; their tests.
- Out: anything else on Google TV's home screen.

## APIs and assumptions

- An in-app broadcast (`quiet.launcher.OPEN_GOOGLE_TV`, sent with `setPackage`) from the home screen's
  process to the service's (`:watcher`), received by a receiver registered with
  `RECEIVER_NOT_EXPORTED`, so only this app can send it.
- `Intent.ACTION_SCREEN_OFF`, which can only be received by a receiver registered at run time.
- Assumption: starting the quiet home screen from the service while the screen is off works, so it
  is in front on waking (AC-8 checks it).

## Delivery

- One pull request, merged with a merge commit.
