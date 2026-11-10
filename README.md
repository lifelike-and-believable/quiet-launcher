# Quiet Launcher

A home screen for Google TV that shows your apps without suggestions or advertising, and keeps
search. It is built for one household and is not published anywhere. The intent is in
`docs/product/concept.md`; how it works for the household is in `docs/user/`.

## Building it

`build.ps1` builds the app and writes the Quiet Launcher folder to `out\QuietLauncher` (the app's
install file, `quiet-launcher.apk`, and, once they exist, the setup and undo scripts). Setting it up
on the TV is described in `docs/user/setup.md`.

You need, on Windows:

- a JDK 17 (for example Microsoft OpenJDK 17);
- the Android SDK command-line tools, with `platforms;android-34` and `build-tools;34.0.0`.

Point `JAVA_HOME` and `ANDROID_HOME` at them, or unpack them as `%LOCALAPPDATA%\AndroidBuild\jdk`
and `%LOCALAPPDATA%\AndroidBuild\sdk`, which `build.ps1` uses when the variables are not set. Then:

```
powershell -ExecutionPolicy Bypass -File .\build.ps1
```

The first build downloads Gradle and the Android libraries (about 1 GB). The tests run with
`.\gradlew.bat :app:testDebugUnitTest`.
