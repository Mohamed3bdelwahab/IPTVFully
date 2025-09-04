package com.example.newiptv.player

import android.content.Context
import android.util.Log
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.EpisodeEntity
import com.example.newiptv.data.db.entities.PlaylistItemEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Playlist Manager for Auto-Play Next Functionality
 * Handles episode progression, movie playlists, and auto-play logic
 */
class PlaylistManager(private val context: Context) {
    
    private val database = DatabaseProvider.getDatabase(context)
    private val episodeDao = database.episodeDao()
    private val playlistItemDao = database.playlistItemDao()
    private val coroutineScope = CoroutineScope(Dispatchers.IO)
    
    // Current playlist information
    private var currentPlaylistId: String? = null
    private var currentItemPosition: Int = 0
    private var currentPlaylistItems: List<PlaylistItemEntity> = emptyList()
    private var currentEpisodes: List<EpisodeEntity> = emptyList()
    
    // Content type tracking
    private var currentContentType: String? = null
    private var currentSeriesId: String? = null
    private var currentSeasonNumber: Int? = null
    private var currentEpisodeIndex: Int = 0
    
    companion object {
        private const val TAG = "PlaylistManager"
        const val CONTENT_TYPE_MOVIE = "movie"
        const val CONTENT_TYPE_SERIES = "series"
        const val CONTENT_TYPE_EPISODE = "episode"
    }
    
    /**
     * Interface for auto-play callbacks
     */
    interface AutoPlayListener {
        fun onPlayNext(
            contentId: String,
            contentType: String,
            title: String,
            url: String?,
            seriesId: String? = null,
            seasonNumber: Int? = null,
            episodeIndex: Int? = null
        )
        fun onPlaylistComplete()
        fun onAutoPlayCancelled(reason: String)
    }
    
    private var autoPlayListener: AutoPlayListener? = null
    
    /**
     * Set auto-play listener
     */
    fun setAutoPlayListener(listener: AutoPlayListener) {
        this.autoPlayListener = listener
    }
    
    /**
     * Initialize playlist for series episodes
     */
    suspend fun initializeSeriesPlaylist(
        seriesId: String,
        seasonNumber: Int,
        startEpisodeIndex: Int = 0
    ) {
        withContext(Dispatchers.IO) {
            try {
                currentContentType = CONTENT_TYPE_SERIES
                currentSeriesId = seriesId
                currentSeasonNumber = seasonNumber
                currentEpisodeIndex = startEpisodeIndex
                
                // Load episodes for the series
                currentEpisodes = episodeDao.getEpisodesSync(seriesId)
                
                Log.d(TAG, "Initialized series playlist: $seriesId, Season: $seasonNumber, Episodes: ${currentEpisodes.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing series playlist", e)
            }
        }
    }
    
    /**
     * Initialize playlist for movies or custom content
     */
    suspend fun initializeCustomPlaylist(playlistId: String, startPosition: Int = 0) {
        withContext(Dispatchers.IO) {
            try {
                currentPlaylistId = playlistId
                currentItemPosition = startPosition
                currentContentType = CONTENT_TYPE_MOVIE
                
                // Load playlist items
                currentPlaylistItems = playlistItemDao.getPlaylistItems(playlistId)
                
                Log.d(TAG, "Initialized custom playlist: $playlistId, Items: ${currentPlaylistItems.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Error initializing custom playlist", e)
            }
        }
    }
    
