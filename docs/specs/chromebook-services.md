---
status: draft
tests: [tests/chromebook]
tests-commit: 372cea1404fa0dadf9e14e9faa4b368776fcde0b
unlocks:
  - tests/chromebook/services-harness.js: the extension's manifest now grants every site, so the test copy sets its host permissions to http://localhost/* alone (rather than adding it), keeping 127.0.0.1 out of reach to test a service that cannot be paused (AC-8); nothing else changes
  - tests/chromebook/services-controller.test.js: AC-5's first test took the first script run in the service's tab to be the pause; slice 5 (chromebook-home-button) also runs a script there, to add the Home button, before it, so the test looks for the run of the pausing function instead; nothing else changes
  - tests/chromebook/services-harness.js: Chromium runs visibly when a display is set (DISPLAY, as under xvfb-run in CI) instead of always headless, because headless Chromium does not move window focus, which AC-5's test checks; nothing else changes
---

# The Chromebook launcher: services, going back, and pausing

<!-- docs/specs/chromebook-services.md. Chromebook delivery, slice 2 of 5 (PLAN.md): the services on
the launcher, each in its own full-screen window, back to the launcher with the video paused, and
back to the last service. From REQ-chromebook-2, 5, 6, 7, 10; story-chromebook-open-service and
story-chromebook-go-back; design-chromebook (Flow, Layout, How it works);
docs-chromebook#watch-something and #go-back-to-the-launcher. -->

## Problem

The launcher has to show the viewer's services and open each one full screen in its own window,
coming back to a service where it was left. Going back to the launcher, however the viewer gets
there, has to pause the video they left (story-chromebook-open-service, story-chromebook-go-back).
Services are added in slice 3; until then the tests put them in the extension's storage directly.

## User stories

- story-chromebook-open-service: open a service full screen, or go back to it where I left it.
- story-chromebook-go-back: go back to the launcher, and the video pauses.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The launcher shows the services kept in `chrome.storage.local` (`services`: `{ id, name, url, site, colour, hidden }`), in their order, hidden ones left out: each a tile with the site's icon and the service's name on its colour, and the name again underneath. With services, the first one is selected when the launcher opens. Nothing else is shown in the grid. | test: services-extension.test.js |
| AC-2 | Choosing a service whose window is not open opens its address in a new `popup` window, full screen and focused, and records that window as the service's. | test: services-controller.test.js, services-extension.test.js |
| AC-3 | Choosing a service whose window is open brings that window forward, full screen, without opening another or reloading it. | test: services-controller.test.js, services-extension.test.js |
| AC-4 | A tile whose service's window is open shows **Open**; the marker goes when that window closes, and the launcher keeps up while it is showing. | test: services-extension.test.js |
| AC-5 | `go-home` from a service's window pauses every playing video in that window, in every frame, before bringing the launcher forward; nothing that is not playing is touched. | test: services-controller.test.js, services-extension.test.js |
| AC-6 | When the launcher's window gains focus any other way (ChromeOS's window switcher, Alt+Tab, a click) and the window focused before it was a service's, that service is paused the same way. | test: services-controller.test.js |
| AC-7 | `go-last` brings the last service the viewer used forward, as it was, full screen; with none, it brings the launcher forward. | test: services-controller.test.js, services-extension.test.js |
| AC-8 | A service whose site the extension has no permission on is still opened and brought back, but not paused (no error shown), and its tile says **Videos won't pause** under its name. | test: services-controller.test.js, services-extension.test.js |
| AC-9 | Arrow keys move between tiles as on the TV, without wrapping; Up from the first row goes to the top-row control nearest above, and Down from the top row to the nearest tile. Enter, a click, or a tap opens a tile. Each tile reads its name, then **Open** or **Videos won't pause** when shown. | test: services-extension.test.js |
| AC-10 | On the project lead's Chromebook, with services put in storage by hand until slice 3: a service opens full screen with no tabs or address bar; Alt+Shift+H pauses it and brings the launcher back; the tablet-mode overview does the same; Alt+Shift+J and choosing it again return to it as left. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-services#AC-1` in its name.

## Scope

- In: `chromebook/lib/services.js` (reading services, the windows that belong to them), the
  controller's `openService`, `goHome` pausing, `goLast`, `onFocus`, and `onWindowRemoved`; the
  launcher's grid; their tests.
- Out: adding, arranging, hiding, and removing services, and asking for site permission (slice 3);
  search (slice 4); the Home button (slice 5).

## APIs and assumptions

- Service windows are remembered in `chrome.storage.session` (`serviceWindows`: service id to
  window id; `lastService`; `lastFocused`), which outlives the service worker sleeping; a removed
  window is dropped from it (`chrome.windows.onRemoved`).
- Pausing runs `video.pause()` on each playing `<video>` in every frame with
  `chrome.scripting.executeScript`, as the spike did on Prime Video. It needs the `scripting`
  permission and host permission on the site. The manifest asks for every `http` and `https` site,
  which Chrome grants at installation (the project lead's decision of 2026-11-17; at first it was an
  optional permission asked for when a service was added, and declared `optional_host_permissions`
  for any `http` or `https`
  site). Pausing twice is harmless, so `go-home` and the focus change may both pause.
- Site icons come from Chrome's own icon cache (`favicon` permission, `/_favicon/?pageUrl=`),
  so nothing is fetched by the launcher.
- `chrome.windows.onFocusChanged` does not fire in headless Chromium (seen in the spike), so AC-6 is
  tested against the in-memory `chrome` API and checked on the Chromebook (AC-10).
- Found while implementing: headless Chromium does not move window focus at all, and on Linux a new
  popup created full screen shows in a normal window until it is drawn. The extension tests run
  Chromium visibly on a virtual display (`xvfb-run`), and the controller asks for full screen again
  once a new window's page has loaded. Under X without a window manager only the window in front is
  full screen, so the tests check the state of the window just brought forward; on ChromeOS every
  window keeps its own (the spike).
- The extension tests load a copy of the extension whose manifest also grants `http://localhost/*`,
  and serve stand-in service pages (a playing video) from a local server, because optional
  permissions cannot be granted without a person in headless Chromium.

## Delivery

- In the same pull request as slice 1 (one branch): this spec, a tests-first commit, then the
  implementation.
