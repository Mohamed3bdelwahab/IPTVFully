# 📚 NewIPTV V2 - Movies API Mapping Documentation

## 🎯 **Overview**
This document outlines the complete API structure for Movies (VOD) in the NewIPTV application, including data flow, API endpoints, and database mapping. The Movies API follows a different structure than Series, using `get_vod_*` endpoints and `stream_id` identifiers.

---

## 🔗 **API Endpoints Structure**

### **1. Categories API**
```
GET: http://hydraa.cc:2095/player_api.php?username=moh7amed819&password=150730&action=get_vod_categories
```
**Response:** JSON array of movie categories
```json
[
  {
    "category_id": "301",
    "category_name": "2025 English | أجنبي",
    "parent_id": 0
  },
  {
    "category_id": "302",
    "category_name": "2025 Arabic | عربي",
    "parent_id": 0
  },
  {
    "category_id": "248",
    "category_name": "2024 Arabic | عربي",
    "parent_id": 0
  }
]
```

**Category Types Include:**
- **Year-based:** 2025 English/Arabic, 2024 English/Arabic, 2023 English/Arabic, 2022 English/Arabic
- **Quality:** 4K | الأعلي جودة
- **Genre:** Action, Comedy, Drama, Horror, Thriller, Romance, Sci-Fi, Fantasy
- **Platform:** Netflix, Disney+, HBO
- **Regional:** Egyptian, Arabic, Turkish, Indian, Asian, European
- **Special:** Oscar Movies, Top 100 IMDB, Box Office, Weekend Movies

### **2. Movies List API**
```
GET: http://hydraa.cc:2095/player_api.php?username=moh7amed819&password=150730&action=get_vod_streams
```
**Response:** JSON array of movies
```json
[
  {
    "num": 1,
    "name": "برشلونة & رايو فاييكانو - الدورى الأسبانى 31.08.2025",
    "stream_type": "movie",
    "stream_id": 590993,
    "stream_icon": "https://mediafireupload.xyz/uploads/175669612231061.png",
    "rating": "",
    "rating_5based": 0,
    "added": "1756696104",
    "is_adult": "0",
    "category_id": "243",
    "container_extension": "mkv",
    "custom_sid": "",
    "direct_source": ""
  },
  {
    "num": 2,
    "name": "أرسنال & ليفربول - الدورى الإنجليزى 31.08.2025",
    "stream_type": "movie",
    "stream_id": 590958,
    "stream_icon": "https://mediafireupload.xyz/uploads/175666350170331.png",
    "rating": "",
    "rating_5based": 0,
    "added": "1756664462",
    "is_adult": "0",
    "category_id": "243",
    "container_extension": "mkv",
    "custom_sid": "",
    "direct_source": ""
  }
]
```

**Key Fields:**
- `stream_id`: Unique identifier for the movie (used for playback)
- `name`: Movie title (can be in Arabic or English)
- `stream_icon`: Cover image URL
- `category_id`: Links to category
- `container_extension`: Video format (mkv, mp4, avi)
- `added`: Unix timestamp when added
- `rating_5based`: Rating on 5-point scale

