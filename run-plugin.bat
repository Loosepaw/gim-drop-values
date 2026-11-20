@echo off
setlocal
cd /d "%~dp0"
call gradlew.bat run
if errorlevel 1 (
  echo.
  echo The client could not start. Read the error above and SETUP.md.
  pause
)
endlocal
