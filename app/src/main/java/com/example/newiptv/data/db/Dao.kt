package com.example.newiptv.data.db

import androidx.room.*
import com.example.newiptv.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE type = :type ORDER BY categoryName")
    fun getCategories(type: String): Flow<List<CategoryEntity>>

    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategoriesSync(type: String): List<CategoryEntity>

    @Query("DELETE FROM categories WHERE type = :type")
    suspend fun deleteCategoriesByType(type: String)
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("SELECT * FROM items WHERE categoryId = :categoryId AND type = :type ORDER BY name")
    fun getItemsByCategory(categoryId: String, type: String): Flow<List<ItemEntity>>

    @Query("SELECT * FROM items WHERE categoryId = :categoryId AND type = :type")
    suspend fun getItemsByCategorySync(categoryId: String, type: String): List<ItemEntity>

    @Query("SELECT * FROM items WHERE itemId = :itemId AND type = :type")
    suspend fun getItemById(itemId: String, type: String): ItemEntity?

    @Query("DELETE FROM items WHERE categoryId = :categoryId AND type = :type")
    suspend fun deleteItemsByCategory(categoryId: String, type: String)

    @Query("DELETE FROM items WHERE type = :type")
    suspend fun deleteItemsByType(type: String)
    
    // Bulk data access methods
    @Query("SELECT * FROM items WHERE type = :type ORDER BY name")
    fun getAllItemsByType(type: String): Flow<List<ItemEntity>>
    
    @Query("SELECT * FROM items WHERE type = :type ORDER BY name")
    suspend fun getAllItemsByTypeSync(type: String): List<ItemEntity>
    
    @Query("""
        SELECT * FROM items
        WHERE
              name     LIKE '%' || :query || '%'
          OR  CAST(itemId AS TEXT) LIKE '%' || :query || '%'
          OR  IFNULL(plot, '')     LIKE '%' || :query || '%'
          OR  IFNULL("cast", '')   LIKE '%' || :query || '%'
          OR  IFNULL(director,'')  LIKE '%' || :query || '%'
          OR  IFNULL(genre,'')     LIKE '%' || :query || '%'
        ORDER BY name
    """)
    fun searchAllContent(query: String): Flow<List<ItemEntity>>
    
    @Query("""
        SELECT * FROM items
        WHERE type = :type AND (
              name     LIKE '%' || :query || '%'
          OR  CAST(itemId AS TEXT) LIKE '%' || :query || '%'
          OR  IFNULL(plot, '')     LIKE '%' || :query || '%'
          OR  IFNULL("cast", '')   LIKE '%' || :query || '%'
          OR  IFNULL(director,'')  LIKE '%' || :query || '%'
          OR  IFNULL(genre,'')     LIKE '%' || :query || '%'
        )
        ORDER BY name
    """)
    fun searchContentByType(type: String, query: String): Flow<List<ItemEntity>>
    
    @Query("SELECT COUNT(*) FROM items WHERE categoryId = :categoryId AND type = :type")
    suspend fun getItemCountByCategory(categoryId: String, type: String): Int
    
    @Query("SELECT COUNT(*) FROM items WHERE type = :type")
    suspend fun getTotalItemCountByType(type: String): Int
}

@Dao
interface InfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: InfoEntity)

    @Query("SELECT * FROM info WHERE itemId = :itemId AND type = :type")
    suspend fun getInfo(itemId: String, type: String): InfoEntity?

    @Query("DELETE FROM info WHERE itemId = :itemId AND type = :type")
    suspend fun deleteInfo(itemId: String, type: String)
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE itemId = :itemId ORDER BY season, episodeNum")
    fun getEpisodes(itemId: String): Flow<List<EpisodeEntity>>

    @Query("SELECT * FROM episodes WHERE itemId = :itemId ORDER BY season, episodeNum")
    suspend fun getEpisodesSync(itemId: String): List<EpisodeEntity>

    @Query("SELECT * FROM episodes WHERE itemId = :itemId AND season = :season ORDER BY episodeNum")
    suspend fun getEpisodesBySeason(itemId: String, season: Int): List<EpisodeEntity>

    @Query("DELETE FROM episodes WHERE itemId = :itemId")
    suspend fun deleteEpisodesByItemId(itemId: String)
}

@Dao
interface PlaybackPositionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPosition(position: PlaybackPositionEntity)

    @Query("SELECT * FROM playback_positions WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getPosition(contentId: String, contentType: String): PlaybackPositionEntity?

    @Query("DELETE FROM playback_positions WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deletePosition(contentId: String, contentType: String)

    @Query("UPDATE playback_positions SET isCompleted = 1 WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun markAsCompleted(contentId: String, contentType: String)

    @Query("SELECT * FROM playback_positions WHERE contentType = :contentType ORDER BY lastUpdated DESC")
    suspend fun getRecentPositions(contentType: String): List<PlaybackPositionEntity>

    @Query("SELECT * FROM playback_positions WHERE isCompleted = 0 AND watchPercentage > 0.1 ORDER BY lastUpdated DESC LIMIT :limit")
    suspend fun getInProgressContent(limit: Int = 50): List<PlaybackPositionEntity>

    @Query("DELETE FROM playback_positions WHERE lastUpdated < :cutoffTime")
    suspend fun deleteOldPositions(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM playback_positions")
    suspend fun getPositionCount(): Int
}

@Dao
interface WatchHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: WatchHistoryEntity)

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
    fun getAllHistory(): Flow<List<WatchHistoryEntity>>

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC")
    suspend fun getAllHistorySync(): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE contentType = :contentType ORDER BY lastWatched DESC")
    suspend fun getHistoryByType(contentType: String): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE contentType = :contentType ORDER BY lastWatched DESC LIMIT :limit")
    suspend fun getRecentHistoryByType(contentType: String, limit: Int = 10): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history ORDER BY lastWatched DESC LIMIT :limit")
    suspend fun getRecentHistory(limit: Int = 20): List<WatchHistoryEntity>

    @Query("SELECT * FROM watch_history WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getHistoryByContent(contentId: String, contentType: String): WatchHistoryEntity?

    @Query("DELETE FROM watch_history WHERE id = :id")
    suspend fun deleteHistoryById(id: String)

    @Query("DELETE FROM watch_history WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun deleteHistoryByContent(contentId: String, contentType: String)

    @Query("DELETE FROM watch_history")
    suspend fun clearAllHistory()
    
    @Query("DELETE FROM watch_history WHERE CAST(contentId AS INTEGER) < 0")
    suspend fun deleteHistoryWithHashIds()

    @Query("DELETE FROM watch_history WHERE lastWatched < :cutoffTime")
    suspend fun deleteOldHistory(cutoffTime: Long)

    @Query("SELECT COUNT(*) FROM watch_history")
    suspend fun getHistoryCount(): Int

    @Query("SELECT COUNT(*) FROM watch_history WHERE contentType = :contentType")
    suspend fun getHistoryCountByType(contentType: String): Int
}

// 🔹 Favorite Playlist DAO
@Dao
interface FavoritePlaylistDao {
    // Playlist operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: FavoritePlaylistEntity)
    
    @Update
    suspend fun updatePlaylist(playlist: FavoritePlaylistEntity)
    
    @Delete
    suspend fun deletePlaylist(playlist: FavoritePlaylistEntity)
    
    @Query("SELECT * FROM favorite_playlists ORDER BY createdDate DESC")
    fun getAllPlaylists(): Flow<List<FavoritePlaylistEntity>>
    
    @Query("SELECT * FROM favorite_playlists ORDER BY createdDate DESC")
    suspend fun getAllPlaylistsSync(): List<FavoritePlaylistEntity>
    
    @Query("SELECT * FROM favorite_playlists WHERE id = :playlistId")
    suspend fun getPlaylistById(playlistId: String): FavoritePlaylistEntity?
    
    @Query("SELECT * FROM favorite_playlists WHERE name = :name")
    suspend fun getPlaylistByName(name: String): FavoritePlaylistEntity?
    
    @Query("DELETE FROM favorite_playlists WHERE id = :playlistId")
    suspend fun deletePlaylistById(playlistId: String)
    
    @Query("UPDATE favorite_playlists SET itemCount = :count WHERE id = :playlistId")
    suspend fun updatePlaylistItemCount(playlistId: String, count: Int)
    
    // Playlist item operations
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylistItem(item: FavoritePlaylistItemEntity)
    
    @Delete
    suspend fun deletePlaylistItem(item: FavoritePlaylistItemEntity)
    
    @Query("SELECT * FROM favorite_playlist_items WHERE playlistId = :playlistId ORDER BY addedDate DESC")
    suspend fun getPlaylistItems(playlistId: String): List<FavoritePlaylistItemEntity>
    
    @Query("SELECT * FROM favorite_playlist_items WHERE playlistId = :playlistId ORDER BY addedDate DESC")
    fun getPlaylistItemsFlow(playlistId: String): Flow<List<FavoritePlaylistItemEntity>>
    
    @Query("SELECT * FROM favorite_playlist_items WHERE contentId = :contentId AND contentType = :contentType")
    suspend fun getPlaylistItemsByContent(contentId: String, contentType: String): List<FavoritePlaylistItemEntity>
    
    @Query("SELECT * FROM favorite_playlist_items WHERE playlistId = :playlistId AND contentId = :contentId AND contentType = :contentType")
    suspend fun getPlaylistItem(playlistId: String, contentId: String, contentType: String): FavoritePlaylistItemEntity?
    
    @Query("DELETE FROM favorite_playlist_items WHERE playlistId = :playlistId AND contentId = :contentId AND contentType = :contentType")
    suspend fun removeItemFromPlaylist(playlistId: String, contentId: String, contentType: String)
    
    @Query("DELETE FROM favorite_playlist_items WHERE playlistId = :playlistId")
    suspend fun deleteAllPlaylistItems(playlistId: String)
    
    @Query("SELECT COUNT(*) FROM favorite_playlist_items WHERE playlistId = :playlistId")
    suspend fun getPlaylistItemCount(playlistId: String): Int
    
    // Combined operations
    @Transaction
    suspend fun deletePlaylistWithItems(playlistId: String) {
        deleteAllPlaylistItems(playlistId)
        deletePlaylistById(playlistId)
    }
    
    @Transaction
    suspend fun addItemToPlaylist(playlistId: String, item: FavoritePlaylistItemEntity) {
        insertPlaylistItem(item)
        val count = getPlaylistItemCount(playlistId)
        updatePlaylistItemCount(playlistId, count)
    }
    
    @Transaction
    suspend fun removeItemFromPlaylistWithCount(playlistId: String, contentId: String, contentType: String) {
        removeItemFromPlaylist(playlistId, contentId, contentType)
        val count = getPlaylistItemCount(playlistId)
        updatePlaylistItemCount(playlistId, count)
    }
}
