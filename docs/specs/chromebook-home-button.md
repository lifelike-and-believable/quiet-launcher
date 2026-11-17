---
status: draft
tests: [tests/chromebook]
---

# The Chromebook launcher: the Home button on service pages

<!-- docs/specs/chromebook-home-button.md. Chromebook delivery, slice 5 of 5 (PLAN.md): a way back to
the launcher by touch, for tablet mode. From REQ-chromebook-12; story-chromebook-go-back;
design-chromebook#the-home-button-on-service-pages; docs-chromebook#go-back-to-the-launcher. -->

## Problem

In tablet mode there is no keyboard for Alt+Shift+H, and the service fills the screen, so the
Chromebook viewer needs a touch control on the service's page that does what the shortcut does
(REQ-chromebook-12). The tablet-mode spike showed one in the bottom-left corner works and gets in
the way of nothing (docs/product/notes.md).

## User stories

- story-chromebook-go-back: in tablet mode, touching a service's page shows a Home button; tapping
  it does what Alt+Shift+H does.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Each page that loads in a service's window gets the Home button, hidden until a touch. Pages in ordinary browsing windows, the launcher, and the search window before it becomes a service's do not. A site the extension may not run on gets none, without an error. | test: home-button-controller.test.js, home-button-extension.test.js |
| AC-2 | A touch anywhere on the page shows the button, 64 px round, 24 px in from the bottom-left corner; it goes 4 seconds after the last touch. A mouse or touchpad does not show it. | test: home-button-extension.test.js |
| AC-3 | Tapping the button does exactly what Alt+Shift+H does: the service's video pauses and the launcher comes forward. | test: home-button-extension.test.js |
| AC-4 | When the site's own player is full screen, the button shows over it. | test: home-button-extension.test.js |
| AC-5 | The site's styles cannot hide or change the button, and its scripts cannot reach it (a closed shadow root). | test: home-button-extension.test.js |
| AC-6 | It reads "Back to the launcher, button". | test: home-button-extension.test.js |
| AC-7 | With reduce motion on, it appears and goes without fading. | test: home-button-extension.test.js |
| AC-8 | On the project lead's Chromebook in tablet mode: touching a service shows the button, over the site's own full-screen player too; tapping it pauses the video and brings the launcher back; it gets in the way of nothing. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-home-button#AC-1` in its name.

## Scope

- In: `chromebook/homebutton.js` (the button, injected into the page), the controller injecting it,
  and the service worker's message for a tap; their tests.
- Out: anything else.

## APIs and assumptions

- The button is injected with `chrome.scripting.executeScript({ files: ['homebutton.js'] })` when a
  page in a service's window finishes loading (`chrome.tabs.onUpdated`, status `complete`), and when
  a window becomes a service's. It needs the same site permission as pausing. Injecting twice is
  harmless: the script adds the button once.
- A tap sends a message to the service worker, which runs `goHome`; the window it came from is the
  one focused, so `goHome` pauses it (chromebook-services#AC-5).
- Touches are told from mouse and touchpad by `PointerEvent.pointerType`. The extension tests send
  touch pointer events into the page, and find the closed shadow root's button by where it is on the
  screen and through Chrome's accessibility tree.

## Delivery

- In the same pull request as slices 1 to 4: this spec, a tests-first commit, then the
  implementation.
