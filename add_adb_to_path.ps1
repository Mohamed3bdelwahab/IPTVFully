# Add ADB to System PATH permanently
# Run this script as Administrator

$adbPath = "C:\Users\Pythonix\AppData\Local\Android\Sdk\platform-tools"

# Check if path already exists
$currentPath = [Environment]::GetEnvironmentVariable("PATH", "User")
if ($currentPath -notlike "*$adbPath*") {
    # Add to user PATH
    [Environment]::SetEnvironmentVariable("PATH", "$currentPath;$adbPath", "User")
    Write-Host "Added ADB to User PATH: $adbPath"
} else {
    Write-Host "ADB path already exists in User PATH"
}

# Also add to current session
$env:PATH += ";$adbPath"
Write-Host "Added ADB to current session PATH"

# Verify
try {
    $adbVersion = & adb version 2>&1
    Write-Host "ADB verification successful:"
    Write-Host $adbVersion
} catch {
    Write-Host "ADB verification failed: $_"
}

Write-Host "Please restart your terminal/PowerShell for changes to take effect"
Write-Host "You can now use 'adb' command from anywhere"
