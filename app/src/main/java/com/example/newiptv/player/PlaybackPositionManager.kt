package com.example.newiptv.player

import android.util.Log
import com.example.newiptv.data.db.AppDatabase
import com.example.newiptv.data.db.entities.PlaybackPositionEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Manages playback position tracking for videos
 * Saves and restores playback positions across app sessions
 */
class PlaybackPositionManager(
    private val database: AppDatabase
) {
    
    companion object {
        private const val TAG = "PlaybackPositionManager"
        private const val POSITION_UPDATE_INTERVAL = 5_000L // 5 seconds
    }
    
    private var currentPosition: Long = 0L
    private var currentContentId: String? = null
    private var currentContentType: String? = null
    private var isPositionSavingEnabled: Boolean = true
    
    /**
     * Initialize position tracking for content
     */
    fun initializePositionTracking(
        contentId: String,
        contentType: String,
        duration: Long
    ) {
        this.currentContentId = contentId
        this.currentContentType = contentType
        
        Log.d(TAG, "🎯 Initializing position tracking for content: $contentId ($contentType)")
        
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
        if (position % POSITION_UPDATE_INTERVAL == 0L) {
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
    suspend fun getSavedPosition(contentId: String, contentType: String): Long {
        return try {
            val position = database.playbackPositionDao().getPosition(contentId, contentType)
            
            Log.d(TAG, "📍 Retrieved saved position for $contentId: ${position?.position ?: 0L}ms")
            
            position?.position ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to retrieve position for content: $contentId", e)
            0L
        }
    }
    
    /**
     * Clear saved position for content
     */
    fun clearPosition(contentId: String, contentType: String) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                database.playbackPositionDao().deletePosition(contentId, contentType)
                Log.d(TAG, "🗑️ Cleared position for content: $contentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to clear position for content: $contentId", e)
        }
    }
    
    /**
     * Mark content as completed
     */
    fun markAsCompleted(contentId: String, contentType: String) {
        try {
            CoroutineScope(Dispatchers.IO).launch {
                database.playbackPositionDao().markAsCompleted(contentId, contentType)
                Log.d(TAG, "✅ Marked content as completed: $contentId")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to mark content as completed: $contentId", e)
        }
    }
    
    /**
     * Toggle position saving preference
     */
    fun setPositionSavingEnabled(enabled: Boolean) {
        this.isPositionSavingEnabled = enabled
        Log.d(TAG, "⚙️ Position saving ${if (enabled) "enabled" else "disabled"}")
    }
    
    private fun loadSavedPosition(contentId: String, contentType: String) {
        // Load saved position asynchronously
        CoroutineScope(Dispatchers.IO).launch {
            val savedPosition = getSavedPosition(contentId, contentType)
            withContext(Dispatchers.Main) {
                onPositionLoaded?.invoke(savedPosition)
            }
        }
    }
    
    private fun savePosition(contentId: String, contentType: String, position: Long) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val duration = getCurrentDuration()
                val watchPercentage = if (duration > 0) (position.toFloat() / duration.toFloat()) else 0f
                
                val positionEntity = PlaybackPositionEntity(
                    contentId = contentId,
                    contentType = contentType,
                    position = position,
                    duration = duration,
                    lastUpdated = System.currentTimeMillis(),
                    isCompleted = false,
                    watchPercentage = watchPercentage
                )
                
                database.playbackPositionDao().insertPosition(positionEntity)
                
                Log.d(TAG, "💾 Saved position for $contentId: ${position}ms (${(watchPercentage * 100).toInt()}%)")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save position for content: $contentId", e)
            }
        }
    }
    
    private fun getCurrentDuration(): Long {
        // This would be called from the video player to get current duration
        // For now, return 0 - will be updated by the video player
        return 0L
    }
    
    /**
     * Set current duration (called by video player)
     */
    fun setCurrentDuration(duration: Long) {
        // This method can be called by the video player to update duration
        // when it becomes available
    }
    
    var onPositionLoaded: ((Long) -> Unit)? = null
}
