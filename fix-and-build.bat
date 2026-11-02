@echo off
cd /d "%~dp0"
echo Stopping old Gradle processes...
call gradlew.bat --stop
echo Clearing build caches (safe, they rebuild themselves)...
if exist .gradle rmdir /s /q .gradle
if exist build rmdir /s /q build
for %%m in (wcore gbsp ps2 sega arcade nintendo) do (
  if exist %%m\.gradle rmdir /s /q %%m\.gradle
  if exist %%m\build rmdir /s /q %%m\build
)
if not exist libs mkdir libs
if not exist "libs\geckolib-fabric-26.3-5.5.7.jar" copy /y "C:\Users\dustin\curseforge\minecraft\Instances\working console\mods\geckolib-fabric-26.3-5.5.7.jar" libs\
echo Building (log: build-log.txt) ...
call gradlew.bat build --no-daemon --stacktrace > build-log.txt 2>&1
set RC=%errorlevel%
>> build-log.txt echo ==== GECKOLIB CHECK ====
>> build-log.txt dir /b "C:\Users\dustin\Documents\Default Project\*.jar"
>> build-log.txt dir /b "C:\Users\dustin\curseforge\minecraft\Instances\working console\mods\geckolib*"
>> build-log.txt echo BUILD EXIT %RC%
echo Done. You can close this window.
