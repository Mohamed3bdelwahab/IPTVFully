# Deploy VLC Debug Version - Enhanced Logging
Write-Host "🚀 Deploying NewIPTV with Enhanced VLC Debug Logging..." -ForegroundColor Green

# Check for connected devices
$devices = adb devices
if ($devices -notmatch "device") {
    Write-Host "❌ No devices connected. Please connect your device and try again." -ForegroundColor Red
    exit 1
}

Write-Host "📱 Installing APK with enhanced VLC debug logging..." -ForegroundColor Cyan
adb install -r app\build\outputs\apk\debug\app-debug.apk

if ($LASTEXITCODE -eq 0) {
    Write-Host "✅ Installation successful!" -ForegroundColor Green
    
    Write-Host "🚀 Launching app..." -ForegroundColor Cyan
    adb shell monkey -p com.example.newiptv -c android.intent.category.LAUNCHER 1
    
    Write-Host "🎉 App launched with enhanced VLC debug logging!" -ForegroundColor Green
    Write-Host ""
    Write-Host "🔍 Debug Features Added:" -ForegroundColor Yellow
    Write-Host "   ✅ Enhanced VLC initialization logging" -ForegroundColor White
    Write-Host "   ✅ Detailed LibVLC creation logs" -ForegroundColor White
    Write-Host "   ✅ MediaPlayer creation logs" -ForegroundColor White
    Write-Host "   ✅ VLC readiness check logs" -ForegroundColor White
    Write-Host "   ✅ Exception handling for VLC creation" -ForegroundColor White
    Write-Host ""
    Write-Host "📋 To monitor VLC initialization logs, run:" -ForegroundColor Yellow
    Write-Host "   adb logcat | findstr 'VLCPlayerWrapper\|IPTVVideoPlayer'" -ForegroundColor White
    Write-Host ""
    Write-Host "🔍 Look for these specific log messages:" -ForegroundColor Yellow
    Write-Host "   - 'Initializing VLC fallback player...'" -ForegroundColor White
    Write-Host "   - 'VLC: Initializing LibVLC with X options...'" -ForegroundColor White
    Write-Host "   - 'VLC: LibVLC instance created: true/false'" -ForegroundColor White
    Write-Host "   - 'VLC: MediaPlayer instance created: true/false'" -ForegroundColor White
    Write-Host "   - 'VLC fallback player initialized - Ready: true/false'" -ForegroundColor White
    Write-Host "   - 'VLC Ready Check: libVLC=true/false, mediaPlayer=true/false'" -ForegroundColor White
    Write-Host ""
    Write-Host "🎯 Test AC3/DTS video and check logs to see VLC initialization status!" -ForegroundColor Cyan
} else {
    Write-Host "❌ Installation failed!" -ForegroundColor Red
}
