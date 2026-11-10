---
id: requirements-search
status: approved
derived-from:
  - concept#vision: b94432cae6a3
  - concept#feasibility: 5a6fcb1f845f
---

# Search requirements

<!-- docs/product/requirements/search.md. Typed and voice search from the home screen. Typed search
across all the household's apps is the product's main reason to exist (concept, Vision). The
project lead's answers of 2026-11-09 are written as decided; the project lead also accepted
the agent's proposals for the rest. -->

## REQ-search-1: A search box on the home screen

Release: first

The home screen has a search box. Someone can type into it with the remote and the on-screen
keyboard without leaving the home screen.

## REQ-search-2: Typed search shows which app has it

Release: first

Submitting text from the search box opens Google's search results for exactly that text: the same
results Google TV's own search would show, including which of the household's apps has a show. On
the reference device, searching for a show's title opens that show's page, naming the app to watch
it in.

## REQ-search-3: Voice search

Release: first

The home screen has a button that starts Google's voice search. The remote's own voice button keeps
working as it does today.

## REQ-search-4: Google decides what a query means

Release: first

The product sends the text as typed and does not reword it. If Google answers with a chat reply
instead of results, that is what the household sees, and they can change the text and search
again.

## REQ-search-5: Back returns home

Release: first

Pressing Back from Google's results page, or its chat reply, returns to the home screen, with
the search box empty.

## Constraints

- Results come from Google's search on the TV; the product keeps no catalogue of what streams where
  (concept, Non-goals).
- The product does not store queries or send them anywhere other than Google's search on the TV.

## Success measures

- On the reference device, searching by title for each of ten shows the household
  watches opens a page naming an installed app that has it, for at least nine of the ten.
