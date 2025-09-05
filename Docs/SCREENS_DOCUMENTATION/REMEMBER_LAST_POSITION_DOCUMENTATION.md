# 📍 Remember Last Position - Comprehensive Documentation

## 📋 **Overview**

The Remember Last Position feature automatically saves and restores the playback position of videos, allowing users to resume watching from where they left off. This feature is essential for long-form content like movies and TV series episodes, providing a seamless viewing experience across app sessions.

## 🎯 **Feature Description**

### **Core Functionality**
- **Automatic Position Saving**: Continuously saves playback position during video playback
- **Session Persistence**: Remembers position across app restarts and device reboots
- **Smart Resume**: Automatically resumes from saved position when video is reopened
- **Multi-Content Support**: Works with movies, TV episodes, and live content
- **User Control**: Users can disable this feature if desired
- **Storage Optimization**: Efficient storage of position data with cleanup mechanisms

### **User Experience Benefits**
- **Seamless Continuity**: No need to manually seek to previous position
- **Time Saving**: Avoids rewatching already-seen content
- **Multi-Device Sync**: Position sync across devices (future enhancement)
- **Binge-Watching Friendly**: Perfect for interrupted viewing sessions
- **Content Discovery**: Encourages users to explore more content

## 🏗️ **Technical Implementation**

### **Architecture Overview**

```kotlin
// Main Components
├── PlaybackPositionManager.kt     # Core position management
├── PositionDatabase.kt            # Local storage for positions
├── PositionSyncService.kt         # Background sync service
├── ResumePlaybackHandler.kt       # Resume logic
└── PositionCleanupManager.kt      # Storage optimization
```

### **1. PlaybackPositionManager.kt**

```kotlin
@Singleton
class PlaybackPositionManager @Inject constructor(
    private val positionDatabase: PositionDatabase,
    private val activityLogger: ActivityLogger,
    private val valueHolder: ValueHolder
) {
    
    private val positionUpdateInterval = 5_000L // 5 seconds
    private var currentPosition: Long = 0L
    private var currentContentId: String? = null
    private var currentContentType: ContentType? = null
    private var isPositionSavingEnabled: Boolean = true
    
    /**
     * Initialize position tracking for content
     */
    fun initializePositionTracking(
        contentId: String,
        contentType: ContentType,
        duration: Long
    ) {
        this.currentContentId = contentId
        this.currentContentType = contentType
        this.isPositionSavingEnabled = getPositionSavingPreference()
        
        activityLogger.logUserAction(
            className = "PlaybackPositionManager",
            methodName = "initializePositionTracking",
            action = "POSITION_TRACKING_INITIALIZED",
            parameters = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name,
                "duration" to duration.toString(),
                "position_saving_enabled" to isPositionSavingEnabled.toString()
            )
        )
        
        if (isPositionSavingEnabled) {
            loadSavedPosition(contentId, contentType)
        }
    }
    
    /**
     * Update current playback position
     */
    fun updatePosition(position: Long) {
        if (!isPositionSavingEnabled || currentContentId == null) return
        
        this.currentPosition = position
        
        // Save position periodically to avoid excessive database writes
        if (position % positionUpdateInterval == 0L) {
            savePosition(currentContentId!!, currentContentType!!, position)
        }
    }
    
    /**
     * Save position immediately (called on pause/stop)
     */
    fun saveCurrentPosition() {
        if (!isPositionSavingEnabled || currentContentId == null) return
        
        savePosition(currentContentId!!, currentContentType!!, currentPosition)
    }
    
    /**
     * Get saved position for content
     */
    suspend fun getSavedPosition(contentId: String, contentType: ContentType): Long {
        return try {
            val position = positionDatabase.getPosition(contentId, contentType)
            
            activityLogger.logUserAction(
                className = "PlaybackPositionManager",
                methodName = "getSavedPosition",
                action = "POSITION_RETRIEVED",
                parameters = mapOf(
                    "content_id" to contentId,
                    "content_type" to contentType.name,
                    "saved_position" to position.toString()
                )
            )
            
            position
        } catch (e: Exception) {
            activityLogger.logError(
                className = "PlaybackPositionManager",
                methodName = "getSavedPosition",
                error = e,
                context = "Failed to retrieve position for content: $contentId"
            )
            0L
        }
    }
    
    /**
     * Clear saved position for content
     */
    fun clearPosition(contentId: String, contentType: ContentType) {
        try {
            positionDatabase.deletePosition(contentId, contentType)
            
            activityLogger.logUserAction(
                className = "PlaybackPositionManager",
                methodName = "clearPosition",
                action = "POSITION_CLEARED",
                parameters = mapOf(
                    "content_id" to contentId,
                    "content_type" to contentType.name
                )
            )
        } catch (e: Exception) {
            activityLogger.logError(
                className = "PlaybackPositionManager",
                methodName = "clearPosition",
                error = e,
                context = "Failed to clear position for content: $contentId"
            )
        }
    }
    
    /**
     * Mark content as completed
     */
    fun markAsCompleted(contentId: String, contentType: ContentType) {
        try {
            positionDatabase.markAsCompleted(contentId, contentType)
            
            activityLogger.logUserAction(
                className = "PlaybackPositionManager",
                methodName = "markAsCompleted",
                action = "CONTENT_COMPLETED",
                parameters = mapOf(
                    "content_id" to contentId,
                    "content_type" to contentType.name
                )
            )
        } catch (e: Exception) {
            activityLogger.logError(
                className = "PlaybackPositionManager",
                methodName = "markAsCompleted",
                error = e,
                context = "Failed to mark content as completed: $contentId"
            )
        }
    }
    
    /**
     * Toggle position saving preference
     */
    fun setPositionSavingEnabled(enabled: Boolean) {
        this.isPositionSavingEnabled = enabled
        valueHolder.setValue("remember_position", enabled.toString(), "CONFIG")
        
        activityLogger.logUserAction(
            className = "PlaybackPositionManager",
            methodName = "setPositionSavingEnabled",
            action = "POSITION_SAVING_TOGGLED",
            parameters = mapOf("enabled" to enabled.toString())
        )
    }
    
    private fun getPositionSavingPreference(): Boolean {
        return valueHolder.getValue("remember_position", "CONFIG")?.toBoolean() ?: true
    }
    
    private fun loadSavedPosition(contentId: String, contentType: ContentType) {
        // Load saved position asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val savedPosition = getSavedPosition(contentId, contentType)
            withContext(Dispatchers.Main) {
                onPositionLoaded?.invoke(savedPosition)
            }
        }
    }
    
    private fun savePosition(contentId: String, contentType: ContentType, position: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                positionDatabase.savePosition(contentId, contentType, position)
            } catch (e: Exception) {
                activityLogger.logError(
                    className = "PlaybackPositionManager",
                    methodName = "savePosition",
                    error = e,
                    context = "Failed to save position for content: $contentId"
                )
            }
        }
    }
    
    var onPositionLoaded: ((Long) -> Unit)? = null
}

enum class ContentType {
    MOVIE,
    EPISODE,
    LIVE_STREAM,
    VOD
}
```

