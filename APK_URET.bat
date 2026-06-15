@echo off
cd /d "%~dp0"
echo MedyaPress APK uretimi basliyor...
gradle assembleDebug
pause
