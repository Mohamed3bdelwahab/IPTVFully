package com.example.newiptv.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.PlaybackPositionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Position Tracker for Video Player
 * Handles position tracking, saving, and restoration with 5-second intervals
 */
class PositionTracker(
    private val context: Context,
    private val player: IPTVVideoPlayer
) {
    private val database = DatabaseProvider.getDatabase(context)
    private val positionDao = database.playbackPositionDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    private val handler = Handler(Looper.getMainLooper())
    private var positionUpdateRunnable: Runnable? = null
    private var isTracking = false
    
    // Current content information
    private var currentContentId: String? = null
    private var currentContentType: String? = null
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentEpisodeNumber: Int? = null
    
    companion object {
        private const val TAG = "PositionTracker"
        private const val UPDATE_INTERVAL = 5000L // 5 seconds
        private const val MIN_POSITION_TO_SAVE = 30000L // 30 seconds minimum
        private const val POSITION_NEAR_END_THRESHOLD = 0.95f // 95% of video length
    }
    
    /**
     * Start position tracking for a content item
     */
    fun startTracking(
        contentId: String,
        contentType: String,
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        Log.d(TAG, "Starting position tracking for $contentType: $contentId")
        
        currentContentId = contentId
        currentContentType = contentType
        currentSeriesId = seriesId
        currentSeasonNumber = seasonNumber
        currentEpisodeNumber = episodeNumber
        
        isTracking = true
        startPeriodicUpdates()
    }
    
    /**
     * Stop position tracking and save final position
     */
    fun stopTracking() {
        Log.d(TAG, "Stopping position tracking")
        
        isTracking = false
        positionUpdateRunnable?.let { handler.removeCallbacks(it) }
        
        // Save final position
        saveFinalPosition()
        
        // Clear current content info
        currentContentId = null
        currentContentType = null
        currentSeriesId = null
        currentSeasonNumber = null
        currentEpisodeNumber = null
    }
    
    /**
     * Get saved position for content
     */
    suspend fun getSavedPosition(contentId: String, contentType: String): Long? {
        return withContext(Dispatchers.IO) {
            try {
                val position = positionDao.getPosition(contentId, contentType)
                position?.let {
                    // Check if position is near the end of the video
                    val progressPercent = it.position.toFloat() / it.duration.toFloat()
                    if (progressPercent >= POSITION_NEAR_END_THRESHOLD) {
                        // If near end, start from beginning
                        Log.d(TAG, "Position near end ($progressPercent), starting from beginning")
                        null
                    } else {
                        Log.d(TAG, "Restored position: ${it.position}ms for $contentType: $contentId")
                        it.position
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting saved position", e)
                null
            }
        }
    }
    
    /**
     * Manually save current position (for immediate save scenarios)
     */
    fun saveCurrentPosition() {
        if (!isTracking || currentContentId == null || currentContentType == null) return
        
        val position = player.getCurrentPosition()
        val duration = player.getDuration()
        
        if (position >= MIN_POSITION_TO_SAVE && duration > 0) {
            savePosition(position, duration)
        }
    }
    
    /**
     * Delete saved position for content
     */
    suspend fun deleteSavedPosition(contentId: String) {
        withContext(Dispatchers.IO) {
            try {
                positionDao.deletePosition(contentId)
                Log.d(TAG, "Deleted saved position for: $contentId")
            } catch (e: Exception) {
                Log.e(TAG, "Error deleting saved position", e)
            }
        }
    }
    
    /**
     * Clean up old positions (older than 30 days)
     */
    suspend fun cleanupOldPositions() {
        withContext(Dispatchers.IO) {
            try {
                val cutoffTime = System.currentTimeMillis() - (30 * 24 * 60 * 60 * 1000L) // 30 days
                positionDao.deleteOldPositions(cutoffTime)
                Log.d(TAG, "Cleaned up old positions")
            } catch (e: Exception) {
                Log.e(TAG, "Error cleaning up old positions", e)
            }
        }
    }
    
    /**
     * Start periodic position updates
     */
    private fun startPeriodicUpdates() {
        positionUpdateRunnable = object : Runnable {
            override fun run() {
                if (isTracking && player.isPlaying()) {
                    updatePosition()
                }
                
                if (isTracking) {
                    handler.postDelayed(this, UPDATE_INTERVAL)
                }
            }
        }
        
        handler.postDelayed(positionUpdateRunnable!!, UPDATE_INTERVAL)
    }
    
    /**
     * Update current position
     */
    private fun updatePosition() {
        if (currentContentId == null || currentContentType == null) return
        
        val position = player.getCurrentPosition()
        val duration = player.getDuration()
        
        if (position >= MIN_POSITION_TO_SAVE && duration > 0) {
            savePosition(position, duration)
        }
    }
    
    /**
     * Save final position when stopping tracking
     */
    private fun saveFinalPosition() {
        if (currentContentId == null || currentContentType == null) return
        
        val position = player.getCurrentPosition()
        val duration = player.getDuration()
        
        if (duration > 0) {
            // Save final position even if less than minimum, but only if not near the end
            val progressPercent = position.toFloat() / duration.toFloat()
            if (progressPercent < POSITION_NEAR_END_THRESHOLD) {
                savePosition(position, duration)
            } else {
                // If near end, delete the saved position (mark as completed)
                currentContentId?.let { contentId ->
                    coroutineScope.launch {
                        deleteSavedPosition(contentId)
                    }
                }
            }
        }
    }
    
    /**
     * Save position to database
     */
    private fun savePosition(position: Long, duration: Long) {
        val contentId = currentContentId ?: return
        val contentType = currentContentType ?: return
        
        val positionEntity = PlaybackPositionEntity(
            contentId = contentId,
            contentType = contentType,
            position = position,
            duration = duration,
            lastUpdated = System.currentTimeMillis(),
            seriesId = currentSeriesId,
            seasonNumber = currentSeasonNumber,
            episodeNumber = currentEpisodeNumber
        )
        
        coroutineScope.launch {
            try {
                positionDao.insertOrUpdate(positionEntity)
                Log.d(TAG, "Saved position: ${position}ms for $contentType: $contentId")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving position", e)
            }
        }
    }
}