### **2. PositionDatabase.kt**

```kotlin
@Database(
    entities = [PlaybackPositionEntity::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class PositionDatabase : RoomDatabase() {
    abstract fun positionDao(): PositionDao
}

@Entity(tableName = "playback_positions")
data class PlaybackPositionEntity(
    @PrimaryKey val contentId: String,
    val contentType: ContentType,
    val position: Long,
    val duration: Long,
    val lastUpdated: Long,
    val isCompleted: Boolean = false,
    val watchPercentage: Float = 0f
)

@Dao
interface PositionDao {
    
    @Query("SELECT * FROM playback_positions WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getPosition(contentId: String, contentType: ContentType): PlaybackPositionEntity?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PlaybackPositionEntity)
    
    @Query("DELETE FROM playback_positions WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deletePosition(contentId: String, contentType: ContentType)
    
    @Query("UPDATE playback_positions SET isCompleted = 1 WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun markAsCompleted(contentId: String, contentType: ContentType)
    
    @Query("SELECT * FROM playback_positions WHERE contentType = :contentType ORDER BY lastUpdated DESC")
    suspend fun getRecentPositions(contentType: ContentType): List<PlaybackPositionEntity>
    
    @Query("SELECT * FROM playback_positions WHERE isCompleted = 0 AND watchPercentage > 0.1 ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun getInProgressContent(limit: Int = 50): List<PlaybackPositionEntity>
    
    @Query("DELETE FROM playback_positions WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldPositions(cutoffTime: Long)
    
    @Query("SELECT COUNT(*) FROM playback_positions")
    suspend fun getPositionCount(): Int
}

class Converters {
    @TypeConverter
    fun fromContentType(contentType: ContentType): String {
        return contentType.name
    }
    
    @TypeConverter
    fun toContentType(contentType: String): ContentType {
        return ContentType.valueOf(contentType)
    }
}
```

