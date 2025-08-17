# IPTV TV App

A modern Android TV application for streaming IPTV channels with a beautiful Material Design 3 interface.

## Features

- 📺 **Live TV Streaming**: Stream IPTV channels using M3U playlists
- 🎮 **TV-Optimized UI**: Built specifically for Android TV with remote navigation
- 🎨 **Modern Design**: Material Design 3 with dynamic theming
- 💾 **Local Storage**: Room database for channel management
- ⭐ **Favorites**: Save and organize your favorite channels
- 🔄 **Auto-refresh**: Automatic playlist updates
- 📱 **Responsive**: Works on both Android TV and mobile devices

## Technology Stack

- **UI Framework**: Jetpack Compose with Material 3
- **TV Components**: Android TV Foundation and Material libraries
- **Video Player**: Media3 ExoPlayer for high-quality streaming
- **Database**: Room for local data persistence
- **Networking**: Retrofit for API calls
- **Image Loading**: Coil for efficient image caching
- **Architecture**: MVVM with Repository pattern

## Setup

### Prerequisites

- Android Studio Arctic Fox or later
- Android SDK 23+
- Kotlin 1.9+
- Android TV device or emulator

### Installation

1. Clone the repository:
```bash
git clone https://github.com/yourusername/iptvtv.git
cd iptvtv
```

2. Open the project in Android Studio

3. Sync Gradle files and build the project

4. Run on Android TV device or emulator

### Configuration

1. Add your M3U playlist URL in the settings
2. Configure EPG (Electronic Program Guide) if available
3. Customize the app theme and colors

## ✅ Completed Features

- ✅ **Real M3U Playlist Loading**: Network-based M3U playlist loading with error handling
- ✅ **EPG Integration**: Complete Electronic Program Guide with XMLTV parsing
- ✅ **Search Functionality**: Real-time search channels by name
- ✅ **Category Filtering**: Dynamic category filtering with chip selection
- ✅ **Favorites System**: Add/remove channels to favorites with database persistence
- ✅ **Favorites Screen**: Dedicated screen to view favorite channels
- ✅ **Settings Screen**: Comprehensive playlist management with M3U/EPG URL loading
- ✅ **TV-Optimized UI**: Remote navigation support with proper focus handling
- ✅ **Real Database Integration**: Room database with proper DAO operations and migrations
- ✅ **Image Loading**: Channel logos with Coil image loading and caching
- ✅ **Error Handling**: Comprehensive error states with user feedback and retry mechanisms
- ✅ **Success Notifications**: User feedback for successful operations
- ✅ **Dependency Injection**: Complete Hilt setup with modules for all components
- ✅ **Network Layer**: Robust Retrofit-based network layer with proper error handling
- ✅ **State Management**: Proper MVVM architecture with StateFlow
- ✅ **Offline Support**: Local database caching for offline viewing
- ✅ **Multiple Playlist Support**: Save and manage multiple M3U playlists
- ✅ **EPG Data Loading**: Load and parse XMLTV format program guides
- ✅ **Loading States**: Proper loading indicators for all async operations

## 🚀 Quick Start

### 1. Build the App
```bash
# Windows
build_and_test.bat

# Or manually
./gradlew assembleDebug
```

### 2. Install on Android TV
```bash
adb install app/build/outputs/apk/debug/app-debug.apk
```

### 3. Add Your Playlists
1. Open the app on your Android TV
2. Click "Settings" button
3. Add your M3U playlist URLs
4. The app will automatically load and parse your channels

### 4. Sample Playlist
Use the included `sample_playlist.m3u` for testing:
- Contains sample channels (BBC News, CNN, ESPN, etc.)
- Demonstrates proper M3U format
- Includes logos and categories

### 5. Using Favorites
- **Add to Favorites**: Click the heart icon on any channel card
- **View Favorites**: Click "Favorites" button in the main screen
- **Remove from Favorites**: Click the filled heart icon to remove
- **Database Storage**: Favorites are saved locally and persist between sessions

## Build Configuration

### Local Build

```bash
./gradlew assembleDebug
```

### Release Build

```bash
./gradlew assembleRelease
```

### Online Build Services

This project is configured for online build services:

#### Bitrise
- Configuration: `bitrise.yml`
- Dashboard: [app.bitrise.io](https://app.bitrise.io/)
- Automatic builds on push to main branch

#### Codemagic
- Configuration: `codemagic.yaml`
- Alternative CI/CD service
- Supports multiple platforms

## Project Structure

```
app/src/main/java/com/example/iptvtv/
├── data/
│   ├── local/          # Room database and DAOs
│   ├── remote/         # API services
│   ├── repository/     # Data repositories
│   └── model/          # Data models
├── navigation/         # Navigation components
├── ui/
│   ├── components/     # Reusable UI components
│   ├── screens/        # App screens
│   └── theme/          # App theming
└── util/              # Utility classes
```

## Contributing

1. Fork the repository
2. Create a feature branch
3. Make your changes
4. Add tests if applicable
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support and questions:
- Create an issue on GitHub
- Check the documentation
- Join our community discussions

## Acknowledgments

- Android TV team for the excellent TV components
- Jetpack Compose team for the modern UI framework
- Media3 team for the powerful video player
