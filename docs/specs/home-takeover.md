---
status: draft
tests: [app/src/test]
tests-commit: 2e159e353d64b8e4106c270e7075d7d605fa93e3
---

# Taking over Home

<!-- docs/specs/home-takeover.md. Delivery slice 2 (PLAN.md, step 10): the accessibility service of
decision-home-takeover, Home resetting the quiet home screen, and Back doing nothing on it. -->

## Problem

Pressing Home must show the quiet home screen, never Google TV's, not even for a moment; and if
Google TV's home screen comes up on its own, it must be covered within half a second
(story-quiet-home-home-key, REQ-home-2). Android cannot be told to use another home screen on the
reference device, so the product does it with an accessibility service that catches the Home key and
watches for Google TV's home screen (decision-home-takeover). Google TV's own pages, such as search
results, must keep working (REQ-setup-2).

## User stories

- story-quiet-home-home-key: as a household viewer, I want Home to bring up a screen with only our
  apps, from anywhere.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The accessibility service consumes the Home key (press, release, and the repeats of a long press) and brings the quiet home screen forward as a Home press, on the first press event. | test: HomeWatcherTest |
| AC-2 | The service lets every other key through untouched: Back, the direction pad, select, the remote's app buttons (`KEYCODE_BUTTON_3`, `KEYCODE_BUTTON_4`), and voice (`KEYCODE_SEARCH`, `KEYCODE_ASSIST`, `KEYCODE_VOICE_ASSIST`). | test: HomeWatcherTest |
| AC-3 | When a window of `com.google.android.apps.tv.launcherx` with class `…home.HomeActivity` or `…home.VanillaModeHomeActivity` comes to the front, the service brings the quiet home screen forward as a Home press. Every other window, including Google TV's own pages (`…entity.EntityActivity`, `…coreservices.bootmode.DispatchActivity`) and other apps, is left alone. | test: HomeWatcherTest |
| AC-4 | A Home press shows the quiet home screen scrolled to the top with the first app selected, also when it was already showing with another app selected (design-home-screen, Where selection lands). | test: HomeResetTest |
| AC-5 | Back on the quiet home screen does nothing: the screen stays and does not close (design-home-screen, Layout). | test: HomeResetTest |
| AC-6 | The service is declared so Android can bind it and only Android can: not exported, protected by `BIND_ACCESSIBILITY_SERVICE`, asking for window-state events and key filtering, and nothing else. | test: ManifestTest |
| AC-7 | On the reference device, with the remote, 20 Home presses from different apps and from Google's page for a show each show the quiet home screen with no frame of Google TV's home screen before it, checked frame by frame in a screen recording (story-quiet-home-home-key). | command: install the build and turn on the service (standing go-ahead), `adb shell screenrecord`, the project lead presses Home 20 times, review the recording, restore the TV |
| AC-8 | On the reference device, the quiet home screen is visible and selectable within 1 second of a Home press (REQ-home-10), measured from the key event to the screen drawn, in the service's log. | command: as AC-7, from `adb logcat` timings |
| AC-9 | On the reference device: Back from an app opened from the quiet home screen returns to it; the remote's voice button still opens Google's voice search; Google TV's search page still opens (the `[to validate]` items in design-home-screen and decision-home-takeover). | command: as AC-7, by hand with screenshots |
| AC-10 | On the reference device, after a restart, the service is running before Google TV's home screen first appears, and covers it within half a second (the `[to validate]` item in design-setup). | command: restart the TV (needs the project lead's go-ahead), `adb logcat` from boot |

A test cites a criterion as `home-takeover#AC-1` in its name or a comment.

## Scope

- In: `HomeWatcher` (the accessibility service) and its configuration, the manifest entry, Home and
  Back handling in `HomeActivity`, and their tests.
- Out: turning the service on (slice 4's setup script; by hand over ADB for AC-7 to AC-10); the top
  row, search, and what Back does from search (slice 3).

## APIs and assumptions

- APIs used: `AccessibilityService.onKeyEvent` with `flagRequestFilterKeyEvents` and
  `canRequestFilterKeyEvents`; `onAccessibilityEvent` for `TYPE_WINDOW_STATE_CHANGED`; starting an
  activity from the service with `FLAG_ACTIVITY_NEW_TASK` (accessibility services are allowed to
  start activities from the background). All used by the spike in `spikes/home-takeover/` on the
  reference device.
- Assumption (from the spike): the remote's Home key reaches `onKeyEvent`; keys sent with
  `adb shell input keyevent` do not, so AC-7 needs a person at the remote.
- Assumption: Google TV's home screen keeps the class names above; a Google TV update could change
  them (decision-home-takeover, Consequences).

## Non-functional requirements

- Nothing but the Home key and Google TV's home screen window is acted on, so the service cannot
  interfere with other apps.
- No new permissions besides binding the accessibility service.

## Delivery

- Plan item: `PLAN.md`, step 10, slice 2.
- Pull request: one, stacked on slice 1 (#31), with the failing tests in their own commit.

## Risks and decisions

- AC-10 needs a restart of the household's TV, which the standing go-ahead does not cover: ask first.
