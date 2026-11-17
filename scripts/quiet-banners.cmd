@echo off
rem TV banners for the Chromebook (docs/user/chromebook.md): double-click to save the banner of every
rem app on the TV in tv-banners.json, for the Chromebook launcher's Import TV pictures. Runs
rem quiet-arrange.ps1 -Banners with the execution policy bypassed for this run only, and waits for a
rem key at the end so the result can be read.
powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quiet-arrange.ps1" -Banners
echo.
pause
