---
status: draft
tests: [tests/chromebook]
tests-commit: 52ae0d9c919d18bfb559ff13f28f16d9990ea64c
---

# The Chromebook launcher: skeleton and launcher window

<!-- docs/specs/chromebook-launcher.md. Chromebook delivery, slice 1 of 5 (PLAN.md): the extension,
its shortcuts, the one full-screen launcher window, and the launcher page with no services yet.
From REQ-chromebook-1, 5, 9, 10, 11; story-chromebook-launcher and story-chromebook-install;
design-chromebook (Layout, States and errors, How it works); docs-chromebook#install-it,
#go-back-to-browsing, #change-the-shortcuts. -->

## Problem

The Chromebook viewer needs one shortcut that brings up the launcher, full screen and in its own
window, from anywhere in Chrome, while Chrome's new tab page and the window they were browsing in
stay as they were (story-chromebook-launcher). This slice builds the extension that later slices
fill: the launcher shows no services until slice 3 (editing) lets them be added.

## User stories

- story-chromebook-launcher: bring up the launcher with one shortcut, full screen, from anywhere in
  Chrome.
- story-chromebook-install: install it from the repository; the shortcuts can be changed and the
  launcher shows the ones in use.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The extension in `chromebook/` is a Manifest V3 extension named **Quiet Launcher**. It does not replace the new tab page (no `chrome_url_overrides`). It declares two commands: `go-home`, suggested Alt+Shift+H, described "Back to the launcher", and `go-last`, suggested Alt+Shift+J, described "Back to the last service" (docs-chromebook#change-the-shortcuts). | test: manifest.test.js |
| AC-2 | `go-home` with no launcher open creates one `popup` window showing the launcher page, puts it in full screen (again, if Chrome did not create it full screen), and gives it focus. | test: controller.test.js, extension.test.js |
| AC-3 | `go-home` with the launcher open brings that window forward, putting it back in full screen if it has left it; there is never a second launcher window. | test: controller.test.js, extension.test.js |
| AC-4 | `go-home` opens no tab in, and changes nothing about, any other window. | test: extension.test.js |
| AC-5 | The launcher page has the top row of design-chromebook#layout: **Search for a show**, **Edit services**, **Back to browsing**, and the clock, which cannot be selected; then the status line, empty. With no services it says **No services yet. Choose Edit services, then Add a service.** with **Edit services** selected. | test: extension.test.js |
| AC-6 | The hint line at the bottom shows the shortcuts Chrome has given the commands, as **<keys> brings you back here · <keys> goes back to the last service**. If `go-home` has no shortcut, the status line says **Alt+Shift+H is used by something else, so it is not set. Set a shortcut at chrome://extensions/shortcuts.** and the hint line says **No shortcut back here yet** in its place; a `go-last` with no shortcut is left out of the hint line. | test: launcher-view.test.js, extension.test.js |
| AC-7 | **Back to browsing** brings forward the last ordinary (`normal`) Chrome window that had focus; if it has closed, any ordinary window; if there is none, a new one. The launcher window stays open. | test: controller.test.js |
| AC-8 | With the keyboard, Left and Right move along the top row's three controls without wrapping, Tab and Shift+Tab move through them in reading order, and Enter or Space activates the selected one. The selected control has the TV's selection outline. | test: extension.test.js |
| AC-9 | The clock shows hours and minutes in the Chromebook's own 12- or 24-hour form, without AM or PM, and keeps time. | test: launcher-view.test.js |
| AC-10 | On the project lead's Chromebook: installed by docs-chromebook#install-it, Alt+Shift+H from an ordinary web page opens the launcher full screen with no tabs or address bar, and again brings the same one forward; a new tab is still Chrome's own; **Back to browsing** returns to the page. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-launcher#AC-1` in its name.

## Scope

- In: `chromebook/` (manifest, service worker, `lib/controller.js`, `lib/launcher-view.js`, the
  launcher page); `tests/chromebook/` (Node's test runner, and Playwright with the repository's
  Chromium for the extension tests); a CI workflow that runs them.
- Out: services, editing, search, pausing, and the Home button (slices 2 to 5). **Search for a
  show** and **Edit services** are on the page but do nothing yet.

## APIs and assumptions

- `chrome.commands.onCommand` for the shortcuts, and `chrome.commands.getAll` for the keys in use.
- `chrome.windows.create({ type: 'popup', state: 'fullscreen' })`: a popup window has no tab strip,
  which the spike showed avoids the flash of tabs and address bar. In headless Chromium the window
  can come back not full screen, so the controller checks and sets full screen again (seen in the
  spike, 2026-11-16).
- The launcher window is found by its tab's address (the extension's `launcher.html`), so nothing
  needs storing to know whether it is open. The last browsing window is kept in
  `chrome.storage.session`, which outlives the service worker sleeping.
- Found while implementing: without the `tabs` permission Chrome does not give an extension the
  address of any tab, not even its own pages, so the launcher window could not be found. The
  extension asks for `tabs`, which slice 2 needs anyway to recognise the services' windows.
- Found while implementing: in headless Chromium a popup created full screen showed as a normal
  window a moment later, so the controller asks for full screen again once the window exists.
- Extension commands cannot be pressed in headless Chromium, so the extension tests call the same
  handler the command calls (`globalThis.quietLauncher.goHome()` in the service worker); AC-10
  checks the real keys.

## Delivery

- One pull request: this spec, a tests-first commit with failing tests against stubs, then the
  implementation. Merged with a merge commit.
