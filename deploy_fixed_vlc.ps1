# Deploy Fixed VLC Integration - No More Crashes!
Write-Host "🚀 Deploying NewIPTV with Fixed VLC Integration..." -ForegroundColor Green

# Check for connected devices
$devices = adb devices
if ($devices -notmatch "device") {
    Write-Host "❌ No devices connected. Please connect your device and try again." -ForegroundColor Red
    exit 1
}

Write-Host "📱 Installing fixed APK with crash-free VLC support..." -ForegroundColor Cyan
adb install -r app\build\outputs\apk\debug\app-debug.apk

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation successful!" -ForegroundColor Green
    
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell monkey -p com.example.newiptv -c android.intent.category.LAUNCHER 1
    
    Write-Host "🎉 App launched with fixed VLC integration!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🔧 Fixes Applied:" -ForegroundColor Yellow
    Write-Host "   ✅ Added VLC initialization checks" -ForegroundColor White
    Write-Host "   ✅ Added null pointer protection" -ForegroundColor White
    Write-Host "   ✅ Added proper error handling" -ForegroundColor White
    Write-Host "   ✅ Added VLC readiness validation" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Testing Instructions:" -ForegroundColor Yellow
    Write-Host "1. Try playing an AC3/DTS video that previously crashed" -ForegroundColor White
    Write-Host "2. App should no longer crash during VLC fallback" -ForegroundColor White
    Write-Host "3. Check logs for proper VLC initialization" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 To monitor logs, run:" -ForegroundColor Yellow
    Write-Host "   adb logcat | findstr 'IPTVVideoPlayer\|VLCPlayerWrapper'" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Look for these log messages:" -ForegroundColor Yellow
    Write-Host "   - 'VLC: Initializing LibVLC...'" -ForegroundColor White
    Write-Host "   - 'VLC: Creating MediaPlayer...'" -ForegroundColor White
    Write-Host "   - 'VLC fallback player initialized'" -ForegroundColor White
    Write-Host "   - 'VLC player not available or not ready' (if VLC fails)" -ForegroundColor White
} else {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
}
