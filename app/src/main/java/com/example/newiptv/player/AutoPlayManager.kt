package com.example.newiptv.player

import android.util.Log
import com.example.newiptv.data.db.entities.EpisodeEntity

/**
 * Manages auto-play functionality for episodes
 * Automatically plays next episode when current episode ends
 */
class AutoPlayManager {
    
    companion object {
        private const val TAG = "AutoPlayManager"
    }
    
    private var isAutoPlayEnabled: Boolean = false
    private var currentEpisodeId: String? = null
    private var currentSeriesId: String? = null
    private var episodeList: List<EpisodeEntity> = emptyList()
    private var currentEpisodeIndex: Int = -1
    
    /**
     * Initialize auto-play for a series
     */
    fun initializeAutoPlay(seriesId: String, episodeId: String, episodes: List<EpisodeEntity>) {
        this.currentSeriesId = seriesId
        this.currentEpisodeId = episodeId
        this.episodeList = episodes
        this.currentEpisodeIndex = findEpisodeIndex(episodeId)
        this.isAutoPlayEnabled = getAutoPlayPreference()
        
        Log.d(TAG, "🎬 Auto-play initialized for series: $seriesId")
        Log.d(TAG, "   Current episode: $episodeId")
        Log.d(TAG, "   Episodes count: ${episodes.size}")
        Log.d(TAG, "   Auto-play enabled: $isAutoPlayEnabled")
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
    fun getNextEpisode(): EpisodeEntity? {
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
        
        Log.d(TAG, "🔄 Updated current episode to: $episodeId (index: $currentEpisodeIndex)")
    }
    
    /**
     * Toggle auto-play setting
     */
    fun setAutoPlayEnabled(enabled: Boolean) {
        this.isAutoPlayEnabled = enabled
        saveAutoPlayPreference(enabled)
        
        Log.d(TAG, "⚙️ Auto-play ${if (enabled) "enabled" else "disabled"}")
    }
    
    /**
     * Check if auto-play is enabled
     */
    fun isAutoPlayEnabled(): Boolean {
        return isAutoPlayEnabled
    }
    
    private fun getAutoPlayPreference(): Boolean {
        // For now, default to true to enable auto-play
        // In a real implementation, this would read from SharedPreferences or database
        return true
    }
    
    private fun saveAutoPlayPreference(enabled: Boolean) {
        // For now, just store in memory
        // In a real implementation, this would save to SharedPreferences or database
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
    
    /**
     * Get current episode info
     */
    fun getCurrentEpisode(): EpisodeEntity? {
        return if (currentEpisodeIndex >= 0 && currentEpisodeIndex < episodeList.size) {
            episodeList[currentEpisodeIndex]
        } else null
    }
    
    /**
     * Get next episode info without triggering auto-play
     */
    fun getNextEpisodeInfo(): EpisodeEntity? {
        val nextIndex = currentEpisodeIndex + 1
        return if (nextIndex < episodeList.size) {
            episodeList[nextIndex]
        } else null
    }
    
    /**
     * Get previous episode info
     */
    fun getPreviousEpisode(): EpisodeEntity? {
        val prevIndex = currentEpisodeIndex - 1
        return if (prevIndex >= 0) {
            episodeList[prevIndex]
        } else null
    }
}
