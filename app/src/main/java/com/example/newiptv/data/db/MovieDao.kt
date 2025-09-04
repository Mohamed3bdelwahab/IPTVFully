package com.example.newiptv.data.db

import androidx.room.*
import com.example.newiptv.data.db.entities.*
import kotlinx.coroutines.flow.Flow

@Dao
interface MovieCategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<MovieCategoryEntity>)

    @Query("SELECT * FROM movie_categories ORDER BY categoryName")
    fun getAllCategories(): Flow<List<MovieCategoryEntity>>

    @Query("SELECT * FROM movie_categories ORDER BY categoryName")
    suspend fun getAllCategoriesSync(): List<MovieCategoryEntity>

    @Query("DELETE FROM movie_categories")
    suspend fun deleteAll()
}

@Dao
interface MovieItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MovieItemEntity>)

    @Query("SELECT * FROM movie_items WHERE categoryId = :categoryId ORDER BY name")
    fun getMoviesByCategory(categoryId: String): Flow<List<MovieItemEntity>>

    @Query("SELECT * FROM movie_items WHERE categoryId = :categoryId ORDER BY name")
    suspend fun getMoviesByCategorySync(categoryId: String): List<MovieItemEntity>

    @Query("SELECT * FROM movie_items ORDER BY name")
    suspend fun getAllMoviesSync(): List<MovieItemEntity>

    @Query("SELECT * FROM movie_items WHERE itemId = :itemId")
    suspend fun getMovieById(itemId: String): MovieItemEntity?

    @Query("DELETE FROM movie_items WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: String)

    @Query("DELETE FROM movie_items")
    suspend fun deleteAll()
}

@Dao
interface MovieInfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: MovieInfoEntity)

    @Query("SELECT * FROM movie_info WHERE itemId = :itemId")
    suspend fun getMovieInfo(itemId: String): MovieInfoEntity?

    @Query("DELETE FROM movie_info WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM movie_info")
    suspend fun deleteAll()
}

@Dao
interface MovieStreamDataDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streamData: MovieStreamDataEntity)

    @Query("SELECT * FROM movie_stream_data WHERE itemId = :itemId")
    suspend fun getStreamData(itemId: String): MovieStreamDataEntity?

    @Query("DELETE FROM movie_stream_data WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)

    @Query("DELETE FROM movie_stream_data")
    suspend fun deleteAll()
}
