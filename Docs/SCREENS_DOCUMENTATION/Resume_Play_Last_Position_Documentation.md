# Resume Play Last Position - Complete Implementation Guide

## Overview

This document provides a comprehensive guide to the resume play last position functionality implemented in the IPTV Android application. The feature allows users to automatically resume video playback from where they left off, providing a seamless viewing experience across app sessions.

## Architecture Overview

The resume play functionality is built using a multi-layered architecture:

```
┌─────────────────────────────────────────────────────────────┐
│                    UI Layer (PlayerScreen.kt)              │
├─────────────────────────────────────────────────────────────┤
│              PlaybackProgressRepository                     │
├─────────────────────────────────────────────────────────────┤
│                    Data Storage Layer                       │
│              (Room Database / SharedPreferences)            │
└─────────────────────────────────────────────────────────────┘
```

## Core Components

### 1. PlayerScreen.kt - Main Implementation

The main player screen contains the complete resume play logic:

#### Key State Variables
```kotlin
// Resume position tracking
var resumePosition by remember { mutableStateOf(0L) }

// Current playback state
var currentPosition by remember { mutableStateOf(0L) }
var duration by remember { mutableStateOf(0L) }
var isPlaying by remember { mutableStateOf(true) }

// Episode information
var currentEpisode by remember { mutableStateOf<EpisodeInfo?>(null) }
```

#### Dependency Injection Setup
```kotlin
// Hilt entry point for dependency injection
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PlayerScreenEntryPoint {
    fun hydraApiService(): HydraApiService
    fun playbackProgressRepository(): PlaybackProgressRepository
}

// Repository injection
val playbackProgressRepository = remember { entry.playbackProgressRepository() }
```

### 2. Resume Position Loading Logic

The resume position is loaded when an episode is selected:

```kotlin
// ---- Load resume ----
LaunchedEffect(episodeId) {
    resumePosition = runCatching {
        if (episodeId == null) 0L
        else playbackProgressRepository.getProgress(episodeId)?.let { 
            if (!it.completed) it.position else 0L 
        } ?: 0L
    }.getOrElse { 0L }
}
```

**Key Features:**
- Only loads resume position if episode is not completed
- Returns 0L (start from beginning) if episode is completed
- Handles null episodeId gracefully
- Uses runCatching for error handling

### 3. ExoPlayer Integration

The resume position is applied when the player becomes ready:

```kotlin
val exoPlayer = remember(url) {
    ExoPlayer.Builder(context)
        .setRenderersFactory(renderersFactory)
        .setLoadControl(loadControl)
        .setTrackSelector(trackSelector)
        .build()
        .apply {
            addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(state: Int) {
                    when (state) {
                        Player.STATE_READY -> {
                            Log.i("PlayerScreen", "✅ Playback Ready - Duration: ${this@apply.duration}ms")
                            duration = this@apply.duration.coerceAtLeast(0L)
                            isPlaying = this@apply.isPlaying
                            isBuffering = false
                            setSpeed(playbackSpeed) // re-apply after prepare
                            
                            // Apply resume position when player is ready
                            if (resumePosition > 0L) {
                                seekTo(resumePosition)
                                resumePosition = 0L // Reset to prevent re-seeking
                            }
                        }
                        // ... other states
                    }
                }
            })
        }
}
```

**Critical Implementation Details:**
- Resume position is applied only when `Player.STATE_READY` is reached
- Position is reset to 0L after seeking to prevent multiple seeks
- Speed is re-applied after player preparation

### 4. Progress Persistence Logic

The application continuously saves playback progress:

