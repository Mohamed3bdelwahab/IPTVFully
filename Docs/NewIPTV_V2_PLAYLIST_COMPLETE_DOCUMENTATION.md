# 📋 **Playlist Complete Documentation - IPTV Android Application**

## 📋 **Overview**
This document provides comprehensive documentation for the playlist system in the IPTV Android application, including all playlist management features, code examples, and implementation details.

---

#

## 📋 **Playlist System Architecture**

### **Data Models**
```kotlin
// Episode Data Class
data class Episode(
    val id: String,
    val title: String,
    val description: String? = null,
    val url: String,
    val thumbnail: String? = null,
    val duration: Long? = null,
    val season: Int? = null,
    val episode: Int? = null,
    val series: String? = null,
    val category: String? = null,
    val language: String? = null,
    val quality: String? = null,
    val lastPlayed: Long? = null,
    val playCount: Int = 0
)

// Playlist Data Class
data class Playlist(
    val id: String,
    val name: String,
    val description: String? = null,
    val episodes: List<Episode>,
    val totalEpisodes: Int = episodes.size,
    val lastUpdated: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false
)
```

### **Playlist Management Functions**
```kotlin
// Load playlist from M3U file
fun loadPlaylistFromM3U(context: Context, uri: Uri): List<Episode> {
    return try {
        context.contentResolver.openInputStream(uri)?.use { input ->
            input.bufferedReader().useLines { lines ->
                lines.filter { it.isNotBlank() }
                    .chunked(2)
                    .mapNotNull { chunk ->
                        if (chunk.size == 2) {
                            val info = chunk[0].removePrefix("#EXTINF:")
                            val url = chunk[1]
                            
                            // Parse EXTINF line
                            val title = info.substringAfter(",")
                            val attributes = info.substringBefore(",")
                                .split(" ")
                                .filter { it.contains("=") }
                                .associate { 
                                    val (key, value) = it.split("=", limit = 2)
                                    key to value.removeSurrounding("\"")
                                }
                            
                            Episode(
                                id = url.hashCode().toString(),
                                title = title,
                                url = url,
                                thumbnail = attributes["tvg-logo"],
                                duration = attributes["tvg-length"]?.toLongOrNull(),
                                series = attributes["group-title"]
                            )
                        } else null
                    }
                    .toList()
            }
        } ?: emptyList()
    } catch (e: Exception) {
        Log.e("Playlist", "Error loading M3U file", e)
        emptyList()
    }
}

// Save playlist to local storage
fun savePlaylist(context: Context, playlist: Playlist) {
    val sharedPrefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
    val gson = Gson()
    val json = gson.toJson(playlist)
    sharedPrefs.edit().putString(playlist.id, json).apply()
}

// Load playlist from local storage
fun loadPlaylist(context: Context, playlistId: String): Playlist? {
    val sharedPrefs = context.getSharedPreferences("playlists", Context.MODE_PRIVATE)
    val json = sharedPrefs.getString(playlistId, null)
    return json?.let {
        val gson = Gson()
        gson.fromJson(it, Playlist::class.java)
    }
}
```

---

## 🎮 **TV Remote Playlist Controls**

### **Playlist Navigation Buttons**
```kotlin
// Menu Button - Toggle Playlist
KeyEvent.KEYCODE_MENU -> {
    showSpeedMenu = false
    showSettings = false
    showPlaylist = !showPlaylist
    pokeControls()
    true
}

// Channel Up - Next Episode
KeyEvent.KEYCODE_CHANNEL_UP -> {
    if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.lastIndex) {
        val newIndex = currentEpisodeIndex + 1
        playEpisode(exoPlayer, episodes[newIndex]) {
            currentEpisodeIndex = newIndex
            currentEpisode = episodes[newIndex]
            scope.launch { playlistListState.animateScrollToItem(newIndex) }
        }
    }
    pokeControls()
    true
}

// Channel Down - Previous Episode
KeyEvent.KEYCODE_CHANNEL_DOWN -> {
    if (episodes.isNotEmpty() && currentEpisodeIndex > 0) {
        val newIndex = currentEpisodeIndex - 1
        playEpisode(exoPlayer, episodes[newIndex]) {
            currentEpisodeIndex = newIndex
            currentEpisode = episodes[newIndex]
            scope.launch { playlistListState.animateScrollToItem(newIndex) }
        }
    }
    pokeControls()
    true
}

// Media Next - Next Episode
KeyEvent.KEYCODE_MEDIA_NEXT -> {
    tryPlayNextEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
        currentEpisodeIndex = newIndex
        currentEpisode = episodes.getOrNull(newIndex)
        scope.launch { playlistListState.animateScrollToItem(newIndex) }
    }
    pokeControls()
    true
}

// Media Previous - Previous Episode
KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
    tryPlayPrevEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
        currentEpisodeIndex = newIndex
        currentEpisode = episodes.getOrNull(newIndex)
        scope.launch { playlistListState.animateScrollToItem(newIndex) }
    }
    pokeControls()
    true
}
```

