Got it 👍
Now we have **three API shapes** to unify into one consistent DB + mapping layer:

1. **Categories** (`get_series_categories`, `get_vod_categories`, `get_live_categories`)
   → category\_id, category\_name, parent\_id

2. **Items list** (`get_series`, `get_vod_streams`, `get_live_streams`)
   → series\_id / stream\_id / movie\_id, name, cover, plot, cast, director, genre, releaseDate, rating, backdrops, category\_id, etc.

3. **Item details** (`get_series_info`, `get_vod_info`, `get_live_info`)
   → info object (name, plot, cast, etc.) + episodes (for series) or direct stream info (movies/live).

---

# 📌 Unified Update for Entities & Mapping

### ✅ Entities (Room)

```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: Int = 0,
    val type: String // "series", "movie", "live"
)

@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemId: String, // series_id, movie_id, stream_id
    val categoryId: String,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdrops: String?, // join list as string
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val type: String // "series", "movie", "live"
)

@Entity(tableName = "info")
data class InfoEntity(
    @PrimaryKey val itemId: String,
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val rating5Based: Double?,
    val backdrops: String?,
    val youtubeTrailer: String?,
    val runTime: String?,
    val type: String
)

@Entity(tableName = "episodes")
data class EpisodeEntity(
    @PrimaryKey val episodeId: String,
    val itemId: String, // links to InfoEntity
    val season: Int,
    val episodeNum: Int,
    val title: String,
    val containerExtension: String?,
    val plot: String?,
    val rating: String?,
    val releaseDate: String?,
    val image: String?
)
```

---

### ✅ API Models (Retrofit / Moshi)

```kotlin
data class ApiCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

data class ApiItem(
    val series_id: Int? = null,
    val movie_id: Int? = null,
    val stream_id: Int? = null,
    val num: Int? = null,
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
    val backdrop_path: Any?, // can be List<String> or String or empty
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String
)

data class ApiInfoResponse(
    val info: InfoDetails,
    val episodes: Map<String, List<ApiEpisode>>?
)

data class InfoDetails(
    val name: String,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val rating: String?,
    val rating_5based: Double?,
    val backdrop_path: List<String>?,
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String
)

data class ApiEpisode(
    val id: String,
    val episode_num: Int,
    val title: String,
    val container_extension: String?,
    val info: ApiEpisodeInfo?,
    val season: Int
)

data class ApiEpisodeInfo(
    val movie_image: String?,
    val plot: String?,
    val rating: String?,
    val releasedate: String?
)
```

---

### ✅ `ApiTVMapping.kt`

```kotlin
object ApiTVMapping {

    fun mapCategory(api: ApiCategory, type: String): CategoryEntity =
        CategoryEntity(
            categoryId = api.category_id,
            categoryName = api.category_name,
            parentId = api.parent_id,
            type = type
        )

    fun mapItem(api: ApiItem, type: String): ItemEntity {
        val id = api.series_id?.toString()
            ?: api.movie_id?.toString()
            ?: api.stream_id?.toString()
            ?: api.num?.toString()
            ?: ""

        val backdrops = when (api.backdrop_path) {
            is List<*> -> api.backdrop_path.filterIsInstance<String>().joinToString(",")
            is String -> api.backdrop_path
            else -> null
        }

        return ItemEntity(
            itemId = id,
            categoryId = api.category_id,
            name = api.name,
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdrops = backdrops,
            youtubeTrailer = api.youtube_trailer,
            episodeRunTime = api.episode_run_time,
            type = type
        )
    }

    fun mapInfo(api: InfoDetails, itemId: String, type: String): InfoEntity =
        InfoEntity(
            itemId = itemId,
            name = api.name,
            cover = api.cover,
            plot = api.plot,
            cast = api.cast,
            director = api.director,
            genre = api.genre,
            releaseDate = api.releaseDate,
            rating = api.rating,
            rating5Based = api.rating_5based,
            backdrops = api.backdrop_path?.joinToString(","),
            youtubeTrailer = api.youtube_trailer,
            runTime = api.episode_run_time,
            type = type
        )

    fun mapEpisode(api: ApiEpisode, itemId: String): EpisodeEntity =
        EpisodeEntity(
            episodeId = api.id,
            itemId = itemId,
            season = api.season,
            episodeNum = api.episode_num,
            title = api.title,
            containerExtension = api.container_extension,
            plot = api.info?.plot,
            rating = api.info?.rating,
            releaseDate = api.info?.releasedate,
            image = api.info?.movie_image
        )
}
```

---

✅ This now handles:

* Categories → `CategoryEntity`
* Items list (like your `get_series` JSON) → `ItemEntity`
* Info (with seasons/episodes) → `InfoEntity` + `EpisodeEntity`

---

⚡ Next step: I can generate the **DAO interfaces + RoomDatabase setup** so you can actually plug this in and test end-to-end (fetch API → map → save → query DB → show UI).

