# 🎬 NewIPTV V2 - Complete Movies System Overview

## 🎯 **System Architecture**

The NewIPTV V2 Movies system is a **completely separate and independent** implementation from the Series system, following the same architectural patterns but with movie-specific logic and API calls. This ensures clean separation of concerns and maintainability.

### **🔹 System Components**

```
📱 UI Layer
├── 🎬 MoviesScreen (Main browsing interface)
├── 🎭 MovieInfoScreen (Detailed movie information)
└── 🎥 VideoPlayerActivity (Movie playback)

🗄️ Data Layer
├── 📊 MovieRepository (Business logic & API coordination)
├── 🔄 MovieApiMapping (API ↔ Database conversion)
└── 🌐 MovieApiClient (Network communication)

💾 Database Layer
├── 🏷️ MovieCategoryEntity (Movie categories)
├── 🎬 MovieItemEntity (Movie items)
├── ℹ️ MovieInfoEntity (Detailed movie metadata)
└── 📺 MovieStreamDataEntity (Stream information)

🌐 API Layer
├── 🎯 MovieApi (Hydra API endpoints)
├── 🎬 TmdbApi (TMDB enhanced metadata)
└── 📡 MovieApiClient (Retrofit configuration)
```

---

## 🚀 **Key Features**

### **1. Complete Movie Browsing**
- **Category Navigation**: Browse movies by year, genre, quality, platform
- **Grid Layout**: 3-column responsive grid for optimal TV viewing
- **Smart Filtering**: A-Z, Z-A, Latest, Oldest, Rating, Year-based sorting
- **Real-time Counts**: Display movie counts per category

### **2. Enhanced Movie Information**
- **Rich Metadata**: Plot, cast, director, genre, release date, rating
- **TMDB Integration**: Enhanced data from The Movie Database
- **High-Quality Images**: Posters and backdrop images
- **Trailer Support**: YouTube trailer integration

### **3. TV Remote Navigation**
- **D-Pad Support**: Full navigation with TV remote
- **Panel Switching**: Left (categories) ↔ Right (movies) navigation
- **Focus Management**: Visual feedback and smooth transitions
- **Enter/OK Support**: Select categories and movies

### **4. Advanced Filtering System**
```
Filter Options:
├── Default (No sorting)
├── A-Z (Alphabetical ascending)
├── Z-A (Alphabetical descending)
├── Latest (Newest first)
├── Oldest (Oldest first)
├── Rating (High to Low)
├── Rating (Low to High)
├── Year (Newest first)
└── Year (Oldest first)
```

---

## 🔗 **API Integration**

### **Primary API (Hydra)**
```
Endpoints:
├── get_vod_categories → Movie categories
├── get_vod_streams → Movie list by category
└── get_vod_info → Detailed movie information
```

### **Enhanced API (TMDB)**
```
Endpoints:
├── /movie/{id} → Enhanced movie details
├── /movie/{id}/credits → Cast & crew
└── /movie/{id}/videos → Trailers & videos
```

### **Video Playback**
```
URL Pattern:
http://aws85485.amazonedge.net//movie/moh7amed819/150730/{stream_id}.{extension}

Example:
http://aws85485.amazonedge.net//movie/moh7amed819/150730/592368.mkv
```

---

## 🗄️ **Database Schema**

### **Movie Categories Table**
```sql
CREATE TABLE movie_categories (
    categoryId TEXT PRIMARY KEY,      -- "301"
    categoryName TEXT NOT NULL,       -- "2025 English | أجنبي"
    parentId INTEGER NOT NULL,        -- 0
    type TEXT NOT NULL               -- "movies"
);
```

### **Movie Items Table**
```sql
CREATE TABLE movie_items (
    itemId TEXT PRIMARY KEY,          -- stream_id as String
    name TEXT NOT NULL,               -- Movie title
    streamType TEXT NOT NULL,         -- "movie"
    streamId TEXT NOT NULL,           -- stream_id for playback
    streamIcon TEXT,                  -- Cover image URL
    rating TEXT,                      -- Rating string
    rating5Based REAL,                -- Rating on 5-point scale
    added TEXT,                       -- Unix timestamp
    isAdult TEXT NOT NULL,            -- "0" or "1"
    categoryId TEXT NOT NULL,         -- category_id
    containerExtension TEXT,          -- mkv, mp4, avi
    customSid TEXT,                   -- Custom session ID
    directSource TEXT,                -- Direct source URL
    type TEXT NOT NULL               -- "movies"
);
```