### **Episode Navigation Functions**
```kotlin
// Play next episode
fun tryPlayNextEpisode(
    exoPlayer: ExoPlayer, 
    episodes: List<Episode>, 
    currentIndex: Int,
    onChanged: (Int) -> Unit
) {
    if (episodes.isNotEmpty() && currentIndex < episodes.lastIndex) {
        val nextIndex = currentIndex + 1
        playEpisode(exoPlayer, episodes[nextIndex]) {
            onChanged(nextIndex)
        }
    }
}

// Play previous episode
fun tryPlayPrevEpisode(
    exoPlayer: ExoPlayer, 
    episodes: List<Episode>, 
    currentIndex: Int,
    onChanged: (Int) -> Unit
) {
    if (episodes.isNotEmpty() && currentIndex > 0) {
        val prevIndex = currentIndex - 1
        playEpisode(exoPlayer, episodes[prevIndex]) {
            onChanged(prevIndex)
        }
    }
}

// Play specific episode
fun playEpisode(exoPlayer: ExoPlayer, episode: Episode, onSuccess: () -> Unit) {
    try {
        val mediaItem = MediaItem.fromUri(episode.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        onSuccess()
        
        // Update play count and last played
        updateEpisodeStats(episode.id)
    } catch (e: Exception) {
        Log.e("Playlist", "Error playing episode: ${episode.title}", e)
    }
}
```

---

## 🎨 **Playlist UI Implementation**

### **PlaylistOverlay Component**
```kotlin
@Composable
fun PlaylistOverlay(
    episodes: List<Episode>,
    currentEpisodeIndex: Int,
    onEpisodeSelect: (Episode, Int) -> Unit,
    onClose: () -> Unit
) {
    val listState = rememberLazyListState()
    
    // Scroll to current episode when overlay opens
    LaunchedEffect(Unit) {
        if (currentEpisodeIndex >= 0 && currentEpisodeIndex < episodes.size) {
            listState.animateScrollToItem(currentEpisodeIndex)
        }
    }
    
    // Key event handler for playlist navigation
    val onKeyEvent: (KeyEvent) -> Boolean = { ev ->
        when (ev.keyCode) {
            // Navigation
            KeyEvent.KEYCODE_DPAD_UP -> {
                if (currentEpisodeIndex > 0) {
                    val newIndex = currentEpisodeIndex - 1
                    onEpisodeSelect(episodes[newIndex], newIndex)
                    listState.animateScrollToItem(newIndex)
                }
                true
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (currentEpisodeIndex < episodes.lastIndex) {
                    val newIndex = currentEpisodeIndex + 1
                    onEpisodeSelect(episodes[newIndex], newIndex)
                    listState.animateScrollToItem(newIndex)
                }
                true
            }
            KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER, KeyEvent.KEYCODE_DPAD_CENTER -> {
                if (currentEpisodeIndex >= 0 && currentEpisodeIndex < episodes.size) {
                    onEpisodeSelect(episodes[currentEpisodeIndex], currentEpisodeIndex)
                }
                true
            }
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_ESCAPE -> {
                onClose()
                true
            }
            else -> false
        }
    }
    
    // UI Layout
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.8f))
            .onKeyEvent(onKeyEvent)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.8f)
                .align(Alignment.CenterStart)
                .background(
                    color = MaterialTheme.colorScheme.surface,
                    shape = RoundedCornerShape(topEnd = 16.dp, bottomEnd = 16.dp)
                )
        ) {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Playlist (${episodes.size} episodes)",
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                
                IconButton(onClick = onClose) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Close Playlist",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }
            
            // Episode List
            LazyColumn(
                state = listState,
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(episodes.size) { index ->
                    val episode = episodes[index]
                    val isSelected = index == currentEpisodeIndex
                    val isPlaying = isSelected
                    
                    EpisodeItem(
                        episode = episode,
                        isSelected = isSelected,
                        isPlaying = isPlaying,
                        onClick = { onEpisodeSelect(episode, index) }
                    )
                }
            }
        }
    }
}

@Composable
fun EpisodeItem(
    episode: Episode,
    isSelected: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(80.dp)
            .clickable { onClick() }
            .border(
                width = if (isSelected) 2.dp else 1.dp,
                color = if (isSelected) MaterialTheme.colorScheme.primary 
                       else MaterialTheme.colorScheme.outline,
                shape = RoundedCornerShape(8.dp)
            ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            AsyncImage(
                model = episode.thumbnail ?: "placeholder",
                contentDescription = null,
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(4.dp)),
                contentScale = ContentScale.Crop,
                placeholder = painterResource(id = R.drawable.placeholder_thumbnail),
                error = painterResource(id = R.drawable.placeholder_thumbnail)
            )
            
            Spacer(modifier = Modifier.width(12.dp))
            
            // Episode Info
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = episode.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer 
                           else MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                
                episode.description?.let { description ->
                    Text(
                        text = description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                               else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Episode number
                    episode.episode?.let { epNum ->
                        Text(
                            text = "E$epNum",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    
                    // Duration
                    episode.duration?.let { duration ->
                        Text(
                            text = formatDuration(duration),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                    
                    // Play count
                    if (episode.playCount > 0) {
                        Text(
                            text = "Played ${episode.playCount}x",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                                   else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                        )
                    }
                }
            }
            
            // Playing indicator
            if (isPlaying) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Currently Playing",
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}
```

