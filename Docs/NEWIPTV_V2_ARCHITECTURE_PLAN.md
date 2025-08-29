# 🚀 **NewIPTV V2 Architecture Plan**

## 📋 **Project Overview**

### **Repository Information**
- **Repository**: https://github.com/Mohamed3bdelwahab/IPTVFully.git
- **Branch**: NewIPTV V2
- **Current Status**: V1 completed with native video player
- **Target**: V2 with comprehensive IPTV platform

---

## 🎯 **NewIPTV V2 Requirements**

### **1. Home Screen Structure**
```
┌─────────────────────────────────────────────────────────────┐
│                    NewIPTV V2 Home Screen                   │
├─────────────────────────────────────────────────────────────┤
│  ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│  │ Series  │ │ Movies  │ │ Live TV │ │Settings │ │History  │ │
│  │         │ │         │ │         │ │         │ │         │ │
│  └─────────┘ └─────────┘ └─────────┘ └─────────┘ └─────────┘ │
│  ┌─────────┐                                                 │
│  │  Fav    │                                                 │
│  │         │                                                 │
│  └─────────┘                                                 │
└─────────────────────────────────────────────────────────────┘
```

### **2. Series Screen Architecture**
```
┌─────────────────────────────────────────────────────────────┐
│                    Series Screen                            │
├─────────────────────┬───────────────────────────────────────┤
│   Left Panel        │           Right Panel                 │
│   (Categories)      │           (Series List)               │
│                     │                                       │
│ ┌─────────────────┐ │ ┌─────────────────────────────────────┐ │
│ │ 📁 Action       │ │ │ 🎬 Series Grid/List                 │
│ │ 📁 Comedy       │ │ │                                     │
│ │ 📁 Drama        │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │ 📁 Horror       │ │ │ │Series 1 │ │Series 2 │ │Series 3 │ │
│ │ 📁 Sci-Fi       │ │ │ │         │ │         │ │         │ │
│ │ 📁 Thriller     │ │ │ └─────────┘ └─────────┘ └─────────┘ │
│ │ 📁 Romance      │ │ │ ┌─────────┐ ┌─────────┐ ┌─────────┐ │
│ │ 📁 Documentary  │ │ │ │Series 4 │ │Series 5 │ │Series 6 │ │
│ │ 📁 Animation    │ │ │ │         │ │         │ │         │ │
│ └─────────────────┘ │ └─────────┘ └─────────┘ └─────────┘ │
│                     │                                     │
│                     │ ┌─────────────────────────────────────┐ │
│                     │ │         Search & Filter             │ │
│                     │ └─────────────────────────────────────┘ │
└─────────────────────┴───────────────────────────────────────┘
```

---

## 🏗️ **Technical Architecture**

### **1. Screen Navigation Flow**
```
HomeScreen
├── SeriesScreen
│   ├── CategorySelection (Left Panel)
│   ├── SeriesList (Right Panel)
│   └── SeriesDetailScreen
│       ├── EpisodeList
│       └── VideoPlayerScreen
├── MoviesScreen
│   ├── MovieCategories
│   ├── MovieList
│   └── MoviePlayerScreen
├── LiveTVScreen
│   ├── ChannelCategories
│   ├── ChannelList
│   └── LivePlayerScreen
├── SettingsScreen
│   ├── AccountSettings
│   ├── PlayerSettings
│   └── AppSettings
├── HistoryScreen
│   └── WatchHistory
└── FavoritesScreen
    └── FavoriteContent
```

