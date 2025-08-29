# 📺 SeriesDetailsScreen Documentation

## 📋 **Overview**
The SeriesDetailsScreen displays comprehensive information about a selected TV series, including episode listings, series metadata, and playback options. It serves as the bridge between series browsing and video playback.

## 🎯 **Purpose**
- **Series Information:** Display detailed series metadata and descriptions
- **Episode Management:** List and organize all episodes in the series
- **Playback Integration:** Launch video player for selected episodes
- **Progress Tracking:** Show viewing progress and resume options
- **Navigation Hub:** Provide access to related series and content

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/SimpleSeriesViewModel.kt 
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `HydraApiService` - API communication
- `SeriesRepository` - Series data management
- `PlaybackProgressRepository` - Progress tracking

## 🎨 **UI Components**

### **1. Series Header**
```kotlin
Column(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)
) {
    // Series poster and basic info
    Row {
        AsyncImage(
            model = series.posterUrl,
            contentDescription = series.title,
            modifier = Modifier.size(120.dp)
        )
        Column(modifier = Modifier.padding(start = 16.dp)) {
            Text(
                text = series.title,
                style = MaterialTheme.typography.headlineMedium
            )
            Text(
                text = series.description,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
```

### **2. Episode List**
- **Season Organization:** Episodes grouped by season
- **Episode Cards:** Individual episode information
- **Progress Indicators:** Show viewing progress
- **Play Buttons:** Direct episode playback

### **3. Series Metadata**
- **Genre Information:** Series categories and genres
- **Release Information:** Year, country, language
- **Rating Information:** User ratings and reviews
- **Cast Information:** Actors and crew details

## 🔧 **Key Features**

### **1. Episode Loading**
```kotlin
// Load episodes for the series
LaunchedEffect(seriesId) {
    try {
        val episodes = hydraApiService.getSeriesEpisodes(seriesId)
        _episodes.value = episodes
    } catch (e: Exception) {
        _error.value = e.message
    }
}
```

### **2. Progress Tracking**
```kotlin
// Track viewing progress
val progress = playbackProgressRepository.getProgress(episode.id)
val progressPercentage = if (progress != null) {
    (progress.position.toFloat() / progress.duration.toFloat()) * 100
} else 0f
```

### **3. Episode Navigation**
- **Season Selection:** Navigate between seasons
- **Episode Filtering:** Filter by watched/unwatched
- **Sort Options:** Sort by episode number, date, or rating
- **Search Episodes:** Quick episode search within series

### **4. Playback Integration**
```kotlin
// Launch video player for episode
onEpisodeClick = { episode ->
    navController.navigate(
        "player?url=${episode.streamUrl}&title=${episode.title}&episode_id=${episode.id}&series_id=$seriesId"
    )
}
```

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Top App Bar             │
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────────┐   │
│    │    Series Header        │   │
│    │  ┌─────┐ ┌─────────────┐│   │
│    │  │Poster│ │Title & Desc ││   │
│    │  └─────┘ └─────────────┘│   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │   Season Navigation     │   │
│    │  [S1] [S2] [S3] [S4]   │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │     Episode List        │   │
│    │  ┌─────────────┐        │   │
│    │  │ Episode 1   │        │   │
│    │  └─────────────┘        │   │
│    │  ┌─────────────┐        │   │
│    │  │ Episode 2   │        │   │
│    │  └─────────────┘        │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Episode Card Design:**
- **Episode Thumbnail:** Episode screenshot or series poster
- **Episode Information:** Title, duration, air date
- **Progress Bar:** Visual progress indicator
- **Play Button:** Large, accessible play button
- **Status Indicators:** Watched, in progress, new

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Series List** → Series Selection → SeriesDetailsScreen
2. **Search Results** → Series Selection → SeriesDetailsScreen
3. **Deep Link** → Direct series URL → SeriesDetailsScreen

### **Navigation Path:**
```
HomeScreen → SimpleSeriesScreen → SeriesDetailsScreen → PlayerScreen
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Episode Selection** → PlayerScreen
3. **Related Series** → Other series details

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class SeriesDetailsViewModel @Inject constructor(
    private val seriesRepository: SeriesRepository,
    private val playbackProgressRepository: PlaybackProgressRepository
) : ViewModel()
```

### **State Variables:**
- **Series Information:** Current series details
- **Episode List:** All episodes in the series
- **Current Season:** Selected season for display
- **Progress Data:** Viewing progress for episodes
- **Loading State:** Data loading indicators
- **Error State:** Error handling and messages

