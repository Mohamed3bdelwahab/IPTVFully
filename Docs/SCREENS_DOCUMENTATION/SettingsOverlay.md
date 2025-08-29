# ⚙️ SettingsOverlay Documentation

## 📋 **Overview**
The SettingsOverlay is a dedicated interface for configuring video playback settings including audio track selection and subtitle controls. It provides a clean, user-friendly interface for managing playback preferences.

## 🎯 **Purpose**
- **Audio Track Selection:** Choose from available audio languages
- **Subtitle Control:** Enable/disable subtitle display
- **Playback Settings:** Configure video playback preferences
- **User Preferences:** Manage viewing experience settings

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/SettingsOverlay.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `androidx.media3.common.Tracks` - Media3 track information
- `androidx.compose.material3` - Material Design 3 components
- `androidx.compose.runtime` - Compose runtime utilities

## 🎨 **UI Components**

### **1. Main Container**
```kotlin
Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    Card(
        modifier = Modifier.fillMaxWidth(0.7f).fillMaxHeight(0.7f),
        colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f))
    ) {
        // Settings content
    }
}
```

### **2. Settings Sections**
- **Header:** Title and close button
- **Audio Track Section:** Language selection with FilterChips
- **Subtitle Section:** Toggle switch for subtitle control
- **Help Text:** Usage instructions and tips

### **3. Interactive Elements**
- **FilterChips:** Audio language selection
- **Switch:** Subtitle toggle control
- **IconButton:** Close button
- **Text Labels:** Section headers and descriptions

## 🔧 **Key Features**

### **1. Audio Track Selection**
```kotlin
private fun extractAudioLanguages(tracks: Tracks): List<Lang> {
    val langs = mutableSetOf<Lang>()
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
                langs.add(Lang(code, name))
            }
        }
    }
    return langs.toList().sortedBy { it.display }
}
```

#### **Audio Track Features:**
- **Automatic Detection:** Scans available audio tracks
- **Language Display:** Shows language names or codes
- **Multiple Selection:** Supports multiple audio languages
- **Default Handling:** Graceful fallback for unknown languages

