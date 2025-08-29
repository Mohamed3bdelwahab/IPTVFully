# 🎵 TrackUtils Documentation

## 📋 **Overview**
TrackUtils is a utility file containing helper functions for media track management, specifically focused on audio track extraction and subtitle detection for Media3/ExoPlayer integration.

## 🎯 **Purpose**
- **Audio Track Extraction:** Extract and process available audio languages
- **Subtitle Detection:** Detect presence of text tracks for subtitles
- **Track Information Processing:** Parse and format track metadata
- **Code Reusability:** Centralize track-related operations

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/TrackUtils.kt
```

### **Dependencies:**
- `androidx.media3.common.Tracks` - Media3 tracks information
- `androidx.media3.common.TrackGroup` - Media3 track groups

## 🔧 **Key Components**

### **1. Audio Language Data Class**
```kotlin
/** Public model for audio language chips */
data class AudioLang(val code: String?, val display: String)
```

#### **Properties:**
- **code (String?):** Language code (e.g., "en", "es", "fr")
- **display (String):** User-friendly display name

#### **Usage Examples:**
```kotlin
val english = AudioLang("en", "English")
val spanish = AudioLang("es", "Spanish")
val unknown = AudioLang(null, "Unknown")
```

### **2. Audio Language Extraction**
```kotlin
/** Single source of truth: audio language extraction */
fun extractAudioLanguages(tracks: Tracks): List<AudioLang> {
    val langs = mutableSetOf<AudioLang>()
    for (g in tracks.groups) {
        val tg = g.mediaTrackGroup
        for (i in 0 until tg.length) {
            val f = tg.getFormat(i)
            if (f.sampleMimeType?.startsWith("audio/") == true) {
                val code = f.language
                val name = when {
                    !f.label.isNullOrBlank() -> f.label!!
                    !code.isNullOrBlank() -> code.uppercase()
                    else -> "Unknown"
                }
                langs.add(AudioLang(code, name))
            }
        }
    }
    return langs.toList().sortedBy { it.display }
}
```

#### **Purpose:**
- **Track Scanning:** Iterate through all available tracks
- **Audio Detection:** Identify audio tracks by MIME type
- **Language Extraction:** Extract language codes and labels
- **Name Generation:** Create user-friendly display names
- **Deduplication:** Remove duplicate language entries
- **Sorting:** Alphabetical ordering for consistency

#### **Process Flow:**
1. **Iterate Track Groups:** Loop through all track groups
2. **Check MIME Type:** Filter for audio tracks (`audio/*`)
3. **Extract Metadata:** Get language code and label
4. **Generate Display Name:** Use label or uppercase code
5. **Deduplicate:** Use Set to avoid duplicates
6. **Sort:** Alphabetical ordering by display name

#### **Usage Examples:**
```kotlin
val tracks = exoPlayer.currentTracks
val audioLanguages = extractAudioLanguages(tracks)

// Result: [AudioLang("en", "English"), AudioLang("es", "Spanish")]
```

### **3. Text Track Detection**
```kotlin
/** Single source of truth: detect if any text track exists */
fun hasTextTrack(tracks: Tracks): Boolean {
    for (g in tracks.groups) {
        val tg = g.mediaTrackGroup
        for (i in 0 until tg.length) {
            val f = tg.getFormat(i)
            if (f.sampleMimeType?.startsWith("text/") == true) return true
        }
    }
    return false
}
```

#### **Purpose:**
- **Subtitle Detection:** Check if subtitles are available
- **Quick Check:** Fast boolean response for UI decisions
- **MIME Type Filtering:** Look for text-based tracks
- **Early Return:** Stop searching when first text track found

#### **Usage Examples:**
```kotlin
val tracks = exoPlayer.currentTracks
val hasSubtitles = hasTextTrack(tracks)

if (hasSubtitles) {
    // Show subtitle toggle in UI
    showSubtitleControls = true
}
```

## 🚀 **Integration Examples**

### **Settings Overlay Integration**
```kotlin
@Composable
fun SettingsOverlay(
    tracks: Tracks,
    onAudioLangSelect: (String?) -> Unit,
    onSubtitlesToggle: (Boolean) -> Unit,
    onClose: () -> Unit
) {
    val audioLangs = remember(tracks) { extractAudioLanguages(tracks) }
    val textAvailable = remember(tracks) { hasTextTrack(tracks) }
    
    // Display audio language options
    audioLangs.forEach { lang ->
        FilterChip(
            selected = false,
            onClick = { onAudioLangSelect(lang.code) },
            label = { Text(lang.display) }
        )
    }
    
    // Show subtitle toggle if available
    if (textAvailable) {
        Switch(
            checked = captionsEnabled,
            onCheckedChange = { onSubtitlesToggle(it) }
        )
    }
}
```

### **Player Screen Integration**
```kotlin
@Composable
fun PlayerScreen() {
    val exoPlayer = remember { ExoPlayer.Builder(context).build() }
    
    // Monitor track changes
    LaunchedEffect(exoPlayer.currentTracks) {
        val tracks = exoPlayer.currentTracks
        val audioLanguages = extractAudioLanguages(tracks)
        val hasSubtitles = hasTextTrack(tracks)
        
        // Update UI based on available tracks
        availableAudioTracks = audioLanguages
        subtitleAvailable = hasSubtitles
    }
}
```

### **Track Selection Integration**
```kotlin
fun selectAudioTrack(langCode: String?) {
    val builder = trackSelector.buildUponParameters()
        .setPreferredAudioLanguage(langCode)
    trackSelector.parameters = builder.build()
}

