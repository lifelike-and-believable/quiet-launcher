# Handoff: quiet-launcher

Last updated: 2026-11-09. Keep this current: update it at the end of each step, before a pull
request, and before any restart.

## Goal

A home screen for the household's Google TV that shows only their apps (no suggestions, banners, or
advertising) and keeps search. Read `docs/product/concept.md` (draft) for the intent and
`PLAN.md` for where the project is. Device facts and everything tried so far are in
`docs/product/notes.md`.

## Repository

| | |
|---|---|
| GitHub | `lifelike-and-believable/quiet-launcher`, public, default branch `main` |
| Local | `C:\AI Projects\quiet-launcher` |
| State | `main` at `8d4a69d` (all design stages approved); branch `slice-1-app-list` (#31) in review |
| Process | rfr-process, solo mode, approver `atgoldberg`. Rules for agents are in `AGENTS.md`. |
| Checks | `node ../rules-for-robots/checks/process-trace.mjs .` (rules-for-robots is checked out beside this repository at `C:\AI Projects\rules-for-robots`, tag `rfr-v5.1.4`) |

Every change from here on goes through a pull request with a tier label (`tier:epic` for concept,
requirements, personas, and epics). Do not merge a pull request that touches `docs/product/`,
`docs/design/`, `docs/user/`, or `docs/tutorials/` unless the project lead says to, and then comment
quoting the instruction. In solo mode, open artifact pull requests with `status: approved` and
hashes recorded (`node ../rules-for-robots/checks/process-relink.mjs <id>`); merging is the
approval.

## Where we stopped

On 2026-11-09 the project lead answered the concept's three open questions, and branch
`concept-answers` writes the answers into the concept:

- The product is for the household only. The repository is public and may be shared, but nothing
  is published or supported for others, so setup may use ADB and accessibility permissions.
- Typed search across all apps is the main reason for the product.
- The watchlist, Play purchases, and live TV do not need to stay reachable, for now.

The search spike ran the same day, with the project lead's go-ahead. Sending a show's name to
Google's search with `GLOBAL_SEARCH` and a `query` extra opened Google TV's page for the show, with
the app that has it. So the concept holds, with two limits now in its Feasibility section: the
Google TV home app must stay installed and unblocked (the results page is its), and a vague query
can get a Gemini chat reply instead. Details are in `docs/product/notes.md`.

Later that day the project lead confirmed every concept claim, and #3 merged: the concept is
approved (`bb761b9`).

Then the project lead answered the requirements questions, and branch `requirements` (PR #4,
`tier:epic`) has the three areas (`docs/product/requirements/home.md`, `search.md`, `setup.md`),
now `status: approved` with hashes recorded. They are approved when the project lead merges #4.

Four spikes the same day settled how to take over Home (`docs/product/notes.md`): `set-home-activity`
and disabling only Google TV's home screen do not work. An accessibility service does: catching the
Home key (tested that evening with the project lead at the remote: 11 presses, no Google TV home
screen seen) avoids a flash, and a check on Google TV's `home.HomeActivity` window covers it if it
comes up on its own (it did once, 1.8 s after a Home press, not reproduced). Search results still
open. The test app is in `spikes/home-takeover/` and was removed from the TV afterwards. REQ-home-2
records the project lead's decision: Home never shows Google TV's home screen, and a stray one is
covered within 0.5 s. Build tools are per user in `%LOCALAPPDATA%\AndroidBuild`.

#4 merged the same day (`13f6a23`): the requirements are approved.

Then two personas (`persona-viewer`, `persona-setup-person`), approved as working assumptions with
`[to validate]` markers, merged as #5; and #6 amended the home requirements after checking them
against the personas (only the set-up person arranges and hides apps; apps show their own artwork
and name).

Then four epics on branch `epics`, `status: approved` with hashes recorded, approved when that PR
merges: `epic-quiet-home`, `epic-search`, `epic-setup` (the first release; apps in install order)
and `epic-arranging` (the second release: on the TV behind a guard, and from the PC; REQ-home-3 and
4 moved to `Release: second`), merged as #11.

Then 12 first-release stories (`docs/product/stories/`), revised after a `persona-walkthrough`
pass, `status: approved` in PR #16 (`tier:story`); approved when it merges. The set-up persona's
title became "Set-up person" so the process check matches "As the set-up person". The project lead
accepted that if the product stops working, Google TV's home screen is the fallback. Merged as #16.

Then the first-release design, `status: approved` in PR #29 (`tier:story`), approved when it
merges: `design-home-screen` (with mock-ups rendered from `docs/design/home-screen/mockup.html` by
`render.sh`, headless Edge), `design-setup` (PowerShell scripts `quiet-setup.ps1` and
`quiet-undo.ps1`), and `decision-home-takeover`. Two read-only checks on the TV settled the app list
(Android's TV apps minus four Google system apps, and minus other home screens) and what Google
shows while a search loads. A persona walkthrough, UX review, and accessibility review led to a
revision. Four `[to validate]` items wait for the first build's on-TV tests (`PLAN.md`, step 8).
Merged as #29.

Then the user documentation (`docs/user/home-screen.md`, `docs/user/setup.md`), revised after a
persona walkthrough, `status: approved` in PR #30, approved when it merges. The project lead also
decided there: the Play Store is shown as the last app (story, design, and mock-ups amended; the
mock-ups use the reference device's real install order), and the Quiet Launcher folder is built
from the repository with `.\build.ps1` into `out\QuietLauncher`; nothing is published.

Merged as #30.

Delivery started (`PLAN.md`, step 10). Slice 1, the app list, is in PR #31: `docs/specs/app-list.md`
(draft, for the project lead), a tests-first commit (`903a182`, 14 failing tests) and the
implementation (14 passing), `build.ps1`, and `.github/workflows/android.yml` (unit tests and build
in CI). Checked on the TV: 19 tiles with real banners in the real install order, Play Store last.

Build: JDK 17, Android SDK 34, and Gradle 8.10.2 are per user in `%LOCALAPPDATA%\AndroidBuild`;
`build.ps1` finds them. Tests: `.\gradlew.bat :app:testDebugUnitTest` (set `JAVA_HOME` and
`ANDROID_HOME`, or run `build.ps1` once). Robolectric's `Activity.currentFocus` does not track focus:
use `window.decorView.findFocus()` in tests.

The project lead gave a standing go-ahead (2026-11-09) to install and remove test builds on the TV
without asking, turning this product's own accessibility service on and off as part of a test, and
always restoring Google TV's home screen afterwards (record the accessibility settings first, put
them back, uninstall, check with a screenshot, sleep the TV if it was asleep). Still ask before other
settings, other apps, or reboots.

Merged as #31 (squash; from slice 2 on, merge with a merge commit so each spec's tests-commit stays
in `main`'s history).

Slice 2, taking over Home, is on branch `slice-2-home-takeover`: `docs/specs/home-takeover.md`, two
tests-first clusters (`2e159e3`, `e62af83`), the accessibility service in its own process with an
instant cover overlay, Home resetting the screen, and Back doing nothing (27 tests pass). On the TV
on 2026-11-09 (`docs/product/notes.md`, "Slice 2 on the TV"): two strays in the first rounds, both
after Google's own screens, led to the separate process and the cover; the final build was clean in
20 of 20 Home presses (AC-7 passes). Still to do: AC-8, one press took 1.5 s with Google's sign-in
dialog up (fix: keep the app list in memory and refresh it in the background); and AC-10 (restart:
Google TV's home screen was resumed 3.7 s before the quiet one; unknown whether the cover hid it). Wireless debugging turns off on restart; the TV's ADB port is now 34447.

Sending a query wakes the TV, which may turn on the family-room TV over HDMI. Put it back with
Back, Home, then `adb shell input keyevent KEYCODE_SLEEP`. With two ADB entries for the same TV
(the IP and the mDNS name), set `ANDROID_SERIAL=10.0.0.202:<port>`. `adb pull` needs a Windows
path (`C:/...`), not `/c/...`.

## The reference device

A Chromecast with Google TV (Android 14) in the family room, at `10.0.0.202`, paired with this PC
for wireless ADB.

- ADB: `C:\Users\Demo\AppData\Local\Microsoft\WinGet\Packages\Google.PlatformTools_Microsoft.Winget.Source_8wekyb3d8bbwe\platform-tools\adb.exe`
  (installed with winget; plain `adb` works in a new terminal).
- The connection port changes whenever wireless debugging restarts. Run `adb mdns services` to find
  the `_adb-tls-connect` port, then `adb connect 10.0.0.202:<port>`. The last port was `39423`.
- In Git Bash, set `MSYS_NO_PATHCONV=1` before `adb shell` commands that name `/sdcard/...`, or Git
  Bash rewrites the path.
- Screenshot: `adb shell screencap -p /sdcard/c.png`, `adb pull /sdcard/c.png <local path>`, then
  delete the file on the TV.

This is the household's TV. Ask before changing settings, installing or removing apps, or changing
what the Home button opens. Look at a screenshot before pressing select in an unfamiliar screen:
one test intent (`com.google.assistant.SEARCH_TAB`) opened the Play Store with **Uninstall**
(Google TV Home) focused.

Current state of the TV: Google TV is the home screen, apps only mode is off, Projectivy Launcher
is installed with its override and accessibility service off, and DAZN was uninstalled at the
project lead's request.

## Lessons from the first session

- Apps only mode turns off typed search, voice, the watchlist, and Play purchases, and still shows
  a featured banner. The project lead rejected it for losing search.
- A replacement launcher that overrides Home blocks typed search, because search is inside the
  Google TV home app. Voice search (Gemini) survives.
- Other apps' search providers are closed to non-system apps (`GLOBAL_SEARCH` permission).