### **2. API Integration (Following Current Structure)**
```kotlin
// Based on existing IPTVApiService.kt
class IPTVApiService {
    // Series API
    suspend fun getSeriesCategories(): ApiResponse<List<SeriesCategory>>
    suspend fun getSeries(categoryId: String): ApiResponse<List<Series>>
    suspend fun getSeriesInfo(seriesId: Int): ApiResponse<SeriesInfo>
    
    // Movies API (New)
    suspend fun getMovieCategories(): ApiResponse<List<MovieCategory>>
    suspend fun getMovies(categoryId: String): ApiResponse<List<Movie>>
    suspend fun getMovieInfo(movieId: Int): ApiResponse<MovieInfo>
    
    // Live TV API (New)
    suspend fun getLiveCategories(): ApiResponse<List<LiveCategory>>
    suspend fun getLiveChannels(categoryId: String): ApiResponse<List<LiveChannel>>
    suspend fun getLiveStreamInfo(channelId: Int): ApiResponse<LiveStreamInfo>
    
    // User Data API (New)
    suspend fun getWatchHistory(): ApiResponse<List<WatchHistoryItem>>
    suspend fun getFavorites(): ApiResponse<List<FavoriteItem>>
    suspend fun addToFavorites(contentId: String, contentType: String): ApiResponse<Boolean>
    suspend fun removeFromFavorites(contentId: String): ApiResponse<Boolean>
}
```

---

## 📱 **UI/UX Design Specifications**

### **1. Home Screen Design**
```kotlin
@Composable
fun HomeScreen(
    onSeriesClick: () -> Unit,
    onMoviesClick: () -> Unit,
    onLiveTVClick: () -> Unit,
    onSettingsClick: () -> Unit,
    onHistoryClick: () -> Unit,
    onFavoritesClick: () -> Unit
) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // App Title
        Text(
            text = "NewIPTV V2",
            style = MaterialTheme.typography.h3,
            modifier = Modifier.padding(32.dp)
        )
        
        // Main Menu Grid
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(16.dp)
        ) {
            item { MenuCard("Series", R.drawable.ic_series, onSeriesClick) }
            item { MenuCard("Movies", R.drawable.ic_movies, onMoviesClick) }
            item { MenuCard("Live TV", R.drawable.ic_live_tv, onLiveTVClick) }
            item { MenuCard("Settings", R.drawable.ic_settings, onSettingsClick) }
            item { MenuCard("History", R.drawable.ic_history, onHistoryClick) }
            item { MenuCard("Favorites", R.drawable.ic_favorites, onFavoritesClick) }
        }
    }
}
```

### **2. Series Screen Design**
```kotlin
@Composable
fun SeriesScreen(
    viewModel: SeriesViewModel = hiltViewModel()
) {
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        // Left Panel - Categories
        CategoryPanel(
            modifier = Modifier
                .width(300.dp)
                .fillMaxHeight(),
            categories = viewModel.categories,
            selectedCategory = viewModel.selectedCategory,
            onCategoryClick = viewModel::selectCategory
        )
        
        // Right Panel - Series List
        SeriesListPanel(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            series = viewModel.seriesList,
            isLoading = viewModel.isLoading,
            onSeriesClick = viewModel::selectSeries
        )
    }
}
```

---

## 🔧 **Implementation Plan**

### **Phase 1: Core Infrastructure**
1. **Create NewIPTV V2 Branch** ✅
2. **Update Project Structure**
   - Create new package structure
   - Set up navigation components
   - Configure dependency injection

### **Phase 2: Home Screen Implementation**
1. **HomeScreen Activity**
   - Create main navigation screen
   - Implement menu grid layout
   - Add navigation to all sections

2. **Navigation Setup**
   - Configure navigation graph
   - Set up deep linking
   - Implement back navigation

### **Phase 3: Series Section (Priority)**
1. **SeriesScreen Activity**
   - Implement dual-panel layout
   - Left panel: Category list
   - Right panel: Series grid/list

2. **SeriesViewModel**
   - Extend existing IPTVApiService
   - Implement category loading
   - Implement series filtering

3. **UI Components**
   - CategoryPanel component
   - SeriesListPanel component
   - SeriesCard component

### **Phase 4: Enhanced Video Player**
1. **VideoPlayerScreen**
   - Integrate existing VideoPlayerActivity
   - Add episode navigation
   - Implement season management

2. **TV Remote Integration**
   - Extend existing TVRemoteHandler
   - Add episode switching controls
   - Implement playlist functionality

### **Phase 5: Additional Sections**
1. **Movies Section**
   - Similar structure to Series
   - Movie-specific API integration

