---
id: spec-chromebook-one-service
status: approved
derived-from:
  - story-chromebook-open-service#acceptance-criteria: 314afdd4d079
  - docs-chromebook#watch-something: dad567bc8618
  - docs-chromebook#find-which-service-has-a-show: 01b59f5064c5
  - design-chromebook#flow: e7083082e76d
  - design-chromebook#how-it-works: ff584664a6b9
  - design-chromebook#layout: 62e7bd978b1c
tests: [tests/chromebook]
tests-commit: 089b592175ded629e41a4833253f487a823dade9
unlocks:
  - tests/chromebook/search-controller.test.js: chromebook-search#AC-4's second test followed a link to Gem in the results after opening Prime Video, but opening Prime Video now closes the results (AC-4); it follows both links first and opens Prime Video last, checking the same things; nothing else changes
  - tests/chromebook/search-controller.test.js: chromebook-search#AC-4's third test checked that the service's old window stays open, but searching now closes it (AC-3); it checks that the results window, not the old one, is the service's; nothing else changes
  - tests/chromebook/back-to-browsing.test.js: chromebook-back-to-browsing#AC-1's test opens two services and a search and expected all six windows open before Back to browsing, but now only the results are (AC-1, AC-3); it expects four, and looks only at the calls Back to browsing makes when checking that the browsing window comes forward before anything closes; nothing else changes
  - tests/chromebook/back-to-browsing-extension.test.js: chromebook-back-to-browsing#AC-1's test opens two services and expected at least three windows besides the browsing ones before Back to browsing, but opening the second closes the first (AC-1); it expects at least two (the second service and the launcher); nothing else changes
---

# The Chromebook launcher: one service open at a time

<!-- docs/specs/chromebook-one-service.md. The revision merged in #76
(docs/decisions/claude-one-service-open.md): the project lead's change of 2026-11-17. -->

## Problem

Choosing service after service while looking for something to watch leaves a full-screen window,
with its video player, open behind the launcher for each one. They use the Chromebook's battery
and memory, some start trailers on their own (Apple TV+ does), and the launcher marks every one
**Open**. The project lead decided that only one service is open at a time.

## User stories

- story-chromebook-open-service: only one service is open at a time; choosing a different one
  closes the one I had open.
- story-chromebook-search: find which service has a show, and go to it.

## Acceptance criteria

| # | Criterion | Verified by | Source |
|---|---|---|---|
| AC-1 | Choosing a service that is not open opens it in its own full-screen window, as before, and closes the window of the service that was open; the launcher then knows only the new one as open (docs-chromebook#watch-something, design-chromebook#flow step 2). | test: one-service.test.js | story-chromebook-open-service#acceptance-criteria, docs-chromebook#watch-something |
| AC-2 | Choosing the service that is open brings its window back as it was left, without reloading it, and closes nothing else (docs-chromebook#watch-something). | test: one-service.test.js | story-chromebook-open-service#acceptance-criteria, docs-chromebook#watch-something |
| AC-3 | Searching opens (or reuses) JustWatch's results window and closes the service that was open (docs-chromebook#find-which-service-has-a-show step 2, design-chromebook#flow step 4). | test: one-service.test.js | docs-chromebook#find-which-service-has-a-show |
| AC-4 | Choosing a service on the launcher while JustWatch's results are open closes the results window (design-chromebook#flow step 4). | test: one-service.test.js | docs-chromebook#find-which-service-has-a-show |
| AC-5 | When a link in the results turns their window into a service's window, any other service window that is open closes, and that window is the one open service (design-chromebook#how-it-works, Windows). | test: one-service.test.js | docs-chromebook#find-which-service-has-a-show |
| AC-6 | Alt+Shift+J (go-last) goes to the one open service; with none open it brings the launcher forward, as before. The launcher's **Open** marker therefore shows on one service at most (design-chromebook#layout). | test: one-service.test.js | docs-chromebook#watch-something |
| AC-7 | In the extension in Chromium, choosing one service and then another leaves exactly one service window open, and the launcher marks only the second **Open**. | test: one-service-extension.test.js | story-chromebook-open-service#acceptance-criteria, docs-chromebook#watch-something |

A test cites a criterion as `chromebook-one-service#AC-1` in its title or a comment.

## Scope

- In: `chromebook/lib/controller.js` (`openService`, `search`, `onTabUpdated`); their tests.
- Out: muting or stopping a video the open service starts by itself while the launcher is in front
  (design-chromebook, States and errors: the launcher does not fight it); Back to browsing, which
  already closes everything.

## APIs and assumptions

- `chrome.windows.remove` closes a window and fires `windows.onRemoved`, which already drops it
  from the session's map (`onWindowRemoved`).
- The new window is opened before the old one is closed, so the launcher or the new service is
  always in front and closing the old one never shows the browsing window.
- The services remember the viewer's place in a show themselves; closing a window loses only where
  the viewer was in the site's pages (docs-chromebook#watch-something).

## Non-functional requirements

Fewer windows open: at most the launcher, one service or the results, and the viewer's own browsing
windows.

## Delivery

- Plan item: PLAN.md, 2026-11-17, one service open at a time.
- One pull request: this spec, the tests first, then the controller change and the tutorial.

## Risks and decisions

- The results window counts as the open service (the revision's choice, which the project lead
  approved in #76).
