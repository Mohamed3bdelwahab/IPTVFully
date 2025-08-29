# 🎬 PlayerScreen Documentation

## 📋 **Overview**
The PlayerScreen is the core video playback interface that provides a complete, professional TV viewing experience. It features full TV remote control support, auto-hiding controls, and advanced playback features comparable to popular streaming platforms.

## 🎯 **Purpose**
- **Video Playback:** High-quality video streaming with ExoPlayer
- **TV Remote Support:** Complete remote control integration
- **Auto-Hide Controls:** Distraction-free viewing experience
- **Advanced Features:** Speed control, playlist access, episode navigation
- **Professional UI:** TV-optimized interface design

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/PlayerScreen.kt
app/src/main/java/com/example/iptvtv/ui/screens/SpeedMenuOverlay.kt
app/src/main/java/com/example/iptvtv/ui/screens/SettingsOverlay.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `ExoPlayer` - Video playback engine
- `PlayerView` - Video rendering component
- `HydraApiService` - API communication
- `PlaybackProgressRepository` - Progress tracking

## 🎨 **UI Components**

### **1. Video Player**
```kotlin
AndroidView(
    modifier = Modifier.fillMaxSize(),
    factory = { context ->
        PlayerView(context).apply {
            useController = false // Custom controls
            resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
            player = exoPlayer
        }
    }
)
```

### **2. Control Overlays**
- **ControlsOverlay:** Main playback controls with play/pause, seek, and episode navigation
- **PlaylistOverlay:** Episode selection interface with season information
- **SpeedMenuOverlay:** Advanced playback speed control with presets
- **SettingsOverlay:** Audio track and subtitle configuration

### **3. Auto-Hide System**
- **Timer-based hiding:** Controls hide after 3 seconds of inactivity
- **User interaction reset:** Any action resets the timer
- **Smooth transitions:** Animated show/hide effects

## 🔧 **Key Features**

### **1. TV Remote Control Support**
```kotlin
val handleKey: (KeyEvent) -> Boolean = { ev ->
    when (ev.keyCode) {
        KeyEvent.KEYCODE_DPAD_CENTER -> { togglePlay(exoPlayer); true }
        KeyEvent.KEYCODE_DPAD_RIGHT -> { seekBy(exoPlayer, +10_000, duration); true }
        KeyEvent.KEYCODE_DPAD_LEFT  -> { seekBy(exoPlayer, -10_000, duration); true }
        // ... more key mappings
    }
}
```

#### **Complete Remote Button Mapping:**
```
🔄 Center/Enter     → Play/Pause
➡️ Right           → Forward 10 seconds
⬅️ Left            → Backward 10 seconds
⏩ Fast Forward    → Forward 30 seconds
⏪ Rewind          → Backward 30 seconds
⏭️ Next           → Next Episode
⏮️ Previous       → Previous Episode
📋 Menu            → Toggle Playlist
ℹ️ Info            → Toggle Speed Menu
🔙 Back            → Exit Player
```

### **2. Advanced Playback Speed Control**
The SpeedMenuOverlay provides comprehensive speed control:

#### **Speed Range & Controls:**
- **Range:** 0.25x to 3.0x (clamped)
- **Incremental:** ±0.25x steps with D-pad Up/Down
- **Quick Presets:** 0.5x, 0.75x, 1.0x, 1.25x, 1.5x, 1.75x, 2.0x
- **Number Keys:** 0-6 for instant preset selection

#### **Speed Menu Features:**
```kotlin
@Composable
fun SpeedMenuOverlay(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
    onClose: () -> Unit
) {
    // Features:
    // - Large speed display (e.g., "1.25x")
    // - Arrow buttons for ±0.25x adjustments
    // - Preset speed list with radio buttons
    // - Full D-pad navigation support
    // - Number key shortcuts (0-6)
    // - Plus/minus key support
}
```

#### **Speed Control Methods:**
- **D-pad Up/Down:** ±0.25x increments
- **Plus/Minus Keys:** ±0.25x increments
- **Number Keys 0-6:** Instant preset speeds
- **Touch Buttons:** Arrow buttons for phone users
- **Preset List:** Clickable speed options

### **3. Auto-Hide Controls**
```kotlin
// Auto-hide controls after 3 seconds of inactivity
LaunchedEffect(lastUserAction) {
    delay(3000)
    if (System.currentTimeMillis() - lastUserAction >= 2800) showControls = false
}
```

### **4. Advanced Playback Features**
- **Playback Speed Control:** 0.25x to 3.0x with presets
- **Episode Navigation:** Previous/Next episode support
- **Progress Tracking:** Automatic progress saving every 10 seconds
- **Resume Playback:** Continue from last position
- **Auto-Play Next:** Automatic episode progression
- **Audio Track Selection:** Multiple language support
- **Subtitle Toggle:** Enable/disable subtitles

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Video Player            │
│                                 │
│    ┌─────────────────────────┐   │
│    │                         │   │
│    │      Video Content      │   │
│    │                         │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │    Control Overlays     │   │
│    │  (Auto-hide after 3s)   │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Control Overlay Design:**
- **Large Touch Targets:** 56dp minimum for TV remote
- **High Contrast:** Dark backgrounds with white text
- **Visual Feedback:** Clear button states and hover effects
- **Accessibility:** Proper content descriptions and focus management

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Series Details** → Episode Selection → PlayerScreen
2. **Deep Link** → Direct video URL → PlayerScreen
3. **External Player** → App launch → PlayerScreen

