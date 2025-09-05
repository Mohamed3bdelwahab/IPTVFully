# 🎬 Auto Play Next Episode - Comprehensive Documentation

## 📋 **Overview**

The Auto Play Next Episode feature automatically plays the next episode in a series when the current episode finishes, providing a seamless viewing experience for users. This feature is configurable and can be enabled/disabled by the user through the app settings.

## 🎯 **Feature Description**

### **Core Functionality**
- **Automatic Episode Transition**: When an episode reaches its end, the next episode in the series automatically starts playing
- **User Control**: Users can enable/disable this feature through settings
- **Smart Detection**: Only triggers when an episode naturally ends (not when user manually stops)
- **Series Context**: Works within the context of a series/episode list
- **Error Handling**: Gracefully handles cases where next episode is unavailable

### **User Experience Benefits**
- **Seamless Viewing**: No interruption between episodes
- **Binge-Watching Friendly**: Perfect for marathon viewing sessions
- **Reduced Interaction**: Less need for manual episode selection
- **Consistent Experience**: Works across all supported video formats

## 🏗️ **Technical Implementation**

### **Architecture Overview**

```kotlin
// Main Components
├── AutoPlayManager.kt          # Core auto-play logic
├── EpisodePlaybackListener.kt  # Monitors playback state
├── SeriesEpisodeManager.kt     # Manages episode sequence
└── AutoPlaySettings.kt         # User preferences
```

### **1. AutoPlayManager.kt**

```kotlin
@Singleton
class AutoPlayManager @Inject constructor(
    private val valueHolder: ValueHolder,
    private val activityLogger: ActivityLogger,
    private val episodeManager: SeriesEpisodeManager
) {
    
    private var isAutoPlayEnabled: Boolean = false
    private var currentEpisodeId: String? = null
    private var currentSeriesId: String? = null
    private var episodeList: List<Episode> = emptyList()
    private var currentEpisodeIndex: Int = -1
    
    /**
     * Initialize auto-play for a series
     */
    fun initializeAutoPlay(seriesId: String, episodeId: String, episodes: List<Episode>) {
        this.currentSeriesId = seriesId
        this.currentEpisodeId = episodeId
        this.episodeList = episodes
        this.currentEpisodeIndex = findEpisodeIndex(episodeId)
        this.isAutoPlayEnabled = getAutoPlayPreference()
        
        activityLogger.logUserAction(
            className = "AutoPlayManager",
            methodName = "initializeAutoPlay",
            action = "AUTO_PLAY_INITIALIZED",
            parameters = mapOf(
                "series_id" to seriesId,
                "episode_id" to episodeId,
                "episodes_count" to episodes.size.toString(),
                "auto_play_enabled" to isAutoPlayEnabled.toString()
            )
        )
    }
    
    /**
     * Check if auto-play should trigger
     */
    fun shouldAutoPlayNext(): Boolean {
        return isAutoPlayEnabled && 
               hasNextEpisode() && 
               isCurrentEpisodeFinished()
    }
    
    /**
     * Get the next episode to play
     */
    fun getNextEpisode(): Episode? {
        if (!shouldAutoPlayNext()) return null
        
        val nextIndex = currentEpisodeIndex + 1
        return if (nextIndex < episodeList.size) {
            episodeList[nextIndex]
        } else null
    }
    
    /**
     * Update current episode index when user manually changes episode
     */
    fun updateCurrentEpisode(episodeId: String) {
        this.currentEpisodeId = episodeId
        this.currentEpisodeIndex = findEpisodeIndex(episodeId)
    }
    
    /**
     * Toggle auto-play setting
     */
    fun setAutoPlayEnabled(enabled: Boolean) {
        this.isAutoPlayEnabled = enabled
        valueHolder.setValue("auto_play", enabled.toString(), "CONFIG")
        
        activityLogger.logUserAction(
            className = "AutoPlayManager",
            methodName = "setAutoPlayEnabled",
            action = "AUTO_PLAY_TOGGLED",
            parameters = mapOf("enabled" to enabled.toString())
        )
    }
    
    private fun getAutoPlayPreference(): Boolean {
        return valueHolder.getValue("auto_play", "CONFIG")?.toBoolean() ?: false
    }
    
    private fun hasNextEpisode(): Boolean {
        return currentEpisodeIndex >= 0 && 
               currentEpisodeIndex < episodeList.size - 1
    }
    
    private fun isCurrentEpisodeFinished(): Boolean {
        // This would be called by the playback listener
        // when episode reaches end
        return true // Placeholder - actual implementation depends on ExoPlayer
    }
    
    private fun findEpisodeIndex(episodeId: String): Int {
        return episodeList.indexOfFirst { it.id == episodeId }
    }
}
```

