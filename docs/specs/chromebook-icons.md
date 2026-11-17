---
status: draft
tests: [tests/chromebook]
---

# The Chromebook launcher: each service's own icon

<!-- docs/specs/chromebook-icons.md. Chromebook delivery, slice 6 (PLAN.md): tiles show each
service's own icon, fetched from its site, instead of only the small icon Chrome keeps. From the
project lead's decision of 2026-11-17 (design-chromebook#layout, #how-it-works), REQ-chromebook-2
("each service appears under its own name, in the same look as the TV"), and
story-chromebook-open-service. -->

## Problem

Chrome's own icon cache has a site's icon only once the site has been visited in Chrome, and at 16
to 32 px, so tiles showed a generic globe or a blurred logo. The TV shows each app's own artwork.
Most streaming sites publish a large icon for phones' home screens and installed web apps; the
launcher uses that.

## User stories

- story-chromebook-open-service: the launcher shows my services, each under its own name in the
  TV's look.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | From a service's page, the icon chosen is the largest offered by its web app manifest (`<link rel="manifest">`, `icons`) or its `apple-touch-icon` and `icon` links with sizes, with addresses taken relative to the page; with none, `/apple-touch-icon.png` on the site. SVG and icons smaller than 96 px are passed over when a larger one is offered. | test: icons.test.js |
| AC-2 | When a service is added and Chrome allows its site, the extension fetches its icon once and keeps it in `chrome.storage.local` (`icons`, by service id) as image data. A response that is not an image is not kept. | test: icons.test.js, icons-extension.test.js |
| AC-3 | A tile with a kept icon shows it, 64 px with rounded corners, above the name on the service's colour; without one, the tile is as before (Chrome's small icon). | test: icons-extension.test.js |
| AC-4 | Nothing is fetched for a site Chrome has not allowed, and nothing from any site other than the service's own (and the addresses its page or manifest names for icons). | test: icons.test.js |
| AC-5 | A site whose icon cannot be had (no page, no icon, not an image, an error) leaves the tile as it was, with no message. | test: icons.test.js, icons-extension.test.js |
| AC-6 | Services that have no kept icon get one when the launcher next opens, if their site is allowed (so services added before this slice, or allowed later with **Allow pausing**, get theirs). | test: icons-extension.test.js |
| AC-7 | Removing a service removes its kept icon. | test: icons-extension.test.js |
| AC-8 | On the project lead's Chromebook, their services show their own icons, crisp at tile size. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-icons#AC-1` in its name.

## Scope

- In: `chromebook/lib/icons.js` (finding and fetching an icon), the service worker fetching them,
  the launcher showing them; their tests.
- Out: choosing an icon by hand.

## APIs and assumptions

- The service worker fetches with `fetch`, which an extension may do from any site it has host
  permission for, without the site's CORS headers. It has no `DOMParser`, so the page's `<link>`
  tags are read with a small parser that only looks at `<link>` tags.
- Icons are kept as `data:` addresses (base64) so the launcher page can show them without fetching;
  a few hundred kilobytes for nine services, well inside `chrome.storage.local`'s 10 MB.
- The extension tests serve a stand-in service with an icon from a local server.

## Delivery

- In the same pull request as slices 1 to 5: this spec, a tests-first commit, then the
  implementation.
