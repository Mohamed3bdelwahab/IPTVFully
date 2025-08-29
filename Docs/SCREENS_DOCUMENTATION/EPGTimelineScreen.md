# 📺 EPGTimelineScreen Documentation

## 📋 **Overview**
The EPGTimelineScreen provides an Electronic Program Guide (EPG) interface for live TV channels. It displays a timeline view of current and upcoming programs, allowing users to browse schedules and plan their viewing.

## 🎯 **Purpose**
- **Live TV Guide:** Display current and upcoming TV programs
- **Channel Navigation:** Browse through different TV channels
- **Schedule Viewing:** View program schedules and timing
- **Program Information:** Show detailed program information
- **Live TV Integration:** Launch live TV playback

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/EPGTimelineScreen.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `EPGService` - EPG data management
- `ChannelRepository` - Channel data management

## 🎨 **UI Components**

### **1. Timeline Header**
```kotlin
Row(
    modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
        .padding(16.dp)
) {
    // Time navigation controls
    IconButton(onClick = { navigateTime(-1) }) {
        Icon(Icons.Default.NavigateBefore, "Previous Hour")
    }
    Text(
        text = currentTime.format(DateTimeFormatter.ofPattern("HH:mm")),
        style = MaterialTheme.typography.headlineMedium
    )
    IconButton(onClick = { navigateTime(1) }) {
        Icon(Icons.Default.NavigateNext, "Next Hour")
    }
}
```

### **2. Channel List**
- **Channel Icons:** Channel logos and names
- **Channel Selection:** Select channels to view
- **Channel Categories:** Group channels by category
- **Favorite Channels:** Quick access to favorites

### **3. Program Timeline**
- **Time Slots:** Hourly time divisions
- **Program Blocks:** Visual program representations
- **Program Information:** Title, duration, description
- **Current Time Indicator:** Highlight current time

## 🔧 **Key Features**

### **1. EPG Data Loading**
```kotlin
// Load EPG data for selected channels
LaunchedEffect(selectedChannels, currentTime) {
    try {
        val epgData = epgService.getEPGData(selectedChannels, currentTime)
        _epgData.value = epgData
    } catch (e: Exception) {
        _error.value = e.message
    }
}
```

### **2. Time Navigation**
```kotlin
// Navigate through time periods
fun navigateTime(hours: Int) {
    currentTime = currentTime.plusHours(hours.toLong())
    loadEPGData()
}
```

### **3. Channel Management**
- **Channel Selection:** Select/deselect channels
- **Channel Filtering:** Filter by category or favorites
- **Channel Search:** Search for specific channels
- **Channel Sorting:** Sort by name, number, or category

### **4. Program Information**
- **Program Details:** Title, description, genre
- **Program Timing:** Start and end times
- **Program Rating:** Content ratings and warnings
- **Program Actions:** Watch, record, set reminder

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Top App Bar             │
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────────┐   │
│    │    Timeline Header      │   │
│    │  [<] 14:00 [>]         │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │   Channel Navigation    │   │
│    │  [Ch1] [Ch2] [Ch3]     │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │    Program Timeline     │   │
│    │  ┌─────────────────────┐│   │
│    │  │ Program Blocks      ││   │
│    │  └─────────────────────┘│   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Timeline Design:**
- **Time Axis:** Horizontal time progression
- **Channel Axis:** Vertical channel list
- **Program Blocks:** Rectangular program representations
- **Current Time Line:** Vertical line showing current time
- **Program Labels:** Program titles and times

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Home Screen** → Live TV → EPGTimelineScreen
2. **Channel List** → EPG View → EPGTimelineScreen
3. **Deep Link** → Direct EPG URL → EPGTimelineScreen

### **Navigation Path:**
```
HomeScreen → EPGTimelineScreen → Program Selection → Live TV Player
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Program Selection** → Live TV Player
3. **Channel Selection** → Different channel view

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class EPGTimelineViewModel @Inject constructor(
    private val epgService: EPGService,
    private val channelRepository: ChannelRepository
) : ViewModel()
```

### **State Variables:**
- **EPG Data:** Current EPG information
- **Selected Channels:** Currently selected channels
- **Current Time:** Current viewing time
- **Channel List:** Available channels
- **Loading State:** Data loading indicators
- **Error State:** Error handling and messages