### **2. EpisodePlaybackListener.kt**

```kotlin
class EpisodePlaybackListener @Inject constructor(
    private val autoPlayManager: AutoPlayManager,
    private val activityLogger: ActivityLogger
) : Player.Listener {
    
    private var isEpisodeFinished = false
    private var hasTriggeredAutoPlay = false
    
    override fun onPlaybackStateChanged(playbackState: Int) {
        when (playbackState) {
            Player.STATE_ENDED -> {
                handleEpisodeEnded()
            }
            Player.STATE_READY -> {
                // Reset flags when new episode starts
                isEpisodeFinished = false
                hasTriggeredAutoPlay = false
            }
        }
    }
    
    override fun onPlayerError(error: PlaybackException) {
        activityLogger.logError(
            className = "EpisodePlaybackListener",
            methodName = "onPlayerError",
            error = error,
            context = "Auto-play error handling"
        )
    }
    
    private fun handleEpisodeEnded() {
        if (isEpisodeFinished || hasTriggeredAutoPlay) return
        
        isEpisodeFinished = true
        
        activityLogger.logUserAction(
            className = "EpisodePlaybackListener",
            methodName = "handleEpisodeEnded",
            action = "EPISODE_ENDED",
            parameters = mapOf("auto_play_available" to autoPlayManager.shouldAutoPlayNext().toString())
        )
        
        if (autoPlayManager.shouldAutoPlayNext()) {
            triggerAutoPlay()
        }
    }
    
    private fun triggerAutoPlay() {
        hasTriggeredAutoPlay = true
        
        val nextEpisode = autoPlayManager.getNextEpisode()
        if (nextEpisode != null) {
            activityLogger.logUserAction(
                className = "EpisodePlaybackListener",
                methodName = "triggerAutoPlay",
                action = "AUTO_PLAY_TRIGGERED",
                parameters = mapOf(
                    "next_episode_id" to nextEpisode.id,
                    "next_episode_title" to nextEpisode.title
                )
            )
            
            // Notify the video player to load next episode
            onNextEpisodeRequested?.invoke(nextEpisode)
        }
    }
    
    var onNextEpisodeRequested: ((Episode) -> Unit)? = null
}
```

### **3. SeriesEpisodeManager.kt**

```kotlin
@Singleton
class SeriesEpisodeManager @Inject constructor(
    private val hydraApiService: HydraApiService,
    private val activityLogger: ActivityLogger
) {
    
    /**
     * Load episodes for a series
     */
    suspend fun loadEpisodes(seriesId: String): Result<List<Episode>> {
        return try {
            activityLogger.logApiCall(
                url = "get_series_info",
                method = "GET",
                parameters = mapOf("series_id" to seriesId)
            )
            
            val response = hydraApiService.getSeriesInfo(seriesId)
            val episodes = response.episodes ?: emptyList()
            
            activityLogger.logApiCall(
                url = "get_series_info",
                method = "GET",
                durationMs = 0, // Would be calculated in real implementation
                error = null
            )
            
            Result.success(episodes)
        } catch (e: Exception) {
            activityLogger.logError(
                className = "SeriesEpisodeManager",
                methodName = "loadEpisodes",
                error = e,
                context = "Failed to load episodes for series: $seriesId"
            )
            Result.failure(e)
        }
    }
    
    /**
     * Get episode by ID from loaded episodes
     */
    fun getEpisodeById(episodeId: String, episodes: List<Episode>): Episode? {
        return episodes.find { it.id == episodeId }
    }
    
    /**
     * Get next episode in sequence
     */
    fun getNextEpisode(currentEpisodeId: String, episodes: List<Episode>): Episode? {
        val currentIndex = episodes.indexOfFirst { it.id == currentEpisodeId }
        return if (currentIndex >= 0 && currentIndex < episodes.size - 1) {
            episodes[currentIndex + 1]
        } else null
    }
    
    /**
     * Get previous episode in sequence
     */
    fun getPreviousEpisode(currentEpisodeId: String, episodes: List<Episode>): Episode? {
        val currentIndex = episodes.indexOfFirst { it.id == currentEpisodeId }
        return if (currentIndex > 0) {
            episodes[currentIndex - 1]
        } else null
    }
}
```

