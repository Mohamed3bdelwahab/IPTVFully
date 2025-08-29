# 🔍 SearchScreen Documentation

## 📋 **Overview**
The SearchScreen provides comprehensive search functionality across all content types in the IPTV application. It features real-time search, filters, and advanced search capabilities to help users quickly find their desired content.

## 🎯 **Purpose**
- **Content Discovery:** Search across series, movies, and live TV
- **Real-time Search:** Instant search results as user types
- **Advanced Filtering:** Filter by content type, category, and other criteria
- **Search History:** Track and display recent searches
- **Quick Access:** Fast content discovery and navigation

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/SearchScreen.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `HydraApiService` - API communication
- `ContentRepository` - Data management
- `SearchRepository` - Search functionality

## 🎨 **UI Components**

### **1. Search Bar**
```kotlin
SearchBar(
    query = searchQuery,
    onQueryChange = { query ->
        viewModel.updateSearchQuery(query)
    },
    onSearch = { query ->
        viewModel.performSearch(query)
    },
    active = isSearchActive,
    onActiveChange = { active ->
        isSearchActive = active
    }
) {
    // Search suggestions and history
}
```

### **2. Search Results**
- **Results Grid:** LazyVerticalGrid for efficient display
- **Content Cards:** Rich media cards with search highlights
- **Loading States:** Skeleton loading and progress indicators
- **Empty States:** No results found messages

### **3. Filter Panel**
- **Content Type Filter:** Series, Movies, Live TV
- **Category Filter:** Genre and category selection
- **Sort Options:** Sort by relevance, date, popularity
- **Advanced Filters:** Year, rating, language

## 🔧 **Key Features**

### **1. Real-time Search**
```kotlin
// Debounced search to avoid excessive API calls
LaunchedEffect(searchQuery) {
    delay(300) // 300ms debounce
    if (searchQuery.isNotEmpty()) {
        viewModel.performSearch(searchQuery)
    }
}
```

### **2. Advanced Filtering**
```kotlin
// Filter by content type
val filteredResults = searchResults.filter { content ->
    when (selectedContentType) {
        ContentType.SERIES -> content is Series
        ContentType.MOVIES -> content is Movie
        ContentType.LIVE_TV -> content is LiveChannel
        else -> true
    }
}
```

### **3. Search History**
- **Recent Searches:** Display last 10 searches
- **Search Suggestions:** Popular and trending searches
- **Quick Access:** One-tap search from history
- **Clear History:** Option to clear search history

### **4. Search Highlights**
- **Query Highlighting:** Highlight search terms in results
- **Relevance Scoring:** Sort by search relevance
- **Fuzzy Matching:** Handle typos and partial matches
- **Synonyms:** Include related terms in search

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Top App Bar             │
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────────┐   │
│    │      Search Bar         │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │     Filter Panel        │   │
│    │  [Type] [Category] [Sort]│   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │    Search Results       │   │
│    │  ┌──┐ ┌──┐ ┌──┐ ┌──┐   │   │
│    │  │R1 │ │R2 │ │R3 │ │R4 │   │   │
│    │  └──┘ └──┘ └──┘ └──┘   │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Search Bar Design:**
- **Large Touch Target:** Easy to tap on TV remote
- **Clear Button:** Quick way to clear search
- **Voice Search:** Voice input support (future)
- **Search Icon:** Visual indicator for search functionality

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Home Screen** → Search Card → SearchScreen
2. **Deep Link** → Direct search query → SearchScreen
3. **Global Search** → App-wide search shortcut → SearchScreen

### **Navigation Path:**
```
HomeScreen → SearchScreen → Search Results → Content Details → PlayerScreen
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Result Selection** → Content details screen
3. **Home Navigation** → HomeScreen

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class SearchViewModel @Inject constructor(
    private val searchRepository: SearchRepository,
    private val contentRepository: ContentRepository
) : ViewModel()
```

### **State Variables:**
- **Search Query:** Current search term
- **Search Results:** Filtered search results
- **Search History:** Recent searches
- **Filter State:** Active filters and sort options
- **Loading State:** Search progress indicators
- **Error State:** Search error handling

## 🎨 **Theming**

