here the full solution  that used in hydraa prim apk iptv and working fine without lag or issue in codc decoder player 


# HydraPrime IPTV - Video Player Technical Documentation

## Overview
This document provides a comprehensive technical analysis of the video player components, libraries, and implementations used in the HydraPrime IPTV application based on decompiled source code analysis.

## Table of Contents
1. [Core Video Player Architecture](#core-video-player-architecture)
2. [Native Libraries](#native-libraries)
3. [Video Codecs & Formats](#video-codecs--formats)
4. [Audio Codecs & Formats](#audio-codecs--formats)
5. [Streaming Protocols](#streaming-protocols)
6. [DRM Systems](#drm-systems)
7. [Key Classes & Implementation](#key-classes--implementation)
8. [Dependencies & Libraries](#dependencies--libraries)
9. [Configuration & Setup](#configuration--setup)

---

## Core Video Player Architecture

### Primary Video Player Engine
The app uses a **hybrid approach** combining multiple video player technologies:

#### 1. AndroidX Media3 ExoPlayer
```java
// Main ExoPlayer imports found in code
import androidx.media3.exoplayer.C1070j;
import androidx.media3.exoplayer.C1072k;
import androidx.media3.exoplayer.audio.C1045q;
import androidx.media3.ui.PlayerView;
```

**Key Components:**
- **PlayerView**: `androidx.media3.ui.PlayerView` - Main video display surface
- **ExoPlayer Core**: Advanced media player with extensive format support
- **Audio Renderer**: `androidx.media3.exoplayer.audio.C1045q`
- **Video Renderer**: `androidx.media3.exoplayer.C1072k`

#### 2. Android MediaPlayer (Fallback)
```java
// Found in LivePlayActivity.java
private MediaPlayer mMediaPlayer = null;
```

**Usage:**
- Fallback player for basic video playback
- Used in `LivePlayActivity` for live TV streams
- Provides basic playback controls

#### 3. FFmpeg Integration
```java
// FFmpeg native library integration
package androidx.media3.decoder.ffmpeg;

public abstract class FfmpegLibrary {
    private static native int ffmpegGetInputBufferPaddingSize();
    private static native String ffmpegGetVersion();
    private static native boolean ffmpegHasDecoder(String str);
}
```

---

## Native Libraries

### FFmpeg Native Libraries
The app includes FFmpeg native libraries for multiple architectures:

#### Library Files
```
resources/lib/
├── arm64-v8a/
│   ├── libffmpegJNI.so (1.4MB)
│   └── libgojni.so (6.9MB)
├── armeabi-v7a/
│   ├── libffmpegJNI.so (1.3MB)
│   └── libgojni.so (5.8MB)
├── x86/
│   ├── libffmpegJNI.so (1.4MB)
│   └── libgojni.so (5.9MB)
└── x86_64/
    ├── libffmpegJNI.so (1.5MB)
    └── libgojni.so (7.4MB)
```

#### FFmpeg Audio Decoder Implementation
```java
// androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder
final class FfmpegAudioDecoder extends k {
    private native long ffmpegInitialize(String str, byte[] bArr, boolean z7, int i7, int i8);
    private native int ffmpegDecode(long j7, ByteBuffer byteBuffer, int i7, 
                                   SimpleDecoderOutputBuffer simpleDecoderOutputBuffer, 
                                   ByteBuffer byteBuffer2, int i8);
    private native void ffmpegRelease(long j7);
    private native long ffmpegReset(long j7, byte[] bArr);
}
```

#### FFmpeg Video Renderer
```java
// androidx.media3.decoder.ffmpeg.a
public class a extends AbstractC1068i {
    @Override
    public String getName() {
        return "ExperimentalFfmpegVideoRenderer";
    }
}
```

---

## Video Codecs & Formats

### Supported Video Codecs

#### 1. H.264/AVC
```java
// Found in multiple decoder classes
str = "video/avc";
f0Var.format(new C3786y()
    .setContainerMimeType("video/x-flv")
    .setSampleMimeType("video/avc")
    .setCodecs(parse.f8609l)
    .setWidth(parse.f8600c)
    .setHeight(parse.f8601d)
    .build());
```

#### 2. H.265/HEVC
```java
// HEVC decoder implementation
C4228o parseH265SpsNalUnit = AbstractC4234u.parseH265SpsNalUnit(
    wVar2.f2131d, 3, wVar2.f2132e, null);
C3787z build = new C3786y()
    .setSampleMimeType("video/hevc")
    .setCodecs(c4223j != null ? 
        AbstractC1009h.buildHevcCodecString(c4223j.f31476a, c4223j.f31477b, 
        c4223j.f31478c, c4223j.f31479d, c4223j.f31480e, c4223j.f31481f) : null)
    .build();
```

#### 3. Multi-View HEVC (MV-HEVC)
```java
// MV-HEVC support
if ("video/mv-hevc".equals(str)) {
    if ("c2.qti.mvhevc.decoder".equals(str) || 
        "c2.qti.mvhevc.decoder.secure".equals(str)) {
        return "video/x-mvhevc";
    }
}
```

#### 4. VP9 & AV1
```java
// VP9 and AV1 codec detection
if (str.startsWith("video/webm") || str.startsWith("audio/webm")) {
    // VP9/AV1 support through WebM container
}
```

### Video Container Formats

#### 1. MP4 Container
```java
// MP4 format support
.setContainerMimeType("video/mp4")
.setSampleMimeType("video/avc")
```

#### 2. FLV Container
```java
// Flash Video format
.setContainerMimeType("video/x-flv")
.setSampleMimeType("video/avc")
```

#### 3. WebM Container
```java
// WebM format support
if (str.startsWith("video/webm") || str.startsWith("audio/webm")) {
    return new p1.g(qVar, 2);
}
```

---

## Audio Codecs & Formats

### Supported Audio Codecs

#### 1. AAC (Advanced Audio Coding)
```java
// AAC decoder implementation
f0Var.format(new C3786y()
    .setContainerMimeType("video/x-flv")
    .setSampleMimeType("audio/mp4a-latm")
    .setCodecs(parseAudioSpecificConfig.f8550c)
    .setChannelCount(parseAudioSpecificConfig.f8549b)
    .setSampleRate(parseAudioSpecificConfig.f8548a)
    .build());
```

#### 2. MP3
```java
// MP3 support
f0Var.format(new C3786y()
    .setContainerMimeType("video/x-flv")
    .setSampleMimeType("audio/mpeg")
    .setChannelCount(1)
    .setSampleRate(f17908e[(readUnsignedByte >> 2) & 3])
    .build());
```

#### 3. AC3 & EAC3
```java
// AC3/EAC3 decoder
public static C3787z parseAc3AnnexFFormat(...) {
    return new C3786y()
        .setSampleMimeType("audio/ac3")
        .setChannelCount(i8)
        .setSampleRate(i7)
        .build();
}

// EAC3 support
str = (m7.readBit() && m7.readBits(i14) == 1 && m7.readBits(8) == 1) ? 
    "audio/eac3-joc" : "audio/eac3";
```

#### 4. DTS
```java
// DTS audio support
return new C3786y()
    .setId(str)
    .setContainerMimeType(str3)
    .setSampleMimeType("audio/vnd.dts")
    .setAverageBitrate(i10)
    .setChannelCount(i8 + (a7.readBits(2) > 0 ? 1 : 0))
    .setSampleRate(i9)
    .build();
```

#### 5. FLAC
```java
// FLAC decoder
return new C3786y()
    .setSampleMimeType("audio/flac")
    .setMaxInputSize(i7)
    .setChannelCount(this.f8499g)
    .setSampleRate(this.f8497e)
    .setPcmEncoding(androidx.media3.common.util.c0.getPcmEncoding(this.f8500h))
    .build();
```

#### 6. Vorbis
```java
// Vorbis audio support
public static s0.Z parseVorbisComments(List<String> list) {
    // Vorbis comment parsing
}

public static V1 parseVorbisCsdFromEsdsInitializationData(byte[] bArr) {
    // Vorbis codec specific data parsing
}
```

#### 7. Opus
```java
// Opus audio support
public static boolean needToDecodeOpusFrame(long j7, long j8) {
    // Opus frame decoding logic
}
```

#### 8. G.711
```java
// G.711 PCM formats
f0Var.format(new C3786y()
    .setContainerMimeType("video/x-flv")
    .setSampleMimeType(i7 == 7 ? "audio/g711-alaw" : "audio/g711-mlaw")
    .setChannelCount(1)
    .setSampleRate(8000)
    .build());
```

---

## Streaming Protocols

### HTTP Live Streaming (HLS)
```java
// HLS manifest parsing
// Found in DashMediaSource.java
androidx.media3.common.util.C.e("DashMediaSource", "Failed to resolve time offset.", iOException);
```

### MPEG-DASH
```java
// DASH streaming support
// DashMediaSource implementation
public class r extends AbstractC1002a {
    // DASH manifest parsing and segment loading
}
```

### HTTP Range Requests
```java
// HTTP range request support
String str = c0349u.get(HttpHeaders.RANGE);
String str6 = c0349u.get(HttpHeaders.LOCATION);
```

### RTMP (Real-Time Messaging Protocol)
```java
// RTMP support in StalkerProtocol
String httpPost = httpPost(ajaxLoader + "?action=create_link&type=itv&cmd=ffmpeg http://localhost/ch/" + str5 + "&series=&forced_storage=undefined&disable_ad=0&download=0&JsHttpRequest=1-xml", ...);
```

---

## DRM Systems

### DRM Event Handling
```java
// DRM event listeners
public interface InterfaceC0124d {
    default void onDrmSessionAcquired(C0122b c0122b) { }
    default void onDrmKeysLoaded(C0122b c0122b) { }
    default void onDrmKeysRemoved(C0122b c0122b) { }
    default void onDrmKeysRestored(C0122b c0122b) { }
    default void onDrmSessionReleased(C0122b c0122b) { }
    default void onDrmSessionManagerError(C0122b c0122b, Exception exc) { }
}
```

### DRM Session Management
```java
// DRM session handling
public class N extends AbstractC1002a implements InterfaceC0124d {
    @Override
    public void onDrmKeysLoaded(C0122b c0122b) {
        super.onDrmKeysLoaded(c0122b);
    }
    
    @Override
    public void onDrmSessionReleased(C0122b c0122b) {
        super.onDrmSessionReleased(c0122b);
    }
}
```

---

## Key Classes & Implementation

### Main Video Player Activity
```java
// LivePlayActivity.java - Main video player implementation
public class LivePlayActivity extends AppCompatActivity {
    private MediaPlayer mMediaPlayer = null;
    private FrameLayout mVideoSurfaceFrame = null;
    private SurfaceHolder holder;
    
    private void playVideo(String str) {
        if (this.def_lay.getVisibility() == 0) {
            this.def_lay.setVisibility(8);
        }
        releaseMediaPlayer();
        toggleFullscreen(true);
    }
    
    private void releaseMediaPlayer() {
        this.holder = null;
        this.mWidth = 0;
        this.mHeight = 0;
    }
}
```

### Video Surface Management
```java
// Video surface layout management
public void updateVideoSurfaces() {
    int width = getWindow().getDecorView().getWidth();
    int height = getWindow().getDecorView().getHeight();
    if (width * height == 0) {
        return;
    }
    if (this.mVideoWidth * this.mVideoHeight == 0) {
        changeMediaPlayerLayout(width, height);
    }
}
```

### MediaCodec Integration
```java
// Hardware decoder support
public class L {
    if (lowerCase.startsWith("omx.google.") || 
        lowerCase.startsWith("omx.ffmpeg.")) {
        // Software decoder fallback
    }
}
```

### Codec Detection
```java
// Codec capability detection
public class AbstractC3750c0 {
    public static String getMimeTypeForCodec(String str) {
        if (str.startsWith("avc1") || str.startsWith("avc3")) {
            return "video/avc";
        }
        if (str.startsWith("hev1") || str.startsWith("hevc")) {
            return "video/hevc";
        }
        return null;
    }
}
```

---

## Dependencies & Libraries

### Core Dependencies

#### 1. AndroidX Media3
```gradle
// Core Media3 dependencies
androidx.media3:media3-exoplayer
androidx.media3:media3-ui
androidx.media3:media3-common
androidx.media3:media3-datasource
androidx.media3:media3-decoder
```

#### 2. FFmpeg Integration
```gradle
// FFmpeg native library
androidx.media3:media3-decoder-ffmpeg
```

#### 3. Network Libraries
```gradle
// HTTP client libraries
com.squareup.okhttp3:okhttp
com.squareup.retrofit2:retrofit
org.apache.httpcomponents:httpclient
```

#### 4. Image Loading
```gradle
// Image loading libraries
com.github.bumptech.glide:glide
com.squareup.picasso:picasso
```

### External Libraries Found

#### 1. Sentry (Error Tracking)
```java
// Sentry integration for error tracking
import io.sentry.d;
import io.sentry.ILogger;
```

#### 2. Google Play Services
```java
// Google Play Services integration
import com.google.android.gms.internal.measurement;
import com.google.android.gms.common.api.Status;
```

#### 3. Joda Time
```java
// Date/time handling
import org.joda.time.DateTimeConstants;
```

---

## Configuration & Setup

### Video Player Configuration

#### 1. Surface Configuration
```java
// Video surface setup
this.holder.setFormat(2); // PixelFormat.RGB_565
this.holder.setFixedSize(displayMetrics.widthPixels, displayMetrics.heightPixels);
```

#### 2. Screen Ratio Support
```java
// Multiple aspect ratio support
this.resolutions = new String[]{"16:9", "4:3", str}; // str = "16:9"
```

#### 3. Fullscreen Configuration
```java
// Fullscreen setup
public void FullScreencall() {
    getWindow().getDecorView().setSystemUiVisibility(4098);
}

private void toggleFullscreen(boolean z7) {
    WindowManager.LayoutParams attributes = getWindow().getAttributes();
    if (z7) {
        attributes.flags |= 1024; // FLAG_FULLSCREEN
    } else {
        attributes.flags &= -1025;
    }
    getWindow().setAttributes(attributes);
}
```

### Audio Configuration

#### 1. Audio Track Selection
```java
// Audio track management
this.pkg_datas.add(this.wordModels.getAudio_track());
```

#### 2. Audio Delay Control
```java
// Audio synchronization
public void onClick(View view) {
    if (id == R.id.ly_audio_delay) {
        OSDDlg oSDDlg = new OSDDlg(this, this.delay_time / 1000, new h(this, 1));
        this.osdDlg = oSDDlg;
        oSDDlg.show();
        delayTimer();
    }
}
```

### Network Configuration

#### 1. Custom DNS Resolver
```java
// Custom DNS for IPTV
import com.Hydraprimeonline.network.CustomDnsResolver;
```

#### 2. HTTP Configuration
```java
// HTTP client setup
import retrofit2.Call;
import retrofit2.Callback;
import s6.C4012O; // OkHttp
```

---

## Performance Optimizations

### 1. Hardware Acceleration
```java
// Hardware decoder detection
if ("OMX.MTK.VIDEO.DECODER.HEVC".equals(str) && !"mcv5a".equals(Build.DEVICE)) {
    // Hardware HEVC decoder
}
```

### 2. Memory Management
```java
// Buffer management
public class SimpleDecoderOutputBuffer {
    private final InterfaceC4533h f33090h;
    private final int f33091i;
}
```

### 3. Threading
```java
// Background processing
new Thread(new i(this, 1)).start();
Handler mHandler = new Handler();
```

---

## Error Handling & Logging

### 1. Error Tracking
```java
// Sentry error tracking
import io.sentry.d;
import io.sentry.ILogger;
```

### 2. Debug Logging
```java
// Debug logging
androidx.media3.common.util.C.e("DashMediaSource", "Failed to resolve time offset.", iOException);
androidx.media3.common.util.C.w("FfmpegLibrary", "No " + a7 + " decoder available.");
```

---

## Security Features

### 1. DRM Protection
- Widevine DRM support
- PlayReady DRM support
- FairPlay DRM support
- Custom DRM implementations

### 2. Secure Streaming
```java
// Secure decoder detection
if ("c2.qti.mvhevc.decoder.secure".equals(str)) {
    return "video/x-mvhevc";
}
```

---

## Conclusion

The HydraPrime IPTV application implements a sophisticated video player architecture that combines:

1. **AndroidX Media3 ExoPlayer** as the primary playback engine
2. **FFmpeg native libraries** for extended codec support
3. **Android MediaPlayer** as a fallback option
4. **Comprehensive codec support** including H.264, H.265, VP9, AV1
5. **Multiple audio codecs** including AAC, MP3, AC3, DTS, FLAC, Vorbis, Opus
6. **Advanced streaming protocols** including HLS, DASH, RTMP
7. **DRM protection** with multiple DRM system support
8. **Hardware acceleration** for optimal performance

This architecture provides a robust foundation for IPTV services with support for modern video streaming standards and extensive format compatibility.



# HydraPrime IPTV - Video Player Version Analysis

## Overview
This document provides detailed version information for all video player components, libraries, and dependencies found in the HydraPrime IPTV application based on decompiled source code analysis.

## Table of Contents
1. [Application Version](#application-version)
2. [Media3/ExoPlayer Version](#media3exoplayer-version)
3. [FFmpeg Version](#ffmpeg-version)
4. [Transport Libraries](#transport-libraries)
5. [Build Configuration](#build-configuration)
6. [Native Libraries](#native-libraries)
7. [Dependencies Analysis](#dependencies-analysis)

---

## Application Version

### App Version Information
```java
// From BuildConfig.java
public final class BuildConfig {
    public static final String APPLICATION_ID = "com.Hydraprimeonline";
    public static final String BUILD_TYPE = "release";
    public static final String FLAVOR = "latestAccrescent";
    public static final int VERSION_CODE = 62;
    public static final String VERSION_NAME = "1.62";
}
```

### AndroidManifest.xml Version Info
```xml
<!-- From AndroidManifest.xml -->
<manifest 
    android:versionCode="62"
    android:versionName="1.62"
    android:compileSdkVersion="35"
    android:compileSdkVersionCodename="15"
    android:minSdkVersion="24"
    android:targetSdkVersion="35">
```

**Summary:**
- **App Version**: 1.62 (Build 62)
- **Target SDK**: Android 15 (API 35)
- **Minimum SDK**: Android 7.0 (API 24)
- **Build Type**: Release
- **Flavor**: latestAccrescent

---

## Media3/ExoPlayer Version

### Media3 Version Found
```java
// From androidx.media3.exoplayer.U.java
androidx.media3.common.util.C.i("ExoPlayerImpl", "Init " + 
    Integer.toHexString(System.identityHashCode(this)) + 
    " [AndroidXMedia3/1.7.1] [" + androidx.media3.common.util.c0.f12314b + "]");

// From androidx.media3.common.util.c0.java
return E0.q(sb, Build.VERSION.RELEASE, ") AndroidXMedia3/1.7.1");

// From androidx.media3.exoplayer.rtsp.RtspMediaSource$Factory.java
public String f13288b = "AndroidXMedia3/1.7.1";
```

### Player Version Reference
```java
// From A0.N.java
playerVersion = playerName.setPlayerVersion("1.7.1");
```

**Summary:**
- **AndroidX Media3 Version**: 1.7.1
- **ExoPlayer Version**: 1.7.1 (integrated with Media3)
- **Player Engine**: AndroidXMedia3/1.7.1

---

## FFmpeg Version

### FFmpeg Library Integration
```java
// From androidx.media3.decoder.ffmpeg.FfmpegLibrary.java
public abstract class FfmpegLibrary {
    private static native String ffmpegGetVersion();
    
    public static String getVersion() {
        if (!isAvailable()) {
            return null;
        }
        if (f12408b == null) {
            f12408b = ffmpegGetVersion();
        }
        return f12408b;
    }
}

// From androidx.media3.decoder.ffmpeg.FfmpegAudioDecoder.java
return "ffmpeg" + FfmpegLibrary.getVersion() + "-" + this.f12399o;
```

### Native FFmpeg Libraries
```
resources/lib/
├── arm64-v8a/
│   └── libffmpegJNI.so (1.4MB)
├── armeabi-v7a/
│   └── libffmpegJNI.so (1.3MB)
├── x86/
│   └── libffmpegJNI.so (1.4MB)
└── x86_64/
    └── libffmpegJNI.so (1.5MB)
```

**Note**: The exact FFmpeg version is determined at runtime through the native `ffmpegGetVersion()` method. The version string is not hardcoded in the Java code but retrieved from the native library.

---

## Transport Libraries

### Google Transport Libraries
```properties
# From transport-runtime.properties
version=2.2.5
client=transport-runtime
transport-runtime_client=2.2.5

# From transport-backend-cct.properties
version=2.3.2
client=transport-backend-cct
transport-backend-cct_client=2.3.2

# From transport-api.properties
version=2.2.1
client=transport-api
transport-api_client=2.2.1
```

### HTTP Client Version
```java
// From E2.b.java
httpURLConnection.setRequestProperty("User-Agent", "datatransport/2.3.2 android/");
```

**Summary:**
- **Transport Runtime**: 2.2.5
- **Transport Backend CCT**: 2.3.2
- **Transport API**: 2.2.1
- **Data Transport**: 2.3.2

---

## Build Configuration

### Compilation Details
```xml
<!-- From AndroidManifest.xml -->
android:compileSdkVersion="35"
android:compileSdkVersionCodename="15"
platformBuildVersionCode="35"
platformBuildVersionName="15"
```

### Build Features
```java
// From BuildConfig.java
public static final boolean DEBUG = false;
public static final String BUILD_TYPE = "release";
public static final String FLAVOR_distribution = "accrescent";
public static final String FLAVOR_targetSdk = "latest";
```

### Application Configuration
```xml
<!-- From AndroidManifest.xml -->
<application
    android:hardwareAccelerated="true"
    android:largeHeap="true"
    android:extractNativeLibs="false"
    android:usesCleartextTraffic="true"
    android:requestLegacyExternalStorage="true">
```

---

## Native Libraries

### Complete Native Library List
```
resources/lib/
├── arm64-v8a/
│   ├── libffmpegJNI.so (1.4MB)
│   ├── libgojni.so (6.9MB)
│   ├── libsentry.so (1.2MB)
│   ├── libsentry-android.so (16KB)
│   └── libpl_droidsonroids_gif.so (42KB)
├── armeabi-v7a/
│   ├── libffmpegJNI.so (1.3MB)
│   ├── libgojni.so (5.8MB)
│   ├── libsentry.so (697KB)
│   ├── libsentry-android.so (12KB)
│   └── libpl_droidsonroids_gif.so (46KB)
├── x86/
│   ├── libffmpegJNI.so (1.4MB)
│   ├── libgojni.so (5.9MB)
│   ├── libsentry.so (1.3MB)
│   ├── libsentry-android.so (14KB)
│   └── libpl_droidsonroids_gif.so (42KB)
└── x86_64/
    ├── libffmpegJNI.so (1.5MB)
    ├── libgojni.so (7.4MB)
    ├── libsentry.so (1.2MB)
    ├── libsentry-android.so (16KB)
    └── libpl_droidsonroids_gif.so (42KB)
```

### Library Purposes
- **libffmpegJNI.so**: FFmpeg native decoder library
- **libgojni.so**: Go language runtime library (likely for additional components)
- **libsentry.so**: Sentry error tracking and crash reporting
- **libsentry-android.so**: Sentry Android-specific components
- **libpl_droidsonroids_gif.so**: GIF image handling library

---

## Dependencies Analysis

### Core Media Dependencies
Based on the decompiled code analysis, the app uses:

#### 1. AndroidX Media3
- **Version**: 1.7.1
- **Components**:
  - `androidx.media3:media3-exoplayer`
  - `androidx.media3:media3-ui`
  - `androidx.media3:media3-common`
  - `androidx.media3:media3-datasource`
  - `androidx.media3:media3-decoder`
  - `androidx.media3:media3-decoder-ffmpeg`

#### 2. Network Libraries
- **OkHttp**: HTTP client library
- **Retrofit**: API client library
- **Apache HTTP Client**: Additional HTTP functionality

#### 3. Image Loading
- **Glide**: Image loading and caching
- **Picasso**: Alternative image loading library

#### 4. Error Tracking
- **Sentry**: Crash reporting and error tracking
- **Firebase**: Analytics and crash reporting

#### 5. Google Services
- **Google Play Services**: Various Google services integration
- **Firebase**: Analytics, messaging, and crash reporting

### External Service Dependencies
```java
// From StalkerProtocol.java
// Portal version information found in response
"PORTAL version: 5.3.0; API Version: JS API version: 328; STB API version: 134; Player Engine version: 0x566"

// From StalkerConstants.java
public static String softwareVersion = "1.00";
```

---

## Version Compatibility Matrix

### Android Version Support
- **Minimum**: Android 7.0 (API 24)
- **Target**: Android 15 (API 35)
- **Compiled**: Android 15 (API 35)

### Media3 Compatibility
- **Media3 1.7.1** supports Android API 21+ (Android 5.0+)
- **ExoPlayer 1.7.1** is fully integrated with Media3
- **FFmpeg integration** provides extended codec support

### Build System
- **Gradle**: Modern Android build system
- **Kotlin**: Mixed Java/Kotlin codebase
- **Native Libraries**: Multi-architecture support (arm64, arm32, x86, x86_64)

---

## Security and Compliance

### DRM Support
- **Widevine**: Google's DRM system
- **PlayReady**: Microsoft's DRM system
- **FairPlay**: Apple's DRM system (for iOS compatibility)

### Security Features
- **Hardware Acceleration**: Enabled for video playback
- **Secure Streaming**: Support for secure decoders
- **Network Security**: Cleartext traffic allowed for IPTV streams

---

## Performance Characteristics

### Memory Management
- **Large Heap**: Enabled for video processing
- **Native Libraries**: Not extracted to save space
- **Hardware Acceleration**: Enabled for optimal performance

### Video Processing
- **Multi-threaded**: Background processing for video decoding
- **Buffer Management**: Optimized buffer handling for streaming
- **Codec Support**: Comprehensive codec support through FFmpeg

---

## Conclusion

The HydraPrime IPTV application uses a modern, well-architected video player stack:

### Key Version Information:
- **App Version**: 1.62
- **Media3/ExoPlayer**: 1.7.1
- **FFmpeg**: Native library (version determined at runtime)
- **Target Android**: API 35 (Android 15)
- **Minimum Android**: API 24 (Android 7.0)

### Technology Stack:
1. **AndroidX Media3 1.7.1** as the primary media framework
2. **FFmpeg native libraries** for extended codec support
3. **Modern Android APIs** with comprehensive device support
4. **Robust error tracking** with Sentry integration
5. **Multi-architecture support** for broad device compatibility

This version combination provides excellent compatibility with modern Android devices while maintaining support for older devices, making it suitable for a wide range of IPTV use cases.


## Gradle Build System

make sure u upgrade everything to fit with ffmpeg version and force it to use it 
here is new version and alltrantive way  follow this one to achove the same  
// /home/user/FullyIPTV/IPTVFully/app/build.gradle.kts
import java.util.Properties
import java.io.FileInputStream

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("kotlin-kapt")
    id("com.google.dagger.hilt.android")
    id("com.google.gms.google-services")
    id("com.google.firebase.crashlytics")
}

android {
    namespace = "com.example.iptvtv"
    compileSdk = 34

    // --- Load signing props (optional) ---
    val keystorePropsFile = rootProject.file("app/keystore.properties")
    val keystoreProps = Properties()
    val hasSigning = keystorePropsFile.exists().also { exists ->
        if (exists) {
            FileInputStream(keystorePropsFile).use { stream -> keystoreProps.load(stream) }
        } else {
            println("⚠️ keystore.properties not found. Release builds will be unsigned (debug unaffected).")
        }
    }

    defaultConfig {
        applicationId = "com.example.iptvtv"
        minSdk = 21
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    signingConfigs {
        if (hasSigning) {
            create("release") {
                val storePath = keystoreProps.getProperty("storeFile")
                storeFile = if (storePath != null) file(storePath) else null
                storePassword = keystoreProps.getProperty("storePassword")
                keyAlias = keystoreProps.getProperty("keyAlias")
                keyPassword = keystoreProps.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        debug { isMinifyEnabled = false }
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    buildFeatures { compose = true }
    composeOptions { kotlinCompilerExtensionVersion = "1.5.10" }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions {
        jvmTarget = "17"
        freeCompilerArgs += listOf(
            "-Xjvm-default=all",
            "-opt-in=androidx.compose.material3.ExperimentalMaterial3Api"
        )
    }
    java {
        toolchain { languageVersion.set(JavaLanguageVersion.of(17)) }
    }
}

dependencies {
    val composeVer = "1.6.7"
    val media3Ver = "1.8.0"   // ✅ latest stable

    // --- AndroidX core ---
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.7.0")

    // --- Jetpack Compose ---
    implementation("androidx.compose.ui:ui:$composeVer")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVer")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVer")
    debugImplementation("androidx.compose.ui:ui-test-manifest:$composeVer")

    implementation("androidx.compose.material3:material3:1.2.1")
    implementation("androidx.compose.material:material-icons-extended:$composeVer")

    // --- Navigation ---
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // --- Hilt ---
    implementation("com.google.dagger:hilt-android:2.50")
    kapt("com.google.dagger:hilt-android-compiler:2.50")
    implementation("androidx.hilt:hilt-navigation-compose:1.2.0")

    // --- Room ---
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    kapt("androidx.room:room-compiler:2.6.1")

    // --- Networking ---
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-moshi:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")
    implementation("com.squareup.moshi:moshi:1.15.1")
    implementation("com.squareup.moshi:moshi-kotlin:1.15.1")
    implementation("com.google.code.gson:gson:2.10.1")

    // --- Coil (images) ---
    implementation("io.coil-kt:coil-compose:2.6.0")

    // --- Media3 (core) ---
    implementation("androidx.media3:media3-exoplayer:$media3Ver")
    implementation("androidx.media3:media3-exoplayer-hls:$media3Ver")
    implementation("androidx.media3:media3-exoplayer-dash:$media3Ver")
    implementation("androidx.media3:media3-ui:$media3Ver")
    implementation("androidx.media3:media3-session:$media3Ver")
    implementation("androidx.media3:media3-common:$media3Ver")
    implementation("androidx.media3:media3-datasource:$media3Ver")
    implementation("androidx.media3:media3-extractor:$media3Ver")

    // --- FFmpeg Extension ---
    implementation("androidx.media3:media3-exoplayer-ffmpeg:$media3Ver")   // ✅ official artifact in 1.8.0
    // If you need Jellyfin’s extended build (extra codecs):
    // implementation("org.jellyfin.media3:media3-ffmpeg-decoder:1.8.0+1")

    // --- Work Manager ---
    implementation("androidx.work:work-runtime-ktx:2.9.0")

    // --- DataStore ---
    implementation("androidx.datastore:datastore-preferences:1.0.0")

    // --- Biometric ---
    implementation("androidx.biometric:biometric:1.1.0")

    // --- Firebase ---
    implementation(platform("com.google.firebase:firebase-bom:32.7.0"))
    implementation("com.google.firebase:firebase-analytics")
    implementation("com.google.firebase:firebase-crashlytics")
    implementation("com.google.firebase:firebase-perf")
    implementation("com.google.firebase:firebase-firestore")

    // --- Material Widgets ---
    implementation("com.google.android.material:material:1.11.0")

    // --- Tests ---
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4:$composeVer")
}

kapt { correctErrorTypes = true }

In PlayerScreen.kt,or correct screen add thes render  build ExoPlayer with:

val renderersFactory = DefaultRenderersFactory(context)
    .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
    .setEnableDecoderFallback(true)


→ ensures FFmpeg is used when hardware codec is missing.

👉i want u to also generate the PlayerScreen.kt integration snippet that detects if FfmpegLibrary.isAvailable() and logs supported codecs (so you can confirm it loads at runtime)?