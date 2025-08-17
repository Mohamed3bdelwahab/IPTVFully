package com.example.iptvtv.data.local

import androidx.room.*
import com.example.iptvtv.data.model.PlaylistConfig
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistConfigDao {
    @Query("SELECT * FROM playlist_configs ORDER BY name ASC")
    fun getAllPlaylists(): Flow<List<PlaylistConfig>>
    
    @Query("SELECT * FROM playlist_configs WHERE isActive = 1 ORDER BY name ASC")
    fun getActivePlaylists(): Flow<List<PlaylistConfig>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistConfig)
    
    @Update
    suspend fun updatePlaylist(playlist: PlaylistConfig)
    
    @Delete
    suspend fun deletePlaylist(playlist: PlaylistConfig)
    
    @Query("DELETE FROM playlist_configs")
    suspend fun deleteAllPlaylists()
}
