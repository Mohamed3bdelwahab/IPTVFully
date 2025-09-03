# Fix ADB PATH permanently
# Run this as Administrator

Write-Host "=== Fixing ADB PATH ===" -ForegroundColor Green

$adbPath = "C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools"

# Method 1: Add to User PATH (doesn't require admin)
Write-Host "Adding ADB to User PATH..." -ForegroundColor Yellow
$userPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($userPath -notlike "*$adbPath*") {
    [Environment]::SetEnvironmentVariable("PATH", "$userPath;$adbPath", "User")
    Write-Host "Added to User PATH" -ForegroundColor Green
} else {
    Write-Host "Already in User PATH" -ForegroundColor Cyan
}

# Method 2: Add to current session
Write-Host "Adding to current session..." -ForegroundColor Yellow
$env:PATH += ";$adbPath"
Write-Host "Added to current session" -ForegroundColor Green

# Method 3: Create a batch file for easy access
Write-Host "Creating batch file for easy access..." -ForegroundColor Yellow
$batchContent = "set PATH=%PATH%;C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools`n"
$batchContent += "echo ADB PATH added to current session`n"
$batchContent += "echo You can now use: adb devices`n"
$batchContent += "echo.`n"
$batchContent += "adb devices"

$batchContent | Out-File -FilePath "use_adb.bat" -Encoding ASCII
Write-Host "Created use_adb.bat" -ForegroundColor Green

# Method 4: Create PowerShell profile script
Write-Host "Creating PowerShell profile script..." -ForegroundColor Yellow
$profileScript = "# Add ADB to PATH for this PowerShell session`n"
$profileScript += "`$env:PATH += `";C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools`"`n"
$profileScript += "Write-Host `"ADB added to PATH`" -ForegroundColor Green"

$profileScript | Out-File -FilePath "add_adb_to_session.ps1" -Encoding UTF8
Write-Host "Created add_adb_to_session.ps1" -ForegroundColor Green

# Test ADB
Write-Host "`nTesting ADB..." -ForegroundColor Yellow
try {
    $adbVersion = & "$adbPath\adb.exe" version 2>&1
    Write-Host "ADB working from full path:" -ForegroundColor Green
    Write-Host $adbVersion
} catch {
    Write-Host "ADB test failed: $_" -ForegroundColor Red
}

Write-Host "`n=== SOLUTIONS ===" -ForegroundColor Green
Write-Host "1. Use full path: $adbPath\adb.exe" -ForegroundColor Yellow
Write-Host "2. Run: .\use_adb.bat" -ForegroundColor Yellow
Write-Host "3. Run: .\add_adb_to_session.ps1" -ForegroundColor Yellow
Write-Host "4. Restart PowerShell for permanent PATH change" -ForegroundColor Yellow

Write-Host "`n=== QUICK TEST ===" -ForegroundColor Green
Write-Host "Testing with full path..." -ForegroundColor Yellow
& "$adbPath\adb.exe" devices