2. **Live TV Section**
   - Channel categories
   - Live stream integration
   - EPG support

3. **Settings Section**
   - User preferences
   - Player settings
   - Account management

4. **History & Favorites**
   - Watch history tracking
   - Favorite content management
   - Local storage integration

---

## 📊 **Data Models**

### **1. Series Models (Extended)**
```kotlin
data class SeriesCategory(
    val category_id: String,
    val category_name: String,
    val parent_id: Int = 0,
    val icon: String? = null,
    val description: String? = null
)

data class Series(
    val series_id: Int,
    val name: String,
    val cover: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val releaseDate: String,
    val rating: String,
    val rating_5based: Double,
    val category_id: String,
    val backdrop_path: List<String>? = null,
    val youtube_trailer: String = "",
    val episode_run_time: String = "0",
    val isFavorite: Boolean = false,
    val lastWatched: String? = null
)
```

### **2. New Models for V2**
```kotlin
// Movie Models
data class MovieCategory(
    val category_id: String,
    val category_name: String,
    val icon: String? = null
)

data class Movie(
    val movie_id: Int,
    val name: String,
    val cover: String,
    val plot: String,
    val cast: String,
    val director: String,
    val genre: String,
    val releaseDate: String,
    val rating: String,
    val duration: String,
    val category_id: String,
    val backdrop_path: List<String>? = null,
    val youtube_trailer: String = "",
    val isFavorite: Boolean = false
)

// Live TV Models
data class LiveCategory(
    val category_id: String,
    val category_name: String,
    val icon: String? = null
)

data class LiveChannel(
    val channel_id: Int,
    val name: String,
    val logo: String,
    val stream_url: String,
    val category_id: String,
    val epg_data: EPGData? = null,
    val isFavorite: Boolean = false
)

// User Data Models
data class WatchHistoryItem(
    val content_id: String,
    val content_type: String, // "series", "movie", "live"
    val title: String,
    val thumbnail: String,
    val watch_date: String,
    val progress: Float, // 0.0 to 1.0
    val duration: Long
)

data class FavoriteItem(
    val content_id: String,
    val content_type: String,
    val title: String,
    val thumbnail: String,
    val added_date: String
)
```

---

## 🎨 **UI Components Design**

### **1. Menu Card Component**
```kotlin
@Composable
fun MenuCard(
    title: String,
    iconRes: Int,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(8.dp)
            .size(120.dp)
            .clickable { onClick() },
        elevation = 8.dp
    ) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = iconRes),
                contentDescription = title,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colors.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.body1,
                textAlign = TextAlign.Center
            )
        }
    }
}
```

### **2. Category Panel Component**
```kotlin
@Composable
fun CategoryPanel(
    modifier: Modifier = Modifier,
    categories: List<SeriesCategory>,
    selectedCategory: SeriesCategory?,
    onCategoryClick: (SeriesCategory) -> Unit
) {
    LazyColumn(
        modifier = modifier
            .background(MaterialTheme.colors.surface)
            .border(1.dp, MaterialTheme.colors.onSurface.copy(alpha = 0.12f))
    ) {
        item {
            Text(
                text = "Categories",
                style = MaterialTheme.typography.h6,
                modifier = Modifier.padding(16.dp)
            )
        }
        
        items(categories) { category ->
            CategoryItem(
                category = category,
                isSelected = category.category_id == selectedCategory?.category_id,
                onClick = { onCategoryClick(category) }
            )
        }
    }
}
```

---

## 🔄 **Navigation Implementation**