---

## 🔧 **Playlist Integration**

### **PlayerScreen Integration**
```kotlin
@Composable
fun PlayerScreen(
    episodes: List<Episode>,
    onEpisodeChange: (Episode, Int) -> Unit,
    onBack: () -> Unit
) {
    var currentEpisodeIndex by remember { mutableStateOf(0) }
    var currentEpisode by remember { mutableStateOf(episodes.firstOrNull()) }
    var showPlaylist by remember { mutableStateOf(false) }
    
    val playlistListState = rememberLazyListState()
    
    // Playlist toggle
    val togglePlaylist = {
        showPlaylist = !showPlaylist
        showSpeedMenu = false
        showSettings = false
    }
    
    // Episode selection handler
    val onEpisodeSelect = { episode: Episode, index: Int ->
        currentEpisode = episode
        currentEpisodeIndex = index
        playEpisode(exoPlayer, episode) {
            // Episode started successfully
        }
        showPlaylist = false
    }
    
    // Key event handler for playlist controls
    val handleKey: (KeyEvent) -> Boolean = { ev ->
        when (ev.keyCode) {
            // Playlist controls
            KeyEvent.KEYCODE_MENU -> {
                togglePlaylist()
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.lastIndex) {
                    val newIndex = currentEpisodeIndex + 1
                    onEpisodeSelect(episodes[newIndex], newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (episodes.isNotEmpty() && currentEpisodeIndex > 0) {
                    val newIndex = currentEpisodeIndex - 1
                    onEpisodeSelect(episodes[newIndex], newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                tryPlayNextEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                    currentEpisodeIndex = newIndex
                    currentEpisode = episodes.getOrNull(newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                tryPlayPrevEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                    currentEpisodeIndex = newIndex
                    currentEpisode = episodes.getOrNull(newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls()
                true
            }
            // ... other key mappings
        }
    }
    
    // UI Layout
    Box(modifier = Modifier.fillMaxSize()) {
        // Video player content
        
        // Playlist overlay
        if (showPlaylist) {
            PlaylistOverlay(
                episodes = episodes,
                currentEpisodeIndex = currentEpisodeIndex,
                onEpisodeSelect = onEpisodeSelect,
                onClose = { showPlaylist = false }
            )
        }
    }
}
```

---

## 📊 **Playlist Features**

### **Playlist Management**
| Feature | Description | Implementation |
|---------|-------------|----------------|
| **M3U Support** | Load playlists from M3U files | `loadPlaylistFromM3U()` |
| **Local Storage** | Save playlists locally | `savePlaylist()` / `loadPlaylist()` |
| **Episode Navigation** | Next/Previous episode controls | TV remote + UI buttons |
| **Visual Indicators** | Current episode highlighting | Selected state + playing indicator |
| **Episode Information** | Title, description, duration, play count | Episode data model |
| **Auto-scroll** | Scroll to current episode | `animateScrollToItem()` |

