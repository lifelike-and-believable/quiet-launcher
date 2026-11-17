---
status: draft
tests: [tests/chromebook]
tests-commit: ac036b5904d607564ed553a83fcd868a79844b42
unlocks:
  - tests/chromebook/pictures-extension.test.js: the sample PNG written into the test was not a valid image (so the launcher rightly refused it) and is replaced with one that is; and the sizes are measured as laid out (offsetWidth), since the selected tile is drawn 5% larger by design; nothing else changes
---

# The Chromebook launcher: bigger icons and a picture of your own

<!-- docs/specs/chromebook-pictures.md. Chromebook delivery, slice 8 (PLAN.md): the project lead's
note of 2026-11-17 that the TV's tiles show larger artwork, usually the logo and lettering
together, while the Chromebook's icons were small. From design-chromebook (Layout, Editing) and
docs-chromebook#arrange-your-services. -->

## Problem

The TV's tiles are each app's banner: wide artwork with the logo and the name's lettering. The
Chromebook's tiles showed a 64 px icon. Sites publish no banners (their share images often promote
shows, which the launcher must not), so the icon grows, and the viewer can give a service a picture
of their own.

## User stories

- story-chromebook-open-service: each service under its own name, in the TV's look.
- story-chromebook-services: choose my services, and arrange them.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | A tile with a kept icon shows it at 100 px (amending chromebook-icons#AC-3's 64 px). | test: pictures-extension.test.js |
| AC-2 | While editing, a service's menu has **Choose picture** after **Hide** or **Show**, and **Remove picture** after it when the service has a picture. | test: pictures-extension.test.js |
| AC-3 | **Choose picture** opens ChromeOS's file picker for images. The chosen image, scaled to fit 640 by 360 px, is kept in `chrome.storage.local` (`pictures`, by service id) and fills the whole tile (cropped to the tile, no icon), and stays when icons are fetched again. | test: pictures-extension.test.js |
| AC-4 | **Remove picture** takes the picture away; the tile shows the icon again. | test: pictures-extension.test.js |
| AC-5 | A file that is not an image is not kept, and the status line says **That file isn't a picture.** | test: pictures-extension.test.js |
| AC-6 | Removing a service removes its picture. | test: pictures-extension.test.js |
| AC-7 | On the project lead's Chromebook: the icons are bigger; a picture chosen for a service fills its tile. | command: the project lead on the Chromebook |

A test cites a criterion as `chromebook-pictures#AC-1` in its name.

## Scope

- In: the launcher page's tiles and menu; their tests. The editing tests that list a service's menu
  are changed, recorded under chromebook-editing's `unlocks:`.
- Out: importing the TV's banners (the next slice).

## APIs and assumptions

- A hidden `<input type="file" accept="image/*">`, clicked from the menu item, opens ChromeOS's own
  picker. The image is decoded in the page and drawn into a canvas at most 640 by 360, then kept as
  a `data:` address (PNG when the file is a PNG, else JPEG), so a big file never fills storage.

## Delivery

- One pull request: this spec, a tests-first commit, then the implementation.
