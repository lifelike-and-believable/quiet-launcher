---
status: draft
tests: [tests/chromebook]
tests-commit: 1b20e1bff7dc5a5e8efeb3ab5b8115fdf41ff5d2
---

# The Chromebook launcher: Back to browsing turns it off

<!-- docs/specs/chromebook-back-to-browsing.md. Chromebook delivery, slice 7 (PLAN.md): the project
lead's decision of 2026-11-17 that going back to browsing turns off the launcher and its services.
Amends chromebook-launcher#AC-7, which kept the launcher open. From design-chromebook (Flow, How it
works) and docs-chromebook#go-back-to-browsing and #remove-it. -->

## Problem

Going back to browsing means turning the launcher off, so its services should go too, rather than
staying open in windows behind the browser. It also means removing the extension after going back
to browsing leaves nothing behind (docs-chromebook#remove-it).

## User stories

- story-chromebook-launcher: bring up the launcher with one shortcut; going back to browsing leaves
  Chrome as it was.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | **Back to browsing** brings forward the browsing window (as chromebook-launcher#AC-7), then closes the launcher's window, every service's window, and the search window. Ordinary browsing windows are left as they were. | test: back-to-browsing.test.js, back-to-browsing-extension.test.js |
| AC-2 | Afterwards nothing remembers those windows: Alt+Shift+H opens a new launcher, choosing a service opens it afresh, and Alt+Shift+J brings up the launcher. | test: back-to-browsing.test.js |
| AC-3 | A window that has already been closed by hand does not stop the others closing. | test: back-to-browsing.test.js |
| AC-4 | On the project lead's Chromebook: with two services open, **Back to browsing** leaves only the browsing window, and Alt+Shift+H brings the launcher back, full screen. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-back-to-browsing#AC-1` in its name.

## Scope

- In: the controller's `backToBrowsing`; its tests. The `chromebook-launcher` test of AC-7 that
  checked the launcher stayed open is changed, with the reason under that spec's `unlocks:`.
- Out: anything else.

## APIs and assumptions

- `chrome.windows.remove` for each window. The launcher page is closed with its window, so the
  service worker does the closing after it has brought the browsing window forward.

## Delivery

- In the same pull request as slices 1 to 6: this spec, a tests-first commit, then the
  implementation.
