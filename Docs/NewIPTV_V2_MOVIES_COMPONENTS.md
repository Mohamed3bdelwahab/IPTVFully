# 🎬 NewIPTV V2 - Movies System Components Documentation

## 🎯 **Component Overview**

This document provides detailed information about each component of the NewIPTV V2 Movies system, including implementation details, usage examples, and technical specifications.

---

## 🗄️ **Database Entities**

### **1. MovieCategoryEntity**
**Purpose**: Represents movie categories (VOD categories) from the API

**Location**: `app/src/main/java/com/example/newiptv/data/db/entities/MovieEntities.kt`

**Structure**:
```kotlin
@Entity(tableName = "movie_categories")
data class MovieCategoryEntity(
    @PrimaryKey val categoryId: String,      // "301"
    val categoryName: String,                // "2025 English | أجنبي"
    val parentId: Int,                       // 0
    val type: String = "movies"              // Always "movies"
)
```

**Usage Example**:
```kotlin
// Create a new category
val category = MovieCategoryEntity(
    categoryId = "301",
    categoryName = "2025 English | أجنبي",
    parentId = 0,
    type = "movies"
)

// Access category properties
val id = category.categoryId        // "301"
val name = category.categoryName    // "2025 English | أجنبي"
```

---

### **2. MovieItemEntity**
**Purpose**: Represents individual movies from the VOD streams API

**Structure**:
```kotlin
@Entity(tableName = "movie_items")
data class MovieItemEntity(
    @PrimaryKey val itemId: String,          // stream_id as String
    val name: String,                        // Movie title
    val streamType: String,                  // "movie"
    val streamId: String,                    // stream_id for playback
    val streamIcon: String?,                 // Cover image URL
    val rating: String?,                     // Rating string
    val rating5Based: Double?,               // Rating on 5-point scale
    val added: String?,                      // Unix timestamp
    val isAdult: String,                     // "0" or "1"
    val categoryId: String,                  // category_id
    val containerExtension: String?,         // mkv, mp4, avi
    val customSid: String?,                  // Custom session ID
    val directSource: String?,               // Direct source URL
    val type: String = "movies"              // Always "movies"
)
```

**Usage Example**:
```kotlin
// Create a new movie item
val movie = MovieItemEntity(
    itemId = "590993",
    name = "برشلونة & رايو فاييكانو - الدورى الأسبانى 31.08.2025",
    streamType = "movie",
    streamId = "590993",
    streamIcon = "https://mediafireupload.xyz/uploads/175669612231061.png",
    rating = "",
    rating5Based = 0.0,
    added = "1756696104",
    isAdult = "0",
    categoryId = "243",
    containerExtension = "mkv",
    customSid = "",
    directSource = "",
    type = "movies"
)

// Access movie properties
val title = movie.name                    // Movie title
val coverUrl = movie.streamIcon           // Cover image URL
val rating = movie.rating5Based          // Rating value
```

---

### **3. MovieInfoEntity**
**Purpose**: Contains detailed metadata about movies from the VOD info API

**Structure**:
```kotlin
@Entity(tableName = "movie_info")
data class MovieInfoEntity(
    @PrimaryKey val itemId: String,          // stream_id
    val name: String?,                       // Movie title
    val originalName: String?,               // o_name
    val coverBig: String?,                   // cover_big
    val movieImage: String?,                 // movie_image
    val releaseDate: String?,                // releasedate
    val episodeRunTime: Int,                 // episode_run_time
    val youtubeTrailer: String?,             // youtube_trailer
    val director: String?,                   // director
    val actors: String?,                     // actors
    val cast: String?,                       // cast
    val description: String?,                // description
    val plot: String?,                       // plot
    val age: String?,                        // age
    val ratingMpaa: String?,                 // rating_mpaa
    val ratingKinopoisk: Double?,            // rating_kinopoisk
    val ratingCountKinopoisk: Int,           // rating_count_kinopoisk
    val country: String?,                    // country
    val genre: String?,                      // genre
    val backdropPath: String?,               // backdrop_path
    val tmdbId: String?,                     // tmdb_id
    val durationSecs: Int,                   // duration_secs
    val duration: String?,                   // duration (HH:MM:SS)
    val bitrate: Int,                        // bitrate
    val backdrop: String?,                   // backdrop
    val rating: String?,                     // rating
    val categoryId: String,                  // category_id
    val type: String = "movies"              // Always "movies"
)
```

