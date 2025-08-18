# 🚀 IPTV App Build Guide

## **Quick Solutions for Java/JDK Issues**

### **🎯 RECOMMENDED: Android Studio Online (Easiest)**

**No Java setup required!** Android Studio Online handles everything automatically.

1. **Visit the repository**: https://github.com/Mohamed3bdelwahab/IPTVFully.git
2. **Click "Open with Android Studio"**
3. **Switch to `FullyIPTV` branch**
4. **Build directly** - No Java installation needed!

---

## **🔧 Local Development Solutions**

### **Problem**: `JAVA_HOME is not set and no 'java' command could be found`

### **Solution 1: Install Java 17 (Windows)**

```bash
# Option A: Download from Eclipse Adoptium
# Visit: https://adoptium.net/temurin/releases/?version=17
# Download and install OpenJDK 17

# Option B: Using Chocolatey (if installed)
choco install openjdk17

# Option C: Using Winget
winget install EclipseAdoptium.Temurin.17.JDK
```

**Set JAVA_HOME (Windows):**
```cmd
# Find your Java installation path (usually):
# C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot

# Set environment variable:
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"

# Add to PATH:
setx PATH "%PATH%;%JAVA_HOME%\bin"

# Restart your terminal/command prompt
```

### **Solution 2: Install Java 17 (macOS)**

```bash
# Using Homebrew:
brew install openjdk@17

# Set JAVA_HOME:
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.zshrc
source ~/.zshrc
```

### **Solution 3: Install Java 17 (Linux)**

```bash
# Ubuntu/Debian:
sudo apt update
sudo apt install openjdk-17-jdk

# Set JAVA_HOME:
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
source ~/.bashrc
```

---

## **✅ Verification Steps**

### **Check Java Installation:**
```bash
java -version
# Should show: openjdk version "17.x.x"

javac -version
# Should show: javac 17.x.x
```

### **Check JAVA_HOME:**
```bash
# Windows:
echo %JAVA_HOME%

# macOS/Linux:
echo $JAVA_HOME
```

---

## **🚀 Build Commands**

### **After Java Setup:**

```bash
# Clone the repository
git clone https://github.com/Mohamed3bdelwahab/IPTVFully.git
cd IPTVFully
git checkout FullyIPTV

# Build the project
./gradlew build

# Build APK
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### **Windows Users:**
```cmd
# Use the provided batch file:
build_and_test.bat
```

---

## **🔍 Troubleshooting**

### **Error: "JAVA_HOME is not set"**

**Quick Fix:**
1. **Use Android Studio Online** (Recommended)
2. **Install Java 17** (see solutions above)
3. **Set JAVA_HOME** environment variable
4. **Restart terminal/command prompt**

### **Error: "Gradle sync failed"**

**Solutions:**
```bash
# Clear Gradle cache
./gradlew clean

# Refresh dependencies
./gradlew --refresh-dependencies

# Update Gradle wrapper
./gradlew wrapper --gradle-version 8.7
```

### **Error: "Compilation failed"**

**Solutions:**
1. **Check Java version**: Must be Java 17+
2. **Sync project** in Android Studio
3. **Invalidate caches**: File → Invalidate Caches
4. **Clean and rebuild**: `./gradlew clean build`

### **Error: "Network issues"**

**Solutions:**
1. **Check internet connection**
2. **Use VPN** if needed
3. **Configure proxy** if behind corporate firewall
4. **Use Android Studio Online** (handles network automatically)

---

## **📱 Running on Android TV**

### **Method 1: Android Studio**
1. **Connect Android TV** via USB
2. **Enable Developer Options** on TV
3. **Enable USB Debugging**
4. **Click "Run"** in Android Studio

### **Method 2: ADB**
```bash
# Install APK via ADB
adb install app/build/outputs/apk/debug/app-debug.apk

# Or use Gradle
./gradlew installDebug
```

### **Method 3: Sideload**
1. **Copy APK** to USB drive
2. **Install via File Manager** on Android TV
3. **Enable "Install from unknown sources"**

---

## **🎯 Alternative Solutions**

### **Option 1: Use Online IDEs**
- **Android Studio Online** (Recommended)
- **GitHub Codespaces**
- **GitPod**
- **Replit**

### **Option 2: Use Docker**
```dockerfile
# Create Dockerfile for consistent environment
FROM openjdk:17-jdk
# ... rest of Docker setup
```

### **Option 3: Use CI/CD**
- **GitHub Actions**
- **Bitrise** (already configured)
- **Codemagic** (already configured)

---

## **📞 Support**

### **Still having issues?**

1. **Use Android Studio Online** - No setup required
2. **Check the troubleshooting section above**
3. **Verify Java 17 installation**
4. **Check network connectivity**
5. **Try different build methods**

### **Quick Test:**
```bash
# Test if everything works:
./gradlew --version
./gradlew tasks
```

---

## **✅ Success Checklist**

- [ ] Java 17 installed and verified
- [ ] JAVA_HOME set correctly
- [ ] Repository cloned and on `FullyIPTV` branch
- [ ] Gradle sync successful
- [ ] Build completed without errors
- [ ] APK generated successfully
- [ ] App installed on Android TV

---

**Repository**: https://github.com/Mohamed3bdelwahab/IPTVFully.git  
**Branch**: FullyIPTV  
**Status**: ✅ Production Ready