### **3. ResumePlaybackHandler.kt**

```kotlin
@Singleton
class ResumePlaybackHandler @Inject constructor(
    private val positionManager: PlaybackPositionManager,
    private val activityLogger: ActivityLogger
) {
    
    /**
     * Handle resume playback for content
     */
    suspend fun handleResumePlayback(
        contentId: String,
        contentType: ContentType,
        duration: Long
    ): ResumeResult {
        return try {
            val savedPosition = positionManager.getSavedPosition(contentId, contentType)
            
            when {
                savedPosition == 0L -> {
                    // No saved position, start from beginning
                    ResumeResult.StartFromBeginning
                }
                
                savedPosition >= duration * 0.95 -> {
                    // Watched 95% or more, consider completed
                    positionManager.markAsCompleted(contentId, contentType)
                    ResumeResult.StartFromBeginning
                }
                
                savedPosition < 30_000 -> {
                    // Less than 30 seconds, start from beginning
                    ResumeResult.StartFromBeginning
                }
                
                else -> {
                    // Resume from saved position
                    ResumeResult.ResumeFromPosition(savedPosition)
                }
            }
        } catch (e: Exception) {
            activityLogger.logError(
                className = "ResumePlaybackHandler",
                methodName = "handleResumePlayback",
                error = e,
                context = "Failed to handle resume playback for content: $contentId"
            )
            ResumeResult.StartFromBeginning
        }
    }
    
    /**
     * Show resume dialog to user
     */
    fun shouldShowResumeDialog(savedPosition: Long, duration: Long): Boolean {
        val watchPercentage = (savedPosition.toFloat() / duration.toFloat()) * 100
        return savedPosition > 30_000 && watchPercentage < 95f
    }
    
    /**
     * Format position for display
     */
    fun formatPosition(position: Long): String {
        val hours = position / 3_600_000
        val minutes = (position % 3_600_000) / 60_000
        val seconds = (position % 60_000) / 1_000
        
        return when {
            hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
            else -> String.format("%d:%02d", minutes, seconds)
        }
    }
}

sealed class ResumeResult {
    object StartFromBeginning : ResumeResult()
    data class ResumeFromPosition(val position: Long) : ResumeResult()
}
```

### **4. PositionCleanupManager.kt**

```kotlin
@Singleton
class PositionCleanupManager @Inject constructor(
    private val positionDatabase: PositionDatabase,
    private val activityLogger: ActivityLogger
) {
    
    private val maxPositions = 1000
    private val cleanupInterval = 7 * 24 * 60 * 60 * 1000L // 7 days
    
    /**
     * Perform periodic cleanup of old positions
     */
    suspend fun performCleanup() {
        try {
            val cutoffTime = System.currentTimeMillis() - cleanupInterval
            
            // Delete old positions
            positionDatabase.positionDao().deleteOldPositions(cutoffTime)
            
            // Limit total positions
            val positionCount = positionDatabase.positionDao().getPositionCount()
            if (positionCount > maxPositions) {
                cleanupExcessPositions(positionCount - maxPositions)
            }
            
            activityLogger.logUserAction(
                className = "PositionCleanupManager",
                methodName = "performCleanup",
                action = "POSITION_CLEANUP_COMPLETED",
                parameters = mapOf(
                    "cutoff_time" to cutoffTime.toString(),
                    "max_positions" to maxPositions.toString()
                )
            )
        } catch (e: Exception) {
            activityLogger.logError(
                className = "PositionCleanupManager",
                methodName = "performCleanup",
                error = e,
                context = "Failed to perform position cleanup"
            )
        }
    }
    
    private suspend fun cleanupExcessPositions(excessCount: Int) {
        // Get oldest positions and delete them
        val allPositions = positionDatabase.positionDao().getRecentPositions(ContentType.MOVIE)
        val positionsToDelete = allPositions.takeLast(excessCount)
        
        positionsToDelete.forEach { position ->
            positionDatabase.positionDao().deletePosition(
                position.contentId,
                position.contentType
            )
        }
    }
    
    /**
     * Get storage statistics
     */
    suspend fun getStorageStats(): StorageStats {
        val positionCount = positionDatabase.positionDao().getPositionCount()
        val inProgressCount = positionDatabase.positionDao().getInProgressContent().size
        
        return StorageStats(
            totalPositions = positionCount,
            inProgressContent = inProgressCount,
            estimatedSizeKB = positionCount * 0.5 // Rough estimate
        )
    }
}

data class StorageStats(
    val totalPositions: Int,
    val inProgressContent: Int,
    val estimatedSizeKB: Double
)
```