**Usage Example**:
```kotlin
// Create movie info
val movieInfo = MovieInfoEntity(
    itemId = "592368",
    name = "Dave Chappelle: Equanimity",
    originalName = "Dave Chappelle: Equanimity",
    coverBig = "http://hytv.xyz:80/images/4EaNffvPj5uTxRbFcmiTkor1lZW_big.jpg",
    releaseDate = "2017-12-31",
    episodeRunTime = 0,
    youtubeTrailer = "cZ5AmFo4vbY",
    director = "Stan Lathan",
    cast = "Dave Chappelle",
    plot = "Comedy legend Dave Chappelle returns to his roots...",
    genre = "Comedy",
    country = "United States of America",
    tmdbId = "488223",
    durationSecs = 3830,
    duration = "01:03:50",
    bitrate = 4239,
    categoryId = "148",
    type = "movies"
)

// Access detailed information
val plot = movieInfo.plot                    // Movie plot
val director = movieInfo.director            // Director name
val cast = movieInfo.cast                    // Cast information
val duration = movieInfo.duration            // Duration in HH:MM:SS
```

---

### **4. MovieStreamDataEntity**
**Purpose**: Additional stream information for movies

**Structure**:
```kotlin
@Entity(tableName = "movie_stream_data")
data class MovieStreamDataEntity(
    @PrimaryKey val itemId: String,          // stream_id
    val name: String?,                       // Movie name
    val added: String?,                      // Added timestamp
    val categoryId: String,                  // category_id
    val containerExtension: String?,         // mkv, mp4, avi
    val customSid: String?,                  // Custom session ID
    val directSource: String?,               // Direct source URL
    val type: String = "movies"              // Always "movies"
)
```

---

## 🔄 **Data Access Objects (DAOs)**

### **1. MovieCategoryDao**
**Purpose**: Database operations for movie categories

**Location**: `app/src/main/java/com/example/newiptv/data/db/MovieDao.kt`

**Key Methods**:
```kotlin
@Dao
interface MovieCategoryDao {
    // Insert multiple categories
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(categories: List<MovieCategoryEntity>)

    // Get all categories as Flow
    @Query("SELECT * FROM movie_categories ORDER BY categoryName")
    fun getAllCategories(): Flow<List<MovieCategoryEntity>>

    // Get all categories synchronously
    @Query("SELECT * FROM movie_categories ORDER BY categoryName")
    suspend fun getAllCategoriesSync(): List<MovieCategoryEntity>

    // Clear all categories
    @Query("DELETE FROM movie_categories")
    suspend fun deleteAll()
}
```

**Usage Example**:
```kotlin
// Get all categories
val categories = movieCategoryDao.getAllCategories()

// Insert categories
val categoryList = listOf(category1, category2, category3)
movieCategoryDao.insertAll(categoryList)

// Get categories synchronously
val categoriesSync = movieCategoryDao.getAllCategoriesSync()
```

---

### **2. MovieItemDao**
**Purpose**: Database operations for movie items

**Key Methods**:
```kotlin
@Dao
interface MovieItemDao {
    // Insert multiple movies
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MovieItemEntity>)

    // Get movies by category as Flow
    @Query("SELECT * FROM movie_items WHERE categoryId = :categoryId ORDER BY name")
    fun getMoviesByCategory(categoryId: String): Flow<List<MovieItemEntity>>

    // Get movies by category synchronously
    @Query("SELECT * FROM movie_items WHERE categoryId = :categoryId ORDER BY name")
    suspend fun getMoviesByCategorySync(categoryId: String): List<MovieItemEntity>

    // Get movie by ID
    @Query("SELECT * FROM movie_items WHERE itemId = :itemId")
    suspend fun getMovieById(itemId: String): MovieItemEntity?

    // Delete movies by category
    @Query("DELETE FROM movie_items WHERE categoryId = :categoryId")
    suspend fun deleteByCategory(categoryId: String)
}
```