### **4. AutoPlaySettings.kt**

```kotlin
@Composable
fun AutoPlaySettings(
    isAutoPlayEnabled: Boolean,
    onAutoPlayToggled: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Auto Play Settings",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = "Auto Play Next Episode",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Automatically play the next episode when current episode ends",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isAutoPlayEnabled,
                    onCheckedChange = onAutoPlayToggled
                )
            }
        }
    }
}
```

## 🎮 **Integration with Video Player**

### **AdvancedVideoPlayerViewModel.kt Integration**

```kotlin
@HiltViewModel
class AdvancedVideoPlayerViewModel @Inject constructor(
    private val autoPlayManager: AutoPlayManager,
    private val episodePlaybackListener: EpisodePlaybackListener,
    private val seriesEpisodeManager: SeriesEpisodeManager,
    // ... other dependencies
) : ViewModel() {
    
    private var _episodes = mutableStateOf<List<Episode>>(emptyList())
    val episodes: State<List<Episode>> = _episodes
    
    private var _isAutoPlayEnabled = mutableStateOf(false)
    val isAutoPlayEnabled: State<Boolean> = _isAutoPlayEnabled
    
    init {
        // Set up auto-play listener
        episodePlaybackListener.onNextEpisodeRequested = { nextEpisode ->
            loadNextEpisode(nextEpisode)
        }
    }
    
    /**
     * Initialize video player with auto-play support
     */
    fun initializePlayer(seriesId: String, episodeId: String) {
        viewModelScope.launch {
            // Load episodes for the series
            seriesEpisodeManager.loadEpisodes(seriesId)
                .onSuccess { episodeList ->
                    _episodes.value = episodeList
                    
                    // Initialize auto-play manager
                    autoPlayManager.initializeAutoPlay(seriesId, episodeId, episodeList)
                    _isAutoPlayEnabled.value = autoPlayManager.isAutoPlayEnabled
                    
                    // Load current episode
                    loadEpisode(episodeId)
                }
                .onFailure { error ->
                    // Handle error
                    handleError("Failed to load episodes", error)
                }
        }
    }
    
    /**
     * Load a specific episode
     */
    private fun loadEpisode(episodeId: String) {
        val episode = _episodes.value.find { it.id == episodeId }
        if (episode != null) {
            // Update auto-play manager with current episode
            autoPlayManager.updateCurrentEpisode(episodeId)
            
            // Load episode in ExoPlayer
            loadVideoUrl(episode.streamUrl)
        }
    }
    
    /**
     * Load next episode (called by auto-play)
     */
    private fun loadNextEpisode(nextEpisode: Episode) {
        activityLogger.logUserAction(
            className = "AdvancedVideoPlayerViewModel",
            methodName = "loadNextEpisode",
            action = "AUTO_PLAY_NEXT_EPISODE",
            parameters = mapOf(
                "episode_id" to nextEpisode.id,
                "episode_title" to nextEpisode.title
            )
        )
        
        loadEpisode(nextEpisode.id)
    }
    
    /**
     * Toggle auto-play setting
     */
    fun toggleAutoPlay() {
        val newValue = !_isAutoPlayEnabled.value
        _isAutoPlayEnabled.value = newValue
        autoPlayManager.setAutoPlayEnabled(newValue)
    }
    
    /**
     * Manually go to next episode
     */
    fun goToNextEpisode() {
        val currentEpisodeId = getCurrentEpisodeId()
        val nextEpisode = seriesEpisodeManager.getNextEpisode(
            currentEpisodeId, 
            _episodes.value
        )
        
        if (nextEpisode != null) {
            loadEpisode(nextEpisode.id)
        }
    }
    
    /**
     * Manually go to previous episode
     */
    fun goToPreviousEpisode() {
        val currentEpisodeId = getCurrentEpisodeId()
        val previousEpisode = seriesEpisodeManager.getPreviousEpisode(
            currentEpisodeId, 
            _episodes.value
        )
        
        if (previousEpisode != null) {
            loadEpisode(previousEpisode.id)
        }
    }
}
```

