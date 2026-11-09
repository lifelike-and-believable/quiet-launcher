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
  Sent with no extras, `GLOBAL_SEARCH` opened Gemini in voice-listening mode. Whether it accepts a
  typed query (for example `SearchManager.QUERY`) is not yet tested.
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
