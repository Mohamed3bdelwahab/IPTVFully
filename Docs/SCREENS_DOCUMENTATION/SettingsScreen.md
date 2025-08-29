# ⚙️ SettingsScreen Documentation

## 📋 **Overview**
The SettingsScreen provides comprehensive configuration options for the IPTV application. It includes user preferences, app settings, server configuration, and advanced options for customizing the viewing experience.

## 🎯 **Purpose**
- **User Preferences:** Customize app behavior and appearance
- **Server Configuration:** Manage IPTV server settings
- **Playback Settings:** Configure video and audio preferences
- **Account Management:** User account and subscription settings
- **Advanced Options:** Developer and debugging settings

## 🏗️ **Architecture**

### **File Location:**
```
app/src/main/java/com/example/iptvtv/ui/screens/SettingsScreen.kt
```

### **Dependencies:**
- `@Composable` - Jetpack Compose UI
- `@HiltViewModel` - Dependency injection
- `@Inject` - Service injection
- `UserPreferencesRepository` - Settings storage
- `IPTVApiService` - Server configuration
- `ThemeManager` - Theme management

## 🎨 **UI Components**

### **1. Settings Categories**
```kotlin
LazyColumn {
    item { GeneralSettingsSection() }
    item { PlaybackSettingsSection() }
    item { ServerSettingsSection() }
    item { AppearanceSettingsSection() }
    item { AdvancedSettingsSection() }
    item { AboutSection() }
}
```

### **2. Setting Items**
- **Toggle Switches:** Boolean settings (on/off)
- **Dropdown Menus:** Selection from predefined options
- **Text Inputs:** Custom value entry
- **Sliders:** Range-based settings
- **Buttons:** Action-based settings

### **3. Settings Sections**
- **General:** Basic app preferences
- **Playback:** Video and audio settings
- **Server:** IPTV server configuration
- **Appearance:** Theme and UI customization
- **Advanced:** Developer and debugging options
- **About:** App information and version

## 🔧 **Key Features**

### **1. User Preferences**
```kotlin
// Theme preference
var isDarkMode by remember { mutableStateOf(preferences.isDarkMode) }
Switch(
    checked = isDarkMode,
    onCheckedChange = { newValue ->
        isDarkMode = newValue
        preferences.setDarkMode(newValue)
        themeManager.updateTheme(newValue)
    }
)
```

### **2. Server Configuration**
```kotlin
// Server URL configuration
var serverUrl by remember { mutableStateOf(preferences.serverUrl) }
OutlinedTextField(
    value = serverUrl,
    onValueChange = { newUrl ->
        serverUrl = newUrl
        preferences.setServerUrl(newUrl)
    },
    label = { Text("Server URL") }
)
```

### **3. Playback Settings**
- **Default Quality:** Preferred video quality
- **Auto-Play:** Automatic episode progression
- **Resume Playback:** Continue from last position
- **Subtitle Language:** Default subtitle preference
- **Audio Language:** Default audio track preference

### **4. Appearance Settings**
- **Theme:** Light, Dark, or System default
- **Font Size:** Text size customization
- **Animation Speed:** UI animation preferences
- **Grid Layout:** Content display preferences

## 📱 **User Interface**

### **Layout Structure:**
```
┌─────────────────────────────────┐
│         Top App Bar             │
├─────────────────────────────────┤
│                                 │
│    ┌─────────────────────────┐   │
│    │    General Settings     │   │
│    │  [Theme] [Language]     │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │   Playback Settings     │   │
│    │  [Quality] [Auto-play]  │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │   Server Settings       │   │
│    │  [URL] [Credentials]    │   │
│    └─────────────────────────┘   │
│                                 │
│    ┌─────────────────────────┐   │
│    │   Advanced Settings     │   │
│    │  [Debug] [Logs]         │   │
│    └─────────────────────────┘   │
└─────────────────────────────────┘
```

### **Settings Design:**
- **Clear Categories:** Logical grouping of settings
- **Descriptive Labels:** Clear explanation of each setting
- **Visual Feedback:** Immediate response to changes
- **Accessibility:** Screen reader and keyboard support

## 🚀 **Navigation Flow**

### **Entry Points:**
1. **Home Screen** → Settings Card → SettingsScreen
2. **Deep Link** → Direct settings navigation
3. **Context Menu** → Quick settings access

### **Navigation Path:**
```
HomeScreen → SettingsScreen → Specific Setting → Save/Apply
```

### **Exit Points:**
1. **Back Button** → Previous screen
2. **Save Button** → Apply changes and return
3. **Cancel Button** → Discard changes and return

## 🔄 **State Management**

### **ViewModel Integration:**
```kotlin
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferencesRepository: UserPreferencesRepository,
    private val themeManager: ThemeManager
) : ViewModel()
```

### **State Variables:**
- **User Preferences:** Current user settings
- **Server Settings:** IPTV server configuration
- **Theme State:** Current theme and appearance
- **Validation State:** Settings validation status
- **Save State:** Settings save progress