## 🎮 **Integration with Video Player**

### **AdvancedVideoPlayerViewModel.kt Integration**

```kotlin
@HiltViewModel
class AdvancedVideoPlayerViewModel @Inject constructor(
    private val positionManager: PlaybackPositionManager,
    private val resumeHandler: ResumePlaybackHandler,
    private val activityLogger: ActivityLogger
    // ... other dependencies
) : ViewModel() {
    
    private var _shouldResume = mutableStateOf(false)
    val shouldResume: State<Boolean> = _shouldResume
    
    private var _resumePosition = mutableStateOf(0L)
    val resumePosition: State<Long> = _resumePosition
    
    private var _showResumeDialog = mutableStateOf(false)
    val showResumeDialog: State<Boolean> = _showResumeDialog
    
    /**
     * Initialize video player with position tracking
     */
    fun initializePlayer(contentId: String, contentType: ContentType, duration: Long) {
        viewModelScope.launch {
            // Initialize position tracking
            positionManager.initializePositionTracking(contentId, contentType, duration)
            
            // Set up position loaded callback
            positionManager.onPositionLoaded = { savedPosition ->
                handleSavedPosition(savedPosition, duration)
            }
            
            // Handle resume playback
            val resumeResult = resumeHandler.handleResumePlayback(contentId, contentType, duration)
            
            when (resumeResult) {
                is ResumeResult.StartFromBeginning -> {
                    _shouldResume.value = false
                    _resumePosition.value = 0L
                }
                is ResumeResult.ResumeFromPosition -> {
                    _shouldResume.value = true
                    _resumePosition.value = resumeResult.position
                    
                    if (resumeHandler.shouldShowResumeDialog(resumeResult.position, duration)) {
                        _showResumeDialog.value = true
                    } else {
                        // Auto-resume without dialog
                        resumeFromPosition(resumeResult.position)
                    }
                }
            }
        }
    }
    
    private fun handleSavedPosition(savedPosition: Long, duration: Long) {
        if (savedPosition > 0) {
            _shouldResume.value = true
            _resumePosition.value = savedPosition
            
            if (resumeHandler.shouldShowResumeDialog(savedPosition, duration)) {
                _showResumeDialog.value = true
            }
        }
    }
    
    /**
     * Resume from saved position
     */
    fun resumeFromPosition(position: Long) {
        _shouldResume.value = true
        _resumePosition.value = position
        _showResumeDialog.value = false
        
        activityLogger.logUserAction(
            className = "AdvancedVideoPlayerViewModel",
            methodName = "resumeFromPosition",
            action = "RESUME_FROM_POSITION",
            parameters = mapOf("position" to position.toString())
        )
    }
    
    /**
     * Start from beginning
     */
    fun startFromBeginning() {
        _shouldResume.value = false
        _resumePosition.value = 0L
        _showResumeDialog.value = false
        
        activityLogger.logUserAction(
            className = "AdvancedVideoPlayerViewModel",
            methodName = "startFromBeginning",
            action = "START_FROM_BEGINNING",
            parameters = emptyMap()
        )
    }
    
    /**
     * Update playback position
     */
    fun updatePlaybackPosition(position: Long) {
        positionManager.updatePosition(position)
    }
    
    /**
     * Save position on pause/stop
     */
    fun savePlaybackPosition() {
        positionManager.saveCurrentPosition()
    }
    
    /**
     * Mark content as completed
     */
    fun markContentCompleted() {
        val contentId = getCurrentContentId()
        val contentType = getCurrentContentType()
        
        if (contentId != null && contentType != null) {
            positionManager.markAsCompleted(contentId, contentType)
        }
    }
    
    /**
     * Clear saved position
     */
    fun clearSavedPosition() {
        val contentId = getCurrentContentId()
        val contentType = getCurrentContentType()
        
        if (contentId != null && contentType != null) {
            positionManager.clearPosition(contentId, contentType)
        }
    }
}
```

### **ExoPlayer Integration**

