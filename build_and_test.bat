@echo off
echo Building IPTV TV App...
echo.

REM Check if Java is available
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo Please install Java 17 or later
    pause
    exit /b 1
)

REM Clean and build the project
echo Cleaning project...
call gradlew clean

echo Building debug APK...
call gradlew assembleDebug

if %errorlevel% equ 0 (
    echo.
    echo Build successful! APK location:
    echo app\build\outputs\apk\debug\app-debug.apk
    echo.
    echo To install on connected device:
    echo adb install app\build\outputs\apk\debug\app-debug.apk
    echo.
) else (
    echo.
    echo Build failed! Check the error messages above.
    echo.
)

pause
