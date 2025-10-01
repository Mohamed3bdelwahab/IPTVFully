# 🔍 **NewIPTV V2 - Debugging Guide**

## 📋 **Overview**
This comprehensive debugging guide provides detailed instructions for monitoring, troubleshooting, and debugging NewIPTV V2 features, including MX Player integration and TV remote navigation.

## 🎯 **Debugging Categories**

### **1. MX Player Integration Debugging**
### **2. TV Remote Navigation Debugging**
### **3. Performance Monitoring**
### **4. Error Analysis**
### **5. Device Compatibility Testing**

## 🎬 **MX Player Integration Debugging**

### **Log Tags to Monitor**
- **`MXPlayerIntegration`**: Core MX Player functionality
- **`SeriesInfoScreen`**: Playlist creation and episode handling
- **`HomeScreen`**: Test playlist functionality

### **Key Log Messages**

#### **Successful Integration**
```
🎬 Attempting to launch season playlist in MX Player...
Episodes count: 10
Start index: 2
Valid episodes for playlist: 10
✅ MX Player playlist launched successfully
✅ Season playlist launched in MX Player
```

#### **Error Scenarios**
```
⚠️ MX Player not installed, cannot launch playlist
❌ Failed to launch season playlist
❌ No valid video URLs found for playlist
⚠️ Skipping episode with empty URL: Episode Title
```

### **Monitoring Commands**

#### **Basic MX Player Monitoring**
```bash
# Monitor all MX Player integration logs
adb logcat | grep "MXPlayerIntegration"

# Monitor playlist creation
adb logcat | grep "Season playlist\|Episodes count"

# Monitor MX Player detection
adb logcat | grep "MX Player.*installed\|MX Player.*detected"
```

#### **Advanced MX Player Monitoring**
```bash
# Monitor playlist launch attempts
adb logcat | grep "Attempting to launch season playlist"

# Monitor playlist success/failure
adb logcat | grep "✅.*playlist\|❌.*playlist"

# Monitor episode handling
adb logcat | grep "Episode.*directSource\|Valid episodes"
```

### **Common MX Player Issues**

#### **1. MX Player Not Detected**
**Symptoms**: Logs show "MX Player not installed"
**Debug Steps**:
```bash
# Check if MX Player packages are installed
adb shell pm list packages | grep mxtech

# Check package visibility permissions
adb shell dumpsys package com.example.newiptv | grep -A 10 "requested permissions"
```

**Solutions**:
- Verify MX Player Pro/Free is installed
- Check Android 15 package visibility permissions
- Grant `QUERY_ALL_PACKAGES` permission via ADB

#### **2. ClassCastException Errors**
**Symptoms**: MX Player logs show type casting errors
**Debug Steps**:
```bash
# Monitor MX Player logs for ClassCastException
adb logcat | grep "ClassCastException"

# Check Intent extras being sent
adb logcat | grep "video_list\|decode_mode"
```

**Solutions**:
- Verify correct data types: `Uri[]`, `String[]`, `Byte`
- Check `MXPlayerIntegration.kt` for proper type conversion
- Ensure `.toTypedArray()` and `.toByte()` are used

#### **3. Playlist Not Appearing**
**Symptoms**: MX Player opens but playlist is empty
**Debug Steps**:
```bash
# Check video URLs being sent
adb logcat | grep "video_list.*http"

# Check episode names
adb logcat | grep "Episode names"

# Verify playlist parameters
adb logcat | grep "video_list.play_index\|start_position"
```

**Solutions**:
- Verify video URLs are valid and accessible
- Check episode names are properly formatted
- Ensure `video_list_is_explicit` is set to true

## 🎮 **TV Remote Navigation Debugging**

### **Log Tags to Monitor**
- **`KeyEventLogger`**: All navigation events
- **`SeriesScreen`**: Series navigation
- **`MoviesScreen`**: Movies navigation
- **`SeriesInfoScreen`**: Episode navigation

### **Key Log Messages**

#### **Successful Navigation**
```
🎮 [SeriesScreen] 🧭 NAVIGATION: UP
🎮 [SeriesScreen] From Panel: Series
🎮 [SeriesScreen] 📍 FOCUS CHANGED: Series at position 3

🎮 [MoviesScreen] 🧭 NAVIGATION: DOWN
🎮 [MoviesScreen] From Panel: Movies
🎮 [MoviesScreen] 📍 FOCUS CHANGED: Movies at position 6
```