fun toggleSubtitles(enable: Boolean) {
    val builder = trackSelector.buildUponParameters()
    trackSelector.parameters = if (enable) {
        builder.setSelectUndeterminedTextLanguage(true).build()
    } else {
        builder.setSelectUndeterminedTextLanguage(false).build()
    }
}
```

## 📊 **Performance Considerations**

### **Optimizations:**
- **Efficient Iteration:** Single pass through track groups
- **Early Termination:** Stop searching when text track found
- **Lazy Evaluation:** Only process tracks when needed
- **Memory Efficient:** Minimal object creation

### **Best Practices:**
- **Remember Results:** Cache extracted languages in Composable
- **Track Changes:** Re-evaluate when tracks change
- **Error Handling:** Handle null or invalid track data
- **UI Updates:** Update UI based on track availability

## 🧪 **Testing**

### **Unit Tests:**
```kotlin
@Test
fun `extractAudioLanguages should return empty list for no tracks`() {
    val emptyTracks = Tracks.EMPTY
    val result = extractAudioLanguages(emptyTracks)
    assertTrue(result.isEmpty())
}

@Test
fun `extractAudioLanguages should extract audio languages correctly`() {
    val mockTracks = createMockTracksWithAudio()
    val result = extractAudioLanguages(mockTracks)
    
    assertEquals(2, result.size)
    assertTrue(result.any { it.code == "en" && it.display == "English" })
    assertTrue(result.any { it.code == "es" && it.display == "Spanish" })
}

@Test
fun `hasTextTrack should return false for no text tracks`() {
    val tracksWithoutText = createMockTracksWithoutText()
    val result = hasTextTrack(tracksWithoutText)
    assertFalse(result)
}

@Test
fun `hasTextTrack should return true when text tracks exist`() {
    val tracksWithText = createMockTracksWithText()
    val result = hasTextTrack(tracksWithText)
    assertTrue(result)
}
```

### **Integration Tests:**
- **Player Integration:** Test with actual ExoPlayer tracks
- **UI Integration:** Test track information in UI components
- **Track Changes:** Test dynamic track updates

## 🐛 **Common Issues & Solutions**

### **1. No Audio Tracks Detected**
**Problem:** Audio languages not showing in UI
**Solution:** Check MIME type filtering and track group iteration

### **2. Language Names Not Displaying**
**Problem:** Language codes showing instead of names
**Solution:** Verify label extraction and fallback logic

### **3. Subtitle Toggle Not Appearing**
**Problem:** Subtitle controls not showing
**Solution:** Check text track detection and UI conditional logic

### **4. Duplicate Languages**
**Problem:** Same language appearing multiple times
**Solution:** Ensure proper deduplication using Set

## 📈 **Analytics & Monitoring**

### **Usage Tracking:**
- **Track Detection:** Monitor track availability patterns
- **Language Usage:** Track most used audio languages
- **Subtitle Usage:** Track subtitle enable/disable patterns

### **Performance Metrics:**
- **Extraction Time:** Time to process track information
- **Memory Usage:** Resource consumption during track processing
- **Error Rate:** Failed track extraction attempts

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Advanced Track Filtering:** Filter by quality, codec, or other properties
2. **Track Quality Selection:** Choose between different quality options
3. **Language Preferences:** User-defined language priorities
4. **Track Caching:** Cache track information for performance

### **Technical Improvements:**
1. **Async Processing:** Non-blocking track extraction
2. **Enhanced Metadata:** Extract more track information
3. **Performance Optimization:** Better memory management
4. **Cross-Platform Support:** Support for different platforms

## 📚 **Related Documentation**

### **Dependencies:**
- [Media3 Tracks](https://developer.android.com/reference/androidx/media3/common/Tracks)
- [Media3 TrackGroup](https://developer.android.com/reference/androidx/media3/common/TrackGroup)
- [ExoPlayer Track Selection](https://exoplayer.dev/track-selection.html)

### **Related Components:**
- [PlayerScreen.md](./PlayerScreen.md)
- [SettingsOverlay.md](./SettingsOverlay.md)
- [PlayerUtils.md](./PlayerUtils.md)

### **API Documentation:**
- [Media3 Tracks Interface](https://developer.android.com/reference/androidx/media3/common/Tracks)
- [TrackGroup](https://developer.android.com/reference/androidx/media3/common/TrackGroup)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready  
**Audio Track Support:** ✅ Fully Implemented  
**Subtitle Detection:** ✅ Complete  
**Performance:** ✅ Optimized
