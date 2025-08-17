package com.example.iptvtv.data.local

import androidx.room.*
import com.example.iptvtv.data.model.EPGProgram
import kotlinx.coroutines.flow.Flow

@Dao
interface EPGDao {
    @Query("SELECT * FROM epg_programs WHERE channelId = :channelId ORDER BY startTime ASC")
    fun getProgramsForChannel(channelId: String): Flow<List<EPGProgram>>
    
    @Query("SELECT * FROM epg_programs WHERE startTime >= :currentTime ORDER BY startTime ASC")
    fun getCurrentPrograms(currentTime: Long): Flow<List<EPGProgram>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProgram(program: EPGProgram)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrograms(programs: List<EPGProgram>)
    
    @Query("DELETE FROM epg_programs WHERE channelId = :channelId")
    suspend fun deleteProgramsForChannel(channelId: String)
    
    @Query("DELETE FROM epg_programs")
    suspend fun deleteAllPrograms()
}
