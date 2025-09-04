@echo off
echo 🚀 Starting NewIPTV Build and Deploy Process...
echo ================================================

REM Step 1: Build the project
echo 📦 Step 1: Building project...
call gradlew assembleDebug
if %ERRORLEVEL% neq 0 (
    echo ❌ Build failed! Stopping deployment.
    pause
    exit /b 1
)
echo ✅ Build completed successfully!
timeout /t 3 /nobreak >nul

REM Step 2: Check if APK exists
echo 🔍 Step 2: Checking APK file...
set "apkPath=app\build\outputs\apk\debug\app-debug.apk"
if not exist "%apkPath%" (
    echo ❌ APK file not found at: %apkPath%
    pause
    exit /b 1
)
echo ✅ APK found: %apkPath%

REM Step 3: Check ADB connection
echo 🔌 Step 3: Checking ADB connection...
set "adbPath=%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe"
if not exist "%adbPath%" (
    echo ❌ ADB not found at: %adbPath%
    pause
    exit /b 1
)
echo ✅ ADB found at: %adbPath%

REM Step 4: Kill and restart ADB server
echo 🔄 Step 4: Restarting ADB server...
"%adbPath%" kill-server
timeout /t 2 /nobreak >nul
"%adbPath%" start-server
timeout /t 2 /nobreak >nul

REM Step 5: Connect to device
echo 📱 Step 5: Connecting to device...
set "deviceIP=emulator-5554"
set "deviceIP=192.168.8.20:5555"
"%adbPath%" connect %deviceIP%
timeout /t 3 /nobreak >nul

REM Step 6: Check device connection
echo 🔍 Step 6: Checking device connection...
"%adbPath%" devices | findstr "%deviceIP%" >nul
if %ERRORLEVEL% neq 0 (
    echo ❌ Device not connected: %deviceIP%
    echo 💡 Try connecting manually or check device IP address
    pause
    exit /b 1
)
echo ✅ Device connected: %deviceIP%

REM Step 7: Install APK
echo 📥 Step 7: Installing APK...
"%adbPath%" -s %deviceIP% install -r "%apkPath%"
if %ERRORLEVEL% neq 0 (
    echo ❌ APK installation failed!
    pause
    exit /b 1
)
echo ✅ APK installed successfully!
timeout /t 3 /nobreak >nul

REM Step 8: Launch app
echo 🚀 Step 8: Launching app...
"%adbPath%" -s %deviceIP% shell am start -n com.example.newiptv/.MainActivity
if %ERRORLEVEL% neq 0 (
    echo ❌ App launch failed!
    pause
    exit /b 1
)
echo ✅ App launched successfully!

REM Final status
echo ================================================
echo 🎉 NewIPTV Build and Deploy Completed Successfully!
echo ================================================
echo.
echo 📱 App is now running on device: %deviceIP%
echo 🔍 Check logs with: "%adbPath%" -s %deviceIP% logcat
echo.
pause