## ⚙️ **Configuration Management**

### **ValueHolder Integration**

```kotlin
// In ValueHolder.kt
object Keys {
    const val AUTO_PLAY = "auto_play"
    const val AUTO_PLAY_DELAY = "auto_play_delay"
    const val AUTO_PLAY_COUNTDOWN = "auto_play_countdown"
}

// Default values
object Defaults {
    const val AUTO_PLAY_DEFAULT = "false"
    const val AUTO_PLAY_DELAY_DEFAULT = "3" // seconds
    const val AUTO_PLAY_COUNTDOWN_DEFAULT = "true"
}

// Helper methods
fun ValueHolder.isAutoPlayEnabled(): Boolean {
    return getValue(Keys.AUTO_PLAY, "CONFIG")?.toBoolean() ?: false
}

fun ValueHolder.setAutoPlayEnabled(enabled: Boolean) {
    setValue(Keys.AUTO_PLAY, enabled.toString(), "CONFIG")
}

fun ValueHolder.getAutoPlayDelay(): Int {
    return getValue(Keys.AUTO_PLAY_DELAY, "CONFIG")?.toInt() ?: 3
}
```

## 🎨 **User Interface Components**

### **Auto-Play Countdown Overlay**

```kotlin
@Composable
fun AutoPlayCountdownOverlay(
    isVisible: Boolean,
    countdown: Int,
    nextEpisodeTitle: String,
    onCancel: () -> Unit,
    onPlayNow: () -> Unit
) {
    if (isVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black.copy(alpha = 0.7f))
                .clickable { onCancel() },
            contentAlignment = Alignment.Center
        ) {
            Card(
                modifier = Modifier
                    .padding(32.dp)
                    .clickable { }, // Prevent click through
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Next Episode",
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    
                    Text(
                        text = nextEpisodeTitle,
                        style = MaterialTheme.typography.bodyLarge,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    // Countdown circle
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primary,
                                shape = CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = countdown.toString(),
                            style = MaterialTheme.typography.headlineLarge,
                            color = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        OutlinedButton(
                            onClick = onCancel,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Cancel")
                        }
                        
                        Button(
                            onClick = onPlayNow,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text("Play Now")
                        }
                    }
                }
            }
        }
    }
}
```

### **Settings Screen Integration**

```kotlin
@Composable
fun VideoPlayerSettings(
    isAutoPlayEnabled: Boolean,
    onAutoPlayToggled: (Boolean) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            AutoPlaySettings(
                isAutoPlayEnabled = isAutoPlayEnabled,
                onAutoPlayToggled = onAutoPlayToggled
            )
        }
        
        // Other settings...
    }
}
```

## 🔄 **State Management**

### **Auto-Play State Flow**

