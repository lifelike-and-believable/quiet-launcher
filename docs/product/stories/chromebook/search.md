---
id: story-chromebook-search
status: approved
derived-from:
  - epic-chromebook#outcome: d32a14d5a50e
  - persona-chromebook-viewer#goals: 322e3fb02f13
  - persona-chromebook-viewer#frustrations: 574438d89687
---

# Search for a title and see which service has it

<!-- docs/product/stories/chromebook/search.md. Release: first, or just after (epic-chromebook).
REQ-chromebook-8. -->

As the Chromebook viewer, I want to type a title on the launcher and see which of my services has
it, so that I do not search each service in turn.

## Acceptance criteria

- The launcher has a search box I can type into with the keyboard, or with the on-screen keyboard
  on the touchscreen.
- Submitting a title opens JustWatch's search for Canada for exactly that text, in its own
  full-screen window, listing every film and show with that name and the services that stream
  each.
- Following a link from there to one of my services makes that window the service's window: the
  shortcuts and pausing work in it, and choosing that service on the launcher brings it back.
- Submitting an empty box does nothing. The launcher keeps no record of what I searched.
