# Quick Deploy and Test Script for AC3 Audio Fix
Write-Host "🚀 Deploying NewIPTV with AC3 Audio Improvements..." -ForegroundColor Green

# Check for connected devices
$devices = adb devices
if ($devices -notmatch "device") {
    Write-Host "❌ No devices connected. Please connect your device and try again." -ForegroundColor Red
    exit 1
}

Write-Host "📱 Installing updated APK..." -ForegroundColor Cyan
adb install -r app\build\outputs\apk\debug\app-debug.apk

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation successful!" -ForegroundColor Green
    
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell monkey -p com.example.newiptv -c android.intent.category.LAUNCHER 1
    
    Write-Host "🎉 App launched! Now test AC3/DTS videos and check the logs:" -ForegroundColor Green
    Write-Host "📋 To monitor logs, run: adb logcat | findstr 'IPTVVideoPlayer'" -ForegroundColor Yellow
    Write-Host "🔍 Look for 'Supported Audio Codecs' and 'Codec Support Status' in the logs" -ForegroundColor Yellow
} else {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
}