**Usage Example**:
```kotlin
// Get movies for a specific category
val movies = movieItemDao.getMoviesByCategory("301")

// Get a specific movie
val movie = movieItemDao.getMovieById("590993")

// Insert movies
val movieList = listOf(movie1, movie2, movie3)
movieItemDao.insertAll(movieList)
```

---

### **3. MovieInfoDao**
**Purpose**: Database operations for movie information

**Key Methods**:
```kotlin
@Dao
interface MovieInfoDao {
    // Insert movie info
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: MovieInfoEntity)

    // Get movie info by ID
    @Query("SELECT * FROM movie_info WHERE itemId = :itemId")
    suspend fun getMovieInfo(itemId: String): MovieInfoEntity?

    // Delete movie info
    @Query("DELETE FROM movie_info WHERE itemId = :itemId")
    suspend fun deleteByItemId(itemId: String)
}
```

---

### **4. MovieStreamDataDao**
**Purpose**: Database operations for movie stream data

**Key Methods**:
```kotlin
@Dao
interface MovieStreamDataDao {
    // Insert stream data
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(streamData: MovieStreamDataEntity)

    // Get stream data by ID
    @Query("SELECT * FROM movie_stream_data WHERE itemId = :itemId")
    suspend fun getStreamData(itemId: String): MovieStreamDataEntity?
}
```

---

## 🌐 **API Models**

### **1. ApiMovieCategory**
**Purpose**: API response model for movie categories

**Location**: `app/src/main/java/com/example/newiptv/data/api/models/MovieApiModels.kt`

**Structure**:
```kotlin
data class ApiMovieCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int
)
```

**Usage Example**:
```kotlin
// Parse API response
val apiCategory = ApiMovieCategory(
    category_id = "301",
    category_name = "2025 English | أجنبي",
    parent_id = 0
)

// Convert to database entity
val categoryEntity = MovieApiMapping.mapMovieCategory(apiCategory)
```

---

### **2. ApiMovieItem**
**Purpose**: API response model for movie items

**Structure**:
```kotlin
data class ApiMovieItem(
    val num: Int?,
    val name: String,
    val stream_type: String,
    val stream_id: Int,
    val stream_icon: String?,
    val rating: String?,
    val rating_5based: Double?,
    val added: String?,
    val is_adult: String?,
    val category_id: String,
    val container_extension: String?,
    val custom_sid: String?,
    val direct_source: String?
)
```

---

### **3. ApiMovieInfoResponse**
**Purpose**: API response model for movie information

**Structure**:
```kotlin
data class ApiMovieInfoResponse(
    val info: ApiMovieInfoDetail?,
    val movie_data: ApiMovieStreamData?
)
```

---

## 🔄 **Data Mapping**

### **MovieApiMapping**
**Purpose**: Converts API responses to database entities

**Location**: `app/src/main/java/com/example/newiptv/data/mapping/MovieApiMapping.kt`

**Key Methods**:
```kotlin
object MovieApiMapping {
    // Map category API to entity
    fun mapMovieCategory(api: ApiMovieCategory): MovieCategoryEntity

    // Map movie item API to entity
    fun mapMovieItem(api: ApiMovieItem): MovieItemEntity

    // Map movie info API to entity
    fun mapMovieInfo(api: ApiMovieInfoDetail, streamData: ApiMovieStreamData?): MovieInfoEntity

    // Construct playback URL
    fun constructMoviePlaybackUrl(streamId: String, containerExtension: String?): String

    // Get TMDB image URL
    fun getTmdbImageUrl(path: String?, size: String = "w500"): String?

    // Get YouTube trailer URL
    fun getYouTubeTrailerUrl(key: String?): String?
}
```

**Usage Example**:
```kotlin
// Map API response to entity
val movieEntity = MovieApiMapping.mapMovieItem(apiMovieItem)

// Construct playback URL
val playbackUrl = MovieApiMapping.constructMoviePlaybackUrl("590993", "mkv")
// Result: "http://aws85485.amazonedge.net//movie/moh7amed819/150730/590993.mkv"

// Get TMDB image URL
val imageUrl = MovieApiMapping.getTmdbImageUrl("/path/to/image.jpg", "w500")
// Result: "https://image.tmdb.org/t/p/w500/path/to/image.jpg"
```

---

## 📡 **API Interfaces**