## 🎨 **Theming**

### **Color Scheme:**
- **Primary:** Brand color for main elements
- **Secondary:** Accent color for highlights
- **Surface:** Card and background colors
- **On Surface:** Text and icon colors

### **Typography:**
- **Channel Names:** Large, bold text for channels
- **Program Titles:** Medium text for program names
- **Time Display:** Regular text for time information
- **Descriptions:** Small text for program details

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lazy Loading:** EPG data loads as needed
- **Data Caching:** EPG data cached for performance
- **Pagination:** Large channel lists loaded in chunks
- **Memory Management:** Efficient resource usage

### **Data Loading:**
```kotlin
// Efficient EPG loading with caching
fun loadEPGData(channels: List<String>, time: LocalDateTime) {
    viewModelScope.launch {
        try {
            val epgData = epgService.getEPGData(channels, time)
            _epgData.value = epgData
        } catch (e: Exception) {
            _error.value = e.message
        }
    }
}
```

## 🔧 **Configuration**

### **EPG Service Configuration:**
```kotlin
// EPG service configuration
@Provides
@Singleton
fun provideEPGService(
    apiService: HydraApiService,
    database: IPTVDatabase
): EPGService {
    return EPGService(apiService, database)
}
```

### **Channel Repository:**
```kotlin
// Channel repository configuration
@Provides
@Singleton
fun provideChannelRepository(
    apiService: HydraApiService,
    database: IPTVDatabase
): ChannelRepository {
    return ChannelRepository(apiService, database)
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **EPG Loading:** Test EPG data loading
- **Time Navigation:** Test time navigation functionality
- **Channel Selection:** Test channel selection
- **UI Testing:** Test timeline interactions

### **Integration Tests:**
- **End-to-End Flow:** Complete EPG browsing journey
- **API Integration:** Test with real EPG endpoints
- **Performance Testing:** Load time and responsiveness
- **Cross-Device Testing:** Different screen sizes

## 🐛 **Common Issues & Solutions**

### **1. EPG Loading Issues**
**Problem:** EPG data not loading or slow loading
**Solution:** Check API connectivity and implement caching

### **2. Time Navigation Problems**
**Problem:** Time navigation not working correctly
**Solution:** Verify time calculation and navigation logic

### **3. Channel Issues**
**Problem:** Channels not displaying
**Solution:** Check channel API and data structure

### **4. Performance Issues**
**Problem:** Slow scrolling or lag
**Solution:** Implement lazy loading and data optimization

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Channel Views:** Track most viewed channels
- **Program Selection:** Monitor program popularity
- **Time Navigation:** Track time browsing patterns
- **EPG Usage:** Monitor EPG feature usage
- **Error Tracking:** Monitor loading and navigation errors

### **Performance Metrics:**
- **Load Times:** EPG data loading performance
- **Data Accuracy:** EPG data accuracy monitoring
- **Memory Usage:** Resource consumption monitoring
- **API Response Times:** Backend performance tracking

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Program Reminders:** Set reminders for programs
2. **Recording Integration:** Schedule program recordings
3. **Program Search:** Search for specific programs
4. **Advanced Filtering:** Filter by genre, rating, time
5. **EPG Analytics:** Detailed viewing analytics
6. **Multi-language Support:** International EPG support

### **UI Improvements:**
1. **Animated Transitions:** Smooth timeline animations
2. **Custom Themes:** EPG-specific themes
3. **Program Previews:** Quick program previews
4. **Advanced Sorting:** Multiple sorting options
5. **Search Integration:** Enhanced search functionality

### **Technical Enhancements:**
1. **Offline EPG:** Download EPG data for offline use
2. **Background Sync:** Automatic EPG updates
3. **Push Notifications:** Program reminder notifications
4. **Cast Integration:** Chromecast and AirPlay support
5. **Analytics Dashboard:** Detailed EPG analytics

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [Coil Image Loading](https://coil-kt.github.io/coil/)

### **Related Screens:**
- [HomeScreen.md](./HomeScreen.md)
- [PlayerScreen.md](./PlayerScreen.md)
- [ChannelListScreen.md](./ChannelListScreen.md)

### **API Documentation:**
- [EPGService](./../services/EPGService.md)
- [ChannelRepository](./../repositories/ChannelRepository.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