### **Movie Info Table**
```sql
CREATE TABLE movie_info (
    itemId TEXT PRIMARY KEY,          -- stream_id
    name TEXT,                        -- Movie title
    originalName TEXT,                -- o_name
    coverBig TEXT,                    -- cover_big
    movieImage TEXT,                  -- movie_image
    releaseDate TEXT,                 -- releasedate
    episodeRunTime INTEGER NOT NULL,  -- episode_run_time
    youtubeTrailer TEXT,              -- youtube_trailer
    director TEXT,                    -- director
    actors TEXT,                      -- actors
    cast TEXT,                        -- cast
    description TEXT,                 -- description
    plot TEXT,                        -- plot
    age TEXT,                         -- age
    ratingMpaa TEXT,                  -- rating_mpaa
    ratingKinopoisk REAL,             -- rating_kinopoisk
    ratingCountKinopoisk INTEGER NOT NULL, -- rating_count_kinopoisk
    country TEXT,                     -- country
    genre TEXT,                       -- genre
    backdropPath TEXT,                -- backdrop_path
    tmdbId TEXT,                      -- tmdb_id
    durationSecs INTEGER NOT NULL,    -- duration_secs
    duration TEXT,                    -- duration (HH:MM:SS)
    bitrate INTEGER NOT NULL,         -- bitrate
    backdrop TEXT,                    -- backdrop
    rating TEXT,                      -- rating
    categoryId TEXT NOT NULL,         -- category_id
    type TEXT NOT NULL               -- "movies"
);
```

---

## 🔄 **Data Flow**

### **1. Category Loading**
```
User opens MoviesScreen
    ↓
Repository.loadMovieCategoriesWithSync()
    ↓
API Call: get_vod_categories
    ↓
Map to MovieCategoryEntity
    ↓
Store in movie_categories table
    ↓
Display in left panel
```

### **2. Movie Loading**
```
User selects category
    ↓
Repository.loadMoviesByCategoryWithSync(categoryId)
    ↓
API Call: get_vod_streams
    ↓
Map to MovieItemEntity
    ↓
Store in movie_items table
    ↓
Display in right panel grid
```

### **3. Movie Details**
```
User selects movie
    ↓
Repository.loadMovieInfoWithSync(movieId)
    ↓
API Call: get_vod_info
    ↓
Map to MovieInfoEntity
    ↓
Enhance with TMDB data (if available)
    ↓
Store in movie_info table
    ↓
Display in MovieInfoScreen
```

### **4. Video Playback**
```
User presses Enter/OK on movie
    ↓
Construct playback URL
    ↓
Launch VideoPlayerActivity
    ↓
Stream movie content
```

---

## 🎮 **User Experience Flow**

### **Navigation Pattern**
```
HomeScreen
    ↓
MoviesScreen (Categories + Movies Grid)
    ↓
MovieInfoScreen (Detailed Information)
    ↓
VideoPlayerActivity (Movie Playback)
```

### **TV Remote Controls**
```
D-Pad Left/Right: Switch between panels
D-Pad Up/Down: Navigate within panel
Enter/OK: Select item or play movie
Back: Return to previous screen
```

---

## 🛠️ **Technical Implementation**

### **Repository Pattern**
- **Single Responsibility**: Each repository handles one content type
- **Error Handling**: Graceful fallback to cached data
- **Caching Strategy**: Local database for offline support
- **API Coordination**: Seamless integration of multiple APIs

### **Adapter Pattern**
- **MovieCategoryAdapter**: Handles category display with counts
- **MoviesAdapter**: Manages movie grid with focus handling
- **Focus Management**: Visual feedback for TV navigation

### **Layout System**
- **Responsive Design**: Adapts to different screen sizes
- **TV Optimized**: Large touch targets and clear focus indicators
- **Material Design**: Consistent with Android design guidelines

---

## 🔧 **Configuration & Setup**

