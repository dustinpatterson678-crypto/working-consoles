@echo off
cd /d "%~dp0"
echo Removing the old Stream Box test files...
del /q "stream\src\main\java\com\dustin\stream\StreamBoxBlock.java" "stream\src\main\java\com\dustin\stream\StreamBoxBlockEntity.java" "stream\src\main\java\com\dustin\stream\client\WindowCapture.java" 2>nul
del /q "stream\src\main\resources\assets\stream\blockstates\stream_box.json" "stream\src\main\resources\assets\stream\items\stream_box.json" "stream\src\main\resources\assets\stream\models\block\stream_box.json" "stream\src\main\resources\assets\stream\models\item\stream_box.json" 2>nul
del /q "stream\src\main\resources\assets\stream\textures\block\stream_box_*.png" "stream\src\main\resources\data\stream\loot_table\blocks\stream_box.json" "stream\src\main\resources\data\stream\recipe\stream_box.json" "stream\src\main\resources\data\wcore\tags\item\consoles.json" 2>nul
echo Building 6.0.0 (log: build-log.txt) ...
call gradlew.bat build > build-log.txt 2>&1
set RC=%errorlevel%
>> build-log.txt echo BUILD EXIT %RC%
echo Done. You can close this window.
