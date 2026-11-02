@echo off
echo ============================================
echo Installing Working Consoles Remake Mods
echo ============================================
echo.
echo Copying jars to your Minecraft mods folder...
echo.

set MODS_DIR=%APPDATA%\.minecraft\mods

if not exist "%MODS_DIR%" (
    echo Creating mods folder: %MODS_DIR%
    mkdir "%MODS_DIR%"
)

echo Installing from: C:\New folder\working-consoles-remake\build\dev-jars
echo To: %MODS_DIR%
echo.

copy /Y "C:\New folder\working-consoles-remake\build\dev-jars\*.jar" "%MODS_DIR%" > nul 2>&1

if errorlevel 1 (
    echo ERROR copying files! Please copy manually.
    pause
    exit /b 1
)

echo.
echo ============================================
echo INSTALLATION COMPLETE!
echo ============================================
echo.
echo Your mods are now in: %MODS_DIR%
echo.
echo Files installed:
dir "%MODS_DIR%\*.jar" /b | findstr "working-consoles OR dustins-arcade OR wcore"
echo.
echo Please close Minecraft completely before launching!
echo Then launch and enjoy your working consoles!
echo.
pause
