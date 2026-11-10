---
status: draft
tests: [app/src/test]
# tests-commit: <sha of the commit that holds the failing tests, once written>
---

# The app list

<!-- docs/specs/app-list.md. Delivery slice 1 (PLAN.md, step 10): the project skeleton and the quiet
home screen's grid of apps. Taking over Home is slice 2; the top row and search are slice 3; the
setup scripts are slice 4. -->

## Problem

The household viewer needs a home screen that shows the TV's apps, looking as they do elsewhere,
and nothing else (story-quiet-home-apps, docs-home-screen#open-an-app). The set-up person needs new
apps to appear and removed ones to go without touching anything
(story-quiet-home-new-and-removed-apps). This slice builds the Android app with that grid, and the
build that produces the Quiet Launcher folder (docs-setup#what-you-need).

## User stories

- story-quiet-home-apps: as a household viewer, I want to see our apps, looking the way I know them,
  and open one with select.
- story-quiet-home-new-and-removed-apps: as the set-up person, I want an app I install to show up on
  its own and one I uninstall to go away.

## Acceptance criteria

| # | Criterion | Verified by |
|---|---|---|
| AC-1 | The list holds the apps Android reports for `MAIN` + `LEANBACK_LAUNCHER`, except this app, any app that has a `HOME` activity, `com.android.tv.settings`, `com.google.android.play.games`, and `com.google.android.videos`; `com.android.vending` (the Play Store) is kept (design-home-screen, How it works). | test: AppCatalogTest |
| AC-2 | Order: first-install time, oldest first; equal times by label, ignoring case; the Play Store always last (design-home-screen, Layout). | test: AppCatalogTest |
| AC-3 | The package reader turns Android's package data into entries: label, first-install time, whether the app has a `HOME` activity, and its artwork (the banner if the app or activity has one, otherwise the icon). | test: PackageSourceTest |
| AC-4 | Each tile shows the app's artwork and its label, the label on at most two lines and cut with an ellipsis beyond that; the grid has five columns. | test: HomeActivityTest |
| AC-5 | Selecting a tile starts that app's TV launch activity (`MAIN` + `LEANBACK_LAUNCHER`, the activity the list came from) in a new task. | test: HomeActivityTest |
| AC-6 | When the screen opens, the first app is selected and the grid is at the top. (Home resetting the selection is slice 2.) | test: HomeActivityTest |
| AC-7 | When Android reports a package added or removed, the grid is rebuilt from the package data, so a new app appears in its install-order place (the end, before the Play Store) and a removed one disappears; the grid is also rebuilt each time the screen is shown again. A rebuild keeps the selection on the same app, or the one now in its place if it was removed (design-home-screen, States and errors). | test: HomeActivityTest |
| AC-8 | With no apps to show, the screen says **No apps to show. Apps installed on this TV appear here.** | test: HomeActivityTest |
| AC-9 | The built app requests no internet permission (design-home-screen, "Never shows promotion"). | test: PermissionsTest |
| AC-10 | `.\build.ps1` builds the app and writes `out\QuietLauncher\quiet-launcher.apk`. | command: `powershell -ExecutionPolicy Bypass -File .\build.ps1` then `Test-Path out\QuietLauncher\quiet-launcher.apk` |
| AC-11 | On the reference device, the grid shows the 19 expected tiles in the expected order (design-home-screen, mock-up "Default"), each with its own banner or icon and its name. | command: install the debug build on the TV (standing go-ahead), launch it, screenshot, compare with `docs/design/home-screen/default.png`, then uninstall |

A test cites a criterion as `app-list#AC-1` in its name or a comment. Only `test:` criteria need a
citing test.

## Scope

- In: the Gradle project (`settings.gradle.kts`, `build.gradle.kts`, `app/`), the Gradle wrapper,
  `build.ps1`, the `HomeActivity` with its grid, `AppCatalog`, `PackageSource`, the tile adapter, and
  their tests.
- Out: the top row, status line, and search (slice 3); taking over Home, Back doing nothing, and the
  accessibility service (slice 2); the setup and undo scripts (slice 4, though `build.ps1` will copy
  them into the folder once they exist); spatial movement rules beyond what the grid gives by default
  (slice 3); TalkBack labels and text scaling (slice 3).

## APIs and assumptions

- APIs used: `PackageManager.queryIntentActivities` (with `<queries>` entries in the manifest for
  `LEANBACK_LAUNCHER` and `HOME`, needed for package visibility on Android 11+),
  `PackageInfo.firstInstallTime`, `ActivityInfo.loadBanner` / `ApplicationInfo.loadBanner` and
  `loadIcon`, `PackageManager.getLeanbackLaunchIntentForPackage`, and the
  `ACTION_PACKAGE_ADDED` / `ACTION_PACKAGE_REMOVED` broadcasts (registered at run time). Checked
  against the Android 14 (API 34) SDK installed in `%LOCALAPPDATA%\AndroidBuild`.
- Toolchain: Gradle 8.10.2 (wrapper), Android Gradle Plugin 8.7, Kotlin 2.0, JDK 17, compile and
  target SDK 34, minimum SDK 26 (Google TV devices run Android 10 or later). Tests: JUnit 4 and
  Robolectric on the JVM.
- Assumption: preinstalled system apps share the factory install time (seen on the reference device:
  2008-12-31 for the four system apps), so ties are real and the alphabetical rule matters.
- Assumption: Robolectric's package manager reports the merged manifest's requested permissions, so
  AC-9 is checked against what is actually built.

## Non-functional requirements

- No internet permission, no network access, no analytics (concept, Non-goals).
- The grid draws from data already on the TV; it must be up within the 1-second Home budget
  (REQ-home-10), measured in slice 2 when Home is taken over.

## Delivery

- Plan item: `PLAN.md`, step 10, slice 1.
- Pull requests: one, with the failing tests in their own commit (`tests-commit`) before the
  implementation.

## Risks and decisions

- Gradle and Kotlin chosen by the project lead (2026-11-09) over a Gradle-free build.
- The Gradle wrapper and its jar are committed so `build.ps1` needs only a JDK and the Android SDK.