```kotlin
// ---- Persist progress ----
LaunchedEffect(exoPlayer, episodeId, currentEpisode) {
    while (true) {
        delay(10_000) // Save every 10 seconds
        val curEp = currentEpisode ?: continue
        if (episodeId != null && isPlaying) {
            runCatching {
                val pos = exoPlayer.currentPosition
                val dur = exoPlayer.duration
                val completed = playbackProgressRepository.isEpisodeCompleted(pos, dur)
                
                playbackProgressRepository.saveProgress(
                    episodeId = episodeId,
                    seriesId = seriesId ?: "",
                    episodeTitle = curEp.title,
                    seriesTitle = curEp.season,
                    position = pos,
                    duration = dur,
                    completed = completed
                )
                
                if (completed) {
                    playbackProgressRepository.markAsCompleted(episodeId)
                }
            }
        }
    }
}
```

**Persistence Features:**
- Saves progress every 10 seconds during playback
- Only saves when video is actively playing
- Determines completion status automatically
- Marks episodes as completed when finished
- Includes comprehensive episode metadata

### 5. Episode Completion Detection

The system automatically detects when an episode is completed:

```kotlin
val completed = playbackProgressRepository.isEpisodeCompleted(pos, dur)
```

**Completion Logic:**
- Compares current position with total duration
- Typically considers episode complete when position is within 95-98% of duration
- Prevents false completion detection for very short videos

## Data Models

### EpisodeInfo Data Class
```kotlin
data class EpisodeInfo(
    val id: String,           // Unique episode identifier
    val title: String,        // Episode title
    val season: String,       // Season information
    val extension: String,    // Video file extension
    val streamUrl: String     // Streaming URL
)
```

### PlaybackProgress Data Structure
The repository manages progress data with the following structure:
```kotlin
data class PlaybackProgress(
    val episodeId: String,    // Unique episode identifier
    val seriesId: String,     // Series identifier
    val episodeTitle: String, // Episode title
    val seriesTitle: String,  // Series title
    val position: Long,       // Current position in milliseconds
    val duration: Long,       // Total duration in milliseconds
    val completed: Boolean,   // Whether episode is completed
    val lastWatched: Long     // Timestamp of last watch
)
```

## Repository Interface

The `PlaybackProgressRepository` provides the following key methods:

### Core Methods
```kotlin
interface PlaybackProgressRepository {
    // Retrieve progress for a specific episode
    suspend fun getProgress(episodeId: String): PlaybackProgress?
    
    // Save current progress
    suspend fun saveProgress(
        episodeId: String,
        seriesId: String,
        episodeTitle: String,
        seriesTitle: String,
        position: Long,
        duration: Long,
        completed: Boolean
    )
    
    // Check if episode is completed based on position/duration
    fun isEpisodeCompleted(position: Long, duration: Long): Boolean
    
    // Mark episode as completed
    suspend fun markAsCompleted(episodeId: String)
    
    // Get all progress for a series
    suspend fun getSeriesProgress(seriesId: String): List<PlaybackProgress>
    
    // Clear progress for an episode
    suspend fun clearProgress(episodeId: String)
}
```

## Implementation Flow

### 1. Episode Selection Flow
```
User selects episode
        ↓
Load episode metadata
        ↓
Check for existing progress
        ↓
Set resumePosition variable
        ↓
Initialize ExoPlayer
        ↓
Wait for Player.STATE_READY
        ↓
Apply resume position (if > 0)
        ↓
Start playback
```

### 2. Progress Saving Flow
```
Video starts playing
        ↓
Wait 10 seconds
        ↓
Check if still playing
        ↓
Get current position & duration
        ↓
Calculate completion status
        ↓
Save progress to repository
        ↓
Mark as completed if applicable
        ↓
Repeat every 10 seconds
```

### 3. Resume Flow
```
App launches
        ↓
User navigates to episode
        ↓
Repository.getProgress(episodeId)
        ↓
Check if episode completed
        ↓
Return saved position or 0L
        ↓
Set resumePosition variable
        ↓
Player becomes ready
        ↓
Seek to resumePosition
        ↓
Continue playback
```

## Error Handling

The implementation includes comprehensive error handling:

### 1. Repository Access Errors
```kotlin
resumePosition = runCatching {
    if (episodeId == null) 0L
    else playbackProgressRepository.getProgress(episodeId)?.let { 
        if (!it.completed) it.position else 0L 
    } ?: 0L
}.getOrElse { 0L } // Fallback to start from beginning
```

### 2. Progress Saving Errors
```kotlin
runCatching {
    // Save progress logic
    playbackProgressRepository.saveProgress(...)
}.onFailure { exception ->
    Log.e("PlayerScreen", "Failed to save progress: ${exception.message}")
}
```

### 3. Player State Errors
```kotlin
override fun onPlayerError(error: PlaybackException) {
    Log.e("PlayerScreen", "❌ Playback Error: ${error.message}")
    errorMessage = "Playback error: ${error.message ?: "Unknown"}"
    isBuffering = false
}
```

## Performance Considerations

### 1. Efficient Progress Saving
- **10-second intervals**: Balances data freshness with performance
- **Only during playback**: Prevents unnecessary saves when paused
- **Background operations**: Uses coroutines for non-blocking saves

### 2. Memory Management
- **State variables**: Uses `remember` for UI state persistence
- **Repository caching**: Likely implements caching for frequent access
- **Cleanup**: Proper disposal of ExoPlayer resources

### 3. Database Optimization
- **Indexed queries**: Episode ID should be indexed for fast lookups
- **Batch operations**: Multiple progress updates can be batched
- **Data retention**: Old progress data can be cleaned up periodically

## Configuration Options

### 1. Save Interval
```kotlin
delay(10_000) // 10 seconds - can be adjusted based on needs
```

### 2. Completion Threshold
```kotlin
// In repository implementation
fun isEpisodeCompleted(position: Long, duration: Long): Boolean {
    return duration > 0 && position >= (duration * 0.95) // 95% threshold
}
```

### 3. Minimum Duration for Saving
```kotlin
// Only save if watched for minimum time
if (position > 30_000) { // 30 seconds minimum
    saveProgress(...)
}
```

## Testing Considerations

### 1. Unit Tests
- Test repository methods with various scenarios
- Test completion detection logic
- Test error handling paths

### 2. Integration Tests
- Test full resume flow with real ExoPlayer
- Test progress persistence across app restarts
- Test edge cases (very short videos, network interruptions)

### 3. UI Tests
- Test resume behavior in different player states
- Test progress bar accuracy
- Test completion notifications

## Future Enhancements

### 1. Advanced Features
- **Smart resume**: Skip intro/outro detection
- **Cross-device sync**: Sync progress across devices
- **Watch history**: Detailed viewing analytics
- **Bookmark system**: Manual bookmark creation

### 2. Performance Improvements
- **Incremental saves**: Only save when position changes significantly
- **Compression**: Compress progress data for storage efficiency
- **Background sync**: Sync progress in background

### 3. User Experience
- **Resume confirmation**: Ask user if they want to resume
- **Progress preview**: Show thumbnail at resume position
- **Bulk operations**: Clear all progress, mark series as watched

## Troubleshooting

### Common Issues

1. **Resume position not applied**
   - Check if `Player.STATE_READY` is reached
   - Verify `resumePosition > 0L` condition
   - Check repository data integrity

2. **Progress not saving**
   - Verify `isPlaying` state is true
   - Check repository permissions
   - Monitor for exceptions in save operations

3. **Incorrect completion detection**
   - Review completion threshold logic
   - Check duration calculation accuracy
   - Verify position tracking precision

### Debug Logging
```kotlin
Log.i("PlayerScreen", "Resume position: $resumePosition")
Log.i("PlayerScreen", "Current position: $currentPosition")
Log.i("PlayerScreen", "Duration: $duration")
Log.i("PlayerScreen", "Completed: $completed")
```

## Conclusion

The resume play last position feature provides a robust, user-friendly experience for video playback continuity. The implementation follows Android best practices with proper error handling, performance optimization, and maintainable code structure. The modular design allows for easy testing and future enhancements while providing reliable functionality across different device configurations and network conditions.
