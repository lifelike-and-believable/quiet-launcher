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
| State | `main` at `13f6a23` (concept and requirements approved); branches `personas` and `persona-amendments` in review |
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

Then two personas from the project lead's answers about the household: `persona-viewer` (the other
adult and guests: open a favourite app, type a title, operate a simple screen but not settings, ask
the project lead for help) and `persona-setup-person` (the project lead at the PC). Both say
`status: approved` as working assumptions, with `[to validate]` markers, in the personas PR. The
check of each requirement against each persona found two amendments, which the project lead decided
and which go in their own PR: only the set-up person arranges and hides apps (REQ-home-3, 4, 9), and
each app shows its own TV artwork and name (new REQ-home-11). Next: epics.

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