```kotlin
class VideoPlayerWithPositionTracking @Inject constructor(
    private val positionManager: PlaybackPositionManager
) {
    
    private var exoPlayer: ExoPlayer? = null
    private var positionUpdateJob: Job? = null
    
    fun initializePlayer(context: Context, contentId: String, contentType: ContentType) {
        exoPlayer = ExoPlayer.Builder(context).build()
        
        // Add position tracking listener
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        startPositionTracking(contentId, contentType)
                    }
                    Player.STATE_ENDED -> {
                        stopPositionTracking()
                        positionManager.markAsCompleted(contentId, contentType)
                    }
                    Player.STATE_PAUSED -> {
                        positionManager.saveCurrentPosition()
                    }
                }
            }
            
            override fun onPlayerError(error: PlaybackException) {
                stopPositionTracking()
            }
        })
    }
    
    private fun startPositionTracking(contentId: String, contentType: ContentType) {
        positionUpdateJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                exoPlayer?.currentPosition?.let { position ->
                    positionManager.updatePosition(position)
                }
                delay(5000) // Update every 5 seconds
            }
        }
    }
    
    private fun stopPositionTracking() {
        positionUpdateJob?.cancel()
        positionUpdateJob = null
    }
    
    fun seekToPosition(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    fun release() {
        stopPositionTracking()
        exoPlayer?.release()
        exoPlayer = null
    }
}
```

## 🎨 **User Interface Components**

### **Resume Dialog**

```kotlin
@Composable
fun ResumePlaybackDialog(
    isVisible: Boolean,
    savedPosition: Long,
    duration: Long,
    onResume: () -> Unit,
    onStartFromBeginning: () -> Unit,
    onDismiss: () -> Unit
) {
    if (isVisible) {
        AlertDialog(
            onDismissRequest = onDismiss,
            title = {
                Text("Resume Playback")
            },
            text = {
                Column {
                    Text(
                        text = "You were watching this content. Would you like to resume from where you left off?",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.PlayArrow,
                                contentDescription = null,
                                modifier = Modifier.padding(end = 8.dp)
                            )
                            
                            Column {
                                Text(
                                    text = "Resume from",
                                    style = MaterialTheme.typography.bodySmall
                                )
                                Text(
                                    text = formatTime(savedPosition),
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            Text(
                                text = "${((savedPosition.toFloat() / duration.toFloat()) * 100).toInt()}% watched",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            },
            confirmButton = {
                Button(onClick = onResume) {
                    Text("Resume")
                }
            },
            dismissButton = {
                TextButton(onClick = onStartFromBeginning) {
                    Text("Start from Beginning")
                }
            }
        )
    }
}

private fun formatTime(timeMs: Long): String {
    val hours = timeMs / 3_600_000
    val minutes = (timeMs % 3_600_000) / 60_000
    val seconds = (timeMs % 60_000) / 1_000
    
    return when {
        hours > 0 -> String.format("%d:%02d:%02d", hours, minutes, seconds)
        else -> String.format("%d:%02d", minutes, seconds)
    }
}
```

### **Continue Watching Section**

```kotlin
@Composable
fun ContinueWatchingSection(
    inProgressContent: List<PlaybackPositionEntity>,
    onContentSelected: (String, ContentType) -> Unit
) {
    if (inProgressContent.isNotEmpty()) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Continue Watching",
                style = MaterialTheme.typography.headlineSmall,
                modifier = Modifier.padding(bottom = 16.dp)
            )
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(inProgressContent) { position ->
                    ContinueWatchingCard(
                        position = position,
                        onClick = { onContentSelected(position.contentId, position.contentType) }
                    )
                }
            }
        }
    }
}

@Composable
fun ContinueWatchingCard(
    position: PlaybackPositionEntity,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .width(200.dp)
            .clickable { onClick() },
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column {
            // Thumbnail placeholder
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp)
                )
            }
            
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = "Content Title", // Would be loaded from content metadata
                    style = MaterialTheme.typography.bodyMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Progress bar
                LinearProgressIndicator(
                    progress = position.watchPercentage,
                    modifier = Modifier.fillMaxWidth()
                )
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "${(position.watchPercentage * 100).toInt()}% watched",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
```

### **Settings Integration**

```kotlin
@Composable
fun PositionSettings(
    isPositionSavingEnabled: Boolean,
    onPositionSavingToggled: (Boolean) -> Unit,
    onClearAllPositions: () -> Unit
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
                text = "Playback Position",
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
                        text = "Remember Last Position",
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Text(
                        text = "Automatically save and restore playback position",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                Switch(
                    checked = isPositionSavingEnabled,
                    onCheckedChange = onPositionSavingToggled
                )
            }
            
            if (isPositionSavingEnabled) {
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedButton(
                    onClick = onClearAllPositions,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Saved Positions")
                }
            }
        }
    }
}
```

