# Simple NewIPTV App Monitor
# This script monitors the app for crashes and debug info

Write-Host "=== NewIPTV App Monitor ===" -ForegroundColor Green

# Check if device is connected
$devices = & adb devices
if ($devices -notlike "*device*") {
    Write-Host "❌ No device connected. Please connect your device first." -ForegroundColor Red
    Write-Host "Try: adb connect 192.168.8.20:5555" -ForegroundColor Yellow
    return
}

Write-Host "✅ Device connected successfully!" -ForegroundColor Green

# Find the NewIPTV package
Write-Host "`nSearching for NewIPTV package..." -ForegroundColor Cyan
$packages = & adb shell pm list packages | ForEach-Object { $_ -replace "package:", "" }
$iptvPackage = $packages | Where-Object { $_ -like "*iptv*" } | Select-Object -First 1

if (-not $iptvPackage) {
    Write-Host "❌ NewIPTV package not found" -ForegroundColor Red
    return
}

Write-Host "✅ Found package: $iptvPackage" -ForegroundColor Green

# Check if app is running
$pid = & adb shell pidof $iptvPackage
if ($pid) {
    Write-Host "✅ App is running (PID: $pid)" -ForegroundColor Green
    Write-Host "Starting log monitoring..." -ForegroundColor Yellow
    Write-Host "Press Ctrl+C to stop" -ForegroundColor Cyan
    
    # Monitor logs
    & adb logcat -v time --pid $pid | Select-String -Pattern "iptv|NewIPTV|SeriesScreen|SeriesInfo|VideoPlayer|FATAL|CRASH|Exception|Error"
} else {
    Write-Host "⚠️ App is not running. Launching..." -ForegroundColor Yellow
    & adb shell monkey -p $iptvPackage -c android.intent.category.LAUNCHER 1
    Start-Sleep -Seconds 3
    
    # Check again
    $pid = & adb shell pidof $iptvPackage
    if ($pid) {
        Write-Host "✅ App launched successfully (PID: $pid)" -ForegroundColor Green
        Write-Host "Starting log monitoring..." -ForegroundColor Yellow
        Write-Host "Press Ctrl+C to stop" -ForegroundColor Cyan
        
        # Monitor logs
        & adb logcat -v time --pid $pid | Select-String -Pattern "iptv|NewIPTV|SeriesScreen|SeriesInfo|VideoPlayer|FATAL|CRASH|Exception|Error"
    } else {
        Write-Host "❌ App failed to launch or crashed immediately" -ForegroundColor Red
        Write-Host "Checking crash logs..." -ForegroundColor Yellow
        
        # Check for crash logs
        & adb logcat -v time | Select-String -Pattern "FATAL|CRASH|Exception|Error|iptv" | Select-Object -First 20
    }
}
