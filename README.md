# IPTVFully

Android/Kotlin IPTV application project focused on M3U playlist playback, program-guide data, favorites, and a TV-oriented user interface.

## Repository capabilities

The source tree includes implementations and supporting code for:

- M3U playlist loading and parsing
- XMLTV / EPG handling
- channel, EPG, and playlist persistence with Room
- Retrofit/OkHttp networking
- Jetpack Compose UI
- Media3 / ExoPlayer playback
- Hilt dependency injection
- favorites and settings flows
- Android TV-oriented navigation and layouts

## Architecture

The project uses an MVVM/repository-style structure with reactive state handling. The repository includes data models, DAOs, repositories, network services, dependency-injection modules, ViewModels, navigation, and Compose screens.

## Status

This is an active portfolio/project repository. `IMPLEMENTATION_SUMMARY.md` records implemented work, while `MISSING_FEATURES.md` also contains an outstanding technical/quality backlog. Because those two historical documents contain some conflicting status language, this README intentionally does **not** describe the application as fully production-ready.

Known backlog areas documented in the repository include testing, security hardening, player enhancements, performance/optimization, accessibility, internationalization, and additional UX work.

## Development

Open the project in Android Studio, sync Gradle dependencies, and use the included Gradle wrapper for builds. A sample M3U playlist is included for development/testing.

## Responsible use

Use only playlist sources and media streams you are authorized to access.
