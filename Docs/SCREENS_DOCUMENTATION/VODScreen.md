# 🎬 VODScreen Documentation

## 📋 **Overview**
The VODScreen provides access to Video on Demand content, including movies and other video content. It features a comprehensive browsing interface with categories, search, and filtering capabilities.

## 🎯 **Purpose**
- **Movie Browsing:** Browse available movies and VOD content
- **Category Navigation:** Navigate through different movie categories
- **Search & Filter:** Find specific movies using search and filters
- **Content Discovery:** Discover new movies and content
- **Playback Integration:** Launch video player for selected content

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/VODScreen.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `HydraApiService` - API communication
- `VODRepository` - VOD data management

## 🎨 **UI Components**

### **1. Category Navigation**
```kotlin
LazyRow(
    modifier = Modifier.padding(horizontal = 16.dp)
) {
    items(categories) { category ->
        CategoryChip(
            category = category,
            isSelected = selectedCategory == category,
            onClick = { onCategorySelected(category) }
        )
    }
}
```

### **2. Movie Grid**
- **LazyVerticalGrid:** Efficient movie display
- **Movie Cards:** Rich media cards with movie information
- **Loading States:** Skeleton loading and progress indicators
- **Error Handling:** User-friendly error messages

### **3. Search & Filter**
- **Search Bar:** Real-time movie search
- **Filter Panel:** Advanced filtering options
- **Sort Options:** Sort by title, year, rating
- **Quick Actions:** Favorites and watchlist

## 🔧 **Key Features**

### **1. Movie Loading**
```kotlin
// Load movies for selected category
LaunchedEffect(selectedCategory) {
    try {
        val movies = vodRepository.getMovies(selectedCategory.id)
        _movies.value = movies
    } catch (e: Exception) {
        _error.value = e.message
    }
}
```

### **2. Search Functionality**
```kotlin
// Real-time search with debouncing
LaunchedEffect(searchQuery) {
    delay(300) // 300ms debounce
    if (searchQuery.isNotEmpty()) {
        val results = vodRepository.searchMovies(searchQuery)
        _searchResults.value = results
    }
}
```

### **3. Category Management**
- **Dynamic Categories:** Load categories from API
- **Category Selection:** Filter movies by category
- **Category Navigation:** Easy category switching
- **Category Metadata:** Category descriptions and counts

### **4. Movie Information**
- **Movie Details:** Title, year, duration, rating
- **Movie Posters:** High-quality movie posters
- **Movie Descriptions:** Plot summaries and metadata
- **Streaming Info:** Direct playback links

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Top App Bar             │
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────────┐   │
│    │   Category Navigation   │   │
│    │  [Cat1] [Cat2] [Cat3]   │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │      Search Bar         │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │      Movie Grid         │   │
│    │  ┌──┐ ┌──┐ ┌──┐ ┌──┐   │   │
│    │  │M1 │ │M2 │ │M3 │ │M4 │   │   │
│    │  └──┘ └──┘ └──┘ └──┘   │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Movie Card Design:**
- **Movie Poster:** High-quality movie thumbnail
- **Movie Information:** Title, year, duration
- **Rating Display:** User ratings and reviews
- **Play Button:** Large, accessible play button
- **Quick Actions:** Add to favorites, watchlist

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Home Screen** → VOD Card → VODScreen
2. **Search Results** → Movie Selection → VODScreen
3. **Deep Link** → Direct movie URL → VODScreen

### **Navigation Path:**
```
HomeScreen → VODScreen → Movie Selection → PlayerScreen
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Movie Selection** → PlayerScreen
3. **Category Navigation** → Different category view

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class VODViewModel @Inject constructor(
    private val vodRepository: VODRepository,
    private val hydraApiService: HydraApiService
) : ViewModel()
```

### **State Variables:**
- **Movies:** Current movie list
- **Categories:** Available movie categories
- **Selected Category:** Currently selected category
- **Search Results:** Search query results
- **Loading State:** Data loading indicators
- **Error State:** Error handling and messages

## 🎨 **Theming**