#### **Navigation Errors**
```
🎮 [SeriesScreen] ❌ ERROR: Cannot navigate UP
🎮 [SeriesScreen] Details: Top row reached

🎮 [MoviesScreen] ❌ ERROR: Cannot navigate DOWN
🎮 [MoviesScreen] Details: Bottom row reached
```

### **Monitoring Commands**

#### **Basic Navigation Monitoring**
```bash
# Monitor all navigation events
adb logcat | grep "🧭 NAVIGATION"

# Monitor focus changes
adb logcat | grep "📍 FOCUS CHANGED"

# Monitor navigation errors
adb logcat | grep "❌ ERROR"
```

#### **Screen-Specific Monitoring**
```bash
# Monitor Series Screen navigation
adb logcat | grep "SeriesScreen.*NAVIGATION\|SeriesScreen.*FOCUS"

# Monitor Movies Screen navigation
adb logcat | grep "MoviesScreen.*NAVIGATION\|MoviesScreen.*FOCUS"

# Monitor Series Info Screen navigation
adb logcat | grep "SeriesInfoScreen.*NAVIGATION\|SeriesInfoScreen.*FOCUS"
```

#### **Advanced Navigation Monitoring**
```bash
# Monitor panel switching
adb logcat | grep "LEFT\|RIGHT.*Panel"

# Monitor grid navigation
adb logcat | grep "spanCount\|currentPosition"

# Monitor focus management
adb logcat | grep "getCurrentFocusedPosition\|updateFocus"
```

### **Common Navigation Issues**

#### **1. UP/DOWN Navigation Not Working**
**Symptoms**: Navigation gets stuck or doesn't move properly
**Debug Steps**:
```bash
# Check navigation method calls
adb logcat | grep "navigateSeriesUp\|navigateSeriesDown"

# Check focus position tracking
adb logcat | grep "getCurrentFocusedPosition"

# Check grid navigation logic
adb logcat | grep "spanCount.*3\|currentPosition"
```

**Solutions**:
- Verify `getCurrentFocusedPosition()` is working correctly
- Check grid navigation logic with `spanCount = 3`
- Ensure proper boundary checking

#### **2. Panel Switching Issues**
**Symptoms**: LEFT/RIGHT keys don't switch panels properly
**Debug Steps**:
```bash
# Check panel switching logic
adb logcat | grep "DPAD_LEFT\|DPAD_RIGHT"

# Check panel state
adb logcat | grep "isInCategoryPanel\|isInSeasonPanel"

# Check focus management
adb logcat | grep "ensureCorrectPanelFocus"
```

**Solutions**:
- Verify panel state tracking (`isInCategoryPanel`, `isInSeasonPanel`)
- Check LEFT key logic for leftmost column detection
- Ensure proper focus management

#### **3. Focus Management Issues**
**Symptoms**: Focus not updating correctly or getting lost
**Debug Steps**:
```bash
# Check focus update methods
adb logcat | grep "updateSeriesFocus\|updateMoviesFocus"

# Check focus change listeners
adb logcat | grep "onFocusChangeListener"

# Check RecyclerView focus
adb logcat | grep "focusedChild\|requestFocus"
```

**Solutions**:
- Verify `updateFocus()` methods are called correctly
- Check RecyclerView focus management
- Ensure proper error handling in focus methods

## 📊 **Performance Monitoring**

### **Navigation Performance**
```bash
# Monitor navigation timing
adb logcat | grep "Navigation.*ms\|Focus.*ms"

# Monitor frame drops
adb logcat | grep "Skipped.*frames\|Choreographer"

# Monitor memory usage
adb logcat | grep "GC\|Memory"
```

### **Playback Performance**
```bash
# Monitor MX Player launch time
adb logcat | grep "launch.*ms\|playlist.*ms"

# Monitor video loading
adb logcat | grep "Video.*load\|Buffer.*ms"

# Monitor codec performance
adb logcat | grep "Codec\|Decoder"
```

## 🐛 **Error Analysis**

### **Common Error Patterns**

#### **1. ClassCastException**
**Pattern**: `java.lang.ClassCastException: java.lang.Integer cannot be cast to java.lang.Byte`
**Cause**: Incorrect data types in Intent extras
**Solution**: Use `.toByte()` for decode_mode