### **Navigation Path:**
```
HomeScreen → SimpleSeriesScreen → SeriesDetailsScreen → PlayerScreen
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Episode End** → Auto-play next or return to series
3. **Error** → Error screen with retry options

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@Composable
fun PlayerScreen(
    url: String,
    title: String?,
    onBack: () -> Unit,
    episodeId: String? = null,
    seriesId: String? = null
)
```

### **State Variables:**
- **Playback State:** Playing, paused, buffering, error
- **Position:** Current playback position
- **Duration:** Total video duration
- **Controls Visibility:** Show/hide state
- **Episode Information:** Current episode details
- **Playlist State:** Episode list and current index
- **Speed State:** Current playback speed with persistence

## 🎨 **Theming**

### **Color Scheme:**
- **Background:** Black for video content
- **Controls:** Semi-transparent overlays (0.8f alpha)
- **Text:** White for high contrast
- **Accents:** Primary color for highlights

### **Typography:**
- **Large Text:** For time display and titles
- **Bold Fonts:** For important information
- **High Contrast:** Maximum readability

## 📊 **Performance Considerations**

### **Optimizations:**
- **Hardware Acceleration:** GPU-accelerated video decoding
- **Memory Management:** Efficient ExoPlayer resource usage
- **Caching:** Local video caching for better performance
- **Background Processing:** Non-blocking UI operations

### **Memory Management:**
```kotlin
DisposableEffect(Unit) {
    onDispose {
        try { exoPlayer.clearVideoSurface() } catch (_: Throwable) {}
        exoPlayer.release()
    }
}
```

## 🔧 **Configuration**

### **ExoPlayer Configuration:**
```kotlin
val exoPlayer = remember(url) {
    ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setTrackSelector(trackSelector)
        .setLoadControl(loadControl)
        .setVideoChangeFrameRateStrategy(C.VIDEO_CHANGE_FRAME_RATE_STRATEGY_OFF)
        .build()
}
```

### **Progress Tracking:**
```kotlin
// Save progress every 10 seconds
LaunchedEffect(exoPlayer, episodeId, currentEpisode) {
    while (true) {
        delay(10_000)
        if (episodeId != null && isPlaying) {
            val pos = exoPlayer.currentPosition
            val dur = exoPlayer.duration
            playbackProgressRepository.saveProgress(
                episodeId = episodeId,
                position = pos,
                duration = dur
            )
        }
    }
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **Playback Testing:** Test play/pause, seek, speed control
- **Remote Control Testing:** Test all remote button mappings
- **Auto-Hide Testing:** Test control visibility timing
- **Error Handling:** Test network and playback errors

### **Integration Tests:**
- **End-to-End Flow:** Complete video playback journey
- **Performance Testing:** Video loading and playback performance
- **Cross-Device Testing:** Different TV models and remotes
- **Accessibility Testing:** Screen reader and keyboard navigation

## 🐛 **Common Issues & Solutions**

### **1. Video Playback Issues**
**Problem:** Video not playing or buffering
**Solution:** Check network connectivity and video URL validity

### **2. Remote Control Problems**
**Problem:** Remote buttons not responding
**Solution:** Verify key event handling and focus management

### **3. Auto-Hide Issues**
**Problem:** Controls not hiding or showing
**Solution:** Check timer logic and user interaction tracking

### **4. Performance Issues**
**Problem:** Video lag or stuttering
**Solution:** Optimize ExoPlayer configuration and hardware acceleration

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Playback Events:** Play, pause, seek, speed changes
- **Remote Usage:** Track most used remote buttons
- **Episode Navigation:** Previous/next episode usage
- **Control Visibility:** Time spent with controls visible
- **Error Tracking:** Playback errors and recovery

### **Performance Metrics:**
- **Video Load Times:** Time to start playback
- **Buffering Events:** Frequency and duration
- **Memory Usage:** Resource consumption during playback
- **Crash Reports:** Player-related crashes and errors

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Picture-in-Picture:** PiP mode for multitasking
2. **Advanced Subtitle Support:** Multiple subtitle tracks and styling
3. **Audio Track Selection:** Multiple audio languages with quality options
4. **Video Quality Selection:** Adaptive bitrate streaming
5. **Cast Support:** Chromecast and AirPlay integration
6. **Download Support:** Offline video downloads

### **UI Improvements:**
1. **Gesture Controls:** Swipe gestures for seek
2. **Voice Commands:** Voice-activated playback control
3. **Custom Overlays:** User-customizable control layouts
4. **Themes:** Multiple visual themes for different preferences
5. **Animations:** Smooth transitions and micro-interactions

### **Technical Enhancements:**
1. **HDR Support:** High dynamic range video playback
2. **Dolby Atmos:** Advanced audio codec support
3. **4K Streaming:** Ultra-high definition video support
4. **Background Playback:** Audio-only background mode
5. **Analytics Dashboard:** Detailed playback analytics

## 📚 **Related Documentation**

### **Dependencies:**
- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Android TV Guidelines](https://developer.android.com/training/tv)

### **Related Screens:**
- [SeriesDetailsScreen.md](./SeriesDetailsScreen.md)
- [SimpleSeriesScreen.md](./SimpleSeriesScreen.md)
- [PiPPlayerActivity.md](./PiPPlayerActivity.md)

### **API Documentation:**
- [HydraApiService](./../services/HydraApiService.md)
- [PlaybackProgressRepository](./../repositories/PlaybackProgressRepository.md)

---

**Last Updated:** December 2024  
**Version:** 2.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready  
**TV Remote Support:** ✅ Fully Implemented  
**Speed Control:** ✅ Advanced with Presets  
**Auto-Hide Controls:** ✅ 3-second Timer
