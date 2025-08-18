@echo off
echo ========================================
echo IPTV Android TV App - Build Script
echo ========================================
echo.

REM Check if Java is installed
echo Checking Java installation...
java -version >nul 2>&1
if %errorlevel% neq 0 (
    echo ERROR: Java is not installed or not in PATH
    echo.
    echo Please install Java 17 JDK from: https://adoptium.net/
    echo Or use Android Studio Online for easier setup
    echo.
    pause
    exit /b 1
)

REM Check Java version
for /f "tokens=3" %%g in ('java -version 2^>^&1 ^| findstr /i "version"') do (
    set JAVA_VERSION=%%g
)
echo Found Java version: %JAVA_VERSION%

REM Check if it's Java 17 or higher
echo %JAVA_VERSION% | findstr /r "17\|18\|19\|20\|21" >nul
if %errorlevel% neq 0 (
    echo WARNING: Java 17 or higher is recommended
    echo Current version: %JAVA_VERSION%
    echo.
)

echo.
echo ========================================
echo Starting Gradle Build...
echo ========================================
echo.

REM Clean previous builds
echo Cleaning previous builds...
call gradlew clean
if %errorlevel% neq 0 (
    echo ERROR: Gradle clean failed
    pause
    exit /b 1
)

REM Build the project
echo Building the project...
call gradlew build
if %errorlevel% neq 0 (
    echo ERROR: Build failed
    echo.
    echo Troubleshooting tips:
    echo 1. Make sure you have Java 17+ installed
    echo 2. Check your internet connection
    echo 3. Try: gradlew clean build
    echo 4. Use Android Studio Online for easier setup
    echo.
    pause
    exit /b 1
)

echo.
echo ========================================
echo Build Successful!
echo ========================================
echo.
echo APK location: app\build\outputs\apk\debug\app-debug.apk
echo.
echo To install on Android TV:
echo 1. Enable Developer Options on your Android TV
echo 2. Enable USB Debugging
echo 3. Connect via ADB: adb install app\build\outputs\apk\debug\app-debug.apk
echo.
echo Or use Android Studio to run directly on device
echo.

pause
