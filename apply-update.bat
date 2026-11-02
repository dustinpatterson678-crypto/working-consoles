@echo off
rem Unpacks any update zips into the project. build-mods.bat and build-release.bat now do this by themselves.
cd /d "%~dp0"
call :unpack
echo Done.
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
