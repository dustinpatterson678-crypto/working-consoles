@echo off
rem DEV build: builds the same 3 jars (versions end in +DEV) and installs them into your game.
rem Every older copy of the mods is removed from the mods folder first. Close Minecraft before running this.
cd /d "%~dp0"
call :unpack
echo Building and installing the 3 dev jars (log: build-log.txt) ...
call gradlew.bat clean build > build-log.txt 2>&1
set RC=%errorlevel%
>> build-log.txt echo BUILD EXIT %RC%
if not "%RC%"=="0" (
  echo.
  echo Build FAILED - see build-log.txt. Is Minecraft still open? Close it and run this again.
  pause
  exit /b %RC%
)
echo.
echo Installed in your game:
dir /b build\dev-jars
pause
exit /b 0

:unpack
for %%z in (*.zip) do (
  echo Unpacking %%z ...
  powershell -NoProfile -Command "Expand-Archive -Force '%%z' ."
  del "%%z"
)
for /d %%d in (update-*) do (
  for %%z in ("%%d\*.zip") do (
    echo Unpacking %%z ...
    if exist "%%~nz\src" rmdir /s /q "%%~nz\src"
    powershell -NoProfile -Command "Expand-Archive -Force '%%z' ."
    del "%%z"
  )
  rmdir "%%d" 2>nul
)
exit /b 0
