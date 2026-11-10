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
intercepts Home (Projectivy's approach), or disabling `launcherx` over ADB. `cmd package set-home-activity` does not work on this build: see the spike below.

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
- Not tried: `android.intent.action.WEB_SEARCH`, and whether a query naming several shows or a
  person gives a results list.

## Spike: `set-home-activity` (2026-11-09)

With the project lead's go-ahead, and Projectivy (override and accessibility service off) standing
in for this product:

- Before: the `android.app.role.HOME` role holder was `com.google.android.tungsten.setupwraith`
  (not the Google TV home app), and Home resolved to
  `com.google.android.apps.tv.launcherx/.home.HomeActivity`. Activities answering HOME: launcherx
  `HomeActivity` and `VanillaModeHomeActivity` (filter priority 2), setupwraith `RecoveryActivity`
  (priority 1), Projectivy (priority 0), and `com.android.tv.settings/.system.FallbackHome`
  (priority -1000).
- `cmd package set-home-activity com.spocky.projengmenu` printed `Success` and made Projectivy the
  role holder, but Home still resolved to launcherx's `HomeActivity`, and pressing Home opened
  Google TV. The priority-2 filter wins over the role holder on this build.
- Restored with `cmd package set-home-activity com.google.android.tungsten.setupwraith`; Home opens
  Google TV again.
- So the role is not the way in.

## Spike: disabling only Google TV's home screen (2026-11-09)

`pm disable --user 0 com.google.android.apps.tv.launcherx/.home.HomeActivity` (and the same for
`.home.VanillaModeHomeActivity`) failed with `SecurityException: Shell cannot change component
state for ComponentInfo{...} to 2`. On Android 14 the shell can disable a whole system package but
not one of its components, and disabling the whole of launcherx would break search. Nothing on the
TV changed.

That left one known route: an accessibility service that answers only Google TV's home screen.

## Spike: an accessibility service that answers only Google TV's home screen (2026-11-09)

It works. With the project lead's go-ahead, the test app in `spikes/home-takeover/` was installed
and its accessibility service turned on over ADB (`enabled_accessibility_services` was `null` and
`accessibility_enabled` `0` before). The service brings its own screen forward when a window-state
event arrives from `com.google.android.apps.tv.launcherx` with class
`com.google.android.apps.tv.launcherx.home.HomeActivity`, and ignores every other window.

| Test | Result |
|---|---|
| Home from TV settings | The test screen, 0.4 s after Google TV's home screen appeared |
| Search from the test screen (`GLOBAL_SEARCH`, `query=Severance`) | Google TV's page for the show (`.entity.EntityActivity`), with Apple TV; it stays open. About 6 s from the button press to the page (Google's time) |
| Back from that page | The test screen |
| Home from that page | The test screen, 0.15 s after Google TV's home screen appeared |
| Sleep 5 s and wake | The last app (Settings); no home screen involved |

Findings:

- Google TV's home screen is drawn for a moment (0.15 to 0.4 s, measured from its window event to
  ours) before the test screen covers it, so its promotion can flash on screen at each Home press.
  A launcherx `coreservices.bootmode.DispatchActivity` window also appears on Home and on wake; it
  is not the home screen and was ignored.
