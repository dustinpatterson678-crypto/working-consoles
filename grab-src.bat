@echo off
rem Packs the source folders Claude needs to read into debug-gc\src.zip. Safe: only copies, changes/deletes nothing.
cd /d "%~dp0"
if not exist debug-gc mkdir debug-gc
for %%m in (wcore gamecube ps2 nintendo gbsp stream) do (
  if exist "%%m\src" xcopy /e /i /y "%%m\src" "debug-gc\pack\%%m" >nul
)
tar -a -cf "debug-gc\src.zip" -C "debug-gc\pack" .
echo.
echo Done - tell Claude "grabbed".
pause
