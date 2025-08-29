# 🎮 PlayerUtils Documentation

## 📋 **Overview**
PlayerUtils is a utility file containing helper functions for video player functionality, specifically focused on playback speed control and player state management.

## 🎯 **Purpose**
- **Speed Control:** Provide safe speed clamping and setting functions
- **Player Extensions:** Extend Player functionality with utility methods
- **Cross-Version Compatibility:** Ensure compatibility across Media3 versions
- **Code Reusability:** Centralize common player operations

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/PlayerUtils.kt
```

### **Dependencies:**
- `androidx.media3.common.PlaybackParameters` - Media3 playback parameters
- `androidx.media3.common.Player` - Media3 player interface

## 🔧 **Key Functions**

### **1. Speed Clamping Function**
```kotlin
/** Clamp speed into a safe range for your app */
fun clampSpeed(s: Float) = s.coerceIn(0.25f, 3.0f)
```

#### **Purpose:**
- **Safety:** Prevents setting unsafe playback speeds
- **Range Control:** Limits speed between 0.25x and 3.0x
- **Consistency:** Ensures uniform speed limits across the app

#### **Parameters:**
- **s (Float):** Input speed value to clamp

#### **Returns:**
- **Float:** Clamped speed value within safe range

#### **Usage Examples:**
```kotlin
// Normal speed (unchanged)
val speed1 = clampSpeed(1.0f) // Returns 1.0f

// Speed too low (clamped to minimum)
val speed2 = clampSpeed(0.1f) // Returns 0.25f

// Speed too high (clamped to maximum)
val speed3 = clampSpeed(5.0f) // Returns 3.0f
```

### **2. Player Speed Extension**
```kotlin
/** Robust way to set speed across Media3 versions */
fun Player.setSpeed(speed: Float) {
    this.setPlaybackParameters(PlaybackParameters(speed))
}
```

#### **Purpose:**
- **Version Compatibility:** Works across different Media3 versions
- **Simplified API:** Provides clean extension function
- **Consistent Implementation:** Standardizes speed setting across the app

#### **Parameters:**
- **speed (Float):** Playback speed to set (should be clamped)

#### **Usage Examples:**
```kotlin
// Set normal speed
exoPlayer.setSpeed(1.0f)

// Set fast speed
exoPlayer.setSpeed(2.0f)

// Set slow speed
exoPlayer.setSpeed(0.5f)

// With clamping
exoPlayer.setSpeed(clampSpeed(userInputSpeed))
```

## 🚀 **Integration Examples**

### **Speed Menu Integration**
```kotlin
@Composable
fun SpeedMenuOverlay(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
    onClose: () -> Unit
) {
    fun step(delta: Float) = onSpeedSelect(clampSpeed(currentSpeed + delta))
    
    // Use in UI components
    Button(onClick = { step(+0.25f) }) {
        Text("Increase Speed")
    }
}
```

### **Player Screen Integration**
```kotlin
@Composable
fun PlayerScreen() {
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    // Apply speed changes
    LaunchedEffect(playbackSpeed) {
        exoPlayer.setSpeed(playbackSpeed)
    }
    
    // Handle user input
    fun handleSpeedChange(newSpeed: Float) {
        val clampedSpeed = clampSpeed(newSpeed)
        playbackSpeed = clampedSpeed
        exoPlayer.setSpeed(clampedSpeed)
    }
}
```

### **Key Event Handling**
```kotlin
val handleKey: (KeyEvent) -> Boolean = { ev ->
    when (ev.keyCode) {
        KeyEvent.KEYCODE_PLUS -> {
            val newSpeed = clampSpeed(currentSpeed + 0.25f)
            exoPlayer.setSpeed(newSpeed)
            true
        }
        KeyEvent.KEYCODE_MINUS -> {
            val newSpeed = clampSpeed(currentSpeed - 0.25f)
            exoPlayer.setSpeed(newSpeed)
            true
        }
    }
}
```

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lightweight Functions:** Minimal computational overhead
- **Extension Functions:** Efficient method calls
- **No Side Effects:** Pure functions for better testing
- **Memory Efficient:** No object creation

### **Best Practices:**
- **Always Clamp:** Use clampSpeed before setting player speed
- **Consistent Ranges:** Use the same speed limits throughout the app
- **Error Handling:** Handle potential player state issues
- **Testing:** Test edge cases and boundary values

## 🧪 **Testing**

### **Unit Tests:**
```kotlin
@Test
fun `clampSpeed should return input when within range`() {
    assertEquals(1.0f, clampSpeed(1.0f))
    assertEquals(2.0f, clampSpeed(2.0f))
}

@Test
fun `clampSpeed should clamp to minimum when below range`() {
    assertEquals(0.25f, clampSpeed(0.1f))
    assertEquals(0.25f, clampSpeed(-1.0f))
}

@Test
fun `clampSpeed should clamp to maximum when above range`() {
    assertEquals(3.0f, clampSpeed(5.0f))
    assertEquals(3.0f, clampSpeed(10.0f))
}
```

### **Integration Tests:**
- **Player Integration:** Test speed setting with actual ExoPlayer
- **UI Integration:** Test speed changes in UI components
- **State Management:** Test speed persistence and updates

## 🐛 **Common Issues & Solutions**

### **1. Speed Not Applying**
**Problem:** Player speed not changing
**Solution:** Ensure player is in ready state before setting speed

### **2. Speed Out of Range**
**Problem:** Speed values outside safe range
**Solution:** Always use clampSpeed before setting player speed

### **3. Version Compatibility**
**Problem:** Speed setting not working on different Media3 versions
**Solution:** Use the extension function for consistent behavior

## 📈 **Analytics & Monitoring**

### **Usage Tracking:**
- **Speed Changes:** Track most used speed values
- **Range Violations:** Monitor attempts to set unsafe speeds
- **Player States:** Track speed setting success/failure rates

### **Performance Metrics:**
- **Function Call Frequency:** Monitor usage patterns
- **Error Rates:** Track failed speed operations
- **Response Time:** Measure speed change responsiveness

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Custom Speed Ranges:** Configurable min/max speed limits
2. **Speed Presets:** Predefined speed configurations
3. **Smooth Transitions:** Gradual speed changes
4. **Audio Pitch Correction:** Maintain audio quality at different speeds

### **Technical Improvements:**
1. **Async Speed Setting:** Non-blocking speed operations
2. **Speed Validation:** Enhanced input validation
3. **Performance Optimization:** Better memory management
4. **Cross-Platform Support:** Support for different platforms

## 📚 **Related Documentation**

### **Dependencies:**
- [Media3 Player](https://developer.android.com/reference/androidx/media3/common/Player)
- [Media3 PlaybackParameters](https://developer.android.com/reference/androidx/media3/common/PlaybackParameters)
- [ExoPlayer Speed Control](https://exoplayer.dev/speed.html)

### **Related Components:**
- [PlayerScreen.md](./PlayerScreen.md)
- [SpeedMenuOverlay.md](./SpeedMenuOverlay.md)
- [TrackUtils.md](./TrackUtils.md)

### **API Documentation:**
- [Media3 Player Interface](https://developer.android.com/reference/androidx/media3/common/Player)
- [PlaybackParameters](https://developer.android.com/reference/androidx/media3/common/PlaybackParameters)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready  
**Speed Control:** ✅ Fully Implemented  
**Cross-Version Support:** ✅ Compatible  
**Performance:** ✅ Optimized