### **Color Scheme:**
- **Primary:** Brand color for search elements
- **Secondary:** Accent color for highlights
- **Surface:** Card and background colors
- **On Surface:** Text and icon colors

### **Typography:**
- **Search Text:** Large, clear search input
- **Results Text:** Readable result descriptions
- **Highlight Text:** Emphasized search terms

## 📊 **Performance Considerations**

### **Optimizations:**
- **Debounced Search:** Avoid excessive API calls
- **Result Caching:** Cache search results for performance
- **Lazy Loading:** Load results as needed
- **Image Optimization:** Efficient thumbnail loading

### **Search Performance:**
```kotlin
// Efficient search with debouncing
private val searchJob = Job()
private val searchScope = CoroutineScope(Dispatchers.IO + searchJob)

fun performSearch(query: String) {
    searchJob.cancel() // Cancel previous search
    searchScope.launch {
        delay(300) // Debounce
        val results = searchRepository.search(query)
        _searchResults.value = results
    }
}
```

## 🔧 **Configuration**

### **Search Configuration:**
```kotlin
// Search repository configuration
@Provides
@Singleton
fun provideSearchRepository(
    apiService: HydraApiService,
    database: IPTVDatabase
): SearchRepository {
    return SearchRepository(apiService, database)
}
```

### **API Configuration:**
```kotlin
// Search API endpoints
interface SearchApi {
    @GET("search")
    suspend fun search(
        @Query("q") query: String,
        @Query("type") type: String? = null,
        @Query("category") category: String? = null
    ): SearchResponse
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **Search Logic:** Test search algorithms and filtering
- **API Integration:** Test search API calls
- **UI Testing:** Test search bar interactions
- **Filter Testing:** Test filter functionality

### **Integration Tests:**
- **End-to-End Search:** Complete search flow testing
- **Performance Testing:** Search response times
- **Cross-Device Testing:** Different screen sizes
- **Accessibility Testing:** Screen reader compatibility

## 🐛 **Common Issues & Solutions**

### **1. Search Performance**
**Problem:** Slow search results
**Solution:** Implement proper debouncing and caching

### **2. Search Accuracy**
**Problem:** Irrelevant search results
**Solution:** Improve search algorithm and relevance scoring

### **3. Filter Issues**
**Problem:** Filters not working correctly
**Solution:** Verify filter logic and state management

### **4. Memory Issues**
**Problem:** High memory usage with large results
**Solution:** Implement pagination and result limiting

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Search Queries:** Track popular search terms
- **Filter Usage:** Monitor filter preferences
- **Result Clicks:** Track which results are selected
- **Search Patterns:** Understand user search behavior
- **Error Tracking:** Monitor search failures

### **Performance Metrics:**
- **Search Response Times:** API performance monitoring
- **Result Relevance:** User satisfaction with results
- **Cache Hit Rates:** Effectiveness of caching
- **Memory Usage:** Resource consumption during search

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Voice Search:** Voice-activated search functionality
2. **Image Search:** Search by image or screenshot
3. **Advanced Filters:** More granular filtering options
4. **Search Suggestions:** AI-powered search suggestions
5. **Search Analytics:** Detailed search behavior analytics
6. **Multi-language Search:** International search support

### **UI Improvements:**
1. **Search Animations:** Smooth search transitions
2. **Advanced Filters UI:** Collapsible filter panels
3. **Search History UI:** Better history management
4. **Result Previews:** Quick preview of search results
5. **Search Shortcuts:** Keyboard shortcuts for power users

### **Technical Enhancements:**
1. **Elasticsearch Integration:** Advanced search engine
2. **Machine Learning:** AI-powered search relevance
3. **Federated Search:** Search across multiple sources
4. **Search Indexing:** Local search index for offline use
5. **Search APIs:** Third-party search service integration

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose Search](https://developer.android.com/jetpack/compose/search)
- [Material Design 3](https://m3.material.io/)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)

### **Related Screens:**
- [HomeScreen.md](./HomeScreen.md)
- [SimpleSeriesScreen.md](./SimpleSeriesScreen.md)
- [PlayerScreen.md](./PlayerScreen.md)

### **API Documentation:**
- [HydraApiService](./../services/HydraApiService.md)
- [SearchRepository](./../repositories/SearchRepository.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
