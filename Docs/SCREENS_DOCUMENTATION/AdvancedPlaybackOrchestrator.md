# 🎬 Advanced Playback Orchestrator Documentation

## 📋 **Overview**

The Advanced Playback Orchestrator is a sophisticated system that provides automatic fallback between hardware (HW) and FFmpeg software decoders, ensuring robust video playback across all Android devices. It monitors playback quality in real-time and automatically switches decoders when performance issues are detected.

---

## 🏗️ **Architecture**

### **Core Components**

```
playback/
├── AnalyticsMonitor.kt      # Real-time playback monitoring
├── PipelineFactory.kt       # HW/FFmpeg pipeline creation
├── PlaybackOrchestrator.kt  # Main orchestrator logic
└── ui/components/
    └── ModeSwitchButton.kt  # Manual mode switching UI
```

### **Design Principles**

- **Single Responsibility**: Each component has one focused purpose
- **Small Code**: Minimal, focused functions
- **OOP Concepts**: Clean separation of concerns
- **Error Prevention**: Avoids previous compilation issues

---

## 🔧 **Components Breakdown**

### **1. AnalyticsMonitor.kt**

**Purpose**: Monitors playback analytics for automatic fallback detection

**Key Features**:
- Frame drop detection (>100 frames in 10s window)
- AV sync drift monitoring (>250ms threshold)
- Buffering timeout detection (>5s repeated)
- Player error detection

**Thresholds**:
```kotlin
FRAME_DROP_THRESHOLD = 100
FRAME_DROP_WINDOW_MS = 10_000L
BUFFERING_TIMEOUT_MS = 5_000L
AV_SYNC_THRESHOLD_MS = 250L
```

**Usage**:
```kotlin
val monitor = AnalyticsMonitor { reason ->
    // Handle fallback trigger
    orchestrator.triggerFallback(reason)
}
```

### **2. PipelineFactory.kt**

**Purpose**: Creates optimized ExoPlayer pipelines for different decoder types

**Pipelines**:
- **HW Pipeline**: Hardware-accelerated, battery-efficient
- **FFmpeg Pipeline**: Software decoder, universal compatibility

**Configuration**:
```kotlin
// HW Pipeline
.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_OFF)

// FFmpeg Pipeline  
.setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
```

### **3. PlaybackOrchestrator.kt**

**Purpose**: Main orchestrator managing player switching and seamless playback

**Key Functions**:
- `play(url, onError)`: Start playback with auto-pipeline selection
- `switchMode()`: Manual mode switching
- `getCurrentMode()`: Get current decoder mode
- `release()`: Clean resource release

**Auto-Fallback Logic**:
1. Start with HW decoder (fast, efficient)
2. Monitor for issues via AnalyticsMonitor
3. Auto-switch to FFmpeg if problems detected
4. Resume from exact position
5. Notify UI of fallback

### **4. ModeSwitchButton.kt**

**Purpose**: UI component for manual mode switching

**Variants**:
- `ModeSwitchButton`: Compact version (120x40dp)
- `ExpandedModeSwitchButton`: Detailed version (160x60dp)

**Visual Indicators**:
- **Blue**: Hardware mode (Fast)
- **Orange**: FFmpeg mode (Universal)

---

## 🚀 **Usage Examples**

### **Basic Integration**

```kotlin
class PlayerScreen : ComponentActivity() {
    private lateinit var orchestrator: PlaybackOrchestrator
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        orchestrator = PlaybackOrchestrator(this)
        
        setContent {
            val player = orchestrator.play(
                url = "https://example.com/stream.m3u8",
                onError = { error ->
                    // Handle errors and fallback notifications
                    Log.e("Player", "Error: $error")
                }
            )
            
            // Use player in ExoPlayerView
            AndroidView(
                factory = { ExoPlayerView(it) },
                modifier = Modifier.fillMaxSize()
            ) { exoPlayerView ->
                exoPlayerView.player = player
            }
        }
    }
}
```

### **Manual Mode Switching**

```kotlin
@Composable
fun PlayerControls(
    currentMode: String,
    onModeSwitch: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Other controls...
        
        ModeSwitchButton(
            currentMode = currentMode,
            onModeSwitch = onModeSwitch
        )
    }
}
```

### **Advanced Integration with ViewModel**

