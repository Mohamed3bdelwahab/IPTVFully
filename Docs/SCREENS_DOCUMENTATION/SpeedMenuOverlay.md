# 🎯 SpeedMenuOverlay Documentation

## 📋 **Overview**
The SpeedMenuOverlay is a dedicated interface for controlling video playback speed with advanced features designed for both TV remote and touch interactions. It provides precise speed control with multiple input methods and visual feedback.

## 🎯 **Purpose**
- **Speed Control:** Precise playback speed adjustment from 0.25x to 3.0x
- **Multiple Input Methods:** TV remote, touch, and keyboard support
- **Visual Feedback:** Clear speed display and preset selection
- **User-Friendly:** Intuitive interface for all device types

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/SpeedMenuOverlay.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `android.view.KeyEvent` - Android key event handling
- `androidx.compose.ui.input.key` - Compose key event system
- `androidx.compose.material3` - Material Design 3 components

## 🎨 **UI Components**

### **1. Main Container**
```kotlin
Box(
    modifier = Modifier
        .fillMaxSize()
        .background(Color.Black.copy(alpha = 0.6f))  // Scrim background
        .focusRequester(focusRequester)
        .focusable()
        .onPreviewKeyEvent { /* Key handling */ }
)
```

### **2. Speed Display Card**
```kotlin
Card(
    modifier = Modifier
        .align(Alignment.Center)
        .fillMaxWidth(0.55f),
    colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
    shape = RoundedCornerShape(16.dp)
) {
    // Speed control content
}
```

### **3. Speed Control Elements**
- **Speed Display:** Large text showing current speed (e.g., "1.25x")
- **Arrow Buttons:** Up/down buttons for ±0.25x adjustments
- **Preset List:** Radio button list of common speeds
- **Instructions:** Help text for different input methods

## 🔧 **Key Features**

### **1. Speed Range & Limits**
```kotlin
private fun clampSpeed(s: Float) = s.coerceIn(0.25f, 3.0f)
```
- **Minimum Speed:** 0.25x (quarter speed)
- **Maximum Speed:** 3.0x (triple speed)
- **Default Speed:** 1.0x (normal speed)

### **2. Multiple Input Methods**

#### **A. TV Remote Control**
```kotlin
.onPreviewKeyEvent { e ->
    val kev = e.nativeKeyEvent
    when (kev.keyCode) {
        AKeyEvent.KEYCODE_DPAD_UP         -> { step(+0.25f); true }
        AKeyEvent.KEYCODE_DPAD_DOWN       -> { step(-0.25f); true }
        AKeyEvent.KEYCODE_PLUS            -> { step(+0.25f); true }
        AKeyEvent.KEYCODE_MINUS           -> { step(-0.25f); true }
        AKeyEvent.KEYCODE_0 -> { onSpeedSelect(0.5f);  true }
        AKeyEvent.KEYCODE_1 -> { onSpeedSelect(0.75f); true }
        // ... more number keys
    }
}
```

#### **B. Touch Controls**
- **Arrow Buttons:** Large 80dp touch targets
- **Preset Selection:** Clickable speed options
- **Close Button:** Easy dismissal

#### **C. Keyboard Support**
- **Plus/Minus Keys:** ±0.25x adjustments
- **Number Keys 0-6:** Instant preset selection
- **Enter/Space:** Confirm selection
- **Escape/Back:** Close menu

### **3. Preset Speed Options**
```kotlin
val speeds = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
```
- **0.5x:** Half speed (slow motion)
- **0.75x:** Three-quarter speed
- **1.0x:** Normal speed
- **1.25x:** Slightly faster
- **1.5x:** 50% faster
- **1.75x:** 75% faster
- **2.0x:** Double speed

### **4. Visual Feedback**
- **Current Speed Highlight:** Selected preset is highlighted
- **Radio Buttons:** Clear selection indicators
- **Check Icons:** Visual confirmation of current speed
- **Large Display:** Prominent speed number

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Speed Menu              │
│                                 │
│    ┌─────────────────────────┐   │
│    │    Playback Speed       │   │
│    │                         │   │
│    │        1.25x            │   │
│    │                         │   │
│    │    [↓]    [↑]          │   │
│    │                         │   │
│    │  ○ 0.5x                 │   │
│    │  ● 0.75x                │   │
│    │  ○ 1.0x                 │   │
│    │  ○ 1.25x                │   │
│    │  ○ 1.5x                 │   │
│    │  ○ 1.75x                │   │
│    │  ○ 2.0x                 │   │
│    │                         │   │
│    │ ↑/↓ or +/- steps • 0–6  │   │
│    │ [Close]                 │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Design Principles:**
- **High Contrast:** Dark background with white text
- **Large Touch Targets:** 80dp minimum for TV remote
- **Clear Typography:** Readable speed display
- **Intuitive Icons:** Up/down arrows for direction
- **Consistent Spacing:** Proper visual hierarchy

## 🚀 **Usage Examples**