```kotlin
// State classes
data class AutoPlayState(
    val isEnabled: Boolean = false,
    val isActive: Boolean = false,
    val countdown: Int = 0,
    val nextEpisode: Episode? = null,
    val isLoading: Boolean = false,
    val error: String? = null
)

// State management in ViewModel
class AutoPlayViewModel @Inject constructor(
    private val autoPlayManager: AutoPlayManager
) : ViewModel() {
    
    private val _state = mutableStateOf(AutoPlayState())
    val state: State<AutoPlayState> = _state
    
    fun enableAutoPlay() {
        _state.value = _state.value.copy(isEnabled = true)
        autoPlayManager.setAutoPlayEnabled(true)
    }
    
    fun disableAutoPlay() {
        _state.value = _state.value.copy(isEnabled = false)
        autoPlayManager.setAutoPlayEnabled(false)
    }
    
    fun startCountdown(nextEpisode: Episode) {
        _state.value = _state.value.copy(
            isActive = true,
            nextEpisode = nextEpisode,
            countdown = 10 // 10 second countdown
        )
        
        // Start countdown timer
        startCountdownTimer()
    }
    
    fun cancelAutoPlay() {
        _state.value = _state.value.copy(
            isActive = false,
            countdown = 0,
            nextEpisode = null
        )
    }
    
    private fun startCountdownTimer() {
        viewModelScope.launch {
            while (_state.value.countdown > 0 && _state.value.isActive) {
                delay(1000)
                _state.value = _state.value.copy(
                    countdown = _state.value.countdown - 1
                )
            }
            
            // Auto-play triggered
            if (_state.value.isActive) {
                triggerAutoPlay()
            }
        }
    }
    
    private fun triggerAutoPlay() {
        _state.value.nextEpisode?.let { episode ->
            // Trigger next episode playback
            onNextEpisodeRequested?.invoke(episode)
        }
        
        _state.value = _state.value.copy(
            isActive = false,
            countdown = 0,
            nextEpisode = null
        )
    }
    
    var onNextEpisodeRequested: ((Episode) -> Unit)? = null
}
```

## 📊 **Analytics & Tracking**

### **Activity Logging**

```kotlin
// Auto-play specific logging
class AutoPlayAnalytics @Inject constructor(
    private val activityLogger: ActivityLogger
) {
    
    fun logAutoPlayEnabled() {
        activityLogger.logUserAction(
            className = "AutoPlayAnalytics",
            methodName = "logAutoPlayEnabled",
            action = "AUTO_PLAY_ENABLED",
            parameters = emptyMap()
        )
    }
    
    fun logAutoPlayDisabled() {
        activityLogger.logUserAction(
            className = "AutoPlayAnalytics",
            methodName = "logAutoPlayDisabled",
            action = "AUTO_PLAY_DISABLED",
            parameters = emptyMap()
        )
    }
    
    fun logAutoPlayTriggered(episodeId: String, seriesId: String) {
        activityLogger.logUserAction(
            className = "AutoPlayAnalytics",
            methodName = "logAutoPlayTriggered",
            action = "AUTO_PLAY_TRIGGERED",
            parameters = mapOf(
                "episode_id" to episodeId,
                "series_id" to seriesId
            )
        )
    }
    
    fun logAutoPlayCancelled(episodeId: String, seriesId: String) {
        activityLogger.logUserAction(
            className = "AutoPlayAnalytics",
            methodName = "logAutoPlayCancelled",
            action = "AUTO_PLAY_CANCELLED",
            parameters = mapOf(
                "episode_id" to episodeId,
                "series_id" to seriesId
            )
        )
    }
    
    fun logAutoPlayError(error: String, episodeId: String) {
        activityLogger.logError(
            className = "AutoPlayAnalytics",
            methodName = "logAutoPlayError",
            error = Exception(error),
            context = "Auto-play error for episode: $episodeId"
        )
    }
}
```

## 🛡️ **Error Handling**

### **Error Scenarios & Solutions**

```kotlin
class AutoPlayErrorHandler @Inject constructor(
    private val activityLogger: ActivityLogger
) {
    
    fun handleAutoPlayError(error: AutoPlayError, context: AutoPlayContext) {
        when (error) {
            is AutoPlayError.NoNextEpisode -> {
                activityLogger.logUserAction(
                    className = "AutoPlayErrorHandler",
                    methodName = "handleAutoPlayError",
                    action = "AUTO_PLAY_NO_NEXT_EPISODE",
                    parameters = mapOf(
                        "current_episode_id" to context.currentEpisodeId,
                        "series_id" to context.seriesId
                    )
                )
                // Show "End of Series" message
            }
            
            is AutoPlayError.NetworkError -> {
                activityLogger.logError(
                    className = "AutoPlayErrorHandler",
                    methodName = "handleAutoPlayError",
                    error = error.originalError,
                    context = "Network error during auto-play"
                )
                // Retry with exponential backoff
                scheduleRetry(context)
            }
            
            is AutoPlayError.EpisodeLoadError -> {
                activityLogger.logError(
                    className = "AutoPlayErrorHandler",
                    methodName = "handleAutoPlayError",
                    error = error.originalError,
                    context = "Failed to load next episode: ${error.episodeId}"
                )
                // Skip to next available episode
                tryNextAvailableEpisode(context)
            }
        }
    }
    
    private fun scheduleRetry(context: AutoPlayContext) {
        // Implement retry logic with exponential backoff
    }
    
    private fun tryNextAvailableEpisode(context: AutoPlayContext) {
        // Try to find next available episode
    }
}

sealed class AutoPlayError : Exception() {
    object NoNextEpisode : AutoPlayError()
    data class NetworkError(val originalError: Throwable) : AutoPlayError()
    data class EpisodeLoadError(val episodeId: String, val originalError: Throwable) : AutoPlayError()
}

data class AutoPlayContext(
    val currentEpisodeId: String,
    val seriesId: String,
    val episodeList: List<Episode>
)
```

