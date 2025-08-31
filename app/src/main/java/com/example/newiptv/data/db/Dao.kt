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
