# NewIPTV Build and Deploy Script
# This script handles terminal interruptions and executes commands in sequence

Write-Host "🚀 Starting NewIPTV Build and Deploy Process..." -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green

# Function to handle terminal interruptions
function Handle-Interruption {
    param($Command)
    
    Write-Host "⏳ Executing: $Command" -ForegroundColor Yellow
    
    try {
        # Execute command and capture result
        $result = Invoke-Expression $Command
        
        # Check exit code
        if ($LASTEXITCODE -eq 0) {
            Write-Host "✅ Command completed successfully!" -ForegroundColor Green
            return $true
        } else {
            Write-Host "❌ Command failed with exit code: $LASTEXITCODE" -ForegroundColor Red
            return $false
        }
    }
    catch {
        Write-Host "❌ Error executing command: $($_.Exception.Message)" -ForegroundColor Red
        return $false
    }
}

# Function to wait for user input if needed
function Wait-ForUser {
    param($Message = "Press any key to continue...")
    
    Write-Host $Message -ForegroundColor Cyan
    $null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
}

# Step 1: Build the project
Write-Host "📦 Step 1: Building project..." -ForegroundColor Blue
$buildSuccess = Handle-Interruption "./gradlew assembleDebug"

if (-not $buildSuccess) {
    Write-Host "❌ Build failed! Stopping deployment." -ForegroundColor Red
    Wait-ForUser
    exit 1
}

Write-Host "✅ Build completed successfully!" -ForegroundColor Green
Start-Sleep -Seconds 3

# Step 2: Check if APK exists
Write-Host "🔍 Step 2: Checking APK file..." -ForegroundColor Blue
$apkPath = "app/build/outputs/apk/debug/app-debug.apk"

if (-not (Test-Path $apkPath)) {
    Write-Host "❌ APK file not found at: $apkPath" -ForegroundColor Red
    Wait-ForUser
    exit 1
}

$apkSize = (Get-Item $apkPath).Length / 1MB
Write-Host "✅ APK found: $apkPath (Size: $([math]::Round($apkSize, 2)) MB)" -ForegroundColor Green

# Step 3: Check ADB connection
Write-Host "🔌 Step 3: Checking ADB connection..." -ForegroundColor Blue
$adbPath = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"

if (-not (Test-Path $adbPath)) {
    Write-Host "❌ ADB not found at: $adbPath" -ForegroundColor Red
    Wait-ForUser
    exit 1
}

Write-Host "✅ ADB found at: $adbPath" -ForegroundColor Green

# Step 4: Kill and restart ADB server
Write-Host "🔄 Step 4: Restarting ADB server..." -ForegroundColor Blue
Handle-Interruption "& '$adbPath' kill-server"
Start-Sleep -Seconds 2
Handle-Interruption "& '$adbPath' start-server"
Start-Sleep -Seconds 2

# Step 5: Connect to device
Write-Host "📱 Step 5: Connecting to device..." -ForegroundColor Blue
$deviceIP = "192.168.8.20:5555"
Handle-Interruption "& '$adbPath' connect $deviceIP"
Start-Sleep -Seconds 3

# Step 6: Check device connection
Write-Host "🔍 Step 6: Checking device connection..." -ForegroundColor Blue
$devices = Handle-Interruption "& '$adbPath' devices"

if ($devices -notlike "*$deviceIP*") {
    Write-Host "❌ Device not connected: $deviceIP" -ForegroundColor Red
    Write-Host "💡 Try connecting manually or check device IP address" -ForegroundColor Yellow
    Wait-ForUser
    exit 1
}

Write-Host "✅ Device connected: $deviceIP" -ForegroundColor Green

# Step 7: Install APK
Write-Host "📥 Step 7: Installing APK..." -ForegroundColor Blue
$installSuccess = Handle-Interruption "& '$adbPath' -s $deviceIP install -r '$apkPath'"

if (-not $installSuccess) {
    Write-Host "❌ APK installation failed!" -ForegroundColor Red
    Wait-ForUser
    exit 1
}

Write-Host "✅ APK installed successfully!" -ForegroundColor Green
Start-Sleep -Seconds 3

# Step 8: Launch app
Write-Host "🚀 Step 8: Launching app..." -ForegroundColor Blue
$launchSuccess = Handle-Interruption "& '$adbPath' -s $deviceIP shell am start -n com.example.newiptv/.MainActivity"

if (-not $launchSuccess) {
    Write-Host "❌ App launch failed!" -ForegroundColor Red
    Wait-ForUser
    exit 1
}

Write-Host "✅ App launched successfully!" -ForegroundColor Green

# Final status
Write-Host "================================================" -ForegroundColor Green
Write-Host "🎉 NewIPTV Build and Deploy Completed Successfully!" -ForegroundColor Green
Write-Host "================================================" -ForegroundColor Green
Write-Host ""
Write-Host "📱 App is now running on device: $deviceIP" -ForegroundColor Cyan
Write-Host "🔍 Check logs with: & '$adbPath' -s $deviceIP logcat" -ForegroundColor Cyan
Write-Host ""

Wait-ForUser "Press any key to exit..."
