# === CONFIGURATION ===
$searchString = "iptv"     # What to look for in package names
$logFilter    = "debug|system|screen|series|player|info"  # What to filter for in logcat output

# === SETUP ===
$adb = "C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools\adb.exe"

Write-Host "=== NewIPTV App Monitor ===" -ForegroundColor Green
Write-Host "Searching for package: $searchString" -ForegroundColor Yellow
Write-Host "Log filter: $logFilter" -ForegroundColor Yellow
Write-Host ""

# === STEP 1: Find matching package ===
Write-Host "Step 1: Finding matching package..." -ForegroundColor Cyan
$packages = & $adb shell pm list packages | ForEach-Object { $_ -replace "package:", "" }

$matchingPkg = $packages | Where-Object { $_ -like "*$searchString*" } | Select-Object -First 1

if (-not $matchingPkg) {
    Write-Host "❌ No package found matching '$searchString'" -ForegroundColor Red
    return
}

Write-Host "✅ Found matching package: $matchingPkg" -ForegroundColor Green

# === STEP 2: Try to get PID ===
Write-Host "`nStep 2: Checking if app is running..." -ForegroundColor Cyan
$appPidRaw = & $adb shell pidof $matchingPkg

if (![string]::IsNullOrWhiteSpace($appPidRaw)) {
    $appPid = $appPidRaw.Trim()
    Write-Host "✅ App is running with PID: $appPid" -ForegroundColor Green
    Write-Host "Starting logcat monitoring..." -ForegroundColor Yellow
    
    # Monitor logs for the running app
    Write-Host "`n=== LOG MONITORING STARTED ===" -ForegroundColor Green
    Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
    Write-Host ""
    
    & $adb logcat -v time --pid $appPid | Select-String -Pattern $logFilter
    
} else {
    Write-Host "⚠️ App '$matchingPkg' is not running. Trying to launch it..." -ForegroundColor Yellow

    # Try launching the app
    Write-Host "Launching app..." -ForegroundColor Cyan
    & $adb shell monkey -p $matchingPkg -c android.intent.category.LAUNCHER 1 > $null
    Start-Sleep -Seconds 3

    # Try again to get PID
    $appPidRaw = & $adb shell pidof $matchingPkg
    if (![string]::IsNullOrWhiteSpace($appPidRaw)) {
        $appPid = $appPidRaw.Trim()
        Write-Host "✅ App launched successfully! PID: $appPid" -ForegroundColor Green
        Write-Host "Starting logcat monitoring..." -ForegroundColor Yellow
        
        # Monitor logs for the newly launched app
        Write-Host "`n=== LOG MONITORING STARTED ===" -ForegroundColor Green
        Write-Host "Press Ctrl+C to stop monitoring" -ForegroundColor Yellow
        Write-Host ""
        
        & $adb logcat -v time --pid $appPid | Select-String -Pattern $logFilter
        
    } else {
        Write-Host "❌ App failed to launch or crashed immediately" -ForegroundColor Red
        Write-Host "Checking for crash logs..." -ForegroundColor Yellow
        
        # Check for crash logs
        & $adb logcat -v time | Select-String -Pattern "FATAL|CRASH|Exception|Error|iptv" | Select-Object -First 20
    }
}

Write-Host "`n=== MONITORING COMPLETED ===" -ForegroundColor Green