## 🧪 **Testing**

### **Unit Tests**

```kotlin
class AutoPlayManagerTest {
    
    @Mock
    private lateinit var valueHolder: ValueHolder
    
    @Mock
    private lateinit var activityLogger: ActivityLogger
    
    @Mock
    private lateinit var episodeManager: SeriesEpisodeManager
    
    private lateinit var autoPlayManager: AutoPlayManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        autoPlayManager = AutoPlayManager(valueHolder, activityLogger, episodeManager)
    }
    
    @Test
    fun `should auto play when enabled and next episode exists`() {
        // Given
        val episodes = listOf(
            Episode("1", "Episode 1", "url1"),
            Episode("2", "Episode 2", "url2")
        )
        every { valueHolder.getValue("auto_play", "CONFIG") } returns "true"
        
        autoPlayManager.initializeAutoPlay("series1", "1", episodes)
        
        // When
        val shouldAutoPlay = autoPlayManager.shouldAutoPlayNext()
        
        // Then
        assertTrue(shouldAutoPlay)
    }
    
    @Test
    fun `should not auto play when disabled`() {
        // Given
        val episodes = listOf(
            Episode("1", "Episode 1", "url1"),
            Episode("2", "Episode 2", "url2")
        )
        every { valueHolder.getValue("auto_play", "CONFIG") } returns "false"
        
        autoPlayManager.initializeAutoPlay("series1", "1", episodes)
        
        // When
        val shouldAutoPlay = autoPlayManager.shouldAutoPlayNext()
        
        // Then
        assertFalse(shouldAutoPlay)
    }
    
    @Test
    fun `should not auto play when no next episode`() {
        // Given
        val episodes = listOf(Episode("1", "Episode 1", "url1"))
        every { valueHolder.getValue("auto_play", "CONFIG") } returns "true"
        
        autoPlayManager.initializeAutoPlay("series1", "1", episodes)
        
        // When
        val shouldAutoPlay = autoPlayManager.shouldAutoPlayNext()
        
        // Then
        assertFalse(shouldAutoPlay)
    }
}
```

### **Integration Tests**

```kotlin
@HiltAndroidTest
class AutoPlayIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var autoPlayManager: AutoPlayManager
    
    @Inject
    lateinit var episodeManager: SeriesEpisodeManager
    
    @Test
    fun `auto play integration test`() {
        // Test complete auto-play flow
        // 1. Initialize with series and episodes
        // 2. Enable auto-play
        // 3. Simulate episode end
        // 4. Verify next episode loads
    }
}
```

## 📱 **Usage Examples**

### **Basic Implementation**

```kotlin
// In your video player activity
class VideoPlayerActivity : ComponentActivity() {
    
    @Inject
    lateinit var autoPlayManager: AutoPlayManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val seriesId = intent.getStringExtra("series_id")
        val episodeId = intent.getStringExtra("episode_id")
        
        // Initialize auto-play
        autoPlayManager.initializeAutoPlay(seriesId, episodeId, episodes)
        
        // Set up ExoPlayer with auto-play listener
        setupExoPlayer()
    }
    
    private fun setupExoPlayer() {
        val exoPlayer = ExoPlayer.Builder(this).build()
        
        // Add auto-play listener
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    if (autoPlayManager.shouldAutoPlayNext()) {
                        val nextEpisode = autoPlayManager.getNextEpisode()
                        nextEpisode?.let { loadEpisode(it) }
                    }
                }
            }
        })
    }
}
```

