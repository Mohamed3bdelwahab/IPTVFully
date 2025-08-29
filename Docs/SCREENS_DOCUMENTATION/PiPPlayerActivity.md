# 🎬 PiPPlayerActivity Documentation

## 📋 **Overview**
The PiPPlayerActivity provides Picture-in-Picture (PiP) functionality for video playback, allowing users to continue watching content while using other apps. It maintains the video player state and controls in a compact overlay.

## 🎯 **Purpose**
- **Picture-in-Picture:** Enable PiP mode for video playback
- **Background Playback:** Continue video while using other apps
- **Compact Controls:** Minimal controls for PiP mode
- **State Management:** Maintain player state during PiP
- **Seamless Transition:** Smooth transition between full-screen and PiP

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/PiPPlayerActivity.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `ExoPlayer` - Video playback engine
- `PlayerView` - Video rendering component

## 🎨 **UI Components**

### **1. PiP Video Player**
```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
        PlayerView(context).apply {
            useController = false // Custom PiP controls
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player = exoPlayer
        }
    }
)
```

### **2. PiP Controls**
- **Minimal Controls:** Play/pause, close buttons
- **Progress Indicator:** Simple progress bar
- **Touch Controls:** Tap to show/hide controls
- **Auto-Hide:** Controls hide after inactivity

### **3. PiP Overlay**
- **Video Container:** Compact video display
- **Control Overlay:** Minimal control interface
- **Status Indicators:** Buffering, error states
- **Close Button:** Exit PiP mode

## 🔧 **Key Features**

### **1. PiP Mode Management**
```kotlin
// Enter PiP mode
private fun enterPictureInPictureMode() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(true)
            .build()
        enterPictureInPictureMode(params)
    }
}
```

### **2. Player State Management**
```kotlin
// Maintain player state during PiP
private fun savePlayerState() {
    val currentPosition = exoPlayer.currentPosition
    val isPlaying = exoPlayer.isPlaying
    // Save state to preferences or database
}
```

### **3. Compact Controls**
- **Play/Pause:** Simple play/pause toggle
- **Close PiP:** Exit PiP and return to full-screen
- **Progress Bar:** Visual progress indicator
- **Auto-Hide:** Controls disappear after inactivity

### **4. Touch Interactions**
- **Tap to Show/Hide:** Tap video to toggle controls
- **Swipe Gestures:** Swipe for seek (optional)
- **Double Tap:** Skip forward/backward
- **Long Press:** Show additional options

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         PiP Video Player        │
│                                 │
│    ┌─────────────────────────┐   │
│    │                         │   │
│    │      Video Content      │   │
│    │                         │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │    PiP Controls         │   │
│    │  [Play] [Close] [Progress]│   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **PiP Design:**
- **Compact Size:** Optimized for small overlay
- **Minimal Controls:** Essential controls only
- **High Contrast:** Clear visibility on any background
- **Touch Friendly:** Large touch targets
- **Auto-Hide:** Controls disappear when not needed

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Full-Screen Player** → PiP Button → PiPPlayerActivity
2. **System PiP Request** → Automatic PiP → PiPPlayerActivity
3. **Background Playback** → PiP Mode → PiPPlayerActivity

### **Navigation Path:**
```
PlayerScreen → PiPPlayerActivity → Full-Screen Player (return)
```

