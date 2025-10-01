# Build and Deploy Script for NewIPTV
# This script builds the app and installs it on the connected device

Write-Host "🚀 Starting NewIPTV Build and Deploy Process..." -ForegroundColor Green

# Configuration
$device = "emulator-5554"  # Change this to your device ID if needed
$adb = "C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools\adb.exe"

# Check if ADB is available
if (-not (Test-Path $adb)) {
    Write-Host "❌ ADB not found at: $adb" -ForegroundColor Red
    Write-Host "Please check your Android SDK installation" -ForegroundColor Yellow
    exit 1
}

# Check device connection
Write-Host "📱 Checking device connection..." -ForegroundColor Cyan
$devices = & $adb devices
if ($devices -notmatch $device) {
    Write-Host "❌ Device $device not found or not connected" -ForegroundColor Red
    Write-Host "Available devices:" -ForegroundColor Yellow
    & $adb devices
    exit 1
}

Write-Host "✅ Device $device is connected" -ForegroundColor Green

# Clean and build
Write-Host "🔨 Building the app..." -ForegroundColor Cyan
./gradlew clean assembleDebug

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Build failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Build successful!" -ForegroundColor Green

# Install the APK
Write-Host "📦 Installing APK on device..." -ForegroundColor Cyan
$apkPath = "app\build\outputs\apk\debug\app-debug.apk"
& $adb -s $device install -r $apkPath

if ($LASTEXITCODE -ne 0) {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
    exit 1
}

Write-Host "✅ Installation successful!" -ForegroundColor Green

# Launch the app
Write-Host "🚀 Launching NewIPTV..." -ForegroundColor Cyan
& $adb -s $device shell monkey -p com.example.newiptv -c android.intent.category.LAUNCHER 1

Write-Host "🎉 NewIPTV is now running on your device!" -ForegroundColor Green
Write-Host "📱 Check your device to see the app" -ForegroundColor Yellow
