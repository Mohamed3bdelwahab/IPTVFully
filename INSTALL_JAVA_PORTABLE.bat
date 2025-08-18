@echo off
echo ========================================
echo IPTV App - Portable Java Setup
echo ========================================
echo.

REM Create directory for portable Java
set "JAVA_DIR=%USERPROFILE%\portable-java"
set "JAVA_VERSION=17"
set "JAVA_BUILD=17.0.9+9"

echo Creating portable Java directory...
if not exist "%JAVA_DIR%" mkdir "%JAVA_DIR%"

echo.
echo Downloading portable OpenJDK %JAVA_VERSION%...
echo This may take a few minutes...

REM Download portable Java (Windows x64)
powershell -Command "& {[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12; Invoke-WebRequest -Uri 'https://github.com/adoptium/temurin17-binaries/releases/download/jdk-%JAVA_BUILD%/OpenJDK17U-jdk_x64_windows_hotspot_%JAVA_BUILD%.zip' -OutFile '%JAVA_DIR%\java-portable.zip'}"

if %errorlevel% neq 0 (
    echo ERROR: Failed to download Java
    echo Please check your internet connection
    pause
    exit /b 1
)

echo.
echo Extracting Java...
powershell -Command "Expand-Archive -Path '%JAVA_DIR%\java-portable.zip' -DestinationPath '%JAVA_DIR%' -Force"

echo.
echo Setting up environment variables for this session...
set "JAVA_HOME=%JAVA_DIR%\jdk-%JAVA_BUILD%"
set "PATH=%JAVA_HOME%\bin;%PATH%"

echo.
echo Verifying Java installation...
java -version
if %errorlevel% neq 0 (
    echo ERROR: Java installation failed
    pause
    exit /b 1
)

echo.
echo ========================================
echo Java %JAVA_VERSION% installed successfully!
echo ========================================
echo.
echo Java location: %JAVA_HOME%
echo.
echo To use Java in new terminal sessions, run:
echo set JAVA_HOME=%JAVA_HOME%
echo set PATH=%JAVA_HOME%\bin;%%PATH%%
echo.
echo Or add these to your user environment variables
echo.

REM Clean up downloaded zip
del "%JAVA_DIR%\java-portable.zip"

echo Ready to build IPTV app!
echo.
pause