### **Exit Points:**
1. **Close Button** → Return to full-screen player
2. **System PiP Exit** → Return to full-screen player
3. **App Switch** → Background playback continues

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class PiPPlayerViewModel @Inject constructor(
    @ApplicationContext private val context: Context
) : ViewModel()
```

### **State Variables:**
- **Player State:** Current playback state
- **Video URL:** Current video source
- **Position:** Current playback position
- **Controls Visible:** Control overlay visibility
- **PiP Mode:** Current PiP state
- **Error State:** Error handling and messages

## 🎨 **Theming**

### **Color Scheme:**
- **Background:** Transparent or dark overlay
- **Controls:** High contrast for visibility
- **Text:** White text for readability
- **Accents:** Primary color for highlights

### **Typography:**
- **Control Labels:** Small, clear text
- **Time Display:** Readable time format
- **Status Messages:** Clear status indicators

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lightweight Rendering:** Optimized for PiP mode
- **Memory Management:** Efficient resource usage
- **Battery Optimization:** Minimize battery consumption
- **Smooth Transitions:** Seamless PiP transitions

### **Resource Management:**
```kotlin
// Efficient PiP resource management
override fun onPictureInPictureModeChanged(isInPictureInPictureMode: Boolean) {
    super.onPictureInPictureModeChanged(isInPictureInPictureMode)
    if (isInPictureInPictureMode) {
        // Optimize for PiP mode
        hideSystemUI()
        showPiPControls()
    } else {
        // Return to normal mode
        showSystemUI()
        hidePiPControls()
    }
}
```

## 🔧 **Configuration**

### **PiP Configuration:**
```kotlin
// PiP parameters configuration
private fun configurePiP() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val params = PictureInPictureParams.Builder()
            .setAspectRatio(Rational(16, 9))
            .setAutoEnterEnabled(true)
            .setSeamlessResizeEnabled(true)
            .build()
        setPictureInPictureParams(params)
    }
}
```

### **Player Configuration:**
```kotlin
// ExoPlayer configuration for PiP
val exoPlayer = remember(url) {
    ExoPlayer.Builder(context)
        .build()
        .apply {
            val mediaItem = MediaItem.fromUri(Uri.parse(url))
            setMediaItem(mediaItem)
            prepare()
            playWhenReady = true
        }
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **PiP Mode Testing:** Test PiP mode transitions
- **Player State Testing:** Test state management
- **Control Testing:** Test PiP controls
- **UI Testing:** Test PiP interface

### **Integration Tests:**
- **End-to-End Flow:** Complete PiP journey
- **Performance Testing:** PiP performance
- **Cross-Device Testing:** Different screen sizes
- **Background Testing:** Background playback

## 🐛 **Common Issues & Solutions**

### **1. PiP Not Supported**
**Problem:** PiP mode not available on device
**Solution:** Check Android version and device support

### **2. Player State Loss**
**Problem:** Player state lost during PiP transition
**Solution:** Implement proper state saving and restoration

### **3. Performance Issues**
**Problem:** Poor performance in PiP mode
**Solution:** Optimize rendering and resource usage

### **4. Control Visibility**
**Problem:** Controls not visible in PiP mode
**Solution:** Ensure high contrast and proper sizing

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **PiP Usage:** Track PiP mode usage
- **Control Interactions:** Monitor control usage
- **Transition Patterns:** Track PiP transitions
- **Error Tracking:** Monitor PiP-related errors

### **Performance Metrics:**
- **PiP Performance:** PiP mode performance
- **Memory Usage:** Resource consumption
- **Battery Impact:** Battery usage in PiP mode
- **Transition Speed:** PiP transition performance

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Advanced PiP Controls:** More control options
2. **PiP Gestures:** Enhanced gesture support
3. **Multi-Window Support:** Multiple PiP windows
4. **PiP Customization:** Customizable PiP interface
5. **PiP Analytics:** Detailed PiP usage analytics
6. **Cross-Platform PiP:** Enhanced PiP compatibility

### **UI Improvements:**
1. **Animated Transitions:** Smooth PiP animations
2. **Custom Themes:** PiP-specific themes
3. **Enhanced Controls:** More control options
4. **Gesture Support:** Advanced gesture controls
5. **Accessibility:** Enhanced accessibility features

### **Technical Enhancements:**
1. **Background Sync:** Enhanced background playback
2. **Memory Optimization:** Better memory management
3. **Battery Optimization:** Improved battery efficiency
4. **Performance Monitoring:** Real-time performance tracking
5. **Analytics Dashboard:** Detailed PiP analytics

## 📚 **Related Documentation**

### **Dependencies:**
- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Android PiP Guidelines](https://developer.android.com/develop/ui/views/picture-in-picture)

### **Related Screens:**
- [PlayerScreen.md](./PlayerScreen.md)
- [HomeScreen.md](./HomeScreen.md)

### **API Documentation:**
- [ExoPlayer](./../services/ExoPlayer.md)
- [PlayerView](./../components/PlayerView.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
