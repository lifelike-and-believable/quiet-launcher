---
id: story-chromebook-open-service
status: approved
derived-from:
  - epic-chromebook#outcome: d32a14d5a50e
  - persona-chromebook-viewer#goals: 322e3fb02f13
  - persona-chromebook-viewer#frustrations: 574438d89687
---

# Open a service full screen, or go back to it where I left it

<!-- docs/product/stories/chromebook/open-service.md. Release: first (epic-chromebook).
REQ-chromebook-2, 5, 7. -->

As the Chromebook viewer, I want to choose a service on the launcher and be watching it full
screen, so that the Chromebook feels like the TV rather than a browser full of tabs.

## Acceptance criteria

- The launcher shows my services, each under its own name in the TV's look, and nothing else: no
  suggestions, previews, banners, or advertising.
- Choosing a service that is not open opens it in its own full-screen window. At no point do
  Chrome's tabs or address bar show, even briefly.
- Choosing a service that is already open brings its window back as I left it: same page, same
  place in the video, still signed in.
- Pressing the Chromebook's full-screen key in a service's window leaves full screen as it does
  anywhere else in Chrome.