```kotlin
@HiltViewModel
class PlayerViewModel @Inject constructor() : ViewModel() {
    
    private var orchestrator: PlaybackOrchestrator? = null
    
    private val _currentMode = MutableStateFlow("Hardware")
    val currentMode: StateFlow<String> = _currentMode.asStateFlow()
    
    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()
    
    fun initializeOrchestrator(context: Context) {
        orchestrator = PlaybackOrchestrator(context)
    }
    
    fun playStream(url: String): ExoPlayer? {
        return orchestrator?.play(url) { error ->
            _errorMessage.value = error
            _currentMode.value = orchestrator?.getCurrentMode() ?: "Hardware"
        }
    }
    
    fun switchMode() {
        orchestrator?.switchMode()
        _currentMode.value = orchestrator?.getCurrentMode() ?: "Hardware"
    }
    
    override fun onCleared() {
        orchestrator?.release()
        super.onCleared()
    }
}
```

---

## 📊 **Fallback Triggers**

### **Automatic Triggers**

| Trigger | Condition | Action |
|---------|-----------|--------|
| **Frame Drops** | >100 frames in 10s | Switch to FFmpeg |
| **AV Sync Drift** | >250ms difference | Switch to FFmpeg |
| **Buffering Timeout** | >5s repeated | Switch to FFmpeg |
| **Player Error** | Any ExoPlayer error | Switch to FFmpeg |

### **Manual Triggers**

- User clicks ModeSwitchButton
- Programmatic call to `switchMode()`

---

## 🔍 **Monitoring & Debugging**

### **Log Tags**

- `AnalyticsMonitor`: Frame drops, sync issues, buffering
- `PlaybackOrchestrator`: Mode switches, fallbacks, errors
- `PipelineFactory`: Pipeline creation

### **Key Log Messages**

```
I/PlaybackOrchestrator: Creating HW pipeline
W/AnalyticsMonitor: Dropped 15 frames in 1000ms (Total: 45)
E/AnalyticsMonitor: Frame drop threshold exceeded: 105
E/PlaybackOrchestrator: Auto-fallback to FFmpeg: Too many dropped frames: 105
I/PlaybackOrchestrator: Creating FFmpeg pipeline
I/PlaybackOrchestrator: Resuming from position: 45000ms
```

---

## ⚡ **Performance Benefits**

### **Hardware Mode**
- ✅ Fast decoding
- ✅ Low battery usage
- ✅ Smooth playback
- ❌ Limited codec support

### **FFmpeg Mode**
- ✅ Universal codec support
- ✅ Reliable playback
- ✅ No hardware dependencies
- ❌ Higher CPU usage
- ❌ More battery consumption

---

## 🛡️ **Error Prevention**

### **Previous Issues Avoided**

1. **Compilation Errors**: No `nativeKeyEvent` imports
2. **Conflicting Overloads**: Single responsibility per file
3. **Memory Leaks**: Proper resource cleanup
4. **Screen Freeze**: Optimized pipeline configuration

### **Best Practices**

- Always call `release()` when done
- Handle errors gracefully
- Monitor performance metrics
- Provide user feedback for mode switches

---

## 📱 **UI Integration**

### **PlayerScreen Integration**

```kotlin
@Composable
fun PlayerScreen(
    viewModel: PlayerViewModel = hiltViewModel()
) {
    val currentMode by viewModel.currentMode.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()
    
    Box(modifier = Modifier.fillMaxSize()) {
        // ExoPlayerView here
        
        // Mode switch button
        ModeSwitchButton(
            currentMode = currentMode,
            onModeSwitch = { viewModel.switchMode() },
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(16.dp)
        )
        
        // Error message display
        errorMessage?.let { message ->
            Text(
                text = message,
                color = Color.Red,
                modifier = Modifier
                    .align(Alignment.TopCenter)
                    .padding(16.dp)
            )
        }
    }
}
```

---

## 🔄 **Future Enhancements**

### **Planned Features**

1. **Adaptive Quality**: Dynamic bitrate switching
2. **Network Monitoring**: Bandwidth-based fallback
3. **Device Profiling**: Per-device optimization
4. **Analytics Dashboard**: Performance metrics UI
5. **Custom Thresholds**: User-configurable limits

### **Integration Points**

- EPG system integration
- Favorites system
- Search functionality
- Theme support

---

## 📚 **References**

- [ExoPlayer Documentation](https://exoplayer.dev/)
- [Media3 Analytics](https://developer.android.com/reference/androidx/media3/exoplayer/analytics/AnalyticsListener)
- [FFmpeg Extension](https://github.com/google/ExoPlayer/tree/release-v2/ext/ffmpeg)

---

**Documentation Created**: December 2024  
**Last Updated**: December 2024  
**Status**: ✅ **COMPLETE**