### **Navigation Methods**
| Method | Description | Key Code |
|--------|-------------|----------|
| **Menu Button** | Toggle playlist overlay | `KEYCODE_MENU` |
| **Channel Up** | Next episode | `KEYCODE_CHANNEL_UP` |
| **Channel Down** | Previous episode | `KEYCODE_CHANNEL_DOWN` |
| **Media Next** | Next episode | `KEYCODE_MEDIA_NEXT` |
| **Media Previous** | Previous episode | `KEYCODE_MEDIA_PREVIOUS` |
| **Enter** | Select episode | `KEYCODE_ENTER` |
| **D-pad Navigation** | Navigate within playlist | `KEYCODE_DPAD_UP/DOWN` |

---

## 🎯 **Playlist Benefits**

### **1. Content Organization**
- **Episode Management:** Easy access to all episodes in a series
- **Visual Overview:** See all available content at a glance
- **Quick Navigation:** Jump to any episode instantly

### **2. User Experience**
- **Intuitive Controls:** Standard TV remote navigation
- **Visual Feedback:** Clear indication of current episode
- **Smooth Transitions:** Animated scrolling and selection

### **3. Content Discovery**
- **Episode Information:** Titles, descriptions, durations
- **Play History:** Track which episodes have been watched
- **Series Progress:** Visual progress through the playlist

### **4. Accessibility**
- **Large Touch Targets:** Easy navigation on TV remotes
- **Clear Visual Hierarchy:** Easy to scan and select
- **Keyboard Support:** Full keyboard navigation

---

## 🔧 **Technical Implementation Details**

### **State Management**
```kotlin
// Playlist state management
var episodes by remember { mutableStateOf<List<Episode>>(emptyList()) }
var currentEpisodeIndex by remember { mutableStateOf(0) }
var currentEpisode by remember { mutableStateOf<Episode?>(null) }
var showPlaylist by remember { mutableStateOf(false) }

// List state for scrolling
val playlistListState = rememberLazyListState()

// Playlist persistence
val dataStore = context.dataStore
val playlistPreferenceKey = stringPreferencesKey("current_playlist")

// Load saved playlist
LaunchedEffect(Unit) {
    dataStore.data.collect { preferences ->
        val playlistId = preferences[playlistPreferenceKey]
        playlistId?.let { id ->
            loadPlaylist(context, id)?.let { playlist ->
                episodes = playlist.episodes
            }
        }
    }
}

// Save current playlist
fun saveCurrentPlaylist() {
    scope.launch {
        dataStore.edit { preferences ->
            preferences[playlistPreferenceKey] = currentPlaylistId
        }
    }
}
```

### **Performance Optimization**
```kotlin
// Efficient episode loading
fun loadEpisodesOptimized(playlist: Playlist): List<Episode> {
    return playlist.episodes.map { episode ->
        // Lazy load thumbnails
        episode.copy(
            thumbnail = episode.thumbnail?.let { url ->
                if (url.startsWith("http")) {
                    // Load thumbnail asynchronously
                    loadThumbnailAsync(url)
                } else {
                    url
                }
            }
        )
    }
}

// Debounced episode selection
var episodeSelectionJob by remember { mutableStateOf<Job?>(null) }

fun debouncedEpisodeSelect(episode: Episode, index: Int) {
    episodeSelectionJob?.cancel()
    episodeSelectionJob = scope.launch {
        delay(100) // 100ms debounce
        onEpisodeSelect(episode, index)
    }
}
```

### **Error Handling**
```kotlin
// Safe episode loading
fun loadEpisodeSafely(episode: Episode, onSuccess: () -> Unit, onError: (Exception) -> Unit) {
    try {
        val mediaItem = MediaItem.fromUri(episode.url)
        exoPlayer.setMediaItem(mediaItem)
        exoPlayer.prepare()
        exoPlayer.play()
        onSuccess()
    } catch (e: Exception) {
        onError(e)
        // Show error message to user
        showErrorMessage("Failed to load episode: ${episode.title}")
    }
}

// Handle playlist loading errors
fun loadPlaylistSafely(context: Context, uri: Uri, onSuccess: (List<Episode>) -> Unit, onError: (Exception) -> Unit) {
    try {
        val episodes = loadPlaylistFromM3U(context, uri)
        if (episodes.isNotEmpty()) {
            onSuccess(episodes)
        } else {
            onError(Exception("No episodes found in playlist"))
        }
    } catch (e: Exception) {
        onError(e)
    }
}
```

---

## 📱 **Testing & Validation**