### **3. Movie Details API**
```
GET: http://hydraa.cc:2095/player_api.php?username=moh7amed819&password=150730&action=get_vod_info&vod_id={stream_id}
```
**Response:** JSON object with movie info and stream data
```json
{
  "info": {
    "kinopoisk_url": "https://www.themoviedb.org/movie/488223",
    "name": "Dave Chappelle: Equanimity",
    "o_name": "Dave Chappelle: Equanimity",
    "cover_big": "http://hytv.xyz:80/images/4EaNffvPj5uTxRbFcmiTkor1lZW_big.jpg",
    "movie_image": "http://hytv.xyz:80/images/4EaNffvPj5uTxRbFcmiTkor1lZW_big.jpg",
    "releasedate": "2017-12-31",
    "episode_run_time": 0,
    "youtube_trailer": "cZ5AmFo4vbY",
    "director": "Stan Lathan",
    "actors": "Dave Chappelle",
    "cast": "Dave Chappelle",
    "description": "Comedy legend Dave Chappelle returns to his roots...",
    "plot": "يعود أسطورة الكوميديا ​​ديف شابيل إلى جذوره...",
    "age": "16+",
    "rating_mpaa": "R",
    "rating_kinopoisk": 7.446,
    "rating_count_kinopoisk": 232,
    "country": "United States of America",
    "genre": "Comedy",
    "backdrop_path": [],
    "tmdb_id": "488223",
    "duration_secs": 3830,
    "duration": "01:03:50",
    "video": { /* video codec details */ },
    "audio": { /* audio codec details */ },
    "bitrate": 4239,
    "backdrop": "",
    "rating": ""
  },
  "movie_data": {
    "stream_id": 592368,
    "name": "Dave Chappelle: Equanimity 2017",
    "added": "1756997229",
    "category_id": "148",
    "container_extension": "mkv",
    "custom_sid": "",
    "direct_source": ""
  }
}
```

---

## 🎬 **Enhanced Movie Data via TMDB API**

### **4. TMDB Movie Details**
```
GET: https://api.themoviedb.org/3/movie/{tmdb_id}?api_key={api_key}
```
**Response:** Enhanced movie metadata
```json
{
  "id": 488223,
  "title": "Dave Chappelle: Equanimity",
  "overview": "Comedy legend Dave Chappelle returns to his roots...",
  "backdrop_path": "/wiRJsxQd5pFUmjw77IR9n0Lv0IX.jpg",
  "poster_path": "/4EaNffvPj5uTxRbFcmiTkor1lZW.jpg",
  "release_date": "2017-12-31",
  "runtime": 64,
  "vote_average": 7.446,
  "vote_count": 232,
  "genres": [{"id": 35, "name": "Comedy"}],
  "production_countries": [{"iso_3166_1": "US", "name": "United States of America"}],
  "spoken_languages": [{"english_name": "English", "iso_639_1": "en", "name": "English"}]
}
```

### **5. TMDB Cast & Crew**
```
GET: https://api.themoviedb.org/3/movie/{tmdb_id}/credits?api_key={api_key}
```
**Response:** Cast and crew information
```json
{
  "cast": [
    {
      "id": 1234,
      "name": "Actor Name",
      "character": "Character Name",
      "profile_path": "/path/to/image.jpg"
    }
  ],
  "crew": [
    {
      "id": 5678,
      "name": "Director Name",
      "job": "Director",
      "profile_path": "/path/to/image.jpg"
    }
  ]
}
```

### **6. TMDB Trailers**
```
GET: https://api.themoviedb.org/3/movie/{tmdb_id}/videos?api_key={api_key}
```
**Response:** Movie trailers and videos
```json
{
  "results": [
    {
      "id": "abc123",
      "key": "cZ5AmFo4vbY",
      "name": "Official Trailer",
      "site": "YouTube",
      "type": "Trailer"
    }
  ]
}
```

---

## 🖼️ **Image URLs Construction**

### **Poster Images**
```
https://image.tmdb.org/t/p/w500/{poster_path}
```
**Example:**
```
https://image.tmdb.org/t/p/w500/4EaNffvPj5uTxRbFcmiTkor1lZW.jpg
```

### **Backdrop Images**
```
https://image.tmdb.org/t/p/w1280/{backdrop_path}
```
**Example:**
```
https://image.tmdb.org/t/p/w1280/wiRJsxQd5pFUmjw77IR9n0Lv0IX.jpg
```

**Available Sizes:**
- `w92`, `w154`, `w185`, `w342`, `w500`, `w780`, `w1280`
- `original` for full resolution

---

## 🎥 **Video Playback URL Construction**

### **Direct Stream URL Pattern**
```
http://aws85485.amazonedge.net//movie/moh7amed819/150730/{stream_id}.{container_extension}
```

**Example:**
```
http://aws85485.amazonedge.net//movie/moh7amed819/150730/592368.mkv
```

