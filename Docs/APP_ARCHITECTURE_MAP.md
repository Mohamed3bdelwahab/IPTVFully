# 📱 IPTV App Architecture Map

## 🏗️ **Core Architecture Overview**

### **Main Navigation Flow:**
```
HomeScreen → SeriesScreen → SeriesInfoView → PlayerScreen
```

## 📋 **Component Hierarchy**

### **1. SimpleSeriesScreen** (`SimpleSeriesScreen.kt`)
**Location:** `app/src/main/java/com/example/iptvtv/ui/screens/SimpleSeriesScreen.kt`

**Purpose:** Main series browsing and navigation screen

**States:**
- `"categories"` - Shows series categories
- `"series"` - Shows series list for selected category  
- `"series_info"` - Shows series details with episodes

**Key Components:**
- `SeriesList` - Displays filtered series
- `SeriesInfoView` - Shows series details and episodes
- `FilterMenu` - Series filtering options
- `LoadingDisplay` - Loading state
- `ErrorDisplay` - Error handling

**Navigation:**
```kotlin
// Navigate to player from episode
navController.navigate("player?url=$encodedUrl&title=$encodedTitle&episode_id=$encodedEpisodeId&series_id=$encodedSeriesId")
```

### **2. SimpleSeriesViewModel** (`SimpleSeriesViewModel.kt`)
**Location:** `app/src/main/java/com/example/iptvtv/ui/viewmodel/SimpleSeriesViewModel.kt`

**Purpose:** State management for series browsing

**Key State Flows:**
- `currentView` - Current screen state ("categories", "series", "series_info")
- `categories` - Series categories list
- `seriesList` - All series for current category
- `filteredSeriesList` - Filtered series based on user selection
- `selectedSeries` - Currently selected series
- `seriesInfo` - Series details with episodes
- `isLoading` - Loading state
- `error` - Error state

**Key Functions:**
- `loadCategories()` - Load series categories
- `selectCategory()` - Select category and load series
- `selectSeries()` - Select series and load details
- `applyFilter()` - Apply series filtering
- `goBack()` - Navigation back

**API Integration:**
- Uses `IPTVApiService` for data fetching
- Uses `ActivityLogger` for tracking

### **3. SeriesInfoView** (Component in SimpleSeriesScreen)
**Location:** `app/src/main/java/com/example/iptvtv/ui/screens/SimpleSeriesScreen.kt`

**Purpose:** Display series details and episodes

**Components:**
- `SeriesHeader` - Series title, plot, metadata
- `SeasonHeader` - Season information
- `EpisodeCard` - Individual episode display

**Current Structure:**
```kotlin
// Shows ALL episodes from ALL seasons at once
seriesInfo.episodes.entries.sortedBy { it.key.toIntOrNull() ?: 0 }.forEach { (season, episodes) ->
    item { SeasonHeader(season = season, episodeCount = episodes.size) }
    items(episodes) { episode ->
        EpisodeCard(episode = episode, seriesName = series.name, onClick = { onEpisodeClick(episode) })
    }
}
```

**Problem:** Shows all episodes from all seasons simultaneously (RAM intensive)

### **4. IPTVApiService** (`IPTVApiService.kt`)
**Location:** `app/src/main/java/com/example/iptvtv/service/IPTVApiService.kt`

**Purpose:** API communication for series data

**Key Data Models:**
- `Series` - Basic series information
- `SeriesInfo` - Detailed series with episodes
- `Episode` - Episode information
- `SeriesCategory` - Category information

**Key Functions:**
- `getSeriesCategories()` - Get series categories
- `getSeries(categoryId)` - Get series for category
- `getSeriesInfo(seriesId)` - Get series details with episodes

## 🔄 **Data Flow**

### **Series Browsing Flow:**
```
1. App Start → SimpleSeriesScreen (categories state)
2. User selects category → selectCategory() → load series
3. User selects series → selectSeries() → load series info
4. SeriesInfoView renders → shows all episodes from all seasons
5. User clicks episode → navigate to PlayerScreen
```

### **Current Episode Loading:**
```
IPTVApiService.getSeriesInfo(seriesId) 
→ Returns SeriesInfo with Map<String, List<Episode>>
→ SeriesInfoView displays ALL episodes from ALL seasons
→ High RAM usage (shows 83 episodes for single season series)
```

## 🎯 **Season-Based Optimization Needed**

