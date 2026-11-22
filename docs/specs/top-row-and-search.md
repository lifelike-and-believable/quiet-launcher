---
status: implemented
tests: [app/src/test]
tests-commit: 11dc7420668d8b4430d553deca738b24cf4464ff
---

# The top row and search

<!-- docs/specs/top-row-and-search.md. Delivery slice 3 (PLAN.md, step 10): the quiet home screen's
top row (Search for a show, Voice search, Settings, the clock), the status line, searching and its
states, movement between the top row and the apps, and accessibility. -->

## Problem

The household viewer needs to type a show's name on the home screen and see which app has it
(story-search-type-and-search, the product's main reason to exist), to come back from Google's
screens with Back (story-search-back-from-results), to search by voice (story-search-voice-search),
and to see the time (story-quiet-home-clock); the set-up person needs the TV's settings from the
home screen (story-quiet-home-tv-settings). Everything follows design-home-screen and
docs-home-screen.

## User stories

- story-search-type-and-search, story-search-back-from-results, story-search-voice-search,
  story-quiet-home-tv-settings, story-quiet-home-clock.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Above the apps is a top row with, left to right, **Search for a show** (a search box), **Voice search**, **Settings**, and the clock; under it a status line whose space is always reserved, so a message never moves the apps. | test: TopRowTest |
| AC-2 | Pressing the keyboard's **Search** key with text in the box sends Google's search the text exactly as typed (a `query`, to the first entry point of `com.google.android.katniss` that the TV has: `ASSIST`, then the older `GLOBAL_SEARCH` trampoline); with nothing typed it sends nothing and keeps the keyboard open. The box asks the keyboard for a Search key. | test: SearchTest |
| AC-3 | From **Search** until Google's search screen opens, an overlay shows **Searching for "<text>"…** and **Google's search will open in a moment. Back cancels.**, with a spinner unless the TV's animations are off; **Search for a show** stays selected; every key except Back and Home is ignored; Back cancels, keeping the text. | test: SearchTest |
| AC-4 | When Google's search screen opens (the quiet home screen stops being in front), the overlay goes and the box is cleared; on coming back, **Search for a show** is selected and empty. | test: SearchTest |
| AC-5 | If Google's search has not opened after 10 seconds, the overlay goes, the text stays in the box, the box stays selected, and the status line says **Google's search did not open. Press select to try again.** | test: SearchTest |
| AC-6 | **Voice search** sends Google's search with no text (which opens Google's voice search); on coming back, **Voice search** is selected. | test: SearchTest |
| AC-7 | **Settings** opens the TV's settings app (`android.settings.SETTINGS`). | test: TopRowTest |
| AC-8 | The clock shows the time from the system clock, following the TV's 24-hour setting, without AM or PM in 12-hour mode, and cannot be selected. | test: TopRowTest |
| AC-9 | Without an internet connection the status line says **No internet connection. Search and voice search need it; your apps still open.**, and it clears when the connection returns. | test: StatusLineTest |
| AC-10 | Movement: left and right stop at the ends of the top row and of each row of apps (no wrapping); down from a top-row control goes to the app nearest below it; up from the first row of apps goes to the top-row control nearest above. | test: MovementTest |
| AC-11 | Back while the search box is selected (keyboard closed) moves the selection to the first app; it never closes the screen. A Home press clears the box. | test: SearchTest |
| AC-12 | With TalkBack: the controls read "Search for a show", "Voice search", and "Settings" with their roles; the magnifier, microphone, and gear are not read; the searching overlay and the status line are announced when they change (the search failure at once, the rest politely); the clock is not announced as it changes. | test: AccessibilityTest |
| AC-13 | On the reference device: typing a show's title and pressing **Search** opens Google's page for it; Google's search screen opens within about half a second of **Search** (the `[to validate]` item in design-home-screen); Back returns with the box selected and empty; **Voice search** and **Settings** work; the screen matches the design's mock-ups. | command: install the build (standing go-ahead), check with the remote and screenshots, restore the TV |
| AC-14 | If, within 20 seconds of Google's search screen opening for a typed search, Google TV puts its own home screen over the results and the service covers it (home-takeover#AC-3), the home screen searches again once for the same text, with the overlay of AC-3 and the failure of AC-5 if it does not open; afterwards AC-4 holds as before. There is never a second retry: for 20 seconds after the retry, a further cover only brings the quiet home screen forward, without a Home press's reset (Google TV reports its home screen again; seen on the TV). A voice search is not retried, and a Home press or any key on the quiet home screen first cancels both; otherwise the cover acts as a Home press. (Added 2026-11-22: launcherx starts its home screen over a page of its own when it has been in the background for a while; see docs/product/notes.md.) | test: SearchTest |

