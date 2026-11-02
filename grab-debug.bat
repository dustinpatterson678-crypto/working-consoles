@echo off
rem Copies the TV browser source + the newest Minecraft log into the "debug" folder so Claude can read them. Safe: only copies, changes nothing.
cd /d "%~dp0"
if not exist debug mkdir debug
xcopy /e /i /y "stream\src\main\java\com\dustin\stream\client" "debug\client" >nul
xcopy /e /i /y "stream\src\main\java\com\dustin\stream\mixin" "debug\mixin" >nul
powershell -NoProfile -Command "$roots=@('C:\Users\dustin\curseforge\minecraft\Instances',$env:APPDATA+'\.minecraft\logs'); $f=Get-ChildItem -Path $roots -Recurse -Filter latest.log -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if($f){Copy-Item $f.FullName 'debug\latest.log' -Force; Set-Content 'debug\log-source.txt' $f.FullName} else {Set-Content 'debug\log-source.txt' 'no latest.log found'}"
echo.
echo Done - tell Claude "grabbed".
pause
