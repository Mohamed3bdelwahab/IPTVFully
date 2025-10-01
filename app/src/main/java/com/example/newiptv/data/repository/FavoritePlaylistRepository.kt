package com.example.newiptv.data.repository

import com.example.newiptv.data.db.FavoritePlaylistDao
import com.example.newiptv.data.db.entities.FavoritePlaylistEntity
import com.example.newiptv.data.db.entities.FavoritePlaylistItemEntity
import kotlinx.coroutines.flow.Flow
import java.util.UUID

/**
 * Repository for managing favorite playlists and their items
 * Provides high-level operations for playlist management
 */
class FavoritePlaylistRepository(
    private val favoritePlaylistDao: FavoritePlaylistDao
) {
    
    // 🔹 Playlist Management
    
    /**
     * Get all playlists as Flow for reactive UI updates
     */
    fun getAllPlaylists(): Flow<List<FavoritePlaylistEntity>> {
        return favoritePlaylistDao.getAllPlaylists()
    }
    
    /**
     * Get all playlists synchronously
     */
    suspend fun getAllPlaylistsSync(): List<FavoritePlaylistEntity> {
        return favoritePlaylistDao.getAllPlaylistsSync()
    }
    
    /**
     * Get playlist by ID
     */
    suspend fun getPlaylistById(playlistId: String): FavoritePlaylistEntity? {
        return favoritePlaylistDao.getPlaylistById(playlistId)
    }
    
    /**
     * Get playlist by name
     */
    suspend fun getPlaylistByName(name: String): FavoritePlaylistEntity? {
        return favoritePlaylistDao.getPlaylistByName(name)
    }
    
    /**
     * Create a new playlist
     */
    suspend fun createPlaylist(name: String, description: String? = null): FavoritePlaylistEntity {
        android.util.Log.d("FavoritePlaylistRepository", "➕ Creating playlist: $name")
        val playlist = FavoritePlaylistEntity(
            id = UUID.randomUUID().toString(),
            name = name,
            description = description,
            createdDate = System.currentTimeMillis(),
            itemCount = 0,
            isDefault = false
        )
        favoritePlaylistDao.insertPlaylist(playlist)
        android.util.Log.d("FavoritePlaylistRepository", "✅ Playlist created successfully: ${playlist.id}")
        return playlist
    }
    
    /**
     * Update playlist
     */
    suspend fun updatePlaylist(playlist: FavoritePlaylistEntity) {
        favoritePlaylistDao.updatePlaylist(playlist)
    }
    
    /**
     * Delete playlist and all its items
     */
    suspend fun deletePlaylist(playlistId: String) {
        favoritePlaylistDao.deletePlaylistWithItems(playlistId)
    }
    
    // 🔹 Playlist Item Management
    
    /**
     * Get all items in a playlist
     */
    suspend fun getPlaylistItems(playlistId: String): List<FavoritePlaylistItemEntity> {
        return favoritePlaylistDao.getPlaylistItems(playlistId)
    }
    
    /**
     * Get all items in a playlist as Flow
     */
    fun getPlaylistItemsFlow(playlistId: String): Flow<List<FavoritePlaylistItemEntity>> {
        return favoritePlaylistDao.getPlaylistItemsFlow(playlistId)
    }
    
    /**
     * Add item to playlist
     */
    suspend fun addItemToPlaylist(
        playlistId: String,
        contentId: String,
        contentType: String,
        title: String,
        cover: String? = null,
        streamUrl: String? = null,
        seriesId: String? = null,
        seasonNumber: Int? = null,
        episodeNumber: Int? = null
    ): Boolean {
        return try {
            // Check if item already exists in playlist
            val existingItem = favoritePlaylistDao.getPlaylistItem(playlistId, contentId, contentType)
            if (existingItem != null) {
                return false // Item already exists
            }
            
            val item = FavoritePlaylistItemEntity(
                id = UUID.randomUUID().toString(),
                playlistId = playlistId,
                contentId = contentId,
                contentType = contentType,
                title = title,
                cover = cover,
                streamUrl = streamUrl,
                seriesId = seriesId,
                seasonNumber = seasonNumber,
                episodeNumber = episodeNumber,
                addedDate = System.currentTimeMillis(),
                sortOrder = 0
            )
            
            favoritePlaylistDao.addItemToPlaylist(playlistId, item)
            true
        } catch (e: Exception) {
            false
        }
    }
    
    /**
     * Remove item from playlist
     */
    suspend fun removeItemFromPlaylist(playlistId: String, contentId: String, contentType: String) {
        favoritePlaylistDao.removeItemFromPlaylistWithCount(playlistId, contentId, contentType)
    }
    
    /**
     * Check if content is in any playlist
     */
    suspend fun isContentInAnyPlaylist(contentId: String, contentType: String): Boolean {
        val items = favoritePlaylistDao.getPlaylistItemsByContent(contentId, contentType)
        return items.isNotEmpty()
    }
    
    /**
     * Get all playlists that contain specific content
     */
    suspend fun getPlaylistsContainingContent(contentId: String, contentType: String): List<FavoritePlaylistEntity> {
        val items = favoritePlaylistDao.getPlaylistItemsByContent(contentId, contentType)
        val playlistIds = items.map { it.playlistId }.distinct()
        
        return playlistIds.mapNotNull { playlistId ->
            favoritePlaylistDao.getPlaylistById(playlistId)
        }
    }
    
    // 🔹 Utility Methods
    
    /**
     * Get playlist count
     */
    suspend fun getPlaylistCount(): Int {
        return favoritePlaylistDao.getAllPlaylistsSync().size
    }
    
    /**
     * Get total items across all playlists
     */
    suspend fun getTotalItemCount(): Int {
        val playlists = favoritePlaylistDao.getAllPlaylistsSync()
        return playlists.sumOf { it.itemCount }
    }
    
    /**
     * Create default playlists if none exist
     */
    suspend fun createDefaultPlaylistsIfNeeded() {
        android.util.Log.d("FavoritePlaylistRepository", "🔍 Checking if default playlists need to be created...")
        val existingPlaylists = favoritePlaylistDao.getAllPlaylistsSync()
        android.util.Log.d("FavoritePlaylistRepository", "📋 Found ${existingPlaylists.size} existing playlists")
        
        if (existingPlaylists.isEmpty()) {
            android.util.Log.d("FavoritePlaylistRepository", "➕ Creating default playlists...")
            // Create default playlists
            createPlaylist("My Favorites", "Your personal favorites")
            createPlaylist("Watch Later", "Content you want to watch later")
            createPlaylist("Movies", "Favorite movies")
            createPlaylist("Series", "Favorite series")
            android.util.Log.d("FavoritePlaylistRepository", "✅ Default playlists created successfully")
        } else {
            android.util.Log.d("FavoritePlaylistRepository", "ℹ️ Default playlists already exist, skipping creation")
        }
    }
    
    /**
     * Search playlists by name
     */
    suspend fun searchPlaylists(query: String): List<FavoritePlaylistEntity> {
        val allPlaylists = favoritePlaylistDao.getAllPlaylistsSync()
        return allPlaylists.filter { 
            it.name.contains(query, ignoreCase = true) ||
            it.description?.contains(query, ignoreCase = true) == true
        }
    }
}