### **1. MovieApi**
**Purpose**: Hydra API endpoints for movies

**Location**: `app/src/main/java/com/example/newiptv/data/api/MovieApi.kt`

**Endpoints**:
```kotlin
interface MovieApi {
    // Get movie categories
    @GET("player_api.php")
    suspend fun getMovieCategories(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_categories"
    ): List<ApiMovieCategory>

    // Get movies by category
    @GET("player_api.php")
    suspend fun getMoviesByCategory(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_streams",
        @Query("category_id") categoryId: String
    ): List<ApiMovieItem>

    // Get movie details
    @GET("player_api.php")
    suspend fun getMovieInfo(
        @Query("username") username: String,
        @Query("password") password: String,
        @Query("action") action: String = "get_vod_info",
        @Query("vod_id") movieId: String
    ): ApiMovieInfoResponse
}
```

---

### **2. TmdbApi**
**Purpose**: TMDB API endpoints for enhanced movie data

**Endpoints**:
```kotlin
interface TmdbApi {
    // Get movie details
    @GET("movie/{movie_id}")
    suspend fun getMovieDetails(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbMovie

    // Get movie cast & crew
    @GET("movie/{movie_id}/credits")
    suspend fun getMovieCredits(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbCredits

    // Get movie trailers
    @GET("movie/{movie_id}/videos")
    suspend fun getMovieVideos(
        @retrofit2.http.Path("movie_id") movieId: String,
        @Query("api_key") apiKey: String
    ): ApiTmdbVideos
}
```

---

## 📊 **Repository Layer**

### **MovieRepository**
**Purpose**: Business logic and API coordination for movies

**Location**: `app/src/main/java/com/example/newiptv/data/repository/MovieRepository.kt`

**Key Methods**:
```kotlin
class MovieRepository(
    private val movieCategoryDao: MovieCategoryDao,
    private val movieItemDao: MovieItemDao,
    private val movieInfoDao: MovieInfoDao,
    private val movieStreamDataDao: MovieStreamDataDao
) {
    // Load movie categories with sync
    fun loadMovieCategoriesWithSync(): Flow<Result<List<MovieCategoryEntity>>>

    // Load movies by category with sync
    fun loadMoviesByCategoryWithSync(categoryId: String): Flow<Result<List<MovieItemEntity>>>

    // Load movie info with sync
    fun loadMovieInfoWithSync(movieId: String): Flow<Result<MovieInfoEntity>>

    // Get movie categories from database
    fun getMovieCategories(): Flow<List<MovieCategoryEntity>>

    // Get movies by category from database
    fun getMoviesByCategory(categoryId: String): Flow<List<MovieItemEntity>>

    // Get movie info from database
    suspend fun getMovieInfo(movieId: String): MovieInfoEntity?

    // Get movie by ID from database
    suspend fun getMovieById(movieId: String): MovieItemEntity?
}
```

**Usage Example**:
```kotlin
// Initialize repository
val repository = MovieRepository(
    database.movieCategoryDao(),
    database.movieItemDao(),
    database.movieInfoDao(),
    database.movieStreamDataDao()
)

// Load categories
repository.loadMovieCategoriesWithSync().collectLatest { result ->
    result.fold(
        onSuccess = { categories -> 
            // Handle success
        },
        onFailure = { exception -> 
            // Handle error
        }
    )
}

// Get movies for category
val movies = repository.getMoviesByCategory("301")
```

---

## 🎨 **UI Components**

### **1. MoviesScreen**
**Purpose**: Main movie browsing interface

**Location**: `app/src/main/java/com/example/newiptv/ui/movies/MoviesScreen.kt`

**Key Features**:
- Category navigation (left panel)
- Movie grid display (right panel)
- TV remote navigation support
- Filter system
- Real-time data loading

**Layout**: `app/src/main/res/layout/activity_movies_screen.xml`

---

### **2. MovieInfoScreen**
**Purpose**: Detailed movie information display

**Location**: `app/src/main/java/com/example/newiptv/ui/movieinfo/MovieInfoScreen.kt`

**Key Features**:
- Movie poster and backdrop display
- Comprehensive movie details
- Play button for immediate playback
- TV remote navigation