### **1. Navigation Graph**
```xml
<?xml version="1.0" encoding="utf-8"?>
<navigation xmlns:android="http://schemas.android.com/apk/res/android"
    xmlns:app="http://schemas.android.com/apk/res-auto"
    android:id="@+id/nav_graph"
    app:startDestination="@id/homeScreen">

    <fragment
        android:id="@+id/homeScreen"
        android:name="com.example.newiptv.ui.home.HomeScreen"
        android:label="Home" />

    <fragment
        android:id="@+id/seriesScreen"
        android:name="com.example.newiptv.ui.series.SeriesScreen"
        android:label="Series" />

    <fragment
        android:id="@+id/moviesScreen"
        android:name="com.example.newiptv.ui.movies.MoviesScreen"
        android:label="Movies" />

    <fragment
        android:id="@+id/liveTVScreen"
        android:name="com.example.newiptv.ui.livetv.LiveTVScreen"
        android:label="Live TV" />

    <fragment
        android:id="@+id/settingsScreen"
        android:name="com.example.newiptv.ui.settings.SettingsScreen"
        android:label="Settings" />

    <fragment
        android:id="@+id/historyScreen"
        android:name="com.example.newiptv.ui.history.HistoryScreen"
        android:label="History" />

    <fragment
        android:id="@+id/favoritesScreen"
        android:name="com.example.newiptv.ui.favorites.FavoritesScreen"
        android:label="Favorites" />

    <activity
        android:id="@+id/videoPlayerActivity"
        android:name="com.example.newiptv.player.VideoPlayerActivity"
        android:label="Video Player" />

</navigation>
```

---

## 📈 **Performance Considerations**

### **1. Lazy Loading**
- Implement lazy loading for series/movies lists
- Use pagination for large datasets
- Cache frequently accessed data

### **2. Image Loading**
- Use Coil or Glide for efficient image loading
- Implement image caching
- Use placeholder images during loading

### **3. Memory Management**
- Implement proper lifecycle management
- Use ViewModels for state management
- Clean up resources properly

---

## 🧪 **Testing Strategy**

### **1. Unit Testing**
- Test ViewModels
- Test API services
- Test data models

### **2. UI Testing**
- Test navigation flows
- Test user interactions
- Test responsive design

### **3. Integration Testing**
- Test API integration
- Test video player functionality
- Test TV remote controls

---

## 📋 **Development Checklist**

### **Phase 1: Setup** ✅
- [x] Create NewIPTV V2 branch
- [x] Push current V1 code to repository
- [ ] Update project structure
- [ ] Configure navigation

### **Phase 2: Home Screen**
- [ ] Create HomeScreen activity
- [ ] Implement menu grid
- [ ] Add navigation logic
- [ ] Test navigation flow

### **Phase 3: Series Section**
- [ ] Create SeriesScreen activity
- [ ] Implement dual-panel layout
- [ ] Create SeriesViewModel
- [ ] Integrate with existing API
- [ ] Test category and series loading

### **Phase 4: Video Player Enhancement**
- [ ] Integrate existing VideoPlayerActivity
- [ ] Add episode navigation
- [ ] Test TV remote controls
- [ ] Implement season management

### **Phase 5: Additional Features**
- [ ] Movies section
- [ ] Live TV section
- [ ] Settings section
- [ ] History and favorites
- [ ] User preferences

---

## 🎯 **Success Criteria**

### **1. Functional Requirements**
- ✅ Home screen with 6 main sections
- ✅ Series section with dual-panel layout
- ✅ Category-based series browsing
- ✅ Video player with TV remote support
- ✅ Navigation between all sections

### **2. Technical Requirements**
- ✅ Follow existing API structure
- ✅ Maintain current video player functionality
- ✅ Implement proper error handling
- ✅ Support TV remote controls
- ✅ Responsive design for TV

### **3. User Experience**
- ✅ Intuitive navigation
- ✅ Fast loading times
- ✅ Smooth video playback
- ✅ Easy content discovery
- ✅ Consistent UI/UX

---

**Plan Created**: December 2024  
**Status**: 📋 **READY FOR IMPLEMENTATION**  
**Next Step**: Awaiting user confirmation to start development

---

## 🚀 **Ready to Start?**

This comprehensive plan follows the **RULES.md** guidelines and provides a clear roadmap for NewIPTV V2 development. The plan:

1. **Maintains existing functionality** (video player, TV remote controls)
2. **Extends the architecture** with new sections
3. **Follows the API structure** from current implementation
4. **Provides detailed UI/UX specifications**
5. **Includes testing and performance considerations**

**Please confirm to start implementation!** 🎯
