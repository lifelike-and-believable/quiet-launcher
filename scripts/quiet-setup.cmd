@echo off
rem Set up the quiet home screen (docs/user/setup.md): double-click this in the Quiet Launcher
rem folder. Runs quiet-setup.ps1 with the quiet-launcher.apk beside it, with the execution policy
rem bypassed for this run only, passes on any options (-Tv, -Pair), and waits for a key at the end
rem so the result can be read.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quiet-setup.ps1" -Apk "%~dp0quiet-launcher.apk" %*
set "code=%errorlevel%"
echo.
pause
exit /b %code%