#### **2. Navigation Stuck**
**Pattern**: Focus stuck at specific position
**Cause**: Incorrect focus position tracking
**Solution**: Use `getCurrentFocusedPosition()` method

#### **3. Panel Switching Failure**
**Pattern**: LEFT/RIGHT keys not switching panels
**Cause**: Incorrect panel state management
**Solution**: Verify `isInCategoryPanel` state tracking

### **Error Logging Commands**
```bash
# Monitor all errors
adb logcat | grep "ERROR\|Exception\|FATAL"

# Monitor specific error types
adb logcat | grep "ClassCastException\|NullPointerException"

# Monitor error recovery
adb logcat | grep "Error.*recovery\|Fallback"
```

## 📱 **Device Compatibility Testing**

### **Android Version Testing**
```bash
# Check Android version
adb shell getprop ro.build.version.release

# Check API level
adb shell getprop ro.build.version.sdk

# Check device type
adb shell getprop ro.build.characteristics
```

### **MX Player Compatibility**
```bash
# Check MX Player installation
adb shell pm list packages | grep mxtech

# Check MX Player version
adb shell dumpsys package com.mxtech.videoplayer.pro | grep versionName

# Check package visibility
adb shell dumpsys package com.example.newiptv | grep -A 5 "requested permissions"
```

### **TV Remote Compatibility**
```bash
# Check input device
adb shell getevent -l

# Check key mapping
adb shell dumpsys input | grep -A 10 "Input Devices"

# Check focus management
adb shell dumpsys window | grep -A 5 "mCurrentFocus"
```

## 🔧 **Advanced Debugging Tools**

### **Custom Log Filters**
```bash
# Create custom log filter for NewIPTV
adb logcat | grep -E "(NewIPTV|MXPlayer|SeriesScreen|MoviesScreen|KeyEventLogger)"

# Filter by log level
adb logcat *:E | grep NewIPTV  # Errors only
adb logcat *:W | grep NewIPTV  # Warnings and above
adb logcat *:I | grep NewIPTV  # Info and above
```

### **Performance Profiling**
```bash
# Monitor CPU usage
adb shell top | grep com.example.newiptv

# Monitor memory usage
adb shell dumpsys meminfo com.example.newiptv

# Monitor network usage
adb shell dumpsys netstats | grep com.example.newiptv
```

### **System Information**
```bash
# Get device information
adb shell getprop | grep -E "(ro.product|ro.build)"

# Get app information
adb shell dumpsys package com.example.newiptv

# Get activity information
adb shell dumpsys activity | grep com.example.newiptv
```

## 📋 **Debugging Checklist**

### **MX Player Integration**
- [ ] MX Player packages detected
- [ ] Android 15 permissions granted
- [ ] Correct data types used (Uri[], String[], Byte)
- [ ] Video URLs are valid and accessible
- [ ] Playlist parameters set correctly
- [ ] Fallback system working

### **TV Remote Navigation**
- [ ] UP/DOWN navigation working in grid
- [ ] LEFT/RIGHT panel switching working
- [ ] Focus management working correctly
- [ ] Boundary checking working
- [ ] Error logging working
- [ ] Performance acceptable

### **General Debugging**
- [ ] No crashes or exceptions
- [ ] Performance within acceptable limits
- [ ] Memory usage stable
- [ ] Network connectivity working
- [ ] Device compatibility verified
- [ ] User experience smooth

## 🎯 **Troubleshooting Quick Reference**

### **Quick Fixes**
```bash
# Clear app data and restart
adb shell pm clear com.example.newiptv

# Restart app
adb shell am force-stop com.example.newiptv
adb shell am start -n com.example.newiptv/.ui.home.HomeScreen

# Check permissions
adb shell pm grant com.example.newiptv android.permission.QUERY_ALL_PACKAGES

# Monitor real-time logs
adb logcat -v time | grep NewIPTV
```

### **Emergency Debugging**
```bash
# Get full crash log
adb logcat -d | grep -A 20 "FATAL EXCEPTION"

# Get system information
adb shell dumpsys system | grep -A 10 "Memory"

# Get app state
adb shell dumpsys activity com.example.newiptv
```

---

**Last Updated**: 2024-09-16  
**Version**: NewIPTV V2.0  
**Debugging Guide Version**: 1.0
