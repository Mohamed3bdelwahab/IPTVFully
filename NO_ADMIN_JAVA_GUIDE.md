# 🚀 Installing Java Without Admin Privileges

## **Why Chocolatey Requires Admin Rights**

Chocolatey installs packages system-wide, which requires administrator privileges. However, there are several ways to install Java without admin rights:

---

## **🎯 RECOMMENDED: Android Studio Online**

**Easiest solution - No Java installation needed!**

1. **Visit**: https://github.com/Mohamed3bdelwahab/IPTVFully.git
2. **Click "Open with Android Studio"**
3. **Switch to `FullyIPTV` branch**
4. **Build directly** - Everything is handled automatically!

---

## **🔧 Alternative Solutions (No Admin Required)**

### **Option 1: Portable Java Installation**

#### **Method A: Batch Script (Windows)**
```cmd
# Run the provided script:
INSTALL_JAVA_PORTABLE.bat
```

#### **Method B: PowerShell Script (Windows)**
```powershell
# Run the provided script:
.\install-java-user.ps1
```

#### **Method C: Manual Installation**
```cmd
# 1. Create directory
mkdir %USERPROFILE%\portable-java

# 2. Download Java 17 from:
# https://adoptium.net/temurin/releases/?version=17
# Choose: Windows x64 ZIP

# 3. Extract to %USERPROFILE%\portable-java

# 4. Set environment variables:
set JAVA_HOME=%USERPROFILE%\portable-java\jdk-17.0.9+9
set PATH=%JAVA_HOME%\bin;%PATH%
```

### **Option 2: User-Level Installation**

#### **Using Scoop (No Admin Required)**
```powershell
# Install Scoop (user-level package manager)
Set-ExecutionPolicy -ExecutionPolicy RemoteSigned -Scope CurrentUser
irm get.scoop.sh | iex

# Install Java 17
scoop install openjdk17

# Verify installation
java -version
```

#### **Using SDKMAN (Cross-platform)**
```bash
# Install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 17
sdk install java 17.0.9-tem

# Set as default
sdk default java 17.0.9-tem

# Verify
java -version
```

### **Option 3: Docker (No Local Java Needed)**
```dockerfile
# Create Dockerfile
FROM openjdk:17-jdk
WORKDIR /app
COPY . .
RUN ./gradlew build
```

```bash
# Build with Docker
docker build -t iptv-app .
docker run -it iptv-app
```

---

## **📋 Step-by-Step Instructions**

### **For Windows Users:**

1. **Download the scripts** from this repository
2. **Run PowerShell as regular user** (not admin)
3. **Execute the installation script**:
   ```powershell
   .\install-java-user.ps1
   ```
4. **Verify installation**:
   ```cmd
   java -version
   ```
5. **Build the IPTV app**:
   ```cmd
   .\gradlew build
   ```

### **For macOS Users:**

```bash
# Install Homebrew (user-level)
/bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"

# Install Java 17
brew install openjdk@17

# Set up environment
echo 'export PATH="/opt/homebrew/opt/openjdk@17/bin:$PATH"' >> ~/.zshrc
source ~/.zshrc
```

### **For Linux Users:**

```bash
# Download and install SDKMAN
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"

# Install Java 17
sdk install java 17.0.9-tem
sdk default java 17.0.9-tem
```

---

## **🔍 Verification Steps**

### **Check Java Installation:**
```bash
java -version
# Should show: openjdk version "17.x.x"

javac -version
# Should show: javac 17.x.x
```

### **Check Environment Variables:**
```cmd
# Windows:
echo %JAVA_HOME%
echo %PATH%

# macOS/Linux:
echo $JAVA_HOME
echo $PATH
```

### **Test Gradle:**
```bash
./gradlew --version
./gradlew tasks
```

---

## **🚨 Troubleshooting**

### **Error: "Permission denied"**
- **Solution**: Use user-level installation methods above
- **Alternative**: Use Android Studio Online

### **Error: "Java not found"**
- **Solution**: Check if JAVA_HOME is set correctly
- **Alternative**: Use portable Java installation

### **Error: "Gradle sync failed"**
- **Solution**: Verify Java 17 is installed and in PATH
- **Alternative**: Use Android Studio Online

### **Error: "Network issues"**
- **Solution**: Check internet connection
- **Alternative**: Download Java manually from https://adoptium.net/

---

## **✅ Success Checklist**

- [ ] Java 17 installed in user directory
- [ ] JAVA_HOME environment variable set
- [ ] Java accessible from command line
- [ ] Gradle wrapper working
- [ ] Project builds successfully
- [ ] APK generated

---

## **🎯 Quick Commands**

### **Install and Build (Windows):**
```cmd
# 1. Install portable Java
INSTALL_JAVA_PORTABLE.bat

# 2. Build the app
build_and_test.bat
```

### **Install and Build (PowerShell):**
```powershell
# 1. Install Java
.\install-java-user.ps1

# 2. Build the app
.\gradlew build
```

### **Install and Build (Cross-platform):**
```bash
# 1. Install SDKMAN and Java
curl -s "https://get.sdkman.io" | bash
source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk install java 17.0.9-tem

# 2. Build the app
./gradlew build
```

---

## **📞 Support**

### **Still having issues?**

1. **Use Android Studio Online** - No setup required
2. **Try different installation methods** above
3. **Check the troubleshooting section**
4. **Verify Java version and PATH**
5. **Use portable Java installation**

### **Quick Test:**
```bash
# Test if everything works:
java -version
./gradlew --version
./gradlew build
```

---

**Repository**: https://github.com/Mohamed3bdelwahab/IPTVFully.git  
**Branch**: FullyIPTV  
**Status**: ✅ Production Ready
