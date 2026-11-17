---
status: draft
tests: [tests/chromebook]
tests-commit: b321a09dfc9c7a34a41e24697c8d51804d2a09df
unlocks:
  - tests/chromebook/editing-extension.test.js: the menu item **Move** was looked up by name, which Playwright matches as a substring, so it also found **Remove**; the lookups of **Move** are made exact, and nothing else changes
---

# The Chromebook launcher: editing services

<!-- docs/specs/chromebook-editing.md. Chromebook delivery, slice 3 of 5 (PLAN.md): adding services
from a list or by address, with Chrome's permission prompt, and moving, hiding, showing, and
removing them. From REQ-chromebook-3, 4, 10; story-chromebook-services; design-chromebook#editing;
docs-chromebook#add-your-services and #arrange-your-services. -->

## Problem

The Chromebook viewer needs the launcher to show exactly their services, in their order
(story-chromebook-services). Slice 2 shows services kept in storage; this slice lets the viewer put
them there, arrange them, and allow pausing on each site.

## User stories

- story-chromebook-services: choose my services, and arrange and hide them.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | **Edit services** starts editing: it reads **Done**; the grid shows the visible services, then an **Add a service** tile, then, if any are hidden, **Hidden services: not on the launcher. Select one to show it again.** and the hidden ones; the hint line reads **Select a service to move, hide, or remove it. Changes are saved as you go.** and "Editing services" is announced. **Done**, or Esc with nothing lifted or open, ends editing. | test: editing-extension.test.js |
| AC-2 | While editing, selecting a service opens its menu: **Move**, **Hide** (or **Show** for a hidden one), **Allow pausing** (only for a service whose site the extension may not run on), **Remove**, and **Cancel**. Esc or **Cancel** closes it. Choosing a service while editing does not open it. | test: editing-extension.test.js |
| AC-3 | **Move** lifts the service. Left and Right (and Up and Down, a row at a time) move it one place, announced as "<name>, moved to <n> of <total>"; Enter puts it down and saves the order; Esc puts it back where it was. With the touchpad or a finger, a service is dragged to its new place and saved when let go. | test: editing.test.js, editing-extension.test.js |
| AC-4 | **Hide** takes a service off the launcher and lists it under the hidden services; **Show** puts it back in its place in the order. | test: editing.test.js, editing-extension.test.js |
| AC-5 | **Remove** asks **Remove <name>? Its window will close.** with **Remove** and **Cancel**; **Remove** takes the service out of the list and closes its window, if open. | test: editing.test.js, editing-extension.test.js |
| AC-6 | **Add a service** opens a panel listing the streaming services available in Canada, with those already added marked and not offered again, and a field for any other site's address with **Add** and **Cancel**. Esc or **Cancel** closes it. | test: editing-extension.test.js |
| AC-7 | Choosing a service from the list, or giving an address and choosing **Add**, asks Chrome for permission on that site (`*://*.<site>/*`) and adds the service at the end whether or not it is allowed; refused, its tile says **Videos won't pause**. An address without `https://` gets it; a service's name from an address is its site without `www.`; text that is not a site address is not added and the status line says **That isn't a site address.** | test: editing.test.js, editing-extension.test.js |
| AC-11 | Chrome accepts the permission the extension asks for, for any service: the real `chrome.permissions.request` shows Chrome's prompt rather than refusing it (found on the project lead's Chromebook, 2026-11-17: `*://` patterns were refused, so no site was ever allowed and nothing paused). | test: permission-request.test.js |
| AC-8 | **Allow pausing** asks Chrome again for permission on the service's site. | test: editing-extension.test.js |
| AC-9 | Every change is saved in `chrome.storage.local` at once, and is there after the launcher, Chrome, or the Chromebook restarts. | test: editing-extension.test.js |
| AC-10 | On the project lead's Chromebook: add their services from the list (allowing each), add one by address, move one with the keyboard and one by dragging, hide and show one, remove one, and see the order kept after Chrome restarts. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-editing#AC-1` in its name.

## Scope

- In: `chromebook/lib/editing.js` (the list changes), `chromebook/lib/catalogue.js` (the services
  in Canada), editing in the launcher page; their tests.
- Out: search (slice 4); the Home button (slice 5).

## APIs and assumptions

- `chrome.permissions.request` needs a user gesture, which the click or key on the launcher page
  gives. It shows Chrome's own prompt, which tests cannot answer, so the extension tests replace
  `chrome.permissions.request` in the launcher page with one that answers yes or no.
- Permission is asked for the whole site (`*://*.<site>/*`), since services move between
  subdomains (`www.`, `app.`, `gem.`); slice 2 checks it per address.
- The catalogue's addresses were checked to answer on 2026-11-17; a service can be added by address
  if one is missing or moves.

## Delivery

- In the same pull request as slices 1 and 2: this spec, a tests-first commit, then the
  implementation.