### **2. Subtitle Control**
```kotlin
private fun hasTextTrack(tracks: Tracks): Boolean {
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

#### **Subtitle Features:**
- **Availability Check:** Detects if subtitles are available
- **Toggle Control:** Enable/disable subtitle display
- **State Persistence:** Remembers subtitle preference
- **Visual Feedback:** Clear on/off indication

### **3. Track Information Processing**
```kotlin
private data class Lang(val code: String?, val display: String)
```

#### **Track Processing:**
- **MIME Type Detection:** Identifies audio and text tracks
- **Language Extraction:** Extracts language codes and labels
- **Display Name Generation:** Creates user-friendly names
- **Sorting:** Alphabetical ordering for consistency

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Settings Panel          │
│                                 │
│    ┌─────────────────────────┐   │
│    │    Settings             │   │
│    │    [×]                  │   │
│    │                         │   │
│    │ Audio Track             │   │
│    │ [English] [Spanish]     │   │
│    │ [French] [German]       │   │
│    │                         │   │
│    │ Subtitles               │   │
│    │ [●] On                  │   │
│    │                         │   │
│    │                         │   │
│    │ Tips: MENU=Playlist     │   │
│    │ INFO=Speed • SETTINGS   │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Design Principles:**
- **High Contrast:** Dark background with white text
- **Clear Hierarchy:** Section headers and content organization
- **Touch-Friendly:** Large touch targets for mobile
- **Consistent Spacing:** Proper visual rhythm
- **Accessible:** Screen reader compatible

## 🚀 **Usage Examples**

### **Basic Usage**
```kotlin
SettingsOverlay(
    tracks = exoPlayer.currentTracks,
    onAudioLangSelect = { langCode ->
        val builder = trackSelector.buildUponParameters()
            .setPreferredAudioLanguage(langCode)
        trackSelector.parameters = builder.build()
    },
    onSubtitlesToggle = { enable ->
        val b = trackSelector.buildUponParameters()
        trackSelector.parameters = if (enable) {
            b.setSelectUndeterminedTextLanguage(true).build()
        } else {
            b.setSelectUndeterminedTextLanguage(false).build()
        }
    },
    onClose = { showSettings = false }
)
```

### **Integration with PlayerScreen**
```kotlin
if (showSettings) {
    SettingsOverlay(
        tracks = exoPlayer.currentTracks,
        onAudioLangSelect = { langCode ->
            val builder = trackSelector.buildUponParameters()
                .setPreferredAudioLanguage(langCode)
            trackSelector.parameters = builder.build()
            lastUserAction = now()
        },
        onSubtitlesToggle = { enable ->
            val b = trackSelector.buildUponParameters()
            trackSelector.parameters = if (enable) {
                b.setSelectUndeterminedTextLanguage(true).build()
            } else {
                b.setSelectUndeterminedTextLanguage(false).build()
            }
            lastUserAction = now()
        },
        onClose = { showSettings = false; lastUserAction = now() }
    )
}
```

## 🔄 **State Management**

### **State Variables:**
- **tracks:** Current media tracks (Tracks)
- **audioLangs:** Extracted audio languages (List<Lang>)
- **textAvailable:** Subtitle availability (Boolean)
- **captionsEnabled:** Subtitle toggle state (Boolean)

### **State Updates:**
- **Track Changes:** Automatic language list updates
- **Selection Changes:** Immediate audio track switching
- **Toggle Changes:** Real-time subtitle control
- **UI Updates:** Responsive interface updates

## 🎨 **Theming**

### **Color Scheme:**
- **Background:** Black with 0.95f alpha
- **Text:** White for maximum contrast
- **Primary:** Material theme primary color
- **Surface:** Semi-transparent overlays
- **Icons:** White for visibility

### **Typography:**
- **Title:** `headlineSmall` for section headers
- **Body Text:** Default text style for content
- **Help Text:** Smaller text with reduced opacity
- **Button Text:** Standard button typography

## 📊 **Performance Considerations**

### **Optimizations:**
- **Efficient Track Processing:** Minimal computation for track analysis
- **Lazy Evaluation:** Only process tracks when needed
- **Memory Management:** Efficient data structures
- **UI Updates:** Minimal recomposition

### **Accessibility:**
- **Content Descriptions:** Proper labels for screen readers
- **Focus Navigation:** Logical tab order
- **Touch Targets:** Large enough for all users
- **Color Contrast:** Meets accessibility standards

## 🧪 **Testing**

### **Unit Tests:**
- **Track Extraction:** Test audio language detection
- **Subtitle Detection:** Test text track identification
- **Language Processing:** Test display name generation
- **State Management:** Test settings persistence

### **Integration Tests:**
- **Player Integration:** Test with ExoPlayer
- **Track Selection:** Test audio track switching
- **Subtitle Toggle:** Test subtitle enable/disable
- **Accessibility:** Test with screen readers

## 🐛 **Common Issues & Solutions**

### **1. Track Detection Issues**
**Problem:** Audio tracks not detected
**Solution:** Check MIME type filtering and track group iteration

### **2. Language Display Issues**
**Problem:** Language names not showing correctly
**Solution:** Verify label and code extraction logic

### **3. Subtitle Toggle Issues**
**Problem:** Subtitles not enabling/disabling
**Solution:** Check track selector parameter updates

### **4. UI Layout Issues**
**Problem:** Settings panel not displaying properly
**Solution:** Check modifier chain and alignment

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Audio Track Changes:** Track most used languages
- **Subtitle Usage:** Track subtitle enable/disable patterns
- **Settings Access:** Track frequency of settings panel usage
- **Session Duration:** Time spent in settings

### **Performance Metrics:**
- **Panel Open Time:** Time to display settings panel
- **Track Processing:** Time to extract track information
- **Selection Response:** Time to apply audio/subtitle changes
- **Error Rate:** Failed track selections

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Advanced Audio Settings:** Audio quality and channel selection
2. **Subtitle Styling:** Custom subtitle appearance
3. **Language Preferences:** User-defined language priorities
4. **Quality Selection:** Video quality options
5. **Accessibility Settings:** Enhanced accessibility options

### **UI Improvements:**
1. **Animated Transitions:** Smooth panel animations
2. **Search Functionality:** Search through available tracks
3. **Favorites System:** Save preferred audio/subtitle settings
4. **Themes:** Multiple visual themes
5. **Accessibility:** Enhanced screen reader support

### **Technical Enhancements:**
1. **Track Caching:** Cache track information for performance
2. **Batch Operations:** Apply multiple settings at once
3. **Cloud Sync:** Sync settings across devices
4. **Analytics Integration:** Better user behavior tracking
5. **Cross-Platform:** Support for different platforms

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Media3 Tracks](https://developer.android.com/reference/androidx/media3/common/Tracks)
- [ExoPlayer Track Selection](https://exoplayer.dev/track-selection.html)

### **Related Components:**
- [PlayerScreen.md](./PlayerScreen.md)
- [SpeedMenuOverlay.md](./SpeedMenuOverlay.md)
- [ControlsOverlay.md](./ControlsOverlay.md)

### **API Documentation:**
- [ExoPlayer TrackSelector](https://exoplayer.dev/doc/reference/com/google/android/exoplayer2/trackselection/TrackSelector.html)
- [Media3 Track Groups](https://developer.android.com/reference/androidx/media3/common/TrackGroup)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready  
**Audio Track Support:** ✅ Fully Implemented  
**Subtitle Support:** ✅ Toggle Control  
**Accessibility:** ✅ WCAG Compliant
