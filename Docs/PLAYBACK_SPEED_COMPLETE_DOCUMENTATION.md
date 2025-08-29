# ⚡ **Playback Speed Complete Documentation - IPTV Android Application**

## 📋 **Overview**
This document provides comprehensive documentation for the playback speed control system in the IPTV Android application, including all speed control methods, code examples, and implementation details.

---

## 🏗️ **Libraries & Dependencies Used**

### **Core Media Libraries**
```kotlin
// Media3 (ExoPlayer) - Core Video Player
implementation("androidx.media3:media3-exoplayer:1.3.1")
implementation("androidx.media3:media3-common:1.3.1")
implementation("androidx.media3:media3-ui:1.3.1")

// Jetpack Compose
implementation("androidx.compose.ui:ui:1.6.7")
implementation("androidx.compose.material3:material3:1.2.1")
implementation("androidx.compose.material:material-icons-extended:1.6.7")
```

---

## ⚡ **Speed Control System Architecture**

### **Speed Range & Constraints**
```kotlin
// Speed Range Configuration
const val MIN_SPEED = 0.25f
const val MAX_SPEED = 3.0f
const val SPEED_INCREMENT = 0.25f

// Speed Presets
val SPEED_PRESETS = listOf(
    0.5f,   // Half speed
    0.75f,  // Three-quarter speed
    1.0f,   // Normal speed
    1.25f,  // Quarter faster
    1.5f,   // Half faster
    1.75f,  // Three-quarter faster
    2.0f    // Double speed
)
```

### **Speed Control Methods**

#### **1. Incremental Speed Adjustment**
```kotlin
// Adjust speed by increment/decrement
fun adjustSpeed(exoPlayer: ExoPlayer, delta: Float, onSpeedChanged: (Float) -> Unit) {
    val currentSpeed = exoPlayer.playbackParameters.speed
    val newSpeed = (currentSpeed + delta).coerceIn(MIN_SPEED, MAX_SPEED)
    exoPlayer.setPlaybackParameters(PlaybackParameters(newSpeed))
    onSpeedChanged(newSpeed)
}

// Safe speed clamping utility
fun clampSpeed(speed: Float): Float {
    return speed.coerceIn(MIN_SPEED, MAX_SPEED)
}
```

#### **2. Direct Speed Setting**
```kotlin
// Set speed directly
fun setSpeed(exoPlayer: ExoPlayer, speed: Float, onSpeedChanged: (Float) -> Unit) {
    val clampedSpeed = clampSpeed(speed)
    exoPlayer.setPlaybackParameters(PlaybackParameters(clampedSpeed))
    onSpeedChanged(clampedSpeed)
}

// Reset to normal speed
fun resetSpeed(exoPlayer: ExoPlayer, onSpeedChanged: (Float) -> Unit) {
    setSpeed(exoPlayer, 1.0f, onSpeedChanged)
}
```

#### **3. Speed Preset Selection**
```kotlin
// Get speed preset by index
fun getSpeedPreset(index: Int): Float {
    return if (index in SPEED_PRESETS.indices) {
        SPEED_PRESETS[index]
    } else {
        1.0f // Default to normal speed
    }
}

// Apply speed preset
fun applySpeedPreset(exoPlayer: ExoPlayer, presetIndex: Int, onSpeedChanged: (Float) -> Unit) {
    val speed = getSpeedPreset(presetIndex)
    setSpeed(exoPlayer, speed, onSpeedChanged)
}
```

---

## 🎮 **TV Remote Speed Controls**

### **Speed Adjustment Buttons**
```kotlin
// Speed Up (+0.25x)
KeyEvent.KEYCODE_NUMPAD_ADD, KeyEvent.KEYCODE_PLUS -> {
    adjustSpeed(exoPlayer, +SPEED_INCREMENT) { playbackSpeed = it }
    pokeControls()
    true
}

// Speed Down (-0.25x)
KeyEvent.KEYCODE_NUMPAD_SUBTRACT, KeyEvent.KEYCODE_MINUS -> {
    adjustSpeed(exoPlayer, -SPEED_INCREMENT) { playbackSpeed = it }
    pokeControls()
    true
}

// D-pad Up/Down in Speed Menu
KeyEvent.KEYCODE_DPAD_UP -> {
    step(+SPEED_INCREMENT)
    true
}
KeyEvent.KEYCODE_DPAD_DOWN -> {
    step(-SPEED_INCREMENT)
    true
}
```

