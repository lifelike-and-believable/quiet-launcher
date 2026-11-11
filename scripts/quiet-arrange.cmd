@echo off
rem Arrange apps (docs/user/arranging.md): double-click to arrange the TV's apps in Notepad, or drop
rem a saved arrangement file on this to send it. Runs quiet-arrange.ps1 with the execution policy
rem bypassed for this run only, and waits for a key at the end so the result can be read.
if "%~1"=="" (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quiet-arrange.ps1"
) else (
    powershell -NoProfile -ExecutionPolicy Bypass -File "%~dp0quiet-arrange.ps1" -File "%~1"
)
echo.
pause