**URL Components:**
- **Base:** `http://aws85485.amazonedge.net//movie/`
- **Credentials:** `moh7amed819/150730/`
- **Stream ID:** `{stream_id}` (from API response)
- **Extension:** `.{container_extension}` (mkv, mp4, avi, etc.)

---

## 🗄️ **Database Entity Mapping**

### **CategoryEntity**
```kotlin
@Entity(tableName = "categories")
data class CategoryEntity(
    @PrimaryKey val categoryId: String,      // "301"
    val categoryName: String,                // "2025 English | أجنبي"
    val parentId: Int,                       // 0
    val type: String                         // "movies"
)
```

### **ItemEntity (Movies)**
```kotlin
@Entity(tableName = "items")
data class ItemEntity(
    @PrimaryKey val itemId: String,          // stream_id as String
    val name: String,                        // Movie title
    val cover: String?,                      // stream_icon or TMDB poster
    val plot: String?,                       // From TMDB overview
    val cast: String?,                       // From TMDB cast
    val director: String?,                   // From TMDB crew
    val genre: String?,                      // From TMDB genres
    val releaseDate: String?,                // From TMDB release_date
    val rating: String?,                     // From TMDB vote_average
    val rating5Based: Double?,               // From TMDB vote_average
    val backdrops: String?,                  // TMDB backdrop_path
    val youtubeTrailer: String?,             // From TMDB videos
    val episodeRunTime: String?,             // From TMDB runtime
    val categoryId: String,                  // category_id
    val type: String,                        // "movies"
    val streamId: String,                    // stream_id for playback
    val containerExtension: String?,         // mkv, mp4, etc.
    val tmdbId: String?                      // TMDB movie ID
)
```

### **InfoEntity (Movie Details)**
```kotlin
@Entity(tableName = "info")
data class InfoEntity(
    @PrimaryKey val itemId: String,          // stream_id
    val name: String?,                       // Movie title
    val cover: String?,                      // TMDB poster
    val plot: String?,                       // TMDB overview
    val cast: String?,                       // TMDB cast
    val director: String?,                   // TMDB crew
    val genre: String?,                      // TMDB genres
    val releaseDate: String?,                // TMDB release_date
    val rating: String?,                     // TMDB vote_average
    val rating5Based: Double?,               // TMDB vote_average
    val backdrops: String?,                  // TMDB backdrop_path
    val youtubeTrailer: String?,             // TMDB trailer
    val runTime: String?,                    // TMDB runtime
    val categoryId: String,                  // category_id
    val type: String,                        // "movies"
    val tmdbId: String?,                     // TMDB movie ID
    val duration: String?,                   // Duration in HH:MM:SS
    val country: String?,                    // Production country
    val language: String?,                   // Spoken language
    val ageRating: String?                   // Age rating (16+, R, etc.)
)
```

---

## 🔄 **Data Flow Implementation**

### **1. Load Movie Categories**
```kotlin
// API Call: get_vod_categories
// Map to: CategoryEntity with type = "movies"
// Store in: categories table
// UI: Display in left panel of SeriesScreen
```

### **2. Load Movies by Category**
```kotlin
// API Call: get_vod_streams
// Map to: ItemEntity with type = "movies"
// Store in: items table
// UI: Display in right panel grid of SeriesScreen
```

### **3. Load Movie Details**
```kotlin
// API Call: get_vod_info&vod_id={stream_id}
// Enhanced with: TMDB API calls using tmdb_id
// Map to: InfoEntity with type = "movies"
// Store in: info table
// UI: Display in SeriesInfoScreen (adapted for movies)
```

### **4. Video Playback**
```kotlin
// Construct URL: aws85485.amazonedge.net//movie/moh7amed819/150730/{stream_id}.{container_extension}
// Launch: VideoPlayerActivity
// Handle: Movie-specific controls (no episode navigation)
```

---

## 🚀 **Implementation Steps - COMPLETED ✅**

