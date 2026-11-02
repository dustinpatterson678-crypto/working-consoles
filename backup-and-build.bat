@echo off
cd /d "%~dp0"
for /f %%i in ('powershell -NoProfile -Command "Get-Date -Format yyyyMMdd-HHmm"') do set STAMP=%%i
echo Backing up to C:\New folder\wc-backups\working-consoles-remake-%STAMP% ...
robocopy "%~dp0." "C:\New folder\wc-backups\working-consoles-remake-%STAMP%" /E /XD build .gradle /NFL /NDL /NJH /NJS /R:1 /W:1
echo Building (log: build-log.txt) ...
call gradlew.bat build > build-log.txt 2>&1
echo BUILD EXIT %errorlevel%>> build-log.txt
echo Done. You can close this window.
