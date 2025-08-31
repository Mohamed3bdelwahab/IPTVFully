# 🗄️ **Room Database Implementation - NewIPTV V2**

## 📋 **Overview**
Complete Room database implementation for NewIPTV V2 with API integration, following the real API structure from Hydra IPTV service.

---

## 🏗️ **Database Architecture**

### **1. Database Schema**

#### **Categories Table**
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: Int,
    val type: String // "series", "movie", "live"
)
```

#### **Items Table (Series, Movies, Live TV)**
```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdropPath: String?, // JSON array as string
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)
```

#### **Info Table (Detailed Metadata)**
```kotlin
@Entity(tableName = "info")
data class InfoEntity(
    @PrimaryKey val itemId: String,
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val lastModified: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdropPath: String?, // JSON array as string
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)
```

#### **Episodes Table**
```kotlin
@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val id: String,
    val itemId: String, // link to Series
    val season: Int,
    val episodeNum: Int,
    val title: String?,
    val containerExtension: String?,
    val movieImage: String?,
    val plot: String?,
    val rating: String?,
    val releaseDate: String?,
    val directSource: String?
)
```

---

## 🔧 **Data Access Objects (DAOs)**

### **CategoryDao**
```kotlin
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
```

### **ItemDao**
```kotlin
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
```

### **InfoDao**
```kotlin
@Dao
interface InfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: InfoEntity)

    @Query("SELECT * FROM info WHERE itemId = :itemId AND type = :type")
    suspend fun getInfo(itemId: String, type: String): InfoEntity?

    @Query("DELETE FROM info WHERE itemId = :itemId AND type = :type")
    suspend fun deleteInfo(itemId: String, type: String)
}
```

### **EpisodeDao**
```kotlin
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
```

---

## 🌐 **API Integration**

### **API Models**

#### **ApiCategory**
```kotlin
data class ApiCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)
```

#### **ApiItem**
```kotlin
data class ApiItem(
    val num: Int?,
    val series_id: String?, // For series
    val stream_id: String?, // For live TV
    val movie_id: String?,  // For movies
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val last_modified: String?,
    val rating: String?,
    val rating_5based: Double?,
    val backdrop_path: Any?, // Can be List<String>, String, or empty
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String
)
```

#### **ApiInfoResponse**
```kotlin
data class ApiInfoResponse(
    val seasons: List<String>?,
    val info: ApiInfoDetail?,
    val episodes: Map<String, List<ApiEpisode>>? // key = season number
)
```

### **Retrofit API Interface**
```kotlin
interface TvApi {
    @GET("player_api.php")
    suspend fun getCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String
    ): List<ApiCategory>

    @GET("player_api.php")
    suspend fun getItems(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String,
        @Query("category_id") categoryId: String? = null
    ): List<ApiItem>

    @GET("player_api.php")
    suspend fun getInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String,
        @Query("series_id") seriesId: String? = null,
        @Query("vod_id") movieId: String? = null,
        @Query("stream_id") liveId: String? = null
    ): ApiInfoResponse
}
```

---

## 🔄 **Data Mapping**

### **ApiTVMapping Object**
```kotlin
object ApiTVMapping {
    private val gson = Gson()

    // Map Categories
    fun mapCategory(api: ApiCategory, type: String): CategoryEntity {
        return CategoryEntity(
            categoryId = api.category_id,
            categoryName = api.category_name,
            parentId = api.parent_id,
            type = type
        )
    }

    // Map Items (Series, Movies, Live TV entry)
    fun mapItem(api: ApiItem, type: String): ItemEntity {
        val itemId = api.series_id ?: api.movie_id ?: api.stream_id ?: api.num?.toString() ?: "0"
        
        val backdropPath = when (api.backdrop_path) {
            is List<*> -> gson.toJson(api.backdrop_path.filterIsInstance<String>())
            is String -> api.backdrop_path
            else -> null
        }

        return ItemEntity(
            itemId = itemId,
            name = api.name,
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            lastModified = api.last_modified,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdropPath = backdropPath,
            youtubeTrailer = api.youtube_trailer,
            episodeRunTime = api.episode_run_time,
            categoryId = api.category_id,
            type = type
        )
    }

    // Map Info (detailed metadata)
    fun mapInfo(api: ApiInfoDetail, itemId: String, type: String): InfoEntity {
        return InfoEntity(
            itemId = itemId,
            name = api.name,
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            lastModified = api.last_modified,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdropPath = api.backdrop_path?.let { gson.toJson(it) },
            youtubeTrailer = api.youtube_trailer,
            episodeRunTime = api.episode_run_time,
            categoryId = api.category_id ?: "",
            type = type
        )
    }

    // Map Episode
    fun mapEpisode(api: ApiEpisode, itemId: String): EpisodeEntity {
        return EpisodeEntity(
            id = api.id,
            itemId = itemId,
            season = api.season,
            episodeNum = api.episode_num,
            title = api.title,
            containerExtension = api.container_extension,
            movieImage = api.info?.movie_image,
            plot = api.info?.plot,
            rating = api.info?.rating,
            releaseDate = api.info?.releasedate,
            directSource = api.direct_source
        )
    }

