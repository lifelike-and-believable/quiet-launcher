---
id: epic-search
status: approved
derived-from:
  - requirements-search#REQ-search-1: e47eda636964
  - requirements-search#REQ-search-2: 2fdc4dcf6cad
  - requirements-search#REQ-search-3: 563b39e1edf8
  - requirements-search#REQ-search-4: ff52c9749dbc
  - requirements-search#REQ-search-5: 366577f821d1
  - persona-viewer#goals: 24b5a84b1f16
  - persona-viewer#frustrations: 8722d20eb95e
---

# Search

<!-- docs/product/epics/search.md: typed and voice search from the home screen. The concept's main
reason to exist. -->

## Outcome

A viewer types a show's name on the home screen and sees Google's page for it, naming the app that
has it, without opening each app in turn. Back brings them home. Voice search still works.

## Personas

- persona-viewer: finding which app has a show is one of their frustrations, and typing a title is
  already how they look for one. A guest can search without being shown how.

## Proposed stories

- Type and search: a search box on the home screen, the on-screen keyboard, and the text sent
  unchanged to Google's results (REQ-search-1, 2, 4).
- Back from results: Back from Google's page or chat reply returns to the home screen, with the box
  empty (REQ-search-5).
- Voice search: a button on the home screen starts Google's voice search, and the remote's own voice
  button keeps working (REQ-search-3).

## Stories

- story-search-back-from-results
- story-search-type-and-search
- story-search-voice-search

## Release groups

- First release (with epic-quiet-home and epic-setup).
- Type and search, with back from results: a search that strands the viewer on Google's page is not
  a release.
- Voice search can ship on its own.

## Dependencies

- epic-quiet-home: the search box lives on the home screen, and REQ-search-5 returns to it.

## Open questions

- None.

## Out of scope

- Steering a vague query away from a Gemini chat reply (REQ-search-4).
- Searching inside individual apps, or keeping a catalogue of what streams where (concept,
  Non-goals).
