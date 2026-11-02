@echo off
rem RELEASE build: clean versions (no +DEV), developer bypass OFF.
rem Makes exactly 3 jars in the "release" folder (upload those to CurseForge) AND installs the same 3 into your game.
rem Every older copy of the mods is removed from the mods folder first. Close Minecraft before running this.
cd /d "%~dp0"
call :unpack
echo Building and installing the 3 release jars (log: release-log.txt) ...
call gradlew.bat clean deployToInstance "-Prelease=true" > release-log.txt 2>&1
set RC=%errorlevel%
>> release-log.txt echo BUILD EXIT %RC%
if not "%RC%"=="0" (
  echo.
  echo Release build FAILED - see release-log.txt. Is Minecraft still open? Close it and run this again.
  pause
  exit /b %RC%
)
echo.
echo Installed in your game, and ready to upload to CurseForge (release folder):
dir /b release
echo.
echo Players also need: Fabric API and GeckoLib 5.5.7 (Mod Menu optional).
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