    // Map multiple episodes for a series
    fun mapEpisodes(episodesMap: Map<String, List<ApiEpisode>>, itemId: String): List<EpisodeEntity> {
        val episodes = mutableListOf<EpisodeEntity>()
        
        episodesMap.forEach { (seasonStr, episodeList) ->
            val season = seasonStr.toIntOrNull() ?: 1
            episodeList.forEach { apiEpisode ->
                episodes.add(mapEpisode(apiEpisode.copy(season = season), itemId))
            }
        }
        
        return episodes
    }

    // Helper function to parse backdrop path from JSON string
    fun parseBackdropPath(backdropPathJson: String?): List<String> {
        return try {
            if (backdropPathJson.isNullOrEmpty()) {
                emptyList()
            } else {
                val type = object : TypeToken<List<String>>() {}.type
                gson.fromJson(backdropPathJson, type) ?: emptyList()
            }
        } catch (e: Exception) {
            emptyList()
        }
    }
}
```

---

## 📊 **Real API Data Examples**

### **Categories API Response**
```json
[
  {
    "category_id": "198",
    "category_name": "مسلسلات اجنبية تعرض الان",
    "parent_id": 0
  },
  {
    "category_id": "159",
    "category_name": "مسلسلات عربية تعرض الأن", 
    "parent_id": 0
  },
  {
    "category_id": "194",
    "category_name": "مسلسلات تركية تعرض الأن",
    "parent_id": 0
  }
]
```

### **Series API Response**
```json
[
  {
    "num": 1,
    "name": "أبو حفيظة 2019",
    "series_id": 3,
    "cover": "https://smtp.cdn77cloud.com/images/hJ7RVaSKSW3sMaAUyqJELVo8voN_big.jpg",
    "plot": "A Comedy Egyptian talk show hosted by Akram Hosny on MBC Masr, performs comedic sketches joined by other celebrity.",
    "cast": "Akram Hosni",
    "director": "",
    "genre": "Comedy",
    "releaseDate": "2019-09-01",
    "last_modified": "1645986245",
    "rating": "8",
    "rating_5based": 4,
    "backdrop_path": [
      "http://smtp.cdn77cloud.com:80/images/93185_tv_backdrop_0.jpg"
    ],
    "youtube_trailer": "",
    "episode_run_time": "60",
    "category_id": "156"
  }
]
```

---

## 🔧 **Database Configuration**

### **AppDatabase**
```kotlin
@Database(
    entities = [
        CategoryEntity::class, 
        ItemEntity::class, 
        InfoEntity::class, 
        EpisodeEntity::class
    ],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun categoryDao(): CategoryDao
    abstract fun itemDao(): ItemDao
    abstract fun infoDao(): InfoDao
    abstract fun episodeDao(): EpisodeDao
}
```

### **API Client Configuration**
```kotlin
object TvApiClient {
    private const val BASE_URL = "http://hydraa.cc:2095/"

    val api: TvApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TvApi::class.java)
    }
}
```

---

## 🎯 **Key Features**

### **1. Type Safety**
- Strongly typed entities and DAOs
- Compile-time error checking
- Type-safe database operations

### **2. Reactive UI Support**
- Flow-based data streams
- Real-time UI updates
- Background data synchronization

### **3. Complex JSON Handling**
- Automatic JSON parsing for backdrop_path arrays
- Flexible data type handling
- Error-safe JSON operations

### **4. Multi-Content Support**
- Series, Movies, and Live TV support
- Unified data structure
- Content type differentiation

### **5. Scalable Architecture**
- Clean separation of concerns
- Repository pattern ready
- Dependency injection ready
- Testable architecture

---

## 🚀 **Usage Examples**

### **Loading Categories**
```kotlin
// In Repository
suspend fun loadCategories(type: String) {
    val apiCategories = tvApi.getCategories(username, password, "get_${type}_categories")
    val entities = apiCategories.map { ApiTVMapping.mapCategory(it, type) }
    categoryDao.insertAll(entities)
}

// In ViewModel
val categories = categoryDao.getCategories("series").asLiveData()
```

### **Loading Series by Category**
```kotlin
// In Repository
suspend fun loadSeries(categoryId: String) {
    val apiSeries = tvApi.getItems(username, password, "get_series", categoryId)
    val entities = apiSeries.map { ApiTVMapping.mapItem(it, "series") }
    itemDao.insertAll(entities)
}

// In ViewModel
val series = itemDao.getItemsByCategory(categoryId, "series").asLiveData()
```

### **Loading Episodes**
```kotlin
// In Repository
suspend fun loadEpisodes(seriesId: String) {
    val apiInfo = tvApi.getInfo(username, password, "get_series_info", seriesId)
    val episodes = ApiTVMapping.mapEpisodes(apiInfo.episodes ?: emptyMap(), seriesId)
    episodeDao.insertAll(episodes)
}

// In ViewModel
val episodes = episodeDao.getEpisodes(seriesId).asLiveData()
```

---

## 📋 **Next Steps**

1. **Repository Implementation** - Connect API and Database
2. **Dependency Injection** - Set up Hilt for clean architecture
3. **ViewModel Integration** - Connect UI to database
4. **UI Updates** - Replace hardcoded data with Room queries
5. **Testing** - Unit and integration tests

---

**Documentation Created**: December 2024  
**Status**: ✅ **COMPLETE**  
**Version**: 1.0.0