## ⚙️ **Configuration Management**

### **ValueHolder Integration**

```kotlin
// In ValueHolder.kt
object Keys {
    const val REMEMBER_POSITION = "remember_position"
    const val POSITION_UPDATE_INTERVAL = "position_update_interval"
    const val AUTO_RESUME_THRESHOLD = "auto_resume_threshold"
    const val MAX_SAVED_POSITIONS = "max_saved_positions"
}

// Default values
object Defaults {
    const val REMEMBER_POSITION_DEFAULT = "true"
    const val POSITION_UPDATE_INTERVAL_DEFAULT = "5000" // milliseconds
    const val AUTO_RESUME_THRESHOLD_DEFAULT = "30000" // milliseconds
    const val MAX_SAVED_POSITIONS_DEFAULT = "1000"
}

// Helper methods
fun ValueHolder.isPositionSavingEnabled(): Boolean {
    return getValue(Keys.REMEMBER_POSITION, "CONFIG")?.toBoolean() ?: true
}

fun ValueHolder.setPositionSavingEnabled(enabled: Boolean) {
    setValue(Keys.REMEMBER_POSITION, enabled.toString(), "CONFIG")
}

fun ValueHolder.getPositionUpdateInterval(): Long {
    return getValue(Keys.POSITION_UPDATE_INTERVAL, "CONFIG")?.toLong() ?: 5000L
}
```

## 📊 **Analytics & Tracking**

### **Position Analytics**

```kotlin
class PositionAnalytics @Inject constructor(
    private val activityLogger: ActivityLogger
) {
    
    fun logPositionSaved(contentId: String, contentType: ContentType, position: Long, duration: Long) {
        val watchPercentage = (position.toFloat() / duration.toFloat()) * 100
        
        activityLogger.logUserAction(
            className = "PositionAnalytics",
            methodName = "logPositionSaved",
            action = "POSITION_SAVED",
            parameters = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name,
                "position" to position.toString(),
                "duration" to duration.toString(),
                "watch_percentage" to watchPercentage.toString()
            )
        )
    }
    
    fun logPositionResumed(contentId: String, contentType: ContentType, position: Long) {
        activityLogger.logUserAction(
            className = "PositionAnalytics",
            methodName = "logPositionResumed",
            action = "POSITION_RESUMED",
            parameters = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name,
                "resume_position" to position.toString()
            )
        )
    }
    
    fun logContentCompleted(contentId: String, contentType: ContentType, totalWatchTime: Long) {
        activityLogger.logUserAction(
            className = "PositionAnalytics",
            methodName = "logContentCompleted",
            action = "CONTENT_COMPLETED",
            parameters = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name,
                "total_watch_time" to totalWatchTime.toString()
            )
        )
    }
    
    fun logPositionCleared(contentId: String, contentType: ContentType) {
        activityLogger.logUserAction(
            className = "PositionAnalytics",
            methodName = "logPositionCleared",
            action = "POSITION_CLEARED",
            parameters = mapOf(
                "content_id" to contentId,
                "content_type" to contentType.name
            )
        )
    }
}
```

## 🛡️ **Error Handling**

### **Error Scenarios & Solutions**

```kotlin
class PositionErrorHandler @Inject constructor(
    private val activityLogger: ActivityLogger
) {
    
    fun handlePositionError(error: PositionError, context: PositionContext) {
        when (error) {
            is PositionError.DatabaseError -> {
                activityLogger.logError(
                    className = "PositionErrorHandler",
                    methodName = "handlePositionError",
                    error = error.originalError,
                    context = "Database error for content: ${context.contentId}"
                )
                // Fallback to in-memory storage
                fallbackToMemoryStorage(context)
            }
            
            is PositionError.StorageFullError -> {
                activityLogger.logError(
                    className = "PositionErrorHandler",
                    methodName = "handlePositionError",
                    error = error.originalError,
                    context = "Storage full, performing cleanup"
                )
                // Trigger cleanup
                triggerStorageCleanup()
            }
            
            is PositionError.CorruptedDataError -> {
                activityLogger.logError(
                    className = "PositionErrorHandler",
                    methodName = "handlePositionError",
                    error = error.originalError,
                    context = "Corrupted position data for content: ${context.contentId}"
                )
                // Clear corrupted data
                clearCorruptedData(context)
            }
        }
    }
    
    private fun fallbackToMemoryStorage(context: PositionContext) {
        // Implement in-memory fallback
    }
    
    private fun triggerStorageCleanup() {
        // Trigger cleanup process
    }
    
    private fun clearCorruptedData(context: PositionContext) {
        // Clear corrupted position data
    }
}

sealed class PositionError : Exception() {
    data class DatabaseError(val originalError: Throwable) : PositionError()
    data class StorageFullError(val originalError: Throwable) : PositionError()
    data class CorruptedDataError(val originalError: Throwable) : PositionError()
}

data class PositionContext(
    val contentId: String,
    val contentType: ContentType,
    val position: Long
)
```