A test cites a criterion as `top-row-and-search#AC-1` in its name or a comment.

## Results on the reference device (2026-11-09)

AC-13, driven over ADB with screenshots (no Home key needed): typing "Severance" and pressing
**Search** handed over to Google's search screen within about 10 ms of the key (Google's page for
the show followed about 5 s later), which confirms the design's `[to validate]` half-second; Back
returned with **Search for a show** selected and empty; **Voice search** opened Google's listening
prompt over the quiet home screen; **Settings** opened the TV's settings. The screen matches the
mock-ups. Found and fixed: **Settings** reopened the Settings app wherever it had been left (deep in
Developer options), so it now always opens a fresh Settings task at the top level.

## Results on the reference device (2026-11-22)

Google's search app (katniss) updated itself to 8.2.17, which removed the `GLOBAL_SEARCH` trampoline,
so **Search** and **Voice search** did nothing (docs/product/notes.md, "Search entry points after
katniss 8.2"). With `ASSIST` first: typing "Severance" and pressing **Search** opened Google TV's
page for the show about 6 s later (Google's "Just a sec…" shows meanwhile); "Star Trek" opened
Gemini's results for the franchise; Back returned with **Search for a show** selected and empty.
**Voice search** opens Google's voice prompt; over ADB it closes at once because no remote has an
open microphone (`AtvRemote.AudioProviderService: No active audio device available`), so listening
with a press on the real remote is still to be checked.

AC-14, from the search box with launcherx in the background for 10 minutes or more: 4 runs, 3 of
which Google TV's home screen covered; each time the retry opened the show's page (12.6 to 14 s from
**Search**), it stayed, and Back returned with the box selected and empty. The retry never had its
home screen come up. Two searches straight after others neither bounced nor retried (5 to 8 s).

## Scope

- In: `HomeActivity`'s layout (top row, status line, overlay), the search flow, voice search,
  Settings, the clock, the connectivity status, movement, and accessibility attributes; the
  `ACCESS_NETWORK_STATE` permission (no internet permission).
- Out: arranging and hiding apps (release 2); the setup scripts (slice 4).

## APIs and assumptions

- APIs used: `EditText` with `imeOptions="actionSearch"` and `OnEditorActionListener`; Google's
  search entry points, looked up at run time with `PackageManager.resolveActivity` and tried in
  order: `Intent.ACTION_ASSIST` to the package `com.google.android.katniss` (katniss 8.2 on), then
  `android.search.action.GLOBAL_SEARCH` to `com.google.android.katniss/
  com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline` (the spike's,
  for TVs not yet updated); a `<queries>` entry for the package, so the look-up can see it;
  `android.provider.Settings.ACTION_SETTINGS`; `TextClock`; `ConnectivityManager.registerDefaultNetworkCallback`
  with `NET_CAPABILITY_INTERNET`; `View.setAccessibilityLiveRegion`; `ValueAnimator.areAnimatorsEnabled`.
- Assumption: Google's search screen opens about half a second after the request (seen in the
  spike); AC-13 checks it.

## Non-functional requirements

- Still no internet permission: the connectivity status needs only `ACCESS_NETWORK_STATE`.
- The 1-second Home budget (home-takeover#AC-8) still holds with the top row.

## Delivery

- Plan item: `PLAN.md`, step 10, slice 3. One pull request, merged with a merge commit.