### **Speed Preset Number Keys**
```kotlin
// Number Keys 0-6 for Speed Presets
in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
    val map = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    val idx = ev.keyCode - KeyEvent.KEYCODE_0
    val sp = if (idx < map.size) map[idx] else 1.0f
    exoPlayer.setSpeed(sp)
    playbackSpeed = sp
    pokeControls()
    true
}

// Individual Speed Preset Mappings
KeyEvent.KEYCODE_0 -> { onSpeedSelect(0.5f); true }   // Half speed
KeyEvent.KEYCODE_1 -> { onSpeedSelect(0.75f); true }  // Three-quarter speed
KeyEvent.KEYCODE_2 -> { onSpeedSelect(1.0f); true }   // Normal speed
KeyEvent.KEYCODE_3 -> { onSpeedSelect(1.25f); true }  // Quarter faster
KeyEvent.KEYCODE_4 -> { onSpeedSelect(1.5f); true }   // Half faster
KeyEvent.KEYCODE_5 -> { onSpeedSelect(1.75f); true }  // Three-quarter faster
KeyEvent.KEYCODE_6 -> { onSpeedSelect(2.0f); true }   // Double speed
```

---

## 🎨 **Speed Menu UI Implementation**

### **SpeedMenuOverlay Component**
```kotlin
@Composable
fun SpeedMenuOverlay(
    currentSpeed: Float,
    onSpeedSelect: (Float) -> Unit,
    onClose: () -> Unit
) {
    var tempSpeed by remember { mutableStateOf(currentSpeed) }
    
    // Speed adjustment function
    fun step(delta: Float) {
        val newSpeed = (tempSpeed + delta).coerceIn(MIN_SPEED, MAX_SPEED)
        tempSpeed = newSpeed
        onSpeedSelect(newSpeed)
    }
    
    // Key event handler
    val onKeyEvent: (KeyEvent) -> Boolean = { ev ->
        when (ev.keyCode) {
            // Speed adjustment
            KeyEvent.KEYCODE_DPAD_UP -> { step(+SPEED_INCREMENT); true }
            KeyEvent.KEYCODE_DPAD_DOWN -> { step(-SPEED_INCREMENT); true }
            KeyEvent.KEYCODE_PLUS, KeyEvent.KEYCODE_NUMPAD_ADD -> { step(+SPEED_INCREMENT); true }
            KeyEvent.KEYCODE_MINUS, KeyEvent.KEYCODE_NUMPAD_SUBTRACT -> { step(-SPEED_INCREMENT); true }
            
            // Speed presets
            KeyEvent.KEYCODE_0 -> { onSpeedSelect(0.5f); true }
            KeyEvent.KEYCODE_1 -> { onSpeedSelect(0.75f); true }
            KeyEvent.KEYCODE_2 -> { onSpeedSelect(1.0f); true }
            KeyEvent.KEYCODE_3 -> { onSpeedSelect(1.25f); true }
            KeyEvent.KEYCODE_4 -> { onSpeedSelect(1.5f); true }
            KeyEvent.KEYCODE_5 -> { onSpeedSelect(1.75f); true }
            KeyEvent.KEYCODE_6 -> { onSpeedSelect(2.0f); true }
            
            // Navigation
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> true
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> { onClose(); true }
            
            else -> false
        }
    }
    
    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .onKeyEvent(onKeyEvent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp)
                .align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // Title
            Text(
                text = "Playback Speed",
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Current Speed Display
            Text(
                text = "${currentSpeed}x",
                style = MaterialTheme.typography.displayLarge,
                color = Color.White,
                textAlign = TextAlign.Center
            )
            
            // Speed Adjustment Buttons
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Speed Down Button
                IconButton(
                    onClick = { step(-SPEED_INCREMENT) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Speed Down",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                
                // Speed Up Button
                IconButton(
                    onClick = { step(+SPEED_INCREMENT) },
                    modifier = Modifier
                        .size(56.dp)
                        .background(
                            color = MaterialTheme.colorScheme.primary,
                            shape = CircleShape
                        )
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Speed Up",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            
            // Speed Presets
            LazyVerticalGrid(
                columns = GridCells.Fixed(3),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(SPEED_PRESETS.size) { index ->
                    val speed = SPEED_PRESETS[index]
                    val isSelected = currentSpeed == speed
                    
                    Button(
                        onClick = { onSpeedSelect(speed) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            }
                        ),
                        border = if (isSelected) {
                            BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                        } else {
                            BorderStroke(1.dp, MaterialTheme.colorScheme.outline)
                        }
                    ) {
                        Text(
                            text = "${speed}x",
                            color = if (isSelected) Color.White else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
            
            // Instructions
            Text(
                text = "Use D-pad Up/Down or +/- keys to adjust speed\nPress 0-6 for quick presets",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )
        }
    }
}
```

---

## 🔧 **Speed Control Integration**

