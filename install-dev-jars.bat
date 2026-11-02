@echo off
rem Installs the 3 dev jars into your Minecraft mods folder.
rem Make sure Minecraft is CLOSED before running this!

cd /d "%~dp0"

echo =========================================
echo Installing Working Consoles Remake to Minecraft
echo =========================================
echo.

if not exist "build\dev-jars" (
    echo ERROR: No dev jars found in build/dev-jars/
    echo Running gradlew releaseJars first...
    .\gradlew.bat cleanRelease releaseJars --console=plain > install-log.txt 2>&1
)

echo.
echo Files to install:
dir /b build\dev-jars
echo.

set /p GAME_DIR="Enter your Minecraft game directory (default: %APPDATA%\.minecraft): "
if "%GAME_DIR%"=="" set GAME_DIR=%APPDATA%\.minecraft

echo Installing to: %GAME_DIR%\mods
echo.

rmdir /s /q "%GAME_DIR%\mods\working-consoles" 2>nul
rmdir /s /q "%GAME_DIR%\mods\dustins-arcade" 2>nul

mkdir "%GAME_DIR%\mods" 2>nul

copy build\dev-jars\*.jar "%GAME_DIR%\mods\" > nul

echo.
echo =========================================
echo INSTALLATION COMPLETE!
echo =========================================
echo Mods installed to: %GAME_DIR%\mods
echo.
echo The following jars are now in your mods folder:
dir /b "%GAME_DIR%\mods"
echo.
pause
exit /b 0
