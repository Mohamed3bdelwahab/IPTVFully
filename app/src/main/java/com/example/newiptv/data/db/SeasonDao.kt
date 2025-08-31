package com.example.newiptv.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.newiptv.data.db.entities.SeasonEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SeasonDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(seasons: List<SeasonEntity>)

    @Query("SELECT * FROM seasons WHERE seriesId = :seriesId ORDER BY seasonNumber ASC")
    fun getSeasonsBySeries(seriesId: String): Flow<List<SeasonEntity>>

    @Query("DELETE FROM seasons WHERE seriesId = :seriesId")
    suspend fun deleteSeasonsBySeries(seriesId: String)
}
