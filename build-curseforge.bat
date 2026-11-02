@echo off
rem Builds the single jar to upload to CurseForge: release\curseforge\working-consoles-and-arcades-<version>.jar
cd /d "%~dp0"
call gradlew.bat curseforgeJar > build-log.txt 2>&1
if errorlevel 1 (echo Build FAILED - see build-log.txt) else (echo Done. Upload the jar in release\curseforge)
explorer release\curseforge
pause