    /**
     * Get next item to play
     */
    suspend fun getNextItem(): NextPlayItem? {
        return withContext(Dispatchers.IO) {
            try {
                when (currentContentType) {
                    CONTENT_TYPE_SERIES -> getNextEpisode()
                    CONTENT_TYPE_MOVIE -> getNextPlaylistItem()
                    else -> {
                        Log.w(TAG, "Unknown content type: $currentContentType")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting next item", e)
                null
            }
        }
    }
    
    /**
     * Get previous item to play
     */
    suspend fun getPreviousItem(): NextPlayItem? {
        return withContext(Dispatchers.IO) {
            try {
                when (currentContentType) {
                    CONTENT_TYPE_SERIES -> getPreviousEpisode()
                    CONTENT_TYPE_MOVIE -> getPreviousPlaylistItem()
                    else -> {
                        Log.w(TAG, "Unknown content type: $currentContentType")
                        null
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error getting previous item", e)
                null
            }
        }
    }
    
    /**
     * Trigger auto-play next
     */
    fun triggerAutoPlayNext() {
        coroutineScope.launch {
            val nextItem = getNextItem()
            if (nextItem != null) {
                Log.d(TAG, "Auto-playing next item: ${nextItem.title}")
                autoPlayListener?.onPlayNext(
                    contentId = nextItem.contentId,
                    contentType = nextItem.contentType,
                    title = nextItem.title,
                    url = nextItem.url,
                    seriesId = nextItem.seriesId,
                    seasonNumber = nextItem.seasonNumber,
                    episodeIndex = nextItem.episodeIndex
                )
            } else {
                Log.d(TAG, "No next item available, playlist complete")
                autoPlayListener?.onPlaylistComplete()
            }
        }
    }
    
    /**
     * Check if there's a next item available
     */
    suspend fun hasNextItem(): Boolean {
        return withContext(Dispatchers.IO) {
            when (currentContentType) {
                CONTENT_TYPE_SERIES -> {
                    currentEpisodeIndex < currentEpisodes.size - 1
                }
                CONTENT_TYPE_MOVIE -> {
                    currentItemPosition < currentPlaylistItems.size - 1
                }
                else -> false
            }
        }
    }
    
    /**
     * Check if there's a previous item available
     */
    suspend fun hasPreviousItem(): Boolean {
        return withContext(Dispatchers.IO) {
            when (currentContentType) {
                CONTENT_TYPE_SERIES -> currentEpisodeIndex > 0
                CONTENT_TYPE_MOVIE -> currentItemPosition > 0
                else -> false
            }
        }
    }
    
    /**
     * Update current position in playlist
     */
    fun updateCurrentPosition(newPosition: Int) {
        when (currentContentType) {
            CONTENT_TYPE_SERIES -> currentEpisodeIndex = newPosition
            CONTENT_TYPE_MOVIE -> currentItemPosition = newPosition
        }
    }
    
    /**
     * Get next episode for series
     */
    private fun getNextEpisode(): NextPlayItem? {
        if (currentEpisodeIndex >= currentEpisodes.size - 1) {
            return null // No more episodes
        }
        
        val nextIndex = currentEpisodeIndex + 1
        val nextEpisode = currentEpisodes[nextIndex]
        
        // Update current position
        currentEpisodeIndex = nextIndex
        
        return NextPlayItem(
            contentId = nextEpisode.id,
            contentType = CONTENT_TYPE_EPISODE,
            title = nextEpisode.title ?: "Episode ${nextEpisode.episodeNum}",
            url = nextEpisode.directSource,
            seriesId = currentSeriesId,
            seasonNumber = currentSeasonNumber,
            episodeIndex = nextIndex
        )
    }
    
    /**
     * Get previous episode for series
     */
    private fun getPreviousEpisode(): NextPlayItem? {
        if (currentEpisodeIndex <= 0) {
            return null // No previous episodes
        }
        
        val prevIndex = currentEpisodeIndex - 1
        val prevEpisode = currentEpisodes[prevIndex]
        
        // Update current position
        currentEpisodeIndex = prevIndex
        
        return NextPlayItem(
            contentId = prevEpisode.id,
            contentType = CONTENT_TYPE_EPISODE,
            title = prevEpisode.title ?: "Episode ${prevEpisode.episodeNum}",
            url = prevEpisode.directSource,
            seriesId = currentSeriesId,
            seasonNumber = currentSeasonNumber,
            episodeIndex = prevIndex
        )
    }
    
    /**
     * Get next item in custom playlist
     */
    private fun getNextPlaylistItem(): NextPlayItem? {
        if (currentItemPosition >= currentPlaylistItems.size - 1) {
            return null // No more items
        }
        
        val nextIndex = currentItemPosition + 1
        val nextItem = currentPlaylistItems[nextIndex]
        
        // Update current position
        currentItemPosition = nextIndex
        
        return NextPlayItem(
            contentId = nextItem.contentId,
            contentType = nextItem.contentType,
            title = nextItem.title,
            url = null, // URL would need to be resolved from content ID
            seriesId = null,
            seasonNumber = null,
            episodeIndex = nextIndex
        )
    }
    
    /**
     * Get previous item in custom playlist
     */
    private fun getPreviousPlaylistItem(): NextPlayItem? {
        if (currentItemPosition <= 0) {
            return null // No previous items
        }
        
        val prevIndex = currentItemPosition - 1
        val prevItem = currentPlaylistItems[prevIndex]
        
        // Update current position
        currentItemPosition = prevIndex
        
        return NextPlayItem(
            contentId = prevItem.contentId,
            contentType = prevItem.contentType,
            title = prevItem.title,
            url = null, // URL would need to be resolved from content ID
            seriesId = null,
            seasonNumber = null,
            episodeIndex = prevIndex
        )
    }
    
    /**
     * Create a custom playlist from episodes
     */
    suspend fun createEpisodePlaylist(
        seriesId: String,
        seasonNumber: Int,
        playlistId: String = "series_${seriesId}_season_${seasonNumber}"
    ) {
        withContext(Dispatchers.IO) {
            try {
                val episodes = episodeDao.getEpisodesSync(seriesId)
                val playlistItems = episodes.mapIndexed { index, episode ->
                    PlaylistItemEntity(
                        itemId = "${playlistId}_${episode.id}",
                        playlistId = playlistId,
                        position = index,
                        contentType = CONTENT_TYPE_EPISODE,
                        contentId = episode.id,
                        title = episode.title ?: "Episode ${episode.episodeNum}",
                        thumbnail = episode.movieImage
                    )
                }
                
                // Clear existing playlist items
                playlistItemDao.deletePlaylistItems(playlistId)
                
                // Insert new playlist items
                playlistItemDao.insertAll(playlistItems)
                
                Log.d(TAG, "Created episode playlist: $playlistId with ${playlistItems.size} items")
            } catch (e: Exception) {
                Log.e(TAG, "Error creating episode playlist", e)
            }
        }
    }
    
    /**
     * Clear current playlist
     */
    fun clearPlaylist() {
        currentPlaylistId = null
        currentItemPosition = 0
        currentPlaylistItems = emptyList()
        currentEpisodes = emptyList()
        currentContentType = null
        currentSeriesId = null
        currentSeasonNumber = null
        currentEpisodeIndex = 0
        
        Log.d(TAG, "Playlist cleared")
    }
}

/**
 * Data class for next play item
 */
data class NextPlayItem(
    val contentId: String,
    val contentType: String,
    val title: String,
    val url: String?,
    val seriesId: String? = null,
    val seasonNumber: Int? = null,
    val episodeIndex: Int? = null
)
