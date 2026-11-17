---
status: draft
tests: [tests/chromebook]
tests-commit: 4882149a16e19656b332376c7081574899721002
unlocks:
  - tests/chromebook/search-extension.test.js: AC-4's test waited for a new page, but following a link reuses the search window's page, so the wait could miss it; the test waits for a page at the service's address instead; and opening the launcher to check for **Open** rightly paused the service, so the test starts its video again before checking that go-home pauses it; nothing else changes
---

# The Chromebook launcher: search through JustWatch

<!-- docs/specs/chromebook-search.md. Chromebook delivery, slice 4 of 5 (PLAN.md): the search box
opens JustWatch's results for Canada in their own full-screen window, and a service reached from
them becomes that service's window. From REQ-chromebook-8; story-chromebook-search;
design-chromebook (Flow, States and errors, How it works); docs-chromebook#find-which-service-has-a-show. -->

## Problem

The Chromebook viewer wants to type a title and see which of their services has it, without
searching each one (story-chromebook-search). The project lead chose JustWatch's search for Canada
after the spike (concept, Feasibility).

## User stories

- story-chromebook-search: search for a title and see which service has it.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Enter in **Search for a show** with text opens `https://www.justwatch.com/ca/search?q=<the text>`, exactly as typed, in its own full-screen popup window with focus, and empties the box. Enter with nothing (or only spaces) typed does nothing. | test: search-controller.test.js, search-extension.test.js |
| AC-2 | Searching again while the search window still shows JustWatch shows the new results in that window and brings it forward, rather than opening another. | test: search-controller.test.js |
| AC-3 | While the box is selected, the status line says **Results open from JustWatch, in their own full-screen window. Enter searches.**; when it is not, the status line is as before. | test: search-extension.test.js |
| AC-4 | When the search window goes to one of the viewer's services (its host is the service's site or under it; if several services share the site, the one whose own address the page is under), that window becomes the service's window: the launcher marks the service **Open**, choosing it brings that window back, and going back to the launcher pauses it. A window the service already had stays open but is no longer its window. | test: search-controller.test.js, search-extension.test.js |
| AC-5 | Nothing about a search is stored: neither `chrome.storage.local` nor `chrome.storage.session` holds the text. | test: search-extension.test.js |
| AC-6 | On the project lead's Chromebook: searching for a show they watch opens JustWatch full screen, and following its link to one of their services gives a window that Alt+Shift+H pauses and the launcher marks **Open**. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-search#AC-1` in its name.

## Scope

- In: the controller's `search` and `onTabUpdated`, the launcher's search box; their tests.
- Out: the Home button (slice 5).

## APIs and assumptions

- The search window is remembered in `chrome.storage.session` (`searchWindow`), as service windows
  are; the text is only in the window's address, never stored.
- `chrome.tabs.onUpdated` reports the search window's new address when the viewer follows a link;
  the `tabs` permission (slice 1) lets the extension read it.
- The extension tests check the address the search window opens, not JustWatch's page, so they do
  not depend on JustWatch being reachable.

## Delivery

- In the same pull request as slices 1 to 3: this spec, a tests-first commit, then the
  implementation.
