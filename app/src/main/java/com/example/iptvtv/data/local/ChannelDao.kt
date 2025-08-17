package com.example.iptvtv.data.local

import androidx.room.*
import com.example.iptvtv.data.model.Channel
import kotlinx.coroutines.flow.Flow

@Dao
interface ChannelDao {
    @Query("SELECT * FROM channels ORDER BY name ASC")
    fun getAllChannels(): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE isFavorite = 1 ORDER BY name ASC")
    fun getFavoriteChannels(): Flow<List<Channel>>
    
    @Query("SELECT * FROM channels WHERE category = :category ORDER BY name ASC")
    fun getChannelsByCategory(category: String): Flow<List<Channel>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannel(channel: Channel)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChannels(channels: List<Channel>)
    
    @Update
    suspend fun updateChannel(channel: Channel)
    
    @Delete
    suspend fun deleteChannel(channel: Channel)
    
    @Query("DELETE FROM channels")
    suspend fun deleteAllChannels()
}