### **Settings Integration**

```kotlin
// In settings screen
@Composable
fun VideoPlayerSettingsScreen() {
    val viewModel: VideoPlayerSettingsViewModel = hiltViewModel()
    val isAutoPlayEnabled by viewModel.isAutoPlayEnabled.collectAsState()
    
    Column {
        Switch(
            checked = isAutoPlayEnabled,
            onCheckedChange = { viewModel.toggleAutoPlay() }
        )
        Text("Auto Play Next Episode")
    }
}
```

## 🚀 **Performance Considerations**

### **Memory Management**
- **Episode List Caching**: Cache episode lists to avoid repeated API calls
- **Lazy Loading**: Load episode metadata only when needed
- **Resource Cleanup**: Properly dispose of listeners and timers

### **Network Optimization**
- **Preloading**: Preload next episode metadata
- **Connection Monitoring**: Check network status before auto-play
- **Fallback Handling**: Graceful degradation when network is poor

### **Battery Optimization**
- **Wake Lock Management**: Minimize wake lock usage
- **Background Processing**: Use appropriate background processing limits
- **CPU Usage**: Optimize for low CPU usage during auto-play

## 🔧 **Configuration Options**

### **User Preferences**

```kotlin
data class AutoPlayPreferences(
    val isEnabled: Boolean = false,
    val countdownDuration: Int = 10, // seconds
    val showCountdown: Boolean = true,
    val skipIntro: Boolean = false,
    val skipCredits: Boolean = false,
    val maxConsecutiveEpisodes: Int = 5
)
```

### **Advanced Settings**

```kotlin
// Advanced auto-play configuration
object AutoPlayConfig {
    const val DEFAULT_COUNTDOWN = 10
    const val MIN_COUNTDOWN = 3
    const val MAX_COUNTDOWN = 30
    const val MAX_CONSECUTIVE_EPISODES = 10
    const val NETWORK_TIMEOUT = 5000L // milliseconds
    const val RETRY_ATTEMPTS = 3
}
```

## 📈 **Analytics & Metrics**

### **Key Metrics to Track**
- **Auto-play Usage Rate**: Percentage of users who enable auto-play
- **Auto-play Completion Rate**: Percentage of auto-play attempts that succeed
- **Episode Completion Rate**: How often users watch episodes to the end
- **Series Binge Rate**: How many episodes users watch in sequence
- **Auto-play Cancellation Rate**: How often users cancel auto-play

### **Performance Metrics**
- **Auto-play Trigger Time**: Time from episode end to next episode start
- **Network Success Rate**: Success rate of next episode loading
- **Error Recovery Time**: Time to recover from auto-play errors

## 🎯 **Future Enhancements**

### **Planned Features**
1. **Smart Auto-play**: AI-based prediction of user preferences
2. **Skip Intro Detection**: Automatic detection and skipping of intros
3. **Multi-series Auto-play**: Auto-play across different series
4. **Offline Auto-play**: Auto-play for downloaded content
5. **Social Auto-play**: Auto-play based on friends' viewing patterns

### **Advanced Features**
1. **Auto-play Scheduling**: Schedule auto-play for specific times
2. **Content Filtering**: Auto-play only certain types of content
3. **Parental Controls**: Auto-play restrictions for children
4. **Bandwidth Management**: Auto-play based on network conditions

---

## 📝 **Summary**

The Auto Play Next Episode feature provides a seamless viewing experience by automatically transitioning to the next episode when the current one ends. The implementation includes:

- **Configurable Settings**: Users can enable/disable auto-play
- **Smart Detection**: Only triggers on natural episode completion
- **Error Handling**: Graceful handling of network and loading errors
- **User Control**: Countdown timer with cancel option
- **Analytics**: Comprehensive tracking of auto-play usage
- **Performance Optimization**: Efficient memory and network usage

This feature enhances user engagement and provides a modern streaming experience similar to popular platforms like Netflix and YouTube.