### **Phase 1: Basic API Integration ✅**
1. ✅ **Updated API Models** to match the exact JSON structure from `get_vod_*` endpoints
2. ✅ **Enhanced ApiTVMapping** to handle movie-specific fields (`stream_id`, `container_extension`)
3. ✅ **Updated Repository** to call correct endpoints based on content type

### **Phase 2: UI Adaptation ✅**
4. ✅ **Created MoviesScreen** to handle movies as single items (no seasons/episodes)
5. ✅ **Created MovieInfoScreen** to show movie details instead of episode lists
6. ✅ **Adapted layouts** to display movie-specific information

### **Phase 3: Enhanced Features ✅**
7. ✅ **Implemented Watch Movie Button** for direct playback integration
8. ✅ **Added image loading** for posters and backdrops using Glide
9. ✅ **Integrated TV Remote Support** for complete navigation

### **Phase 4: Playback & Polish ✅**
10. ✅ **Updated VideoPlayerActivity** to handle movie playback URLs
11. ✅ **Added movie-specific controls** (no next/previous episode)
12. ✅ **Implemented proper error handling** and user feedback

## 🎯 **Current Implementation Status**

### **✅ COMPLETED FEATURES:**
- **Movies API Integration** - All `get_vod_*` endpoints implemented
- **Movies Screen** - Full movie browsing with categories and grid
- **Movie Info Screen** - Detailed movie information display
- **Watch Movie Button** - Direct playback integration
- **Database Schema** - Movie entities and data storage
- **TV Remote Support** - Complete remote control integration
- **Error Handling** - Graceful error display and recovery
- **Performance Optimization** - Efficient data loading and caching

### **🔄 FUTURE ENHANCEMENTS:**
- **TMDB API Integration** for enhanced movie metadata
- **Trailer Support** from YouTube/TMDB
- **Advanced Filtering** and search functionality
- **Favorites System** and user preferences
- **Quality Selection** for multiple video sources

---

## ⚠️ **Key Differences from Series**

| Aspect | Series | Movies |
|--------|--------|--------|
| **API Endpoints** | `get_series_*` | `get_vod_*` |
| **ID Field** | `series_id` | `stream_id` |
| **Structure** | Seasons → Episodes | Single Item |
| **Playback** | Episode-based | Direct movie file |
| **Navigation** | Season/Episode lists | Movie details only |
| **Container** | N/A | `container_extension` |
| **Enhanced Data** | Limited | TMDB integration |

---

## 🔧 **Technical Considerations**

### **Error Handling**
- **API Failures:** Fallback to basic movie data if TMDB fails
- **Missing Images:** Use placeholder images for missing posters/backdrops
- **Playback Issues:** Handle different video formats and codecs

### **Performance**
- **Image Caching:** Implement Glide for efficient image loading
- **Data Caching:** Store movie data locally to reduce API calls
- **Lazy Loading:** Load movie details only when selected

### **User Experience**
- **Loading States:** Show progress indicators during API calls
- **Error Messages:** User-friendly error messages for failed operations
- **Offline Support:** Basic functionality when network is unavailable

---

## 📱 **UI/UX Adaptations**

### **SeriesScreen (Adapted for Movies)**
- **Left Panel:** Movie categories with counts
- **Right Panel:** Movie grid (3 columns)
- **Filter Options:** Year, Rating, Genre, Language
- **Search:** Movie title search functionality

### **SeriesInfoScreen (Adapted for Movies)**
- **Header:** Movie title, poster, backdrop
- **Details:** Plot, cast, director, genre, release date
- **Media:** Trailer button, image gallery
- **Playback:** Single play button (no episode selection)

### **VideoPlayerActivity**
- **Controls:** Play/pause, seek, volume, quality
- **Info:** Movie title, duration, progress
- **Navigation:** Back to movie details (no episode navigation)

---

This comprehensive mapping ensures that our dynamic SeriesScreen approach will work perfectly for both Series and Movies, with the correct API calls, data handling, and user experience for each content type.
