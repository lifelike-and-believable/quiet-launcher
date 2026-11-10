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
| 3 | Requirements | `docs/product/requirements/{home,search,setup}.md` | Approved 2026-11-09 (#4 merged, `13f6a23`) | #4 |
| 4 | Personas | `docs/product/personas/viewer.md`, `setup-person.md` | Approved 2026-11-09 (#5, #6) | #5, #6 |
| 5 | Epics | `docs/product/epics/{quiet-home,search,arranging,setup}.md` | Approved 2026-11-09 (#11) | #11 |
| 6 | Stories | `docs/product/stories/{quiet-home,search,setup}/` (first release, 12) | Approved 2026-11-09 (#16) | #16 |
| 7 | Design and UX, with mock-ups | `docs/design/home-screen.md`, `setup.md`, `decisions/home-takeover.md` | Approved 2026-11-09 (#29; Play Store and build source amended in #30) | #29, #30 |
| 8 | User documentation | `docs/user/home-screen.md`, `setup.md` | Approved 2026-11-09 (#30) | #30 |
| 9 | Specs, tests first, implementation | `docs/specs/`, `app/`, `build.ps1` | Slice 1 merged (#31); slice 2 in #32; slices 3 and 4 not started | #31, #32 |
| 10 | Tutorials (`runner: manual` until there is an on-device runner) | `docs/tutorials/` | Not started | |
| 11 | Release | | Not started | |

## Next steps

1. Done (2026-11-09): the project lead answered the concept's open questions (see Decisions).
2. Done (2026-11-09) for `GLOBAL_SEARCH`; `set-home-activity` tried later (step 4); `WEB_SEARCH` untried. Spike: send
   `android.search.action.GLOBAL_SEARCH` and `android.intent.action.WEB_SEARCH` with a
   `query` extra to `com.google.android.katniss`, and see whether Google's results open for the
   typed text. Also check whether `cmd package set-home-activity` works on this build. Record the
   results in `docs/product/notes.md`.
3. Done (2026-11-09): the project lead confirmed every `[proposed]` claim; the concept says
   `status: approved` in #3 (`tier:epic`), merged 2026-11-09.
4. Done (2026-11-09): requirements for home, search, and setup, from the project lead's answers.
   Four spikes on the TV settled how to take over Home (notes): `set-home-activity` and disabling
   only Google TV's home screens fail; an accessibility service that catches the Home key, with a
   check that covers Google TV's home screen if it comes up anyway, works and keeps search. The
   files were approved when #4 merged.
5. Done (2026-11-09): two personas from the project lead's answers, the household viewer (the other
   adult and guests) and the set-up person (the project lead), approved as working assumptions
   (`[to validate]`). The check of each requirement against each persona led to two amendments in
   their own PR: only the set-up person arranges and hides apps, and each app shows its own artwork
   and name.
6. Done (2026-11-09): four epics. First release: quiet-home, search, setup (apps in install
   order). Second release: arranging (on the TV behind a guard, and from the PC); REQ-home-3 and 4
   moved to `Release: second`.
7. Done (2026-11-09): 12 first-release stories, revised after a persona walkthrough, approved in
   #16. Arranging's stories come with the second release.
8. Done (2026-11-09): the first-release design, checked on the TV (the app list, the search wait)
   and revised after a persona walkthrough, a UX review, and an accessibility review; approved in
   #29. Four `[to validate]` items for the first build's on-TV tests: the service is running at
   start-up before Google TV's home screen appears; Back from an app opened from the quiet home
   screen returns to it; the remote's voice button is unaffected; Google's search screen opens in
   about half a second.
9. Done (2026-11-09): user documentation for the household and the set-up person, revised after a
   persona walkthrough, approved in #30. With it, the Play Store is shown as the last app, and the
   Quiet Launcher folder is built from the repository (`.\build.ps1`); nothing is published.
10. Delivery, in four slices, each a spec, a tests-first commit, and the implementation:
    1. Merged as #31: the project skeleton (Gradle, Kotlin, `build.ps1`, CI) and the app list
       (`docs/specs/app-list.md`); checked on the TV.
    2. Merged as #32: taking over Home (the accessibility service in its own process, an instant cover,
       Back doing nothing, the list drawn before the apps are read again); 20 of 20 clean Home
       presses on the TV, 52 to 84 ms to draw. Three of the four `[to validate]` items confirmed
       (Back from an app, the voice button, restart as observed); the fourth (Google's search
       screen opening in about half a second) belongs to slice 3.
    3. Merged as #33: the top row and search (search box, searching and failed states, voice
       search, Settings, clock, status line, TalkBack, movement); checked on the TV.
    4. The **Google TV** button (the project lead's request, 2026-11-09): documents approved in #34;
       the code in #36, checked on the TV with the project lead.
    5. The setup and undo scripts, tested against a fake ADB, then on the TV.

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
| 2026-11-09 | Pressing Home never shows Google TV's home screen; if it comes up on its own, it is covered within 0.5 s | `docs/product/requirements/home.md` (REQ-home-2) |
| 2026-11-09 | Personas: household viewer and set-up person, as working assumptions | `docs/product/personas/` |
| 2026-11-09 | Only the set-up person arranges and hides apps; apps show their own artwork and name | `docs/product/requirements/home.md` |
| 2026-11-09 | First release: home, search, setup; arranging and hiding in the second release, on the TV behind a guard and from the PC | `docs/product/epics/` |
| 2026-11-09 | If the product stops working, Google TV's home screen is the fallback; no self-detection or recovery in the first release | `docs/product/epics/setup.md` |
| 2026-11-09 | Apps stay in install order until arranging; other home-screen apps (Projectivy) are left out | `docs/design/home-screen.md` |
| 2026-11-09 | Play Store shown as the last app; the folder is built from the repository, not published | `docs/design/`, `docs/user/setup.md` |
| 2026-11-09 | Gradle and Kotlin; standing go-ahead to install and remove test builds on the TV, restoring Google TV afterwards | `README.md`, `HANDOFF.md` |
| 2026-11-09 | A **Google TV** button in the top row opens Google TV's own home screen, for anyone, until the next Home press or sleep (concept non-goal changed, REQ-home-12) | `docs/product/`, `docs/design/`, `docs/user/` |
| 2026-11-09 | Take over Home without disabling or blocking the Google TV home app, because its results page is what search shows | `docs/product/concept.md` (Feasibility) |
