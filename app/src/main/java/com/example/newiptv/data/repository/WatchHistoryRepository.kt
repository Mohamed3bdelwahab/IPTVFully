package com.example.newiptv.data.repository

import com.example.newiptv.data.db.AppDatabase
import com.example.newiptv.data.db.entities.WatchHistoryEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

class WatchHistoryRepository(private val database: AppDatabase) {
    
    private val watchHistoryDao = database.watchHistoryDao()
    
    /**
     * Get all watch history ordered by last watched (most recent first)
     */
    fun getAllHistory(): Flow<List<WatchHistoryEntity>> {
        return watchHistoryDao.getAllHistory()
    }
    
    /**
     * Get all watch history synchronously
     */
    suspend fun getAllHistorySync(): List<WatchHistoryEntity> {
        return watchHistoryDao.getAllHistorySync()
    }
    
    /**
     * Get watch history by content type (movie, series, episode)
     */
    suspend fun getHistoryByType(contentType: String): List<WatchHistoryEntity> {
        return watchHistoryDao.getHistoryByType(contentType)
    }
    
    /**
     * Get recent watch history by content type with limit
     */
    suspend fun getRecentHistoryByType(contentType: String, limit: Int = 10): List<WatchHistoryEntity> {
        return watchHistoryDao.getRecentHistoryByType(contentType, limit)
    }
    
    /**
     * Get recent watch history with limit
     */
    suspend fun getRecentHistory(limit: Int = 20): List<WatchHistoryEntity> {
        return watchHistoryDao.getRecentHistory(limit)
    }
    
    /**
     * Get watch history for specific content
     */
    suspend fun getHistoryByContent(contentId: String, contentType: String): WatchHistoryEntity? {
        return watchHistoryDao.getHistoryByContent(contentId, contentType)
    }
    
    /**
     * Add or update watch history entry
     */
    suspend fun addToHistory(
        contentId: String,
        contentType: String,
        title: String,
        cover: String? = null,
        streamUrl: String? = null,
        categoryId: String? = null,
        categoryName: String? = null,
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null,
        watchDuration: Long = 0L,
        totalDuration: Long = 0L,
        watchPercentage: Float = 0f,
        isCompleted: Boolean = false,
        resumePosition: Long = 0L
    ) {
        val existingHistory = watchHistoryDao.getHistoryByContent(contentId, contentType)
        val historyId = existingHistory?.id ?: UUID.randomUUID().toString()
        
        val historyEntry = WatchHistoryEntity(
            id = historyId,
            contentId = contentId,
            contentType = contentType,
            title = title,
            cover = cover,
            streamUrl = streamUrl,
            categoryId = categoryId,
            categoryName = categoryName,
            seriesId = seriesId,
            seasonNumber = seasonNumber,
            episodeNumber = episodeNumber,
            lastWatched = System.currentTimeMillis(),
            watchDuration = watchDuration,
            totalDuration = totalDuration,
            watchPercentage = watchPercentage,
            isCompleted = isCompleted,
            resumePosition = resumePosition
        )
        
        watchHistoryDao.insertHistory(historyEntry)
    }
    
    /**
     * Update watch progress for existing history entry
     */
    suspend fun updateWatchProgress(
        contentId: String,
        contentType: String,
        watchDuration: Long,
        totalDuration: Long,
        watchPercentage: Float,
        isCompleted: Boolean = false,
        resumePosition: Long = 0L
    ) {
        val existingHistory = watchHistoryDao.getHistoryByContent(contentId, contentType)
        if (existingHistory != null) {
            val updatedHistory = existingHistory.copy(
                lastWatched = System.currentTimeMillis(),
                watchDuration = watchDuration,
                totalDuration = totalDuration,
                watchPercentage = watchPercentage,
                isCompleted = isCompleted,
                resumePosition = resumePosition
            )
            watchHistoryDao.insertHistory(updatedHistory)
        }
    }
    
    /**
     * Delete specific history entry
     */
    suspend fun deleteHistoryById(id: String) {
        watchHistoryDao.deleteHistoryById(id)
    }
    
    /**
     * Delete history for specific content
     */
    suspend fun deleteHistoryByContent(contentId: String, contentType: String) {
        watchHistoryDao.deleteHistoryByContent(contentId, contentType)
    }
    
    /**
     * Clear all watch history
     */
    suspend fun clearAllHistory() {
        watchHistoryDao.clearAllHistory()
    }
    
    /**
     * Clear watch history with hash code IDs (for fixing content ID issues)
     */
    suspend fun clearHistoryWithHashIds() {
        // Clear history entries that have negative IDs (hash codes)
        watchHistoryDao.deleteHistoryWithHashIds()
    }
    
    /**
     * Delete old history entries (older than specified time)
     */
    suspend fun deleteOldHistory(cutoffTime: Long) {
        watchHistoryDao.deleteOldHistory(cutoffTime)
    }
    
    /**
     * Get total history count
     */
    suspend fun getHistoryCount(): Int {
        return watchHistoryDao.getHistoryCount()
    }
    
    /**
     * Get history count by content type
     */
    suspend fun getHistoryCountByType(contentType: String): Int {
        return watchHistoryDao.getHistoryCountByType(contentType)
    }
    
    /**
     * Convert WatchHistoryEntity to ItemEntity for display in categories
     */
    fun convertToItemEntity(history: WatchHistoryEntity): com.example.newiptv.data.db.entities.ItemEntity {
        return com.example.newiptv.data.db.entities.ItemEntity(
            itemId = history.contentId,
            name = history.title,
            cover = history.cover,
            plot = null,
            cast = null,
            director = null,
            genre = null,
            releaseDate = null,
            lastModified = null,
            rating = null,
            rating5Based = null,
            backdropPath = null,
            youtubeTrailer = null,
            episodeRunTime = null,
            categoryId = history.categoryId ?: "recent_watched",
            type = history.contentType
        )
    }
}