### **PlayerScreen Integration**
```kotlin
@Composable
fun PlayerScreen(
    // ... other parameters
) {
    var playbackSpeed by remember { mutableStateOf(1.0f) }
    var showSpeedMenu by remember { mutableStateOf(false) }
    
    // Speed menu toggle
    val toggleSpeedMenu = {
        showSpeedMenu = !showSpeedMenu
        showPlaylist = false
        showSettings = false
    }
    
    // Speed selection handler
    val onSpeedSelect = { speed: Float ->
        setSpeed(exoPlayer, speed) { newSpeed ->
            playbackSpeed = newSpeed
        }
        showSpeedMenu = false
    }
    
    // Key event handler for speed controls
    val handleKey: (KeyEvent) -> Boolean = { ev ->
        when (ev.keyCode) {
            // Speed controls
            KeyEvent.KEYCODE_NUMPAD_ADD, KeyEvent.KEYCODE_PLUS -> {
                adjustSpeed(exoPlayer, +SPEED_INCREMENT) { playbackSpeed = it }
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT, KeyEvent.KEYCODE_MINUS -> {
                adjustSpeed(exoPlayer, -SPEED_INCREMENT) { playbackSpeed = it }
                pokeControls()
                true
            }
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9 -> {
                val map = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
                val idx = ev.keyCode - KeyEvent.KEYCODE_0
                val sp = if (idx < map.size) map[idx] else 1.0f
                setSpeed(exoPlayer, sp) { playbackSpeed = it }
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_INFO -> {
                toggleSpeedMenu()
                pokeControls()
                true
            }
            // ... other key mappings
        }
    }
    
    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Video player content
        
        // Speed menu overlay
        if (showSpeedMenu) {
            SpeedMenuOverlay(
                currentSpeed = playbackSpeed,
                onSpeedSelect = onSpeedSelect,
                onClose = { showSpeedMenu = false }
            )
        }
    }
}
```

---

## 📊 **Speed Control Features**

### **Speed Range & Presets**
| Speed | Description | Use Case |
|-------|-------------|----------|
| **0.25x** | Quarter speed | Very slow playback for detailed analysis |
| **0.5x** | Half speed | Slow playback for learning |
| **0.75x** | Three-quarter speed | Slightly slow for comprehension |
| **1.0x** | Normal speed | Standard playback |
| **1.25x** | Quarter faster | Slightly faster for efficiency |
| **1.5x** | Half faster | Faster playback for review |
| **1.75x** | Three-quarter faster | Quick review |
| **2.0x** | Double speed | Very fast for scanning |
| **2.5x** | Two and half speed | Ultra-fast scanning |
| **3.0x** | Triple speed | Maximum speed for overview |

### **Control Methods**
| Method | Description | Precision |
|--------|-------------|-----------|
| **D-pad Up/Down** | Incremental adjustment | ±0.25x |
| **Plus/Minus Keys** | Incremental adjustment | ±0.25x |
| **Number Keys (0-6)** | Preset selection | Exact preset |
| **Speed Menu** | Visual interface | ±0.25x or presets |
| **Touch Controls** | Button interface | ±0.25x or presets |

---

## 🎯 **Speed Control Benefits**

### **1. Learning & Education**
- **Slow Playback:** Better comprehension of complex content
- **Repeat Sections:** Easy to rewind and replay at slower speeds
- **Note Taking:** Time to write notes during playback

### **2. Time Efficiency**
- **Fast Playback:** Quickly scan through content
- **Review Mode:** Rapid review of familiar material
- **Content Discovery:** Fast browsing through episodes

### **3. Accessibility**
- **Hearing Impaired:** Slower speeds for better lip reading
- **Language Learning:** Slower speeds for non-native speakers
- **Cognitive Support:** Adjustable speeds for different processing needs

### **4. Professional Use**
- **Content Creation:** Precise control for editing
- **Analysis:** Detailed examination of content
- **Training:** Customized learning speeds

---

## 🔧 **Technical Implementation Details**

### **State Management**
```kotlin
// Speed state management
var playbackSpeed by remember { mutableStateOf(1.0f) }
var showSpeedMenu by remember { mutableStateOf(false) }

// Speed persistence (optional)
val dataStore = context.dataStore
val speedPreferenceKey = floatPreferencesKey("playback_speed")

// Load saved speed
LaunchedEffect(Unit) {
    dataStore.data.collect { preferences ->
        playbackSpeed = preferences[speedPreferenceKey] ?: 1.0f
    }
}

// Save speed changes
fun saveSpeed(speed: Float) {
    scope.launch {
        dataStore.edit { preferences ->
            preferences[speedPreferenceKey] = speed
        }
    }
}
```