**Layout**: `app/src/main/res/layout/activity_movie_info_screen.xml`

---

### **3. MovieCategoryAdapter**
**Purpose**: Adapter for movie category list

**Location**: `app/src/main/java/com/example/newiptv/ui/movies/MovieCategoryAdapter.kt`

**Key Features**:
- Category name display
- Movie count badges
- Focus change logging
- Dynamic updates

---

### **4. MoviesAdapter**
**Purpose**: Adapter for movie grid

**Location**: `app/src/main/java/com/example/newiptv/ui/movies/MoviesAdapter.kt`

**Key Features**:
- Movie cover image loading
- Title, rating, and year display
- Click handling for movie selection
- Focus management for TV navigation

---

## 🔧 **Configuration**

### **MovieApiClient**
**Purpose**: Retrofit configuration for movie APIs

**Location**: `app/src/main/java/com/example/newiptv/data/api/MovieApiClient.kt`

**Configuration**:
```kotlin
object MovieApiClient {
    // Hydra API
    private const val HYDRA_BASE_URL = "http://hydraa.cc:2095/"
    
    val hydraApi: MovieApi by lazy {
        Retrofit.Builder()
            .baseUrl(HYDRA_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MovieApi::class.java)
    }
    
    // TMDB API
    private const val TMDB_BASE_URL = "https://api.themoviedb.org/3/"
    private const val TMDB_API_KEY = "your_api_key_here"
    
    val tmdbApi: TmdbApi by lazy {
        Retrofit.Builder()
            .baseUrl(TMDB_BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(TmdbApi::class.java)
    }
}
```

---

## 📱 **Layout Resources**

### **1. Movie Category Item**
**File**: `app/src/main/res/layout/item_movie_category.xml`

**Features**:
- Horizontal layout with category name and count
- Focus-aware background selector
- Responsive text sizing

### **2. Movie Item**
**File**: `app/src/main/res/layout/item_movie.xml`

**Features**:
- Card-based design with rounded corners
- Movie cover image display
- Title, rating, and year information
- Focus-aware background selector

### **3. Movie Info Screen**
**File**: `app/src/main/res/layout/activity_movie_info_screen.xml`

**Features**:
- Hero section with backdrop and poster
- Comprehensive information layout
- Responsive design for different screen sizes

---

## 🎯 **Usage Examples**

### **Complete Movie Loading Flow**
```kotlin
// 1. Initialize repository
val repository = MovieRepository(
    database.movieCategoryDao(),
    database.movieItemDao(),
    database.movieInfoDao(),
    database.movieStreamDataDao()
)

// 2. Load categories
repository.loadMovieCategoriesWithSync().collectLatest { result ->
    result.fold(
        onSuccess = { categories ->
            // Display categories in UI
            categoryAdapter.updateCategories(categories)
            
            // Load movies for first category
            if (categories.isNotEmpty()) {
                loadMoviesForCategory(categories[0].categoryId)
            }
        },
        onFailure = { exception ->
            // Handle error
        }
    )
}

// 3. Load movies for category
fun loadMoviesForCategory(categoryId: String) {
    repository.loadMoviesByCategoryWithSync(categoryId).collectLatest { result ->
        result.fold(
            onSuccess = { movies ->
                // Display movies in grid
                moviesAdapter.updateMovies(movies)
            },
            onFailure = { exception ->
                // Handle error
            }
        )
    }
}

// 4. Load movie details
fun loadMovieDetails(movieId: String) {
    repository.loadMovieInfoWithSync(movieId).collectLatest { result ->
        result.fold(
            onSuccess = { movieInfo ->
                // Display movie details
                displayMovieInfo(movieInfo)
            },
            onFailure = { exception ->
                // Handle error
            }
        )
    }
}
```

---

## 🔍 **Debugging & Testing**

### **Logging**
All components use `KeyEventLogger` for comprehensive logging:
- Focus changes
- Navigation events
- API calls
- Error conditions

### **Error Handling**
- Graceful fallback to cached data
- User-friendly error messages
- Comprehensive exception logging

### **Performance Monitoring**
- Database query performance
- API response times
- UI rendering metrics

---

This component documentation provides a comprehensive understanding of each part of the Movies system, enabling developers to work with, modify, and extend the system effectively.
