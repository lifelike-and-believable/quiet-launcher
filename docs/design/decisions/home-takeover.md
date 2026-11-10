---
id: decision-home-takeover
status: approved
derived-from:
  - requirements-home#REQ-home-2: 1de9a9580c82
  - epic-quiet-home#outcome: ad1e8e9b386c
  - epic-setup#outcome: fcfa341d2d11
  - persona-viewer#frustrations: 8722d20eb95e
  - persona-setup-person#goals: 1b5715ea18d9
---

# Decision: take over Home with an accessibility service that catches the Home key

<!-- docs/design/decisions/home-takeover.md. Spans epic-quiet-home (what Home shows) and epic-setup
(what setup turns on and undo turns off). Evidence: the spikes of 2026-11-09 in
docs/product/notes.md and spikes/home-takeover/. -->

## Context

Google TV has no setting for choosing a home screen, and search results are pages of Google TV's
own home app, so that app must stay installed, enabled, and free to open its pages. Pressing Home
must never show Google TV's home screen, even for a moment, and if it comes up on its own it must
be covered within half a second (REQ-home-2); this matters to the viewer, whose frustrations are
the promoted rows. Whatever is chosen, setup has to turn it on from
the PC and undo has to turn it off without leaving the TV without a home screen.

## Options

| Option | For | Against |
|---|---|---|
| Make this app the home role holder (`cmd package set-home-activity`) | The standard Android way; nothing intercepted | Does not work on the reference device: Google TV's home screen has a higher-priority filter and still opens (spike) |
| Disable only Google TV's home screen activities over ADB | Home would fall through to this app | Refused by Android 14 for another package's components (spike); disabling the whole app breaks search |
| Accessibility service that reacts to every Google TV window (Projectivy's approach) | Proven by an existing product | Also intercepts Google TV's search pages, so typed search breaks (tried 2026-11-08) |
| Accessibility service that covers only Google TV's home screen when it appears | Works; search pages open (spike) | Google TV's home screen shows for 0.15 to 0.4 s first, which REQ-home-2 forbids |
| Accessibility service that catches the Home key, and covers Google TV's home screen if it appears anyway | In the spike, none of 11 real-remote presses showed Google TV's home screen before the quiet one; search pages open | Needs accessibility permission; Google TV's long-press Home action is lost; relies on Android delivering the remote's Home key to accessibility services; once, Google TV's home screen came up on its own 1.8 s after a press (cause unknown), which the fallback covers |

## Decision

The last option. The product's accessibility service consumes the Home key (press and release) and
brings the quiet home screen forward; as a fallback it watches for Google TV's home screen window
(`com.google.android.apps.tv.launcherx`, `home.HomeActivity` or `home.VanillaModeHomeActivity`) and
covers it, for the cases where Google TV brings it up without a Home press (an app closing, waking,
start-up). It reacts to nothing else, so Google TV's search pages, the remote's app buttons, and its
voice button work as before. It is the only option that meets REQ-home-2 and keeps search.

## Consequences

- Setup must turn the accessibility service on over ADB, without disturbing any other enabled
  service, and undo must restore the previous setting (design-setup).
- Google TV's long-press Home action is not available (story-quiet-home-home-key).
- The quiet home screen is an ordinary app, not the system's home: Android still regards Google
  TV's home as home, so anything that opens "home" directly lands there first and is covered by the
  fallback within half a second.
- Google TV's home screen can still show briefly (up to half a second, 0.15 to 0.4 s in the spike)
  when it comes up without a Home press: for example after Back from an app opened with the
  remote's YouTube or Netflix button, after waking, or at start-up. REQ-home-2 allows this, as
  long as it is covered within half a second. Back from an app opened from the quiet home screen
  is expected to return to it directly, as Back from Google's search did in the spike [to validate].
- The quiet home screen must never close itself: Back on it does nothing (design-home-screen), or
  closing it would reveal Google TV's home screen.
- Google TV's home screen can be opened on purpose with the **Google TV** button (REQ-home-12): the
  service then leaves it uncovered until the next Home press or the screen turning off.
- Other home-screen apps (Projectivy Launcher) are left off the quiet home screen, so the household
  cannot open a second launcher by accident.
- If the service is turned off (by an update, say), Google TV's home screen is what the household
  sees, which the project lead accepted (epic-setup, Out of scope).
- A Google TV update could rename the home screen's window class; the fallback would stop covering
  it while the Home key still works. The setup checks would show it.