## 🧪 **Testing**

### **Unit Tests**

```kotlin
class PlaybackPositionManagerTest {
    
    @Mock
    private lateinit var positionDatabase: PositionDatabase
    
    @Mock
    private lateinit var activityLogger: ActivityLogger
    
    @Mock
    private lateinit var valueHolder: ValueHolder
    
    private lateinit var positionManager: PlaybackPositionManager
    
    @Before
    fun setup() {
        MockKAnnotations.init(this)
        positionManager = PlaybackPositionManager(positionDatabase, activityLogger, valueHolder)
    }
    
    @Test
    fun `should save position when enabled`() {
        // Given
        every { valueHolder.getValue("remember_position", "CONFIG") } returns "true"
        coEvery { positionDatabase.getPosition(any(), any()) } returns null
        
        positionManager.initializePositionTracking("content1", ContentType.MOVIE, 100000L)
        
        // When
        positionManager.updatePosition(50000L)
        positionManager.saveCurrentPosition()
        
        // Then
        coVerify { positionDatabase.savePosition(any(), any(), 50000L) }
    }
    
    @Test
    fun `should not save position when disabled`() {
        // Given
        every { valueHolder.getValue("remember_position", "CONFIG") } returns "false"
        
        positionManager.initializePositionTracking("content1", ContentType.MOVIE, 100000L)
        
        // When
        positionManager.updatePosition(50000L)
        positionManager.saveCurrentPosition()
        
        // Then
        coVerify(exactly = 0) { positionDatabase.savePosition(any(), any(), any()) }
    }
    
    @Test
    fun `should retrieve saved position`() = runTest {
        // Given
        val savedPosition = PlaybackPositionEntity(
            contentId = "content1",
            contentType = ContentType.MOVIE,
            position = 50000L,
            duration = 100000L,
            lastUpdated = System.currentTimeMillis()
        )
        coEvery { positionDatabase.getPosition("content1", ContentType.MOVIE) } returns savedPosition
        
        // When
        val position = positionManager.getSavedPosition("content1", ContentType.MOVIE)
        
        // Then
        assertEquals(50000L, position)
    }
}
```

### **Integration Tests**

```kotlin
@HiltAndroidTest
class PositionIntegrationTest {
    
    @get:Rule
    val hiltRule = HiltAndroidRule(this)
    
    @Inject
    lateinit var positionManager: PlaybackPositionManager
    
    @Inject
    lateinit var positionDatabase: PositionDatabase
    
    @Test
    fun `position persistence integration test`() = runTest {
        // Test complete position persistence flow
        // 1. Save position
        // 2. Retrieve position
        // 3. Verify persistence across app restarts
    }
}
```

## 📱 **Usage Examples**

### **Basic Implementation**

```kotlin
// In your video player activity
class VideoPlayerActivity : ComponentActivity() {
    
    @Inject
    lateinit var positionManager: PlaybackPositionManager
    
    @Inject
    lateinit var resumeHandler: ResumePlaybackHandler
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        val contentId = intent.getStringExtra("content_id")
        val contentType = ContentType.valueOf(intent.getStringExtra("content_type") ?: "MOVIE")
        
        // Initialize position tracking
        positionManager.initializePositionTracking(contentId, contentType, duration)
        
        // Set up ExoPlayer with position tracking
        setupExoPlayer()
    }
    
    private fun setupExoPlayer() {
        val exoPlayer = ExoPlayer.Builder(this).build()
        
        // Add position tracking listener
        exoPlayer.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        // Check for saved position
                        checkForSavedPosition()
                    }
                    Player.STATE_ENDED -> {
                        positionManager.markAsCompleted(contentId, contentType)
                    }
                    Player.STATE_PAUSED -> {
                        positionManager.saveCurrentPosition()
                    }
                }
            }
        })
    }
    
    private fun checkForSavedPosition() {
        lifecycleScope.launch {
            val savedPosition = positionManager.getSavedPosition(contentId, contentType)
            if (savedPosition > 0) {
                // Show resume dialog or auto-resume
                showResumeDialog(savedPosition)
            }
        }
    }
}
```