- Catching the Home key itself (the service's `key` mode) could not be tried over ADB: keys injected
  with `adb shell input keyevent` never reached the service's `onKeyEvent`. It was tried that
  evening with the real remote; see the next section.
- Not tried: surviving a restart, a long sleep (when Google TV may go to its home screen on wake),
  and Google TV updates.

Cleanup: `settings delete secure enabled_accessibility_services`,
`settings put secure accessibility_enabled 0`, `adb uninstall quiet.launcher.spike`. Home opens
Google TV again; the home role holder is still `com.google.android.tungsten.setupwraith`.

Build tools for the spike, per user with no administrator rights: Microsoft OpenJDK 17 and the
Android command-line tools (platform 34, build-tools 34.0.0) under `%LOCALAPPDATA%\AndroidBuild`.
The JDK's MSI installer needs administrator approval, which was cancelled.

## Spike: catching the Home key with the real remote (2026-11-09, evening)

The same test app in `key` mode (it consumes `KEYCODE_HOME` and opens its own screen; it does not
react to windows), with the project lead pressing the remote's buttons and the log read over ADB.

- The real remote's keys reach the service's `onKeyEvent`: Home, Back, select, the direction pad,
  and the YouTube and Netflix buttons (`KEYCODE_BUTTON_3` and `KEYCODE_BUTTON_4`). A Home press
  usually arrives as down and up 0 to 7 ms apart; held presses arrive with the real gap.
- 11 Home presses, from the Apple TV app, YouTube, Netflix, Google TV's page for a show, and the test
  screen, including three held for 2 to 6.5 s. Every one opened the test screen, 0.03 to 0.7 s after
  the press, and the project lead saw no Google TV home screen before it.
- Once, on the first press (from the Apple TV app, held 0.22 s), Google TV's `home.HomeActivity`
  came up 1.8 s after the test screen and stayed, because `key` mode does not react to windows.
  Repeating the same steps from the Apple TV app, and short and held presses from other apps, did
  not bring it back. Cause unknown.
- Consuming Home also stops Google TV's own long-press Home action.
- So catching the key avoids the flash. A product would also need the window check from the earlier
  spike as a fallback, so that Google TV's home screen, if it comes up on its own as it did once, is
  covered within about 0.4 s.

Cleanup as before; Home opens Google TV again. The full log was kept outside the repository.

## Google TV's app list and search wait (2026-11-09, evening)

With the project lead's go-ahead, read-only looks at the reference device for the home-screen
design:

- Android's list of TV-launchable apps (`cmd package query-activities -a android.intent.action.MAIN
  -c android.intent.category.LEANBACK_LAUNCHER`) has 23 entries. Google TV's Apps tab, **Your
  apps**, shows 19: the same list without `com.android.tv.settings` (TV settings),
  `com.android.vending` (Play Store), `com.google.android.play.games` (Play Games), and
  `com.google.android.videos` (Google TV). It includes Projectivy Launcher and ExpressVPN. Its order
  is the household's own (it has a **Reorder** item), not install order.
- The Apps tab itself opens with large promoted tiles for apps that are not installed (ICI TOU.TV,
  CBC Gem) above **Your apps**.
- During a typed search (`GLOBAL_SEARCH`, `query=Severance`), Google dims the screen behind and
  shows the query in a glowing box at the bottom for a few seconds, then the show's page fades in,
  blurred at first. The page was in front about 2 to 3 seconds after the request.

## Slice 2 on the TV: taking over Home (2026-11-09, evening)

The slice 2 build (`docs/specs/home-takeover.md`) on the reference device, with the project lead at
the remote, under the standing go-ahead for test installs. Evidence: Android's activity log
(`adb logcat -b events`, `wm_set_resumed_activity` and `wm_new_intent`), the service's own log (tag
`QuietLauncher`), and screen recordings classified frame by frame.

| Round | Build | Home presses | Google TV's home screen seen |
|---|---|---|---|
| 1 | first | 10 (5 YouTube, 5 Netflix) | once, about 0.75 s |
| 2 | service in its own process | 10 (5 Netflix, 5 YouTube) | never |
| 3 | same | 5 Netflix, then voice searches | once, about 2.3 s, after a voice search |
| 4 | with the instant cover | 4 from Google's page after a voice search | never |
| 5 | same | 5 Netflix, 4 with Google's sign-in dialog up | never |
| 6 | same | 11 (4 YouTube, 4 Netflix with the sign-in dialog twice, 3 after a voice search) | never |

- Both strays came after Google's own screens: Google's "sign in to Netflix with Google" dialog
  (`com.google.android.gms/.auth.api.credentials.assistedsignin.ui.AtvAssistedSignInActivity`) in
  round 1, and Google's voice search in round 3. Each time Android was asked to open Google TV's
  home screen with the same flags (`wm_new_intent ... launcherx/.home.HomeActivity ... 276824320`),
  and the service then covered it by starting the quiet home screen, which took 0.6 to 2.3 s
  (longer when Android had to restart Google TV's app for lack of memory).
- Two changes: the service runs in its own process (`:watcher`), so the home screen loading its
  banners never delays its answer to a key; and it covers Google TV's home screen at once with a
  plain overlay (`TYPE_ACCESSIBILITY_OVERLAY`) while the quiet home screen starts
  (home-takeover#AC-11). With Google TV's home screen opened directly, a recording showed no frame of
  it: quiet home screen, 0.9 s of plain cover, quiet home screen.
- With the final build, 20 of 20 Home presses (rounds 4 to 6) showed no Google TV home screen: Android
  never started it, and no recorded frame was classified as it (home-takeover#AC-7).
- With the final build, every Home press reached the service (the service now logs every key). The
  quiet home screen was drawn 0.06 to 0.7 s after it, except once: 1.5 s after a Home press from
  Netflix with Google's sign-in dialog up (round 6), over the 1-second budget (REQ-home-10,
  home-takeover#AC-8). Likely cause: each time it is shown the screen re-reads every app and loads
  all banners on its main thread, while the TV is short of memory. Fixed by loading each app's artwork once
  and drawing the list the screen already has before reading the apps again (home-takeover#AC-12):
  afterwards, 6 Home presses from Netflix, 3 with Google's sign-in dialog up, drew in 52 to 84 ms.
- The remote's Netflix (`KEYCODE_BUTTON_4`, 191) and YouTube (`KEYCODE_BUTTON_3`) buttons and its voice
  button pass through the service untouched; voice search opened Google's page for the show each
  time.
- Back from an app opened from the quiet home screen (CBC News) returned straight to it with that
  app selected; Google TV's home screen was not involved. The Vimeo app ignores Back: it shows its
  own "we ended support for our TV apps" dialog.
- Google's page for a show (`launcherx/.entity.EntityActivity`) is never covered.
- Restart (AC-10): after `adb reboot`, Android showed its own start-up home screen
  (`com.android.tv.settings/.system.FallbackHome`), then Google TV's home screen at 22:14:14.5, then
  the quiet home screen at 22:14:18.2, 3.7 s later. The service's start-up log had rotated out, so
  whether the cover hid those 3.7 s is not known from the logs. The project lead, watching, did not
  notice Google TV's home screen. Next time the TV restarts anyway, capture `adb logcat` from boot.
- After the restart, wireless debugging was off (Android does not keep it on across a restart on this
  build), and the address's port changed (39423 to 34447). Turning it back on needs Settings.
- With this build there was no way into Settings with the remote (the **Settings** button is slice 3,
  and Google TV's own settings are covered); the project lead got there with the voice button
  ("open settings"). Until slice 3 ships, a test build must not be left on the TV across a restart.

## Slice 3 on the TV: the top row and search (2026-11-09, late evening)

Driven over ADB under the standing go-ahead (`docs/specs/top-row-and-search.md`, AC-13):

- `adb shell input text` types into **Search for a show** through the TV's keyboard, and
  `KEYCODE_ENTER` submits it.
- The quiet home screen was paused by Google's search screen about 10 ms after **Search**; Google's
  page for the show (`launcherx/.entity.EntityActivity`) came about 5 s later. In between, the quiet
  home screen is briefly resumed again under Google's own dimmed overlay with the typed text.
- Google's voice search is a window over the current screen, not an activity: the quiet home screen
  stays the resumed activity underneath.
- The Settings app keeps its task: opening it again returns to wherever it was left, so the quiet
  home screen opens it with `FLAG_ACTIVITY_CLEAR_TASK`.