### **Required Dependencies**
```gradle
dependencies {
    // Room Database
    implementation "androidx.room:room-runtime:2.6.1"
    implementation "androidx.room:room-ktx:2.6.1"
    
    // Retrofit for API calls
    implementation "com.squareup.retrofit2:retrofit:2.9.0"
    implementation "com.squareup.retrofit2:converter-gson:2.9.0"
    
    // Glide for image loading
    implementation "com.github.bumptech.glide:glide:4.16.0"
    
    // Coroutines for async operations
    implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3"
}
```

### **API Configuration**
```kotlin
// Hydra API
HYDRA_BASE_URL = "http://hydraa.cc:2095/"

// TMDB API
TMDB_BASE_URL = "https://api.themoviedb.org/3/"
TMDB_API_KEY = "your_api_key_here"
```

---

## 📱 **Screen Specifications**

### **MoviesScreen**
- **Layout**: Horizontal split (Categories | Movies Grid)
- **Categories**: Left panel with filter spinner
- **Movies**: Right panel with 3-column grid
- **Navigation**: D-Pad left/right for panel switching

### **MovieInfoScreen**
- **Header**: Backdrop image with movie cover overlay
- **Details**: Comprehensive movie information
- **Actions**: Play button for immediate playback
- **Navigation**: Back button to return to browsing

---

## 🚀 **Performance Optimizations**

### **Image Loading**
- **Glide Integration**: Efficient image caching and loading
- **Placeholder Images**: Fallback for missing content
- **Memory Management**: Automatic cache size optimization

### **Database Operations**
- **Async Operations**: Coroutines for non-blocking database calls
- **Batch Operations**: Efficient bulk insert/update operations
- **Query Optimization**: Indexed queries for fast retrieval

### **Network Operations**
- **Caching Strategy**: Local storage for offline functionality
- **Error Handling**: Graceful degradation on network failures
- **Retry Logic**: Automatic retry for failed requests

---

## 🔍 **Debugging & Monitoring**

### **Logging System**
- **KeyEventLogger**: Comprehensive navigation and user interaction logging
- **API Logging**: Request/response logging for debugging
- **Error Logging**: Detailed error information for troubleshooting

### **Performance Monitoring**
- **Database Metrics**: Query performance and cache hit rates
- **Network Metrics**: API response times and success rates
- **UI Metrics**: Navigation patterns and user interaction data

---

## 🎯 **Future Enhancements**

### **Phase 1: Core Functionality** ✅
- [x] Basic movie browsing and playback
- [x] Category navigation and filtering
- [x] TV remote navigation support
- [x] Database integration and caching

### **Phase 2: Enhanced Features** 🔄
- [ ] Advanced search functionality
- [ ] User preferences and favorites
- [ ] Watch history tracking
- [ ] Quality selection options

### **Phase 3: Advanced Integration** 📋
- [ ] Multiple language support
- [ ] Subtitle integration
- [ ] Social features (ratings, reviews)
- [ ] Recommendation engine

---

## 📚 **Related Documentation**

- **[Movies API Mapping](./NewIPTV_V2_MOVIES_API_MAPPING.md)**: Detailed API structure and endpoints
- **[Series System Overview](./NewIPTV_V2_SERIES_SCREEN_ENHANCEMENTS.md)**: Series system documentation
- **[Database Implementation](./ROOM_DATABASE_IMPLEMENTATION.md)**: Database architecture details
- **[TV Navigation Guide](./NewIPTV_V2_LEANBACK_IMPLEMENTATION.md)**: TV remote navigation guide

---

## 🎉 **Conclusion**

The NewIPTV V2 Movies system represents a **complete, production-ready** implementation that provides users with a seamless movie browsing and playback experience. Built with modern Android development practices, it offers:

- **🎬 Rich Content**: Comprehensive movie metadata and information
- **🎮 TV Optimized**: Perfect for Android TV and Fire TV devices
- **🚀 High Performance**: Efficient data handling and smooth navigation
- **🔧 Maintainable**: Clean architecture and comprehensive documentation

This system serves as a **foundation** for future enhancements and demonstrates the scalability and robustness of the NewIPTV V2 architecture.
