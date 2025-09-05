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
import java.util.concurrent.TimeUnit

/**
 * PlaybackPositionTracker - Tracks and saves video playback positions
 * Saves position every 5 seconds to database for resume functionality
 */
class PlaybackPositionTracker(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "PlaybackPositionTracker"
        private const val POSITION_SAVE_INTERVAL = 5000L // 5 seconds
        private const val MIN_POSITION_TO_SAVE = 10000L // Don't save positions less than 10 seconds
    }
    
    private val handler = Handler(Looper.getMainLooper())
    private val database = DatabaseProvider.getDatabase(context)
    private val positionUpdateRunnable = object : Runnable {
        override fun run() {
            saveCurrentPosition()
            handler.postDelayed(this, POSITION_SAVE_INTERVAL)
        }
    }
    
    private var videoId: String? = null
    private var contentType: String? = null
    private var seriesId: String? = null
    private var seasonNumber: Int? = null
    private var episodeNumber: Int? = null
    private var exoPlayer: androidx.media3.exoplayer.ExoPlayer? = null
    private var isTracking = false
    
    /**
     * Start tracking playback position for a video
     */
    fun startTracking(
        videoId: String,
        contentType: String,
        exoPlayer: androidx.media3.exoplayer.ExoPlayer,
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ) {
        this.videoId = videoId
        this.contentType = contentType
        this.seriesId = seriesId
        this.seasonNumber = seasonNumber
        this.episodeNumber = episodeNumber
        this.exoPlayer = exoPlayer
        this.isTracking = true
        
        Log.d(TAG, "Started tracking position for video: $videoId ($contentType)")
        
        // Start the position update loop
        handler.post(positionUpdateRunnable)
    }
    
    /**
     * Stop tracking playback position
     */
    fun stopTracking() {
        if (isTracking) {
            isTracking = false
            handler.removeCallbacks(positionUpdateRunnable)
            saveCurrentPosition()
            Log.d(TAG, "Stopped tracking position for video: $videoId")
        }
    }
    
    /**
     * Save current position to database
     */
    private fun saveCurrentPosition() {
        if (!isTracking || exoPlayer == null || videoId == null) {
            return
        }
        
        val currentPosition = exoPlayer!!.currentPosition
        val duration = exoPlayer!!.duration
        
        // Don't save positions that are too short (less than 10 seconds)
        if (currentPosition < MIN_POSITION_TO_SAVE) {
            return
        }
        
        // Don't save if we're near the end (within 30 seconds of end)
        if (duration > 0 && currentPosition > duration - 30000) {
            return
        }
        
        val positionEntity = PlaybackPositionEntity(
            videoId = videoId!!,
            contentType = contentType!!,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            position = currentPosition,
            duration = duration,
            lastUpdated = System.currentTimeMillis()
        )
        
        // Save to database in background
        CoroutineScope(Dispatchers.IO).launch {
            try {
                database.playbackPositionDao().savePosition(positionEntity)
                Log.d(TAG, "Saved position for $videoId: ${formatTime(currentPosition)}/${formatTime(duration)}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save position for $videoId", e)
            }
        }
    }
    
    /**
     * Get saved position for a video
     */
    suspend fun getSavedPosition(videoId: String): PlaybackPositionEntity? {
        return try {
            val position = database.playbackPositionDao().getPosition(videoId)
            if (position != null) {
                Log.d(TAG, "Retrieved saved position for $videoId: ${formatTime(position.position)}/${formatTime(position.duration)}")
            }
            position
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get saved position for $videoId", e)
            null
        }
    }
    
    /**
     * Delete saved position for a video
     */
    suspend fun deleteSavedPosition(videoId: String) {
        try {
            database.playbackPositionDao().deletePosition(videoId)
            Log.d(TAG, "Deleted saved position for $videoId")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to delete saved position for $videoId", e)
        }
    }
    
    /**
     * Check if there's a saved position for a video
     */
    suspend fun hasSavedPosition(videoId: String): Boolean {
        return getSavedPosition(videoId) != null
    }
    
    /**
     * Get all saved positions for a content type
     */
    suspend fun getPositionsByType(contentType: String): List<PlaybackPositionEntity> {
        return try {
            database.playbackPositionDao().getPositionsByType(contentType)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get positions by type: $contentType", e)
            emptyList()
        }
    }
    
    /**
     * Get all saved positions for a series
     */
    suspend fun getPositionsBySeries(seriesId: String): List<PlaybackPositionEntity> {
        return try {
            database.playbackPositionDao().getPositionsBySeries(seriesId)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get positions by series: $seriesId", e)
            emptyList()
        }
    }
    
    /**
     * Format time in milliseconds to readable format
     */
    private fun formatTime(timeMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        stopTracking()
        handler.removeCallbacksAndMessages(null)
    }
}