### **Current Problem:**
- **SeriesInfoView** shows ALL episodes from ALL seasons at once
- **High RAM consumption** (83 episodes in one view)
- **No season navigation** - can't switch between seasons
- **Poor performance** on TV devices

### **Required Changes:**

#### **1. Update SimpleSeriesViewModel**
Add season management:
```kotlin
// New state flows needed:
private val _currentSeason = MutableStateFlow(1)
val currentSeason: StateFlow<Int> = _currentSeason.asStateFlow()

private val _currentSeasonEpisodes = MutableStateFlow<List<IPTVApiService.Episode>>(emptyList())
val currentSeasonEpisodes: StateFlow<List<IPTVApiService.Episode>> = _currentSeasonEpisodes.asStateFlow()

// New functions needed:
fun switchSeason(seasonNumber: Int)
fun loadEpisodesForSeason(seasonNumber: Int)
```

#### **2. Update SeriesInfoView**
Add season navigation:
```kotlin
// Add SeasonButtons component
// Show only current season episodes
// Add season switching functionality
```

#### **3. Create SeasonButtons Component**
```kotlin
@Composable
fun SeasonButtons(
    seasons: List<SeasonInfo>,
    currentSeason: Int,
    onSeasonClick: (Int) -> Unit
)
```

## 📊 **Performance Impact**

### **Current State:**
- **Series "Evil"**: 4 seasons, 50 total episodes → Shows ALL 50 episodes
- **Series "╪ú┘à┘è"**: 1 season, 83 episodes → Shows ALL 83 episodes
- **RAM Usage**: High (all episodes loaded in memory)

### **Optimized State:**
- **Series "Evil"**: 4 seasons → Shows only current season episodes (13 episodes max)
- **Series "╪ú┘à┘è"**: 1 season → Shows only current season episodes (83 episodes, but with season navigation)
- **RAM Usage**: Reduced by ~75% (only current season in memory)

## 🔧 **Implementation Plan**

### **Phase 1: Update SimpleSeriesViewModel**
1. Add season management state flows
2. Add season switching functions
3. Add episode filtering by season

### **Phase 2: Update SeriesInfoView**
1. Add SeasonButtons component
2. Modify episode display to show only current season
3. Add season navigation

### **Phase 3: Create SeasonButtons Component**
1. Create reusable season navigation component
2. Add visual feedback for current season
3. Handle single season series gracefully

### **Phase 4: Performance Monitoring**
1. Add RAM usage display
2. Monitor episode loading performance
3. Track season switching performance

## 🎨 **UI/UX Improvements**

### **Current UI:**
- All episodes listed vertically
- No season separation
- No season navigation
- Poor performance on TV

### **Optimized UI:**
- Season buttons at top
- Only current season episodes shown
- Clear season navigation
- Better TV performance
- RAM usage indicator

## 📱 **Navigation Integration**

### **Current Navigation:**
```
SimpleSeriesScreen → SeriesInfoView → PlayerScreen
```

### **Optimized Navigation:**
```
SimpleSeriesScreen → SeriesInfoView (with season navigation) → PlayerScreen
```

### **Season State Persistence:**
- Remember last selected season per series
- Restore season state when returning to series
- Handle series switching properly

## 🔍 **Debug Logging**

### **Current Debug Messages:**
```
DEBUG: SimpleSeriesViewModel - selectSeries called for series: Evil (ID: 406)
DEBUG: SimpleSeriesViewModel - Series info loaded successfully
DEBUG: SimpleSeriesViewModel - Found 4 seasons
DEBUG: SimpleSeriesViewModel - Season 1 has 13 episodes
DEBUG: SeriesInfoView - Rendering series: Evil
DEBUG: SeriesInfoView - Total seasons: 4
```

### **Additional Debug Messages Needed:**
```
DEBUG: SimpleSeriesViewModel - Season switched to: 2
DEBUG: SimpleSeriesViewModel - Loading episodes for season: 2
DEBUG: SeriesInfoView - Showing episodes for season: 2 (13 episodes)
DEBUG: PerformanceMonitor - RAM usage: 45MB / 2GB (2.25%)
```

## 🚀 **Next Steps**

1. **Implement season management in SimpleSeriesViewModel**
2. **Create SeasonButtons component**
3. **Update SeriesInfoView for season-based display**
4. **Add performance monitoring**
5. **Test with various series (single/multi-season)**
6. **Monitor RAM usage improvements**