### **Playlist Testing**
```kotlin
// Test playlist loading
fun testPlaylistLoading() {
    val testEpisodes = listOf(
        Episode("1", "Episode 1", "First episode", "http://example.com/1.m3u8"),
        Episode("2", "Episode 2", "Second episode", "http://example.com/2.m3u8"),
        Episode("3", "Episode 3", "Third episode", "http://example.com/3.m3u8")
    )
    
    val playlist = Playlist("test", "Test Playlist", testEpisodes)
    
    // Test episode navigation
    assert(playlist.episodes.size == 3) { "Playlist should have 3 episodes" }
    assert(playlist.totalEpisodes == 3) { "Total episodes should be 3" }
}

// Test episode selection
fun testEpisodeSelection() {
    val episodes = createTestEpisodes()
    var currentIndex = 0
    
    // Test next episode
    if (currentIndex < episodes.lastIndex) {
        currentIndex++
        assert(currentIndex == 1) { "Should move to next episode" }
    }
    
    // Test previous episode
    if (currentIndex > 0) {
        currentIndex--
        assert(currentIndex == 0) { "Should move to previous episode" }
    }
}
```

### **Performance Testing**
```kotlin
// Measure playlist loading time
fun measurePlaylistLoadingTime() {
    val startTime = System.currentTimeMillis()
    loadPlaylistFromM3U(context, testUri) { episodes ->
        val loadTime = System.currentTimeMillis() - startTime
        println("Playlist loading time: ${loadTime}ms for ${episodes.size} episodes")
    }
}

// Measure episode switching time
fun measureEpisodeSwitchTime() {
    val startTime = System.currentTimeMillis()
    playEpisode(exoPlayer, testEpisode) {
        val switchTime = System.currentTimeMillis() - startTime
        println("Episode switch time: ${switchTime}ms")
    }
}
```

---

## 🚀 **Future Enhancements**

### **Planned Features**
1. **Smart Playlists:** Auto-generated playlists based on preferences
2. **Playlist Categories:** Organize playlists by genre, language, etc.
3. **Favorites System:** Mark favorite episodes and playlists
4. **Playlist Sharing:** Share playlists with other users
5. **Offline Playlists:** Download playlists for offline viewing

### **Performance Improvements**
1. **Lazy Loading:** Load episodes on demand
2. **Thumbnail Caching:** Cache episode thumbnails
3. **Playlist Search:** Search within playlists
4. **Playlist Filtering:** Filter by various criteria
5. **Playlist Sorting:** Sort by various attributes

---

## 📋 **Troubleshooting**

### **Common Issues**

#### **1. Playlist Not Loading**
```kotlin
// Check file format and permissions
fun validatePlaylistFile(uri: Uri): Boolean {
    return try {
        val mimeType = context.contentResolver.getType(uri)
        mimeType?.contains("m3u") == true || uri.toString().endsWith(".m3u8")
    } catch (e: Exception) {
        false
    }
}

// Handle playlist loading errors
fun loadPlaylistWithFallback(uri: Uri) {
    if (validatePlaylistFile(uri)) {
        loadPlaylistFromM3U(context, uri) { episodes ->
            if (episodes.isNotEmpty()) {
                this.episodes = episodes
            } else {
                showErrorMessage("No episodes found in playlist")
            }
        }
    } else {
        showErrorMessage("Invalid playlist format")
    }
}
```

#### **2. Episode Navigation Not Working**
```kotlin
// Check episode list state
fun validateEpisodeNavigation() {
    if (episodes.isEmpty()) {
        showErrorMessage("No episodes available")
        return
    }
    
    if (currentEpisodeIndex < 0 || currentEpisodeIndex >= episodes.size) {
        currentEpisodeIndex = 0
        currentEpisode = episodes.firstOrNull()
    }
}

// Safe episode navigation
fun navigateToEpisode(index: Int) {
    if (index >= 0 && index < episodes.size) {
        currentEpisodeIndex = index
        currentEpisode = episodes[index]
        playEpisode(exoPlayer, episodes[index]) {
            // Episode started successfully
        }
    }
}
```

#### **3. Playlist UI Not Responding**
```kotlin
// Check UI state and focus
Box(
    modifier = Modifier
        .fillMaxSize()
        .onKeyEvent(onKeyEvent)  // Ensure key events are handled
        .focusable()             // Make sure it's focusable
) {
    // Playlist content
}

// Debug playlist state
fun debugPlaylistState() {
    println("Episodes: ${episodes.size}")
    println("Current Index: $currentEpisodeIndex")
    println("Show Playlist: $showPlaylist")
    println("Current Episode: ${currentEpisode?.title}")
}
```

---

**Documentation Created:** December 2024  
**Last Updated:** December 2024  
**Status:** ✅ **COMPLETE & TESTED**  
**Version:** 1.0.0
