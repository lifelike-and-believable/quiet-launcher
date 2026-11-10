# Spike: taking over Home with an accessibility service

Throwaway code, not the product. Question: can an app take over the Home button on the reference
device while Google TV's search results (its `.entity.EntityActivity`) still open? This settles
the `[assumed]` in REQ-setup-2. Results go in `docs/product/notes.md`.

- `MainActivity` stands in for the quiet home screen: a label and a button that sends
  `GLOBAL_SEARCH` with `query=Severance` to Google's search.
- `HomeWatcher` is an accessibility service. It logs every window change and key (logcat tag
  `QLSPIKE`). In mode `window` (default) it brings `MainActivity` forward only when Google TV's
  `home.HomeActivity` comes up. In mode `key` it consumes the Home key instead. Set the mode with
  `adb shell am start -n quiet.launcher.spike/.MainActivity --es mode key`.

## Build

Needs the JDK 17 and Android SDK (platform 34, build-tools 34.0.0) under
`%LOCALAPPDATA%\AndroidBuild` (`jdk\`, `sdk\`), installed per user on 2026-11-09. Then, from Git
Bash: `bash build.sh`. No Gradle.

## Run (each step changes the household's TV: ask the project lead first)

```
adb install build/quiet-spike.apk
adb shell settings get secure enabled_accessibility_services     # note it, to restore
adb shell settings put secure enabled_accessibility_services quiet.launcher.spike/.HomeWatcher
adb shell settings put secure accessibility_enabled 1
adb logcat -s QLSPIKE
```

Undo: put back the noted `enabled_accessibility_services` value (and `accessibility_enabled`),
then `adb uninstall quiet.launcher.spike`.