## 🎨 **Theming**

### **Color Scheme:**
- **Primary:** Brand color for main elements
- **Secondary:** Accent color for highlights
- **Surface:** Card and background colors
- **On Surface:** Text and icon colors

### **Typography:**
- **Series Title:** Large, bold text for series name
- **Episode Titles:** Medium text for episode names
- **Descriptions:** Regular text for details
- **Metadata:** Small text for additional information

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lazy Loading:** Episodes load as needed
- **Image Caching:** Thumbnails cached for performance
- **Progress Caching:** Viewing progress cached locally
- **Memory Management:** Efficient resource usage

### **Data Loading:**
```kotlin
// Efficient episode loading with pagination
fun loadEpisodes(seasonId: String) {
    viewModelScope.launch {
        try {
            val episodes = seriesRepository.getEpisodes(seriesId, seasonId)
            _episodes.value = episodes
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
```

## 🔧 **Configuration**

### **Repository Configuration:**
```kotlin
// Series repository configuration
@Provides
@Singleton
fun provideSeriesRepository(
    apiService: HydraApiService,
    database: IPTVDatabase
): SeriesRepository {
    return SeriesRepository(apiService, database)
}
```

### **Progress Tracking:**
```kotlin
// Playback progress repository
@Provides
@Singleton
fun providePlaybackProgressRepository(
    database: IPTVDatabase
): PlaybackProgressRepository {
    return PlaybackProgressRepository(database)
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **Data Loading:** Test series and episode loading
- **Progress Tracking:** Test progress calculation
- **Navigation Testing:** Test episode selection
- **UI Testing:** Test series details display

### **Integration Tests:**
- **End-to-End Flow:** Complete series browsing journey
- **API Integration:** Test with real API endpoints
- **Performance Testing:** Load time and responsiveness
- **Cross-Device Testing:** Different screen sizes

## 🐛 **Common Issues & Solutions**

### **1. Episode Loading Issues**
**Problem:** Episodes not loading or slow loading
**Solution:** Check API connectivity and implement caching

### **2. Progress Tracking Problems**
**Problem:** Progress not showing correctly
**Solution:** Verify progress repository and data synchronization

### **3. Navigation Issues**
**Problem:** Episode selection not working
**Solution:** Check navigation setup and URL parameters

### **4. Memory Issues**
**Problem:** High memory usage with large episode lists
**Solution:** Implement pagination and lazy loading

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Series Views:** Track most viewed series
- **Episode Selection:** Monitor episode popularity
- **Season Navigation:** Track season browsing patterns
- **Progress Tracking:** Monitor viewing completion rates
- **Error Tracking:** Monitor loading and navigation errors

### **Performance Metrics:**
- **Load Times:** Series and episode loading performance
- **Image Loading:** Thumbnail loading performance
- **Memory Usage:** Resource consumption monitoring
- **API Response Times:** Backend performance tracking

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Episode Recommendations:** AI-powered episode suggestions
2. **Watchlist Integration:** Add series to watchlist
3. **Social Features:** Share and recommend series
4. **Advanced Filtering:** Filter by genre, rating, year
5. **Series Analytics:** Detailed viewing analytics
6. **Multi-language Support:** International series support

### **UI Improvements:**
1. **Animated Transitions:** Smooth navigation animations
2. **Custom Themes:** Series-specific themes
3. **Episode Previews:** Quick episode previews
4. **Advanced Sorting:** Multiple sorting options
5. **Search Integration:** Episode search within series

### **Technical Enhancements:**
1. **Offline Support:** Download series for offline viewing
2. **Background Sync:** Automatic content updates
3. **Push Notifications:** New episode notifications
4. **Cast Integration:** Chromecast and AirPlay support
5. **Analytics Dashboard:** Detailed series analytics

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coil Image Loading](https://coil-kt.github.io/coil/)

### **Related Screens:**
- [SimpleSeriesScreen.md](./SimpleSeriesScreen.md)
- [PlayerScreen.md](./PlayerScreen.md)
- [SearchScreen.md](./SearchScreen.md)

### **API Documentation:**
- [HydraApiService](./../services/HydraApiService.md)
- [SeriesRepository](./../repositories/SeriesRepository.md)
- [PlaybackProgressRepository](./../repositories/PlaybackProgressRepository.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