### **Color Scheme:**
- **Primary:** Brand color for main elements
- **Secondary:** Accent color for highlights
- **Surface:** Card and background colors
- **On Surface:** Text and icon colors

### **Typography:**
- **Movie Titles:** Large, bold text for movie names
- **Category Names:** Medium text for categories
- **Descriptions:** Regular text for details
- **Metadata:** Small text for additional information

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lazy Loading:** Movies load as needed
- **Image Caching:** Posters cached for performance
- **Pagination:** Large lists loaded in chunks
- **Memory Management:** Efficient resource usage

### **Data Loading:**
```kotlin
// Efficient movie loading with pagination
fun loadMovies(categoryId: String, page: Int = 1) {
    viewModelScope.launch {
        try {
            val movies = vodRepository.getMovies(categoryId, page)
            _movies.value = movies
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
```

## 🔧 **Configuration**

### **Repository Configuration:**
```kotlin
// VOD repository configuration
@Provides
@Singleton
fun provideVODRepository(
    apiService: HydraApiService,
    database: IPTVDatabase
): VODRepository {
    return VODRepository(apiService, database)
}
```

### **API Configuration:**
```kotlin
// VOD API endpoints
interface VODApi {
    @GET("vod")
    suspend fun getMovies(
        @Query("category") category: String? = null,
        @Query("page") page: Int = 1
    ): VODResponse
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **Data Loading:** Test movie and category loading
- **Search Testing:** Test search functionality
- **Filter Testing:** Test category filtering
- **UI Testing:** Test movie grid interactions

### **Integration Tests:**
- **End-to-End Flow:** Complete VOD browsing journey
- **API Integration:** Test with real API endpoints
- **Performance Testing:** Load time and responsiveness
- **Cross-Device Testing:** Different screen sizes

## 🐛 **Common Issues & Solutions**

### **1. Movie Loading Issues**
**Problem:** Movies not loading or slow loading
**Solution:** Check API connectivity and implement caching

### **2. Search Problems**
**Problem:** Search not working correctly
**Solution:** Verify search implementation and debouncing

### **3. Category Issues**
**Problem:** Categories not displaying
**Solution:** Check category API and data structure

### **4. Memory Issues**
**Problem:** High memory usage with large movie lists
**Solution:** Implement pagination and lazy loading

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Movie Views:** Track most viewed movies
- **Category Usage:** Monitor category preferences
- **Search Patterns:** Track search behavior
- **Playback Starts:** Monitor movie playback initiation
- **Error Tracking:** Monitor loading and navigation errors

### **Performance Metrics:**
- **Load Times:** Movie and category loading performance
- **Image Loading:** Poster loading performance
- **Memory Usage:** Resource consumption monitoring
- **API Response Times:** Backend performance tracking

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Movie Recommendations:** AI-powered movie suggestions
2. **Watchlist Integration:** Add movies to watchlist
3. **Social Features:** Share and recommend movies
4. **Advanced Filtering:** Filter by genre, rating, year
5. **Movie Analytics:** Detailed viewing analytics
6. **Multi-language Support:** International movie support

### **UI Improvements:**
1. **Animated Transitions:** Smooth navigation animations
2. **Custom Themes:** Movie-specific themes
3. **Movie Previews:** Quick movie previews
4. **Advanced Sorting:** Multiple sorting options
5. **Search Integration:** Enhanced search functionality

### **Technical Enhancements:**
1. **Offline Support:** Download movies for offline viewing
2. **Background Sync:** Automatic content updates
3. **Push Notifications:** New movie notifications
4. **Cast Integration:** Chromecast and AirPlay support
5. **Analytics Dashboard:** Detailed VOD analytics

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coil Image Loading](https://coil-kt.github.io/coil/)

### **Related Screens:**
- [HomeScreen.md](./HomeScreen.md)
- [PlayerScreen.md](./PlayerScreen.md)
- [SearchScreen.md](./SearchScreen.md)

### **API Documentation:**
- [HydraApiService](./../services/HydraApiService.md)
- [VODRepository](./../repositories/VODRepository.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
