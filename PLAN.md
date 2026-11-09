# Plan

The plan follows the rfr-process stages (rules-for-robots `plugins/process/GUIDE.md`). Each stage's
artifacts are approved by the project lead merging the pull request that marks them approved. This
file tracks where the project is; it is not a process artifact.

## Stages

| # | Stage | Artifacts | Status | Pull requests |
|---|---|---|---|---|
| 0 | Set-up | `docs/product/process.yml`, `.github/workflows/rfr-process.yml`, `AGENTS.md` | Done (`2c3f4fb`, first commit) | none (first commit) |
| 1 | Concept | `docs/product/concept.md` | Approved 2026-11-09 (#3 merged, `bb761b9`) | #3 |
| – | Spike: search entry points | `docs/product/notes.md` | Done 2026-11-09: a typed query from another app opens Google TV's results | #3 |
| 2 | Discovery (optional) | `docs/product/discovery/<topic>.md` | Probably skip | |
| 3 | Requirements | `docs/product/requirements/{home,search,setup}.md` | Draft; every answer written in; one `[assumed]` left (REQ-setup-2) | (this branch) |
| 4 | Personas | `docs/product/personas/` (only the web-app `end-user` starter so far) | Not started | |
| 5 | Epics | `docs/product/epics/` | Not started | |
| 6 | Stories | `docs/product/stories/` | Not started | |
| 7 | Design and UX, with mock-ups | `docs/design/` | Not started | |
| 8 | User documentation | `docs/user/` | Not started | |
| 9 | Specs, tests first, implementation | | Not started | |
| 10 | Tutorials (`runner: manual` until there is an on-device runner) | `docs/tutorials/` | Not started | |
| 11 | Release | | Not started | |

## Next steps

1. Done (2026-11-09): the project lead answered the concept's open questions (see Decisions).
2. Done (2026-11-09) for `GLOBAL_SEARCH`; `WEB_SEARCH` and `set-home-activity` untried. Spike: send
   `android.search.action.GLOBAL_SEARCH` and `android.intent.action.WEB_SEARCH` with a
   `query` extra to `com.google.android.katniss`, and see whether Google's results open for the
   typed text. Also check whether `cmd package set-home-activity` works on this build. Record the
   results in `docs/product/notes.md`.
3. Done (2026-11-09): the project lead confirmed every `[proposed]` claim; the concept says
   `status: approved` in #3 (`tier:epic`), merged 2026-11-09.
4. In progress: requirements for home, search, and setup, written from the project lead's answers
   of 2026-11-09. REQ-setup-2's assumption is settled: on 2026-11-09 an accessibility service that
   answers only Google TV's home screen took over Home while search results kept working
   (`set-home-activity` and disabling only Google TV's home screens did not work; notes). But Google
   TV's home screen flashes for 0.15 to 0.4 s at each Home press, and the project lead decided no
   flash is allowed (REQ-home-2). Left before approval: the project lead tests catching the Home
   key with the real remote (spike `key` mode; see HANDOFF.md).
5. Personas, then check each requirement against each persona.

## Decisions so far

| Date | Decision | Where |
|---|---|---|
| 2026-11-08 | Project in `lifelike-and-believable/quiet-launcher`; public since 2026-11-09 (Actions minutes for private repositories were blocked by billing) | GitHub |
| 2026-11-08 | rfr-process in solo mode; `atgoldberg` approves every stage | `docs/product/process.yml` |
| 2026-11-08 | Borrow the `web-app` profile's templates; no profile fits an Android TV app | `AGENTS.md` |
| 2026-11-08 | Pin the process workflow to rules-for-robots `rfr-v5.1.4` | `.github/workflows/rfr-process.yml` |
| 2026-11-09 | For the project lead's household only; the repository may be shared, but nothing is published | `docs/product/concept.md` |
| 2026-11-09 | Typed search across all apps is the product's main value | `docs/product/concept.md` |
| 2026-11-09 | The watchlist, Play purchases, and live TV need not stay reachable, for now | `docs/product/concept.md` |
| 2026-11-09 | Every concept claim confirmed as written | `docs/product/concept.md` |
| 2026-11-09 | Requirements answers: all apps shown, household arranges and hides them; one shared screen; search box on Home sends text to Google's results; chat replies accepted; voice button, settings, clock; set up and undone by the project lead from a PC; Home in 1 second; survives reboots | `docs/product/requirements/` |
| 2026-11-09 | Google TV's home screen must not show, even briefly, when Home is pressed | `docs/product/requirements/home.md` (REQ-home-2) |
| 2026-11-09 | Take over Home without disabling or blocking the Google TV home app, because its results page is what search shows | `docs/product/concept.md` (Feasibility) |