Do you want me to prepare those DAOs + `AppDatabase.kt` next?
Got it 👍 we’ll do this **step-by-step but fully integrated** so you end up with a working baseline.  
I’ll prepare one **`ApiTVMapping.kt` + Entities + DAOs + Database** file so your Room database is ready, and then we’ll add the Retrofit `TvApi.kt` interface afterwards.  

---

# 📂 `data/db/entities/Entities.kt`
All tables in one file for simplicity.

```kotlin
package com.example.newiptv.data.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

// 🔹 Categories (Series, Movies, Live TV)
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,
    val categoryName: String,
    val parentId: Int,
    val type: String // "series", "movie", "live"
)

// 🔹 Items (Series, Movies, Live TV entries)
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
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)

// 🔹 Info Screen (Detailed metadata for a single item)
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
    val youtubeTrailer: String?,
    val episodeRunTime: String?,
    val categoryId: String,
    val type: String
)

// 🔹 Episodes (only for series, linked to itemId)
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

# 📂 `data/db/Dao.kt`
DAOs for each table.

```kotlin
package com.example.newiptv.data.db

import androidx.room.*
import com.example.newiptv.data.db.entities.*

@Dao
interface CategoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<CategoryEntity>)

    @Query("SELECT * FROM categories WHERE type = :type")
    suspend fun getCategories(type: String): List<CategoryEntity>
}

@Dao
interface ItemDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<ItemEntity>)

    @Query("SELECT * FROM items WHERE categoryId = :categoryId")
    suspend fun getItemsByCategory(categoryId: String): List<ItemEntity>
}

@Dao
interface InfoDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: InfoEntity)

    @Query("SELECT * FROM info WHERE itemId = :itemId")
    suspend fun getInfo(itemId: String): InfoEntity?
}

@Dao
interface EpisodeDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(episodes: List<EpisodeEntity>)

    @Query("SELECT * FROM episodes WHERE itemId = :itemId ORDER BY season, episodeNum")
    suspend fun getEpisodes(itemId: String): List<EpisodeEntity>
}
```

---

# 📂 `data/db/AppDatabase.kt`
Room database holding everything.

```kotlin
package com.example.newiptv.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.example.newiptv.data.db.entities.*

