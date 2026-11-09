# Notes

Implementation facts and findings. This file is not a process artifact: nothing links to it, and it
changes without approval. When a requirement or design depends on something here, move that
content into the artifact.

## Reference device (2026-11-08)

- Chromecast with Google TV (4K), `sabrina`, Android 14 (SDK 34), Cast firmware 3.72, locale en-CA.
- Google TV home screen: `com.google.android.apps.tv.launcherx`. Search and Gemini:
  `com.google.android.katniss`. An older launcher, `com.google.android.tvlauncher`, is also
  installed.
- Wireless debugging is on; this PC is paired with it. The connection port changes when wireless
  debugging restarts; `adb mdns services` finds the current one.

## What was tried on the reference device (2026-11-08)

- Apps only mode (Settings, Accounts & Profiles, the account, Apps only mode) hides the
  recommendation rows and the Movies and Shows tabs. It also turns off search and voice, the
  watchlist, and Google Play purchases, and still shows a rotating "featured" banner above
  "Your apps". Turned back off.
- Projectivy Launcher with its accessibility service and "Override current launcher" took over the
  Home button. Gemini voice search still worked; typed search across apps did not, because it is
  part of the Google TV home app, and Projectivy intercepts every launch of that app (including a
  direct launch of its Gemini tab). The override and the accessibility service are now off;
  Projectivy is still installed.

## Search entry points found on the device

From `cmd package query-activities` and `dumpsys package`:

- `android.search.action.GLOBAL_SEARCH`, `android.intent.action.ASSIST`, and
  `android.speech.action.WEB_SEARCH` all resolve to
  `com.google.android.katniss/com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline`.
  Sent with no extras, `GLOBAL_SEARCH` opened Gemini in voice-listening mode. With a `query`
  extra it takes the text as typed: see the spike below.
- `com.google.assistant.SEARCH_TAB` opened katniss's `LauncherUpdateActivity`, which sent the TV to
  the Play Store page for Google TV Home. Not useful.
- `android.apps.tv.launcherx.GEMINI_CHAT` is handled by the Google TV home app (intercepted while
  Projectivy's override was on; not tried since).
- Apps that accept `android.intent.action.SEARCH`: Spotify, Tubi, CTV, VLC, CBC News, TuneIn, the
  Play Store, and ExpressVPN (its split-tunnelling screen). Netflix, Prime Video, Disney+, Apple TV,
  Plex, MUBI, and the Criterion Channel do not declare it.
- Other apps' search suggestion providers need `android.permission.GLOBAL_SEARCH`, which only
  system apps can hold.

## Becoming the home screen

Google TV has no setting for choosing a home screen. Known routes: an accessibility service that
intercepts Home (Projectivy's approach), or disabling `launcherx` over ADB. Whether
`cmd package set-home-activity` works on this build is not yet tested.

## Spike: a typed query from another app (2026-11-09)

Sent over ADB, with Google TV as the home screen and the TV asleep:

```
am start -a android.search.action.GLOBAL_SEARCH   -n com.google.android.katniss/com.google.android.apps.tvsearch.app.launch.trampoline.SearchActivityTrampoline   --es query 'Severance'
```

- Google TV's own page for the show opened (`com.google.android.apps.tv.launcherx/.entity.EntityActivity`):
  title, rating, synopsis, reviews, and **Apple TV, Watch now**, plus Seasons, Watchlist, and cast.
  This is the "which app has it" answer the concept wants, reached from outside Google's home app.
- Gemini decides what a query means. A query of just "The" (a quoting mistake) got a Gemini chat
  reply asking for the rest of the sentence, with a "Stop audio" control, not a results page. So a
  vague query may get a conversation rather than results.
- The results page belongs to the Google TV home app (`launcherx`), not to katniss. Any way of
  taking over the Home button has to leave `launcherx` installed, enabled, and not intercepted;
  disabling it over ADB, or Projectivy-style interception of all its launches, would break search.
- The first launch (Gemini chat) did not wake the TV; the second (the show page) did. Back, Home,
  and `KEYCODE_SLEEP` returned it to Google TV's home screen, asleep. A launcher only sends the
  query while someone is using the TV, so waking is not a concern for the product, but further
  tests should expect the TV to turn on.
- Not tried: `android.intent.action.WEB_SEARCH`, whether a query naming several shows or a person
  gives a results list, and `cmd package set-home-activity` (it changes the home screen, so it
  needs the project lead's go-ahead).
