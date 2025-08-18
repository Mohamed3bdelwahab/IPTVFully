# IPTV Android TV Application

A complete Android TV application for streaming IPTV channels with EPG support, favorites, and modern UI.

## 🚀 **Quick Start (Android Studio Online)**

### **Recommended Method - No Java Setup Required**

1. **Open in Android Studio Online:**
   - Visit: https://github.com/Mohamed3bdelwahab/IPTVFully.git
   - Click "Open with Android Studio" 
   - Switch to `FullyIPTV` branch
   - Android Studio Online will handle all Java/JDK setup automatically

2. **Build the Project:**
   - Wait for Gradle sync to complete
   - Click "Build" → "Make Project"
   - Or use the build button in the toolbar

3. **Run on Android TV:**
   - Connect your Android TV device
   - Click "Run" to install and test the app

---

## 🔧 **Local Development Setup**

### **Prerequisites**

- **Java 17 JDK** (Required)
- **Android Studio** (Latest version)
- **Android TV Device** or **Emulator**

### **Java 17 Setup**

#### **Windows:**
```bash
# Download OpenJDK 17 from: https://adoptium.net/
# Or use Chocolatey:
choco install openjdk17

# Set JAVA_HOME environment variable:
setx JAVA_HOME "C:\Program Files\Eclipse Adoptium\jdk-17.x.x.x-hotspot"
```

#### **macOS:**
```bash
# Using Homebrew:
brew install openjdk@17

# Set JAVA_HOME:
echo 'export JAVA_HOME=/opt/homebrew/opt/openjdk@17' >> ~/.zshrc
source ~/.zshrc
```

#### **Linux:**
```bash
# Ubuntu/Debian:
sudo apt update
sudo apt install openjdk-17-jdk

# Set JAVA_HOME:
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
source ~/.bashrc
```

### **Verify Java Installation:**
```bash
java -version
# Should show: openjdk version "17.x.x"
```

### **Build Commands:**

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

---

## 📱 **Features**

✅ **Real M3U Playlist Loading**  
✅ **EPG (Electronic Program Guide)**  
✅ **Channel Favorites**  
✅ **Modern TV-Optimized UI**  
✅ **Database Integration**  
✅ **Image Loading & Caching**  
✅ **Settings Management**  
✅ **Error Handling**  

---

## 🛠 **Technical Stack**

- **UI**: Jetpack Compose + Material 3
- **TV Components**: Android TV Foundation
- **Architecture**: MVVM + Repository Pattern
- **Database**: Room with Flow
- **Networking**: Retrofit + OkHttp
- **Dependency Injection**: Hilt
- **Image Loading**: Coil
- **Video Player**: Media3 ExoPlayer

---

## 📋 **Build Requirements**

- **Minimum SDK**: 23 (Android 6.0)
- **Target SDK**: 34 (Android 14)
- **Java Version**: 17
- **Gradle Version**: 8.7
- **Android Gradle Plugin**: 8.2.2

---

## 🚨 **Troubleshooting**

### **Java/JDK Issues:**

**Error**: `JAVA_HOME is not set and no 'java' command could be found`

**Solutions:**
1. **Use Android Studio Online** (Recommended) - No setup required
2. **Install Java 17** locally (see setup instructions above)
3. **Set JAVA_HOME** environment variable
4. **Verify installation**: `java -version`

### **Gradle Issues:**

**Error**: `Gradle sync failed`

**Solutions:**
1. **Check internet connection** - Gradle needs to download dependencies
2. **Clear Gradle cache**: `./gradlew clean`
3. **Invalidate caches** in Android Studio: File → Invalidate Caches
4. **Update Gradle wrapper**: `./gradlew wrapper --gradle-version 8.7`

### **Build Issues:**

**Error**: `Compilation failed`

**Solutions:**
1. **Sync project** with Gradle files
2. **Clean and rebuild**: `./gradlew clean build`
3. **Check Java version**: Must be Java 17
4. **Update dependencies** if needed

---

## 📞 **Support**

If you encounter any issues:

1. **Check the troubleshooting section above**
2. **Use Android Studio Online** for easiest setup
3. **Verify Java 17 is installed** for local development
4. **Check the GitHub repository** for latest updates

---

**Repository**: https://github.com/Mohamed3bdelwahab/IPTVFully.git  
**Branch**: FullyIPTV  
**Status**: ✅ Production Ready