## 🎨 **Theming**

### **Color Scheme:**
- **Primary:** Brand color for main elements
- **Secondary:** Accent color for highlights
- **Surface:** Card and background colors
- **On Surface:** Text and icon colors

### **Typography:**
- **Section Headers:** Large, bold text for categories
- **Setting Labels:** Clear, readable text
- **Descriptions:** Smaller text for explanations

## 📊 **Performance Considerations**

### **Optimizations:**
- **Lazy Loading:** Settings load as needed
- **State Persistence:** Settings saved immediately
- **Validation:** Real-time settings validation
- **Memory Management:** Efficient resource usage

### **Settings Persistence:**
```kotlin
// Immediate settings save
fun updateSetting(key: String, value: Any) {
    viewModelScope.launch {
        userPreferencesRepository.setPreference(key, value)
        // Trigger UI update
        _settingsState.value = _settingsState.value.copy(
            lastUpdated = System.currentTimeMillis()
        )
    }
}
```

## 🔧 **Configuration**

### **Preferences Configuration:**
```kotlin
// User preferences repository
@Provides
@Singleton
fun provideUserPreferencesRepository(
    context: Context
): UserPreferencesRepository {
    return UserPreferencesRepository(context)
}
```

### **Theme Configuration:**
```kotlin
// Theme manager configuration
@Provides
@Singleton
fun provideThemeManager(
    userPreferencesRepository: UserPreferencesRepository
): ThemeManager {
    return ThemeManager(userPreferencesRepository)
}
```

## 🧪 **Testing**

### **Unit Tests:**
- **Settings Logic:** Test setting validation and persistence
- **Theme Testing:** Test theme switching functionality
- **Server Testing:** Test server configuration validation
- **UI Testing:** Test settings interactions

### **Integration Tests:**
- **End-to-End Settings:** Complete settings flow testing
- **Persistence Testing:** Test settings save and load
- **Cross-Device Testing:** Different screen sizes
- **Accessibility Testing:** Screen reader compatibility

## 🐛 **Common Issues & Solutions**

### **1. Settings Not Saving**
**Problem:** Settings changes not persisted
**Solution:** Check preferences repository and storage permissions

### **2. Server Connection Issues**
**Problem:** Server settings not working
**Solution:** Validate server URL and credentials

### **3. Theme Not Updating**
**Problem:** Theme changes not applied
**Solution:** Verify theme manager and UI state updates

### **4. Performance Issues**
**Problem:** Settings screen slow to load
**Solution:** Implement lazy loading and caching

## 📈 **Analytics & Monitoring**

### **User Interaction Tracking:**
- **Settings Usage:** Track most used settings
- **Configuration Changes:** Monitor setting modifications
- **Server Configurations:** Track server setup patterns
- **Theme Preferences:** Monitor theme usage
- **Error Tracking:** Monitor settings-related errors

### **Performance Metrics:**
- **Settings Load Times:** Performance monitoring
- **Save Success Rates:** Settings persistence tracking
- **Validation Errors:** Settings validation monitoring
- **Memory Usage:** Resource consumption during settings

## 🔮 **Future Enhancements**

### **Planned Features:**
1. **Profile Management:** Multiple user profiles
2. **Backup & Restore:** Settings backup functionality
3. **Import/Export:** Settings file import/export
4. **Advanced Analytics:** Detailed settings analytics
5. **Remote Configuration:** Server-side settings management
6. **Custom Themes:** User-created theme support

### **UI Improvements:**
1. **Settings Search:** Quick settings search functionality
2. **Settings Shortcuts:** Quick access to common settings
3. **Settings Wizard:** Guided settings setup
4. **Settings Recommendations:** AI-powered settings suggestions
5. **Settings Reset:** One-click settings reset

### **Technical Enhancements:**
1. **Settings Sync:** Cloud-based settings synchronization
2. **Settings API:** REST API for settings management
3. **Settings Encryption:** Secure settings storage
4. **Settings Migration:** Automatic settings migration
5. **Settings Validation:** Advanced validation rules

## 📚 **Related Documentation**

### **Dependencies:**
- [Jetpack Compose UI](https://developer.android.com/jetpack/compose)
- [Material Design 3](https://m3.material.io/)
- [Hilt Dependency Injection](https://developer.android.com/training/dependency-injection/hilt-android)
- [DataStore Preferences](https://developer.android.com/topic/libraries/architecture/datastore)

### **Related Screens:**
- [HomeScreen.md](./HomeScreen.md)
- [PlayerScreen.md](./PlayerScreen.md)

### **API Documentation:**
- [UserPreferencesRepository](./../repositories/UserPreferencesRepository.md)
- [ThemeManager](./../services/ThemeManager.md)

---

**Last Updated:** December 2024  
**Version:** 1.0.0  
**Author:** IPTV Development Team  
**Status:** ✅ Production Ready