@Database(
    entities = [CategoryEntity::class, ItemEntity::class, InfoEntity::class, EpisodeEntity::class],
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

---

# 📂 `data/mapping/ApiTVMapping.kt`
Maps API responses → Entities.

```kotlin
package com.example.newiptv.data.mapping

import com.example.newiptv.data.db.entities.*

object ApiTVMapping {

    // 🔹 Map Categories
    fun mapCategory(api: Map<String, Any>, type: String): CategoryEntity {
        return CategoryEntity(
            categoryId = api["category_id"].toString(),
            categoryName = api["category_name"]?.toString() ?: "",
            parentId = (api["parent_id"]?.toString() ?: "0").toInt(),
            type = type
        )
    }

    // 🔹 Map Items (Series, Movies, Live TV entry)
    fun mapItem(api: Map<String, Any>, type: String): ItemEntity {
        return ItemEntity(
            itemId = api["series_id"]?.toString() ?: api["id"]?.toString() ?: "0",
            name = api["name"]?.toString() ?: "",
            cover = api["cover"]?.toString(),
            plot = api["plot"]?.toString(),
            cast = api["cast"]?.toString(),
            director = api["director"]?.toString(),
            genre = api["genre"]?.toString(),
            releaseDate = api["releaseDate"]?.toString(),
            lastModified = api["last_modified"]?.toString(),
            rating = api["rating"]?.toString(),
            rating5Based = api["rating_5based"]?.toString()?.toDoubleOrNull(),
            youtubeTrailer = api["youtube_trailer"]?.toString(),
            episodeRunTime = api["episode_run_time"]?.toString(),
            categoryId = api["category_id"]?.toString() ?: "",
            type = type
        )
    }

    // 🔹 Map Info (detailed metadata)
    fun mapInfo(api: Map<String, Any>, itemId: String, type: String): InfoEntity {
        return InfoEntity(
            itemId = itemId,
            name = api["name"]?.toString(),
            cover = api["cover"]?.toString(),
            plot = api["plot"]?.toString(),
            cast = api["cast"]?.toString(),
            director = api["director"]?.toString(),
            genre = api["genre"]?.toString(),
            releaseDate = api["releaseDate"]?.toString(),
            lastModified = api["last_modified"]?.toString(),
            rating = api["rating"]?.toString(),
            rating5Based = api["rating_5based"]?.toString()?.toDoubleOrNull(),
            youtubeTrailer = api["youtube_trailer"]?.toString(),
            episodeRunTime = api["episode_run_time"]?.toString(),
            categoryId = api["category_id"]?.toString() ?: "",
            type = type
        )
    }

    // 🔹 Map Episode
    fun mapEpisode(api: Map<String, Any>, itemId: String): EpisodeEntity {
        val info = api["info"] as? Map<String, Any> ?: emptyMap()
        return EpisodeEntity(
            id = api["id"]?.toString() ?: "0",
            itemId = itemId,
            season = api["season"]?.toString()?.toIntOrNull() ?: 0,
            episodeNum = api["episode_num"]?.toString()?.toIntOrNull() ?: 0,
            title = api["title"]?.toString(),
            containerExtension = api["container_extension"]?.toString(),
            movieImage = info["movie_image"]?.toString(),
            plot = info["plot"]?.toString(),
            rating = info["rating"]?.toString(),
            releaseDate = info["releasedate"]?.toString(),
            directSource = api["direct_source"]?.toString()
        )
    }
}
```

---

✅ Now you have:
- **Entities** (tables)  
- **DAOs** (queries)  
- **AppDatabase**  
- **Mapping from API → DB**

---

👉 Next step is to build the Retrofit **`TvApi.kt`** interface that hits:
- `get_series_categories`
- `get_series`
- `get_series_info`
(and same for Movies + Live TV).

---

Do you want me to now **write `TvApi.kt` with Retrofit + Moshi** (so your Repository can plug in and call `ApiTVMapping`)?




Perfect 👍 let’s wire up your **`TvApi.kt`** Retrofit interface so the Repository can call Hydra API for **Series, Movies, and Live TV**.
We’ll also prepare the **API models** (`data class`) that match the JSON structure, so Retrofit + Moshi/Gson can parse them automatically.

---

# 📂 `data/api/TvApi.kt`

Retrofit interface for Hydra API.

```kotlin
package com.example.newiptv.data.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

// Base: http://hydraa.cc:2095/player_api.php?username=xxx&password=yyy

interface TvApi {

    // 🔹 Dynamic endpoint loader (if you want to pass whole URL)
    @GET
    suspend fun getDynamic(@Url url: String): Any

    // 🔹 Categories (Series, Movies, Live TV)
    @GET("player_api.php")
    suspend fun getCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String // e.g. "get_series_categories"
    ): List<ApiCategory>

    // 🔹 Items list (Series, Movies, Live TV entries)
    @GET("player_api.php")
    suspend fun getItems(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // e.g. "get_series"
        @Query("category_id") categoryId: String? = null
    ): List<ApiItem>

    // 🔹 Detailed Info for one Series/Movie
    @GET("player_api.php")
    suspend fun getInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String, // e.g. "get_series_info"
        @Query("series_id") seriesId: String? = null,
        @Query("vod_id") movieId: String? = null,
        @Query("stream_id") liveId: String? = null
    ): ApiInfoResponse
}
```

---

# 📂 `data/api/models/ApiModels.kt`

DTOs for Hydra API.

```kotlin
package com.example.newiptv.data.api.models

// 🔹 Category JSON
data class ApiCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)

// 🔹 Item JSON (series, movies, live tv entry)
data class ApiItem(
    val num: Int?,
    val series_id: String?, // For series
    val stream_id: String?, // For live TV
    val movie_id: String?,  // For movies (if API supports)
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
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String
)

// 🔹 Info Response (Series/Movies)
data class ApiInfoResponse(
    val seasons: List<String>?,
    val info: ApiInfoDetail?,
    val episodes: Map<String, List<ApiEpisode>>? // key = season number
)

// 🔹 Info Details (metadata)
data class ApiInfoDetail(
    val name: String?,
    val cover: String?,
    val plot: String?,
    val cast: String?,
    val director: String?,
    val genre: String?,
    val releaseDate: String?,
    val last_modified: String?,
    val rating: String?,
    val rating_5based: Double?,
    val backdrop_path: List<String>?,
    val youtube_trailer: String?,
    val episode_run_time: String?,
    val category_id: String?
)

// 🔹 Episode JSON
data class ApiEpisode(
    val id: String,
    val episode_num: Int,
    val title: String?,
    val container_extension: String?,
    val info: ApiEpisodeInfo?,
    val added: String?,
    val season: Int,
    val direct_source: String?
)

data class ApiEpisodeInfo(
    val movie_image: String?,
    val plot: String?,
    val rating: String?,
    val releasedate: String?
)
```

---

# 📂 Retrofit Builder (Singleton)

Put in `TvApiClient.kt`.

```kotlin
package com.example.newiptv.data.api

import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object TvApiClient {
    private const val BASE_URL = "http://hydraa.cc:2095/"

    val api: TvApi by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(MoshiConverterFactory.create())
            .build()
            .create(TvApi::class.java)
    }
}
```

---

# 🔗 How this plugs into your DB

* `TvApi` fetches → `ApiModels`
* `ApiTVMapping` maps `ApiModels` → Room Entities
* DAO saves them in **Room DB**
* UI observes DB tables via LiveData/Flow

So whether it’s **Series, Movies, or Live TV**, the flow is the same.

---

✅ Next step: I’ll **write the Repository** so your UI calls one function like

```kotlin
repository.syncCategories("series")
repository.syncItems("series", categoryId)
repository.syncInfo("series", seriesId)
```

and it updates Room DB automatically.

Do you want me to generate the **Repository layer** now?
