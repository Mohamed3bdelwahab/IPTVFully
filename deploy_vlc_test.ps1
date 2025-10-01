# Deploy and Test VLC Integration for AC3/DTS Support
Write-Host "🚀 Deploying NewIPTV with VLC Integration for AC3/DTS Support..." -ForegroundColor Green

# Check for connected devices
$devices = adb devices
if ($devices -notmatch "device") {
    Write-Host "❌ No devices connected. Please connect your device and try again." -ForegroundColor Red
    exit 1
}

Write-Host "📱 Installing updated APK with VLC support..." -ForegroundColor Cyan
adb install -r app\build\outputs\apk\debug\app-debug.apk

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation successful!" -ForegroundColor Green
    
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell monkey -p com.example.newiptv -c android.intent.category.LAUNCHER 1
    
    Write-Host "🎉 App launched with VLC integration!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🔍 Testing Instructions:" -ForegroundColor Yellow
    Write-Host "1. Try playing an AC3/DTS video that previously failed" -ForegroundColor White
    Write-Host "2. Watch for automatic fallback to VLC player" -ForegroundColor White
    Write-Host "3. Check logs for VLC fallback messages" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 To monitor logs, run:" -ForegroundColor Yellow
    Write-Host "   adb logcat | findstr 'IPTVVideoPlayer\|VLCPlayerWrapper'" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Look for these log messages:" -ForegroundColor Yellow
    Write-Host "   - 'VLC fallback player initialized'" -ForegroundColor White
    Write-Host "   - 'Attempting VLC fallback for AC3/DTS support'" -ForegroundColor White
    Write-Host "   - 'Switched to VLC player for AC3/DTS support'" -ForegroundColor White
    Write-Host "   - 'VLC Player ready'" -ForegroundColor White
} else {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
}