### **Basic Usage**
```kotlin
SpeedMenuOverlay(
    currentSpeed = 1.0f,
    onSpeedSelect = { newSpeed ->
        exoPlayer.setSpeed(newSpeed)
        playbackSpeed = newSpeed
    },
    onClose = { showSpeedMenu = false }
)
```

### **Integration with PlayerScreen**
```kotlin
if (showSpeedMenu) {
    SpeedMenuOverlay(
        currentSpeed = playbackSpeed,
        onSpeedSelect = { s ->
            val sp = clampSpeed(s)
            exoPlayer.setSpeed(sp)
            playbackSpeed = sp
            showSpeedMenu = false
            lastUserAction = now()
        },
        onClose = { showSpeedMenu = false; lastUserAction = now() }
    )
}
```

## 🔄 **State Management**

### **State Variables:**
- **currentSpeed:** Current playback speed (Float)
- **focusRequester:** Focus management for TV remote
- **Local State:** Captured in composable scope

### **State Updates:**
- **Speed Changes:** Immediate feedback to ExoPlayer
- **UI Updates:** Real-time speed display updates
- **Focus Management:** Automatic focus when menu opens

## 🎨 **Theming**

### **Color Scheme:**
- **Background:** Black with 0.6f alpha (scrim)
- **Card Background:** Black with 0.95f alpha
- **Text:** White for maximum contrast
- **Primary:** Material theme primary color
- **Icons:** White for visibility

### **Typography:**
- **Speed Display:** `displaySmall` for large numbers
- **Title:** `headlineSmall` for section headers
- **Preset Text:** `titleLarge` for speed options
- **Instructions:** Smaller text with reduced opacity

## 📊 **Performance Considerations**

### **Optimizations:**
- **Efficient Recomposition:** Minimal state changes
- **Focus Management:** Proper focus handling
- **Key Event Filtering:** Only process relevant keys
- **Memory Management:** No heavy computations

### **Accessibility:**
- **Content Descriptions:** Proper labels for screen readers
- **Focus Navigation:** Logical tab order
- **Touch Targets:** Large enough for all users
- **Color Contrast:** Meets accessibility standards

## 🧪 **Testing**

### **Unit Tests:**
- **Speed Calculation:** Test clampSpeed function
- **Key Event Handling:** Test all key mappings
- **UI State:** Test speed display updates
- **Focus Management:** Test focus behavior

### **Integration Tests:**
- **Player Integration:** Test with ExoPlayer
- **Remote Control:** Test with TV remote
- **Touch Interaction:** Test touch controls
- **Accessibility:** Test with screen readers

## 🐛 **Common Issues & Solutions**

### **1. Key Event Issues**
**Problem:** Remote buttons not responding
**Solution:** Check focus management and key event filtering

### **2. Speed Display Issues**
**Problem:** Speed not updating correctly
**Solution:** Verify state management and ExoPlayer integration

### **3. UI Layout Issues**
**Problem:** Menu not displaying properly
**Solution:** Check modifier chain and alignment

### **4. Performance Issues**
**Problem:** Menu lag or stuttering
**Solution:** Optimize recomposition and state updates

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Speed Changes:** Track most used speeds
- **Input Methods:** Track remote vs touch usage
- **Preset Usage:** Track most popular presets
- **Session Duration:** Time spent in speed menu

### **Performance Metrics:**
- **Menu Open Time:** Time to display speed menu
- **Response Time:** Speed change responsiveness
- **Error Rate:** Failed speed changes
- **User Satisfaction:** Speed control effectiveness

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Custom Speed Input:** Manual speed entry
2. **Speed Presets:** User-defined speed presets
3. **Speed History:** Remember recent speeds
4. **Gesture Controls:** Swipe gestures for speed
5. **Voice Commands:** Voice-activated speed control

### **UI Improvements:**
1. **Animated Transitions:** Smooth speed change animations
2. **Haptic Feedback:** Tactile feedback for speed changes
3. **Visual Indicators:** Progress bars for speed ranges
4. **Themes:** Multiple visual themes
5. **Accessibility:** Enhanced screen reader support

### **Technical Enhancements:**
1. **Speed Smoothing:** Gradual speed transitions
2. **Audio Pitch Correction:** Maintain audio quality at different speeds
3. **Performance Optimization:** Better memory management
4. **Cross-Platform:** Support for different platforms
5. **Analytics Integration:** Better user behavior tracking

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Android Key Events](https://developer.android.com/reference/android/view/KeyEvent)
- [ExoPlayer Speed Control](https://exoplayer.dev/speed.html)

### **Related Components:**
- [PlayerScreen.md](./PlayerScreen.md)
- [SettingsOverlay.md](./SettingsOverlay.md)
- [ControlsOverlay.md](./ControlsOverlay.md)

### **API Documentation:**
- [ExoPlayer PlaybackParameters](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/PlaybackParameters.html)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready  
**TV Remote Support:** ✅ Fully Implemented  
**Touch Support:** ✅ Optimized  
**Accessibility:** ✅ WCAG Compliant