### **Settings Integration**

```kotlin
// In settings screen
@Composable
fun VideoPlayerSettingsScreen() {
    val viewModel: VideoPlayerSettingsViewModel = hiltViewModel()
    val isPositionSavingEnabled by viewModel.isPositionSavingEnabled.collectAsState()
    
    Column {
        Switch(
            checked = isPositionSavingEnabled,
            onCheckedChange = { viewModel.togglePositionSaving() }
        )
        Text("Remember Last Position")
    }
}
```

## 🚀 **Performance Considerations**

### **Database Optimization**
- **Batch Updates**: Batch position updates to reduce database writes
- **Indexing**: Proper database indexing for fast queries
- **Connection Pooling**: Efficient database connection management
- **Background Processing**: Use background threads for database operations

### **Memory Management**
- **Position Caching**: Cache frequently accessed positions
- **Lazy Loading**: Load positions only when needed
- **Resource Cleanup**: Proper cleanup of listeners and timers
- **Memory Monitoring**: Monitor memory usage for position data

### **Storage Optimization**
- **Data Compression**: Compress position data when possible
- **Cleanup Policies**: Automatic cleanup of old positions
- **Size Limits**: Limit total number of saved positions
- **Efficient Queries**: Optimize database queries for performance

## 🔧 **Configuration Options**

### **User Preferences**

```kotlin
data class PositionPreferences(
    val isEnabled: Boolean = true,
    val updateInterval: Long = 5000L, // milliseconds
    val autoResumeThreshold: Long = 30000L, // milliseconds
    val maxSavedPositions: Int = 1000,
    val cleanupInterval: Long = 7 * 24 * 60 * 60 * 1000L, // 7 days
    val showResumeDialog: Boolean = true
)
```

### **Advanced Settings**

```kotlin
// Advanced position configuration
object PositionConfig {
    const val DEFAULT_UPDATE_INTERVAL = 5000L
    const val MIN_UPDATE_INTERVAL = 1000L
    const val MAX_UPDATE_INTERVAL = 30000L
    const val DEFAULT_AUTO_RESUME_THRESHOLD = 30000L
    const val MIN_AUTO_RESUME_THRESHOLD = 5000L
    const val MAX_AUTO_RESUME_THRESHOLD = 300000L
    const val DEFAULT_MAX_POSITIONS = 1000
    const val MIN_MAX_POSITIONS = 100
    const val MAX_MAX_POSITIONS = 10000
}
```

## 📈 **Analytics & Metrics**

### **Key Metrics to Track**
- **Position Save Rate**: Percentage of videos with saved positions
- **Resume Rate**: Percentage of users who resume from saved positions
- **Completion Rate**: How often users complete videos after resuming
- **Storage Usage**: Amount of storage used for position data
- **Error Rate**: Frequency of position-related errors

### **Performance Metrics**
- **Position Save Time**: Time to save position data
- **Position Load Time**: Time to load saved positions
- **Database Query Time**: Performance of position queries
- **Storage Cleanup Time**: Time for cleanup operations

## 🎯 **Future Enhancements**

### **Planned Features**
1. **Cloud Sync**: Sync positions across devices
2. **Smart Resume**: AI-based resume point detection
3. **Social Features**: Share resume points with friends
4. **Offline Support**: Position tracking for downloaded content
5. **Advanced Analytics**: Detailed viewing behavior analysis

### **Advanced Features**
1. **Position Sharing**: Share specific moments in videos
2. **Bookmark System**: Save multiple positions per video
3. **Watch History**: Complete viewing history with positions
4. **Recommendation Engine**: Content recommendations based on viewing patterns
5. **Parental Controls**: Position restrictions for children's content

---

## 📝 **Summary**

The Remember Last Position feature provides a seamless viewing experience by automatically saving and restoring playback positions. The implementation includes:

- **Automatic Position Tracking**: Continuous saving of playback positions
- **Smart Resume Logic**: Intelligent decision-making for when to resume
- **User Control**: Configurable settings and manual position management
- **Storage Optimization**: Efficient storage with automatic cleanup
- **Error Handling**: Robust error handling and recovery mechanisms
- **Analytics**: Comprehensive tracking of position-related activities

This feature significantly enhances user experience by eliminating the need to manually seek to previous positions and encourages continued engagement with content.
