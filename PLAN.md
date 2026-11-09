# Plan

The plan follows the rfr-process stages (rules-for-robots `plugins/process/GUIDE.md`). Each stage's
artifacts are approved by the project lead merging the pull request that marks them approved. This
file tracks where the project is; it is not a process artifact.

## Stages

| # | Stage | Artifacts | Status | Pull requests |
|---|---|---|---|---|
| 0 | Set-up | `docs/product/process.yml`, `.github/workflows/rfr-process.yml`, `AGENTS.md` | Done (`2c3f4fb`, first commit) | none (first commit) |
| 1 | Concept | `docs/product/concept.md` | Draft, waiting for the project lead's read and answers to its 3 open questions | |
| – | Spike: search entry points | `docs/product/notes.md` | Not started; answers concept open question 2 | |
| 2 | Discovery (optional) | `docs/product/discovery/<topic>.md` | Probably skip | |
| 3 | Requirements | `docs/product/requirements/<area>.md` (likely areas: home, search, setup) | Not started | |
| 4 | Personas | `docs/product/personas/` (only the web-app `end-user` starter so far) | Not started | |
| 5 | Epics | `docs/product/epics/` | Not started | |
| 6 | Stories | `docs/product/stories/` | Not started | |
| 7 | Design and UX, with mock-ups | `docs/design/` | Not started | |
| 8 | User documentation | `docs/user/` | Not started | |
| 9 | Specs, tests first, implementation | | Not started | |
| 10 | Tutorials (`runner: manual` until there is an on-device runner) | `docs/tutorials/` | Not started | |
| 11 | Release | | Not started | |

## Next steps

1. The project lead reads the concept and answers its open questions:
   - household only, or published for other Google TV owners;
   - typed search across all apps a must for the first release, or search inside one app enough;
   - whether the watchlist, Google Play purchases, and live TV must stay reachable.
2. Spike (needs the project lead's go-ahead, since it touches the TV): send
   `android.search.action.GLOBAL_SEARCH` and `android.intent.action.WEB_SEARCH` with a
   `query` extra to `com.google.android.katniss`, and see whether Google's results open for the
   typed text. Also check whether `cmd package set-home-activity` works on this build. Record the
   results in `docs/product/notes.md`.
3. Update the concept from the answers, set it to `status: approved`, and open a `tier:epic` pull
   request. Merging it is the approval.
4. Requirements: one file per area, then the cross-area check against the concept before approval.
5. Personas, then check each requirement against each persona.

## Decisions so far

| Date | Decision | Where |
|---|---|---|
| 2026-11-08 | Project in `lifelike-and-believable/quiet-launcher`; public since 2026-11-09 (Actions minutes for private repositories were blocked by billing) | GitHub |
| 2026-11-08 | rfr-process in solo mode; `atgoldberg` approves every stage | `docs/product/process.yml` |
| 2026-11-08 | Borrow the `web-app` profile's templates; no profile fits an Android TV app | `AGENTS.md` |
| 2026-11-08 | Pin the process workflow to rules-for-robots `rfr-v5.1.4` | `.github/workflows/rfr-process.yml` |
