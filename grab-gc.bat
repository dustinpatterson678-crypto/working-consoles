@echo off
rem Copies the GameCube mod source + file lists + newest Minecraft log into "debug-gc" so Claude can read them. Safe: only copies, changes nothing.
cd /d "%~dp0"
if not exist debug-gc mkdir debug-gc
xcopy /e /i /y "gamecube\src\main" "debug-gc\gc" >nul
dir /s /b "wcore\src\main\java" > "debug-gc\wcore-java-files.txt" 2>nul
dir /s /b "nintendo\src\main" > "debug-gc\nintendo-files.txt" 2>nul
dir /s /b "libs" > "debug-gc\libs-files.txt" 2>nul
dir /s /b "tools" > "debug-gc\tools-files.txt" 2>nul
dir /s /b "gamecube\build\libs" > "debug-gc\gc-jars.txt" 2>nul
powershell -NoProfile -Command "$roots=@('C:\Users\dustin\curseforge\minecraft\Instances',$env:APPDATA+'\.minecraft\logs'); $f=Get-ChildItem -Path $roots -Recurse -Filter latest.log -ErrorAction SilentlyContinue | Sort-Object LastWriteTime -Descending | Select-Object -First 1; if($f){Copy-Item $f.FullName 'debug-gc\latest.log' -Force; Set-Content 'debug-gc\log-source.txt' $f.FullName; $inst=Split-Path (Split-Path $f.FullName); Get-ChildItem -Path (Join-Path $inst 'gamecube') -Recurse -ErrorAction SilentlyContinue | Select-Object FullName,Length | Out-String | Set-Content 'debug-gc\instance-gamecube-folder.txt'} else {Set-Content 'debug-gc\log-source.txt' 'no latest.log found'}"
echo.
echo Done - tell Claude "grabbed".
pause
