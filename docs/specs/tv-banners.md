---
status: draft
tests: [app/src/test, scripts/tests, tests/chromebook]
---

# The TV's banners on the Chromebook's tiles

<!-- docs/specs/tv-banners.md. Chromebook delivery, slice 9 (PLAN.md): the third of the project
lead's choices of 2026-11-17 for bigger artwork ("Let's do all 3"; "go for it" to this plan): the
TV app gives the PC each app's banner, a PC script saves them in one file, and the Chromebook
launcher takes its tiles' pictures from that file. From design-chromebook#editing,
docs-chromebook#arrange-your-services, and design-arranging#how-it-works (the receiver). -->

## Problem

The TV's tiles show each app's banner, with the logo and the name's lettering together; sites
publish nothing like it (chromebook-pictures). The banners are on the TV, inside each app, and only
something on the TV can draw them. The TV app already answers the PC over ADB, behind a permission
only ADB's shell has (arrange-receiver#AC-1), so it can answer one more question: "what is this
app's banner?". The PC then has every banner in one file, which the project lead copies to the
Chromebook and imports, instead of choosing a picture for each service by hand.

## User stories

- story-chromebook-open-service: each service under its own name, in the TV's look.
- story-chromebook-services: choose my services, and arrange them.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | Asked for an app's banner (`quiet.launcher.action.GET_BANNER`, `--es package <package>`), the TV app answers **done** with the app's name, its package, and its banner drawn as a PNG, scaled down (never up) to fit 640 by 360 px, keeping its shape. | test: BannerRequestsTest |
| AC-2 | Asked for the banner of a package that is not an app on the home screen, or of an app with no banner, it answers **refused** with the reason, and draws nothing. | test: BannerRequestsTest |
| AC-3 | The question goes to the same receiver as the arrangement, so only ADB's shell can ask it (arrange-receiver#AC-1). | test: BannerRequestsTest |
| AC-4 | `quiet-arrange.ps1 -Banners` (and its launcher, **quiet-banners.cmd**) connects to the TV as arranging does, asks for the banner of every app the TV lists (shown and hidden), and saves them in `tv-banners.json` in the QuietLauncher folder: `{"from": "Quiet Launcher TV", "apps": [{"name", "package", "picture"}]}`, each picture a `data:image/png;base64,` address. It says how many apps' banners were saved and where, names the apps with none, and says what to do next. It changes nothing on the TV. | test: test_banners.py |
| AC-5 | If the TV's quiet launcher is older and does not answer the question, the script saves nothing and says to update it with quiet-setup. If the quiet home screen is not set up, it says so, as arranging does. | test: test_banners.py |
| AC-6 | While editing, the launcher shows an **Import TV pictures** tile after **Add a service**. It opens ChromeOS's file picker for the file. | test: tv-banners-extension.test.js |
| AC-7 | Each of the viewer's services, hidden ones too, is matched to at most one of the file's apps: first by name, ignoring case, spaces, punctuation, a leading "The", and a trailing "+" (Apple TV+ is Apple TV); failing that, by the service's site name being one part of exactly one app's package (youtube.com and `com.google.android.youtube.tv`). A matched service's picture is set to that app's banner (scaled to fit 640 by 360 px, as a chosen picture is), replacing any it had. Unmatched services are left as they were. | test: tv-banners.test.js, tv-banners-extension.test.js |
| AC-8 | The status line then says **Pictures set for <n> of <total> services.**, and, when some did not match, **No TV banner for <names>.** | test: tv-banners.test.js, tv-banners-extension.test.js |
| AC-9 | A file that is not a TV banners file (not JSON, or without its `apps`, or with no picture in it) changes nothing, and the status line says **That file isn't a TV pictures file. Make one with quiet-banners on the PC.** | test: tv-banners.test.js, tv-banners-extension.test.js |
| AC-10 | On the reference device, as a test install (asked first: it changes the household's TV): `quiet-banners.cmd` saves the banners; on the project lead's Chromebook, **Import TV pictures** with that file gives the services the TV's banners. The TV is then left with the updated app only if the project lead says so. | command: the project lead, on the PC and the Chromebook |

A test cites a criterion as `tv-banners#AC-1` in its name or a comment.

## Scope

- In: `BannerRequests` and its action in `ArrangeReceiver` and the manifest; `quiet-arrange.ps1`'s
  `-Banners`, `quiet-banners.cmd`, and the fake ADB's answer; `chromebook/lib/tv-banners.js` and the
  launcher's **Import TV pictures**; their tests; the amendments to design-chromebook,
  docs-chromebook, and docs-arranging that describe them.
- Out: sending pictures to the Chromebook directly (the file is copied by the project lead, with
  Google Drive or a USB stick); banners for apps the home screen leaves out.

## APIs and assumptions

- Called as `adb shell am broadcast -n quiet.launcher/.ArrangeReceiver -a
  quiet.launcher.action.GET_BANNER --es package <package>`. Result codes as arrange-receiver's: 1
  done, 3 refused. Data is base64 of JSON: `{"name", "package", "png": "<base64>"}` or
  `{"error": "<reason>"}`. A TV app from before this slice ignores the action, and `am broadcast`
  prints `result=0` with no data.
- The banner is the one the home screen shows (`ActivityInfo.loadBanner`, which falls back to the
  application's banner), drawn into a bitmap at its own size, or 640 by 360 if it has none (a
  colour). At 640 by 360 a banner's PNG is tens of kilobytes; twice base64-encoded it stays well
  under what a broadcast's result can carry.
- One question per app, so a large answer never carries more than one banner.
- The Robolectric tests draw with its native graphics, so the PNG is real and can be decoded.
- The file is read on the Chromebook with a hidden `<input type="file" accept=".json,application/json">`,
  as **Choose picture** reads an image.

## Delivery

- One pull request: this spec, a tests-first commit with failing tests against stubs, then the
  implementation and the documents' amendments. AC-10 after the project lead agrees to the test
  install on the TV.
