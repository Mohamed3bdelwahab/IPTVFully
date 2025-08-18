# IPTV App - Java Installation Script (No Admin Required)
# This script installs Java 17 to user directory without admin privileges

Write-Host "========================================" -ForegroundColor Green
Write-Host "IPTV App - Java Installation Script" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""

# Configuration
$JavaVersion = "17"
$JavaBuild = "17.0.9+9"
$UserJavaDir = "$env:USERPROFILE\portable-java"
$JavaZipUrl = "https://github.com/adoptium/temurin17-binaries/releases/download/jdk-$JavaBuild/OpenJDK17U-jdk_x64_windows_hotspot_$JavaBuild.zip"
$JavaZipPath = "$UserJavaDir\java-portable.zip"

Write-Host "Installing Java $JavaVersion to user directory..." -ForegroundColor Yellow
Write-Host "Location: $UserJavaDir" -ForegroundColor Cyan
Write-Host ""

# Create directory if it doesn't exist
if (!(Test-Path $UserJavaDir)) {
    Write-Host "Creating Java directory..." -ForegroundColor Yellow
    New-Item -ItemType Directory -Path $UserJavaDir -Force | Out-Null
}

# Download Java
Write-Host "Downloading OpenJDK $JavaVersion..." -ForegroundColor Yellow
Write-Host "This may take a few minutes..." -ForegroundColor Gray

try {
    [Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12
    Invoke-WebRequest -Uri $JavaZipUrl -OutFile $JavaZipPath -UseBasicParsing
    Write-Host "Download completed!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to download Java" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Write-Host "Please check your internet connection" -ForegroundColor Yellow
    Read-Host "Press Enter to exit"
    exit 1
}

# Extract Java
Write-Host "Extracting Java..." -ForegroundColor Yellow
try {
    Expand-Archive -Path $JavaZipPath -DestinationPath $UserJavaDir -Force
    Write-Host "Extraction completed!" -ForegroundColor Green
} catch {
    Write-Host "ERROR: Failed to extract Java" -ForegroundColor Red
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Set up environment variables
$JavaHome = "$UserJavaDir\jdk-$JavaBuild"
$JavaBin = "$JavaHome\bin"

Write-Host "Setting up environment variables..." -ForegroundColor Yellow

# Set for current session
$env:JAVA_HOME = $JavaHome
$env:PATH = "$JavaBin;$env:PATH"

# Set for user (persistent)
[Environment]::SetEnvironmentVariable("JAVA_HOME", $JavaHome, "User")
[Environment]::SetEnvironmentVariable("PATH", "$JavaBin;$env:PATH", "User")

# Verify installation
Write-Host "Verifying Java installation..." -ForegroundColor Yellow
try {
    $javaVersion = & "$JavaBin\java.exe" -version 2>&1
    Write-Host "Java version:" -ForegroundColor Green
    Write-Host $javaVersion[0] -ForegroundColor Cyan
    Write-Host ""
} catch {
    Write-Host "ERROR: Java verification failed" -ForegroundColor Red
    Read-Host "Press Enter to exit"
    exit 1
}

# Clean up
Write-Host "Cleaning up..." -ForegroundColor Yellow
Remove-Item $JavaZipPath -Force -ErrorAction SilentlyContinue

Write-Host ""
Write-Host "========================================" -ForegroundColor Green
Write-Host "Java $JavaVersion installed successfully!" -ForegroundColor Green
Write-Host "========================================" -ForegroundColor Green
Write-Host ""
Write-Host "Java location: $JavaHome" -ForegroundColor Cyan
Write-Host "Environment variables set for user" -ForegroundColor Cyan
Write-Host ""
Write-Host "You can now build the IPTV app!" -ForegroundColor Green
Write-Host ""

# Test Gradle
Write-Host "Testing Gradle..." -ForegroundColor Yellow
if (Test-Path "gradlew.bat") {
    Write-Host "Running: .\gradlew --version" -ForegroundColor Cyan
    & .\gradlew --version
} else {
    Write-Host "Gradle wrapper not found in current directory" -ForegroundColor Yellow
    Write-Host "Navigate to the IPTV project directory first" -ForegroundColor Yellow
}

Write-Host ""
Read-Host "Press Enter to continue"