### **Performance Optimization**
```kotlin
// Efficient speed changes
fun setSpeedOptimized(exoPlayer: ExoPlayer, speed: Float) {
    // Only update if speed actually changed
    if (exoPlayer.playbackParameters.speed != speed) {
        exoPlayer.setPlaybackParameters(PlaybackParameters(speed))
    }
}

// Debounced speed updates
var speedUpdateJob by remember { mutableStateOf<Job?>(null) }

fun debouncedSpeedUpdate(speed: Float) {
    speedUpdateJob?.cancel()
    speedUpdateJob = scope.launch {
        delay(100) // 100ms debounce
        setSpeed(exoPlayer, speed) { playbackSpeed = it }
    }
}
```

### **Error Handling**
```kotlin
// Safe speed setting with error handling
fun setSpeedSafely(exoPlayer: ExoPlayer, speed: Float, onSuccess: (Float) -> Unit, onError: (Exception) -> Unit) {
    try {
        val clampedSpeed = clampSpeed(speed)
        exoPlayer.setPlaybackParameters(PlaybackParameters(clampedSpeed))
        onSuccess(clampedSpeed)
    } catch (e: Exception) {
        onError(e)
        // Fallback to normal speed
        try {
            exoPlayer.setPlaybackParameters(PlaybackParameters(1.0f))
        } catch (_: Exception) {
            // Player might be in invalid state
        }
    }
}
```

---

## 📱 **Testing & Validation**

### **Speed Control Testing**
```kotlin
// Test speed range
fun testSpeedRange() {
    val testSpeeds = listOf(0.25f, 0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f, 2.5f, 3.0f)
    
    testSpeeds.forEach { speed ->
        setSpeed(exoPlayer, speed) { actualSpeed ->
            assert(actualSpeed == speed) { "Speed mismatch: expected $speed, got $actualSpeed" }
        }
    }
}

// Test speed clamping
fun testSpeedClamping() {
    val invalidSpeeds = listOf(-1.0f, 0.0f, 5.0f, 10.0f)
    
    invalidSpeeds.forEach { speed ->
        setSpeed(exoPlayer, speed) { actualSpeed ->
            assert(actualSpeed >= MIN_SPEED && actualSpeed <= MAX_SPEED) {
                "Speed not clamped: $actualSpeed"
            }
        }
    }
}
```

### **Performance Testing**
```kotlin
// Measure speed change latency
fun measureSpeedChangeLatency() {
    val startTime = System.currentTimeMillis()
    setSpeed(exoPlayer, 2.0f) {
        val latency = System.currentTimeMillis() - startTime
        println("Speed change latency: ${latency}ms")
    }
}
```

---

## 🚀 **Future Enhancements**

### **Planned Features**
1. **Custom Speed Presets:** User-defined speed presets
2. **Speed Profiles:** Different speed settings for different content types
3. **Auto-Speed:** Automatic speed adjustment based on content
4. **Speed Analytics:** Track user speed preferences
5. **Voice Speed Control:** Voice commands for speed changes

### **Performance Improvements**
1. **Smooth Speed Transitions:** Animated speed changes
2. **Speed Memory:** Remember speed per episode/series
3. **Batch Speed Changes:** Apply speed to multiple episodes
4. **Speed Synchronization:** Sync speed across devices

---

## 📋 **Troubleshooting**

### **Common Issues**

#### **1. Speed Not Changing**
```kotlin
// Check if player is ready
if (exoPlayer.playbackState == Player.STATE_READY) {
    setSpeed(exoPlayer, newSpeed) { /* handle success */ }
} else {
    // Wait for player to be ready
    exoPlayer.addListener(object : Player.Listener {
        override fun onPlaybackStateChanged(state: Int) {
            if (state == Player.STATE_READY) {
                setSpeed(exoPlayer, newSpeed) { /* handle success */ }
            }
        }
    })
}
```

#### **2. Speed Reset on Episode Change**
```kotlin
// Preserve speed across episodes
var globalPlaybackSpeed by remember { mutableStateOf(1.0f) }

fun playEpisode(exoPlayer: ExoPlayer, episode: Episode) {
    // ... episode loading logic
    
    // Apply saved speed
    setSpeed(exoPlayer, globalPlaybackSpeed) { speed ->
        globalPlaybackSpeed = speed
    }
}
```

#### **3. Speed Menu Not Responding**
```kotlin
// Check focus and key event handling
Box(
    modifier = Modifier
        .fillMaxSize()
        .onKeyEvent(onKeyEvent)  // Ensure key events are handled
        .focusable()             // Make sure it's focusable
) {
    // Speed menu content
}
```

---

**Documentation Created:** December 2024  
**Last Updated:** December 2024  
**Status:** ✅ **COMPLETE & TESTED**  
**Version:** 1.0.0
