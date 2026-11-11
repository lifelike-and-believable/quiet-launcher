@echo off
rem Remove the quiet home screen (docs/user/setup.md): double-click this in the Quiet Launcher folder.
rem Runs quiet-undo.ps1 with the execution policy bypassed for this run only, passes on any options
rem (-Tv, -Pair), and waits for a key at the end so the result can be read.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quiet-undo.ps1" %*
set "code=%errorlevel%"
echo.
pause
exit /b %code%
