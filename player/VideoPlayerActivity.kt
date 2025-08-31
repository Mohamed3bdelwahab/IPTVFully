package com.example.iptvtv.ui.screens

import android.app.Activity
import android.net.Uri
import android.util.Log
import android.view.KeyEvent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.iptvtv.R
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import androidx.media3.ui.AspectRatioFrameLayout
import com.example.iptvtv.data.repository.PlaybackProgressRepository
import com.example.iptvtv.service.HydraApiService

import dagger.hilt.android.EntryPointAccessors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.max

// ---------------------------- Small utils ----------------------------
private fun now() = System.currentTimeMillis()

// Prefer non-Amlogic codecs if available (A)
private fun amlogicCautiousSelector(): MediaCodecSelector = MediaCodecSelector { mime, secure, tunneled ->
    val all = MediaCodecSelector.DEFAULT.getDecoderInfos(mime, secure, tunneled)
    val filtered = all.filterNot { info ->
        val n = info.name.lowercase()
        n.startsWith("c2.amlogic") || n.contains("amlogic")
    }
    if (filtered.isNotEmpty()) filtered else all
}

// Better player configuration constants
private object BetterPlayerConfig {
    const val BUFFER_SIZE = 50 * 1024 * 1024 // 50MB buffer (much larger than default)
    const val CONNECT_TIMEOUT = 30L // 30 seconds
    const val READ_TIMEOUT = 30L // 30 seconds
    const val MIN_BUFFER_MS = 30_000 // 30 seconds minimum buffer
    const val MAX_BUFFER_MS = 60_000 // 60 seconds maximum buffer
    const val BUFFER_FOR_PLAYBACK_MS = 1_500 // 1.5 seconds for playback
    const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 3_000 // 3 seconds after rebuffer
}

// ---------------------------- Screen ----------------------------
@Composable
fun PlayerScreen(
    url: String,
    title: String?,
    onBack: () -> Unit,
    episodeId: String? = null,
    seriesId: String? = null
) {
    val context = LocalContext.current
    val activity = remember(context) { context.findActivity() }
    val scope = rememberCoroutineScope()
    val focusManager = LocalFocusManager.current
    val focusRequester = remember { FocusRequester() }

    // ---- DI (Hilt entry point) ----
    val entry = remember {
        EntryPointAccessors.fromApplication(
            context.applicationContext,
            PlayerScreenEntryPoint::class.java
        )
    }
    val hydraApiService = remember { entry.hydraApiService() }
    val playbackProgressRepository = remember { entry.playbackProgressRepository() }

    // ---- UI State ----
    var showPlaylist by rememberSaveable { mutableStateOf(false) }
    var showControls by rememberSaveable { mutableStateOf(true) }
    var showSpeedMenu by rememberSaveable { mutableStateOf(false) }
    var showSettings by rememberSaveable { mutableStateOf(false) }
    var isPlaying by remember { mutableStateOf(true) }
    var isBuffering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var currentPosition by remember { mutableStateOf(0L) }
    var duration by remember { mutableStateOf(0L) }
    var bufferedPosition by remember { mutableStateOf(0L) }

    var playbackSpeed by rememberSaveable { mutableStateOf(1.0f) }
    var autoPlayNext by rememberSaveable { mutableStateOf(true) }
    var lastUserAction by remember { mutableStateOf(System.currentTimeMillis()) }
    


    // Aspect ratio (no black edges by default)
    var aspectMode by rememberSaveable { mutableStateOf(AspectRatioFrameLayout.RESIZE_MODE_ZOOM) }

    // ---- Episodes / Playlist ----
    var episodes by remember { mutableStateOf<List<EpisodeInfo>>(emptyList()) }
    var currentEpisodeIndex by rememberSaveable { mutableStateOf(0) }
    var currentEpisode by remember { mutableStateOf<EpisodeInfo?>(null) }
    val playlistListState = remember { LazyListState() }

    // ---- Resume ----
    var resumePosition by remember { mutableStateOf(0L) }

    // ---- Track selector ----
    val trackSelector = remember {
        DefaultTrackSelector(context).apply {
            setParameters(
                buildUponParameters()
                    .setPreferredAudioLanguage(null)
                    .setTunnelingEnabled(false)
            )
        }
    }

    // ---- Better Load Control with 50MB buffer and optimized settings ----
    val loadControl = remember {
        DefaultLoadControl.Builder()
            .setBufferDurationsMs(
                /* minBufferMs = */ BetterPlayerConfig.MIN_BUFFER_MS,
                /* maxBufferMs = */ BetterPlayerConfig.MAX_BUFFER_MS,
                /* bufferForPlaybackMs = */ BetterPlayerConfig.BUFFER_FOR_PLAYBACK_MS,
                /* bufferForPlaybackAfterRebufferMs = */ BetterPlayerConfig.BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
            )
            .setTargetBufferBytes(BetterPlayerConfig.BUFFER_SIZE)
            .build()
    }

    // ---- Renderers factory with decoder fallback + cautious selector (A) ----
    val renderersFactory = remember {
        DefaultRenderersFactory(context)
            .setEnableDecoderFallback(true)
            .setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_PREFER)
            .setMediaCodecSelector(amlogicCautiousSelector())
    }

    // ---- Better ExoPlayer with OkHttp support ----
    val exoPlayer = remember(url) {
        // Create OkHttp client with better timeouts
        val okHttpClient = OkHttpClient.Builder()
            .connectTimeout(BetterPlayerConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(BetterPlayerConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .build()
        
        // Create data source factory with OkHttp support
        val dataSourceFactory = OkHttpDataSource.Factory(okHttpClient)
        
        Log.i("PlayerScreen", "🎬 Creating Better Player with:")
        Log.i("PlayerScreen", "   Buffer: ${BetterPlayerConfig.BUFFER_SIZE / (1024 * 1024)}MB")
        Log.i("PlayerScreen", "   Timeouts: ${BetterPlayerConfig.CONNECT_TIMEOUT}s connect, ${BetterPlayerConfig.READ_TIMEOUT}s read")
        Log.i("PlayerScreen", "   URL: $url")
        
        ExoPlayer.Builder(context)
            .setRenderersFactory(renderersFactory)
            .setLoadControl(loadControl)
            .setTrackSelector(trackSelector)
            .setMediaSourceFactory(DefaultMediaSourceFactory(dataSourceFactory as androidx.media3.datasource.DataSource.Factory))
            .build()
            .apply {
                videoScalingMode = C.VIDEO_SCALING_MODE_SCALE_TO_FIT_WITH_CROPPING
                setSpeed(playbackSpeed) // from PlayerUtils.kt

                addListener(object : Player.Listener {
                    override fun onPlaybackStateChanged(state: Int) {
                        when (state) {
                            Player.STATE_READY -> {
                                Log.i("PlayerScreen", "✅ Playback Ready - Duration: ${this@apply.duration}ms")
                                duration = this@apply.duration.coerceAtLeast(0L)
                                isPlaying = this@apply.isPlaying
                                isBuffering = false
                                setSpeed(playbackSpeed) // re-apply after prepare
                                if (resumePosition > 0L) {
                                    seekTo(resumePosition)
                                    resumePosition = 0L
                                }
                            }
                            Player.STATE_BUFFERING -> {
                                Log.i("PlayerScreen", "🔄 Buffering...")
                                isBuffering = true
                            }
                            Player.STATE_ENDED -> {
                                Log.i("PlayerScreen", "⏹️ Playback Ended")
                                isPlaying = false
                                if (autoPlayNext) {
                                    tryPlayNextEpisode(this@apply, episodes, currentEpisodeIndex) { newIndex ->
                                        currentEpisodeIndex = newIndex
                                        currentEpisode = episodes.getOrNull(newIndex)
                                        scope.launch { playlistListState.animateScrollToItem(newIndex) }
                                    }
                                }
                            }
                            Player.STATE_IDLE -> {
                                Log.i("PlayerScreen", "⏸️ Player Idle")
                            }
                        }
                    }

                    override fun onIsPlayingChanged(playing: Boolean) {
                        Log.i("PlayerScreen", "🎬 Playing state changed: $playing")
                        isPlaying = playing
                    }

                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        Log.e("PlayerScreen", "❌ Better Player Error: ${error.message}")
                        Log.e("PlayerScreen", "Error Code: ${error.errorCode}")
                        Log.e("PlayerScreen", "Error Cause: ${error.cause}")
                        Log.e("PlayerScreen", "Error Type: ${error.errorCodeName}")
                        
                        // Better error categorization
                        val errorType = when (error.errorCode) {
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_FAILED,
                            androidx.media3.common.PlaybackException.ERROR_CODE_IO_NETWORK_CONNECTION_TIMEOUT -> "Network Error"
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED,
                            androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> "Decoder Error"
                            androidx.media3.common.PlaybackException.ERROR_CODE_AUDIO_TRACK_INIT_FAILED -> "Audio Error"
                            else -> "Playback Error"
                        }
                        
                        errorMessage = "$errorType: ${error.message ?: "Unknown error occurred"}"
                        isBuffering = false
                        
                        // Log additional context for debugging
                        Log.e("PlayerScreen", "Current URL: $url")
                        Log.e("PlayerScreen", "Buffer Size: ${BetterPlayerConfig.BUFFER_SIZE / (1024 * 1024)}MB")
                        Log.e("PlayerScreen", "Timeouts: ${BetterPlayerConfig.CONNECT_TIMEOUT}s connect, ${BetterPlayerConfig.READ_TIMEOUT}s read")
                    }
                })
            }
    }

    // ---- Cleanup ----
    DisposableEffect(Unit) {
        onDispose {
            try { exoPlayer.clearVideoSurface() } catch (_: Throwable) {}
            exoPlayer.release()
        }
    }

    // ---- Immersive mode / keep screen on ----
    LaunchedEffect(isPlaying, showControls, showPlaylist, showSpeedMenu, showSettings) {
        val immersive = isPlaying && !showControls && !showPlaylist && !showSpeedMenu && !showSettings
        activity?.let { setImmersive(it, immersive) }
    }
    DisposableEffect(Unit) {
        activity?.window?.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        onDispose { activity?.window?.clearFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON) }
    }

    // ---- Initial media setup ----
    LaunchedEffect(url, currentEpisode) {
        val mediaUrl = currentEpisode?.streamUrl ?: url
        if (mediaUrl.isNotEmpty()) {
            try {
                Log.i("PlayerScreen", "Setting up media item for URL: $mediaUrl")
                val mediaItem = MediaItem.fromUri(Uri.parse(mediaUrl))
                exoPlayer.setMediaItem(mediaItem)
                exoPlayer.prepare()
                exoPlayer.playWhenReady = true
                Log.i("PlayerScreen", "✅ Media item set up successfully")
            } catch (e: Exception) {
                Log.e("PlayerScreen", "❌ Error setting up media item: ${e.message}")
                errorMessage = "Failed to load video: ${e.message}"
            }
        }
    }

    // ---- Load episodes (if series) ----
    LaunchedEffect(seriesId) {
        if (seriesId != null) {
            runCatching {
                val res = hydraApiService.getEpisodesAlternative(seriesId.toIntOrNull() ?: 0)
                if (res.success && res.data != null) {
                    episodes = res.data.map { e ->
                        EpisodeInfo(
                            id = e.id,
                            title = e.title,
                            season = e.season.toString(),
                            extension = e.containerExtension,
                            // NOTE: placeholder stream URL — replace with your service URL
                            streamUrl = "http://aws85485.amazonedge.net//series/moh7amed819/150730/${e.id}.mkv"
                        )
                    }
                } else {
                    Log.e("PlayerScreen", "Failed to load episodes: ${res.message}")
                    errorMessage = "Failed to load episodes: ${res.message}"
                    if (episodes.isEmpty()) {
                        episodes = listOf(
                            EpisodeInfo("test1", "Test Episode 1", "1", "mkv", "http://test.com/episode1.mkv"),
                            EpisodeInfo("test2", "Test Episode 2", "1", "mkv", "http://test.com/episode2.mkv")
                        )
                    }
                }

                episodeId?.let { id ->
                    val idx = episodes.indexOfFirst { it.id == id }
                    if (idx >= 0) {
                        currentEpisodeIndex = idx
                        currentEpisode = episodes[idx]
                        scope.launch { playlistListState.scrollToItem(max(0, idx - 2)) }
                    }
                }
            }.onFailure {
                Log.e("PlayerScreen", "Exception loading episodes", it)
                errorMessage = "Failed to load episodes: ${it.message}"
                if (episodes.isEmpty()) {
                    episodes = listOf(
                        EpisodeInfo("test1", "Test Episode 1", "1", "mkv", "http://test.com/episode1.mkv"),
                        EpisodeInfo("test2", "Test Episode 2", "1", "mkv", "http://test.com/episode2.mkv")
                    )
                }
            }
        }
    }

    // ---- Load resume ----
    LaunchedEffect(episodeId) {
        resumePosition = runCatching {
            if (episodeId == null) 0L
            else playbackProgressRepository.getProgress(episodeId)?.let { if (!it.completed) it.position else 0L } ?: 0L
        }.getOrElse { 0L }
    }

    // ---- Update timecodes ----
    LaunchedEffect(exoPlayer) {
        while (true) {
            currentPosition = exoPlayer.currentPosition
            bufferedPosition = exoPlayer.bufferedPosition
            delay(500)
        }
    }

    // ---- Auto-hide controls ----
    LaunchedEffect(lastUserAction) {
        delay(3000)
        if (System.currentTimeMillis() - lastUserAction >= 2800) showControls = false
    }

    // ---- Persist progress ----
    LaunchedEffect(exoPlayer, episodeId, currentEpisode) {
        while (true) {
            delay(10_000)
            val curEp = currentEpisode ?: continue
            if (episodeId != null && isPlaying) {
                runCatching {
                    val pos = exoPlayer.currentPosition
                    val dur = exoPlayer.duration
                    val completed = playbackProgressRepository.isEpisodeCompleted(pos, dur)
                    playbackProgressRepository.saveProgress(
                        episodeId = episodeId,
                        seriesId = seriesId ?: "",
                        episodeTitle = curEp.title,
                        seriesTitle = curEp.season,
                        position = pos,
                        duration = dur,
                        completed = completed
                    )
                    if (completed) playbackProgressRepository.markAsCompleted(episodeId)
                }
            }
        }
    }

    // ---- TV remote handler ----
    val handleKey: (KeyEvent) -> Boolean = { ev ->
        fun pokeControls() { lastUserAction = now(); showControls = true }
        
        // If any overlay is open, only handle back button, let overlays handle other keys
        if (showPlaylist || showSpeedMenu || showSettings) {
            // Only handle back button when overlays are open
            if (ev.keyCode == KeyEvent.KEYCODE_BACK) {
                showPlaylist = false
                showSpeedMenu = false
                showSettings = false
                pokeControls()
                true
            } else {
                false
            }
        } else if (ev.action != KeyEvent.ACTION_DOWN) {
            false
        } else {
            when (ev.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                    togglePlay(exoPlayer); pokeControls(); true
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> { seekBy(exoPlayer, +10_000, duration); pokeControls(); true }
                KeyEvent.KEYCODE_DPAD_LEFT  -> { seekBy(exoPlayer, -10_000, duration); pokeControls(); true }

                KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> { togglePlay(exoPlayer); pokeControls(); true }
                KeyEvent.KEYCODE_MEDIA_PLAY       -> { exoPlayer.play(); pokeControls(); true }
                KeyEvent.KEYCODE_MEDIA_PAUSE      -> { exoPlayer.pause(); pokeControls(); true }
                KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> { seekBy(exoPlayer, +30_000, duration); pokeControls(); true }
                KeyEvent.KEYCODE_MEDIA_REWIND       -> { seekBy(exoPlayer, -30_000, duration); pokeControls(); true }
                KeyEvent.KEYCODE_MEDIA_NEXT -> {
                    tryPlayNextEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                        currentEpisodeIndex = newIndex
                        currentEpisode = episodes.getOrNull(newIndex)
                        scope.launch { playlistListState.animateScrollToItem(newIndex) }
                    }
                    pokeControls(); true
                }
                KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                    tryPlayPrevEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                        currentEpisodeIndex = newIndex
                        currentEpisode = episodes.getOrNull(newIndex)
                        scope.launch { playlistListState.animateScrollToItem(newIndex) }
                    }
                    pokeControls(); true
                }

                // Panels
                KeyEvent.KEYCODE_MENU -> { 
                    if (showPlaylist) {
                        showPlaylist = false
                    } else {
                        showSpeedMenu = false
                        showSettings = false
                        showPlaylist = true
                    }
                    pokeControls(); true 
                }
                KeyEvent.KEYCODE_INFO -> { 
                    if (showSpeedMenu) {
                        showSpeedMenu = false
                    } else {
                        showPlaylist = false
                        showSettings = false
                        showSpeedMenu = true
                    }
                    pokeControls(); true 
                }
                KeyEvent.KEYCODE_SETTINGS -> { 
                    if (showSettings) {
                        showSettings = false
                    } else {
                        showPlaylist = false
                        showSpeedMenu = false
                        showSettings = true
                    }
                    pokeControls(); true 
                }

                // Channel up/down jump playlist
                KeyEvent.KEYCODE_CHANNEL_UP -> {
                    if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.lastIndex) {
                        val newIndex = currentEpisodeIndex + 1
                        playEpisode(exoPlayer, episodes[newIndex]) {
                            currentEpisodeIndex = newIndex
                            currentEpisode = episodes[newIndex]
                            scope.launch { playlistListState.animateScrollToItem(newIndex) }
                        }
                    }
                    pokeControls(); true
                }
                KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                    if (episodes.isNotEmpty() && currentEpisodeIndex > 0) {
                        val newIndex = currentEpisodeIndex - 1
                        playEpisode(exoPlayer, episodes[newIndex]) {
                            currentEpisodeIndex = newIndex
                            currentEpisode = episodes[newIndex]
                            scope.launch { playlistListState.animateScrollToItem(newIndex) }
                        }
                    }
                    pokeControls(); true
                }

                // Speed shortcuts - removed, handled by SpeedMenuOverlay

                            KeyEvent.KEYCODE_BACK -> {
                try { exoPlayer.clearVideoSurface() } catch (_: Throwable) {}
                exoPlayer.release()
                onBack()
                true
            }
                else -> false
            }
        }
    }
    val onKeyEvent: (androidx.compose.ui.input.key.KeyEvent) -> Boolean = { ke ->
        fun pokeControls() { 
            lastUserAction = now(); 
            showControls = true 
        }
        
        // If any overlay is open, only handle back button, let overlays handle other keys
        if (showPlaylist || showSpeedMenu || showSettings) {
            // Only handle back button when overlays are open
            if (ke.nativeKeyEvent.keyCode == KeyEvent.KEYCODE_BACK) {
                showPlaylist = false
                showSpeedMenu = false
                showSettings = false
                pokeControls()
                true
            } else {
                false
            }
        } else if (ke.nativeKeyEvent.action != KeyEvent.ACTION_DOWN) {
            false
        } else {
            when (ke.nativeKeyEvent.keyCode) {
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER, KeyEvent.KEYCODE_NUMPAD_ENTER -> {
                togglePlay(exoPlayer); pokeControls(); true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                seekBy(exoPlayer, +10_000, duration); pokeControls(); true
            }
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                seekBy(exoPlayer, -10_000, duration); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
                togglePlay(exoPlayer); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_PLAY -> {
                exoPlayer.play(); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_PAUSE -> {
                exoPlayer.pause(); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD -> {
                seekBy(exoPlayer, +30_000, duration); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND -> {
                seekBy(exoPlayer, -30_000, duration); pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                tryPlayNextEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                    currentEpisodeIndex = newIndex
                    currentEpisode = episodes.getOrNull(newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls(); true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                tryPlayPrevEpisode(exoPlayer, episodes, currentEpisodeIndex) { newIndex ->
                    currentEpisodeIndex = newIndex
                    currentEpisode = episodes.getOrNull(newIndex)
                    scope.launch { playlistListState.animateScrollToItem(newIndex) }
                }
                pokeControls(); true
            }
            // Panels
            KeyEvent.KEYCODE_MENU -> {
                if (showPlaylist) {
                    showPlaylist = false
                } else {
                    showSpeedMenu = false
                    showSettings = false
                    showPlaylist = true
                }
                pokeControls(); true
            }
            KeyEvent.KEYCODE_INFO -> {
                if (showSpeedMenu) {
                    showSpeedMenu = false
                } else {
                    showPlaylist = false
                    showSettings = false
                    showSpeedMenu = true
                }
                pokeControls(); true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                if (showSettings) {
                    showSettings = false
                } else {
                    showPlaylist = false
                    showSpeedMenu = false
                    showSettings = true
                }
                pokeControls(); true
            }
            // Channel up/down jump playlist
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.lastIndex) {
                    val newIndex = currentEpisodeIndex + 1
                    playEpisode(exoPlayer, episodes[newIndex]) {
                        currentEpisodeIndex = newIndex
                        currentEpisode = episodes[newIndex]
                        scope.launch { playlistListState.animateScrollToItem(newIndex) }
                    }
                }
                pokeControls(); true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                if (episodes.isNotEmpty() && currentEpisodeIndex > 0) {
                    val newIndex = currentEpisodeIndex - 1
                    playEpisode(exoPlayer, episodes[newIndex]) {
                        currentEpisodeIndex = newIndex
                        currentEpisode = episodes[newIndex]
                        scope.launch { playlistListState.animateScrollToItem(newIndex) }
                    }
                }
                pokeControls(); true
            }
            // Speed shortcuts - removed, handled by SpeedMenuOverlay
                KeyEvent.KEYCODE_BACK -> {
                    try { exoPlayer.clearVideoSurface() } catch (_: Throwable) {}
                    exoPlayer.release()
                    onBack()
                    true
                }
                else -> false
            }
        }
    }

    // ---------------------------- UI ----------------------------
    Scaffold(
        topBar = {
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                TopAppBar(
                    title = {
                        Text(
                            text = currentEpisode?.title ?: title ?: "IPTV Player",
                            maxLines = 1, overflow = TextOverflow.Ellipsis
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = {
                            try { exoPlayer.clearVideoSurface() } catch (_: Throwable) {}
                            exoPlayer.release()
                            onBack()
                        }) { Icon(Icons.Default.ArrowBack, contentDescription = "Back") }
                    },
                    actions = {
                        IconButton(onClick = {
                            // Toggle playlist menu
                            if (showPlaylist) {
                                showPlaylist = false
                            } else {
                                showSpeedMenu = false
                                showSettings = false
                                showPlaylist = true
                            }
                            lastUserAction = now()
                        }) { Icon(Icons.Default.PlaylistPlay, contentDescription = "Playlist") }
                        IconButton(onClick = {
                            // Toggle speed menu
                            if (showSpeedMenu) {
                                showSpeedMenu = false
                            } else {
                                showPlaylist = false
                                showSettings = false
                                showSpeedMenu = true
                            }
                            lastUserAction = now()
                        }) { Icon(Icons.Default.Speed, contentDescription = "Speed") }
                        IconButton(onClick = {
                            // Toggle settings menu
                            if (showSettings) {
                                showSettings = false
                            } else {
                                showPlaylist = false
                                showSpeedMenu = false
                                showSettings = true
                            }
                            lastUserAction = now()
                        }) { Icon(Icons.Default.Settings, contentDescription = "Settings") }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Black.copy(alpha = 0.8f),
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .padding(padding)
                .focusRequester(focusRequester)
                .focusable()
                .onKeyEvent(onKeyEvent)
                .clickable { showControls = !showControls; lastUserAction = now() }
        ) {
            // PlayerView composed from XML
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { ctx ->
                    val view = android.view.LayoutInflater.from(ctx)
                        .inflate(R.layout.compose_player_view, null, false)

                    val pv = view.findViewById<androidx.media3.ui.PlayerView>(R.id.playerView)
                    pv.keepScreenOn = true
                    pv.setShutterBackgroundColor(android.graphics.Color.BLACK)
                    pv.setKeepContentOnPlayerReset(true)
                    pv.player = exoPlayer
                    pv.resizeMode = aspectMode

                    view
                },
                update = { view ->
                    val pv = view.findViewById<androidx.media3.ui.PlayerView>(R.id.playerView)
                    pv.player = exoPlayer
                    pv.resizeMode = aspectMode
                }
            )

            // Buffering overlay
            if (isBuffering) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.7f))) {
                        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                            Spacer(Modifier.width(12.dp))
                            Text("Loading…", color = Color.White)
                        }
                    }
                }
            }

            // Error overlay
            if (errorMessage != null) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Card(colors = CardDefaults.cardColors(containerColor = Color.Red.copy(alpha = 0.8f))) {
                        Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(Icons.Default.Error, contentDescription = "Error", tint = Color.White, modifier = Modifier.size(32.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("Playback Error", color = Color.White, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(4.dp))
                            Text(errorMessage!!, color = Color.White, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                            Spacer(Modifier.height(12.dp))
                            Button(
                                onClick = {
                                    errorMessage = null
                                    // Retry playback
                                    if (url.isNotEmpty()) {
                                        try {
                                            val mediaItem = MediaItem.fromUri(Uri.parse(url))
                                            exoPlayer.setMediaItem(mediaItem)
                                            exoPlayer.prepare()
                                            exoPlayer.playWhenReady = true
                                        } catch (e: Exception) {
                                            errorMessage = "Retry failed: ${e.message}"
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color.Red)
                            ) {
                                Text("Retry")
                            }
                        }
                    }
                }
            }

            // Controls
            AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
                ControlsOverlay(
                    isPlaying = isPlaying,
                    currentPosition = currentPosition,
                    duration = duration,
                    onPlayPause = { togglePlay(exoPlayer); lastUserAction = now() },
                    onSeek = { exoPlayer.seekTo(it); lastUserAction = now() },
                    onSkipBackward = { seekBy(exoPlayer, -5_000, duration); lastUserAction = now() },
                    onSkipForward = { seekBy(exoPlayer, +5_000, duration); lastUserAction = now() },
                    onSkip30Backward = { seekBy(exoPlayer, -30_000, duration); lastUserAction = now() },
                    onSkip30Forward = { seekBy(exoPlayer, +30_000, duration); lastUserAction = now() },
                    onPreviousEpisode = {
                        tryPlayPrevEpisode(exoPlayer, episodes, currentEpisodeIndex) { idx ->
                            currentEpisodeIndex = idx; currentEpisode = episodes.getOrNull(idx)
                            scope.launch { playlistListState.animateScrollToItem(idx) }
                        }
                        lastUserAction = now()
                    },
                    onNextEpisode = {
                        tryPlayNextEpisode(exoPlayer, episodes, currentEpisodeIndex) { idx ->
                            currentEpisodeIndex = idx; currentEpisode = episodes.getOrNull(idx)
                            scope.launch { playlistListState.animateScrollToItem(idx) }
                        }
                        lastUserAction = now()
                    },
                    hasPreviousEpisode = currentEpisodeIndex > 0,
                    hasNextEpisode = currentEpisodeIndex < episodes.lastIndex,
                    onToggleControls = { showControls = !showControls; lastUserAction = now() }
                )
            }

            // Playlist
            if (showPlaylist) {
                PlaylistOverlay(
                    episodes = episodes,
                    currentIndex = currentEpisodeIndex,
                    listState = playlistListState,
                    onEpisodeSelect = { ep, idx ->
                        playEpisode(exoPlayer, ep) {
                            currentEpisodeIndex = idx
                            currentEpisode = ep
                        }
                        showPlaylist = false
                        lastUserAction = now()
                    },
                    onClose = { showPlaylist = false; lastUserAction = now() }
                )
            }

            // Speed menu
            if (showSpeedMenu) {
                SpeedMenuOverlay(
                    currentSpeed = playbackSpeed,
                    onSpeedSelect = { s ->
                        val sp = clampSpeed(s)
                        exoPlayer.setSpeed(sp)
                        playbackSpeed = sp
                        // Don't close menu on speed change - only close on explicit close action
                        lastUserAction = now()
                    },
                    onClose = { showSpeedMenu = false; lastUserAction = now() }
                )
            }

            // Settings
            if (showSettings) {
                SettingsOverlay(
                    tracks = exoPlayer.currentTracks,
                    onAudioLangSelect = { langCode ->
                        val builder = trackSelector.buildUponParameters().setPreferredAudioLanguage(langCode)
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
            


            // Error banner
            errorMessage?.let { msg ->
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .align(Alignment.TopCenter)
                ) {
                    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)) {
                        Row(
                            Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Error, contentDescription = null, tint = MaterialTheme.colorScheme.onErrorContainer)
                            Spacer(Modifier.width(8.dp))
                            Text(msg, color = MaterialTheme.colorScheme.onErrorContainer, modifier = Modifier.weight(1f))
                            TextButton(onClick = { errorMessage = null }) { Text("Dismiss") }
                        }
                    }
                }
            }
        }
    }

    // initial focus for DPAD
    LaunchedEffect(Unit) { focusRequester.requestFocus() }
}

// ---------------------------- Composables ----------------------------
@Composable
private fun ControlsOverlay(
    isPlaying: Boolean,
    currentPosition: Long,
    duration: Long,
    onPlayPause: () -> Unit,
    onSeek: (Long) -> Unit,
    onSkipBackward: () -> Unit,
    onSkipForward: () -> Unit,
    onSkip30Backward: () -> Unit,
    onSkip30Forward: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onNextEpisode: () -> Unit,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    onToggleControls: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onToggleControls() }
    ) {
        // Center Play/Pause
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .size(120.dp)
                .clickable { onPlayPause() },
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.8f)),
            shape = RoundedCornerShape(60.dp)
        ) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                    contentDescription = if (isPlaying) "Pause" else "Play",
                    tint = Color.White,
                    modifier = Modifier.size(48.dp)
                )
            }
        }

        // Bottom controls
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.8f))
                .padding(24.dp)
        ) {
            // Progress
            Slider(
                value = currentPosition.coerceAtLeast(0L).toFloat(),
                onValueChange = { onSeek(it.toLong()) },
                valueRange = 0f..max(1L, duration).toFloat(),
                modifier = Modifier.fillMaxWidth(),
                colors = SliderDefaults.colors(
                    thumbColor = MaterialTheme.colorScheme.primary,
                    activeTrackColor = MaterialTheme.colorScheme.primary,
                    inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                )
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(formatTime(currentPosition), color = Color.White, fontWeight = FontWeight.Bold)
                Text(formatTime(duration), color = Color.White, fontWeight = FontWeight.Bold)
            }

            Spacer(Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPreviousEpisode) RoundButton(Icons.Default.SkipPrevious, "Prev", onPreviousEpisode)
                RoundButton(Icons.Default.Replay30, "Back 30s", onSkip30Backward)
                RoundButton(Icons.Default.Replay5, "Back 5s", onSkipBackward)

                Card(
                    modifier = Modifier
                        .size(72.dp)
                        .clickable { onPlayPause() },
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                    shape = RoundedCornerShape(36.dp)
                ) {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }

                RoundButton(Icons.Default.Forward5, "Fwd 5s", onSkipForward)
                RoundButton(Icons.Default.Forward30, "Fwd 30s", onSkip30Forward)
                if (hasNextEpisode) RoundButton(Icons.Default.SkipNext, "Next", onNextEpisode)
            }
        }
    }
}

@Composable
private fun RoundButton(icon: androidx.compose.ui.graphics.vector.ImageVector, desc: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .size(56.dp)
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.18f)),
        shape = RoundedCornerShape(28.dp)
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Icon(icon, contentDescription = desc, tint = Color.White)
        }
    }
}

@Composable
private fun PlaylistOverlay(
    episodes: List<EpisodeInfo>,
    currentIndex: Int,
    listState: LazyListState,
    onEpisodeSelect: (EpisodeInfo, Int) -> Unit,
    onClose: () -> Unit
) {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.TopEnd) {
        Card(
            modifier = Modifier
                .fillMaxHeight(0.7f)
                .widthIn(min = 350.dp, max = 400.dp)
                .padding(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.Black.copy(alpha = 0.95f)),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(Modifier.fillMaxHeight().padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Playlist", color = Color.White, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                    IconButton(onClick = onClose) { Icon(Icons.Default.Close, contentDescription = "Close", tint = Color.White) }
                }
                Spacer(Modifier.height(8.dp))
                LazyColumn(
                    state = listState,
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    itemsIndexed(episodes) { index, ep ->
                        val isCurrent = index == currentIndex
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onEpisodeSelect(ep, index) },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isCurrent) MaterialTheme.colorScheme.primary.copy(alpha = 0.35f)
                                else Color.White.copy(alpha = 0.08f)
                            ),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Row(
                                Modifier.fillMaxWidth().padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    if (isCurrent) Icons.Default.PlayArrow else Icons.Default.PlayCircle,
                                    contentDescription = null,
                                    tint = if (isCurrent) Color.White else Color.Gray,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(Modifier.width(8.dp))
                                Column(Modifier.weight(1f)) {
                                    Text(
                                        ep.title, 
                                        color = if (isCurrent) Color.White else Color.Gray, 
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                                        style = MaterialTheme.typography.bodyMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        "Season ${ep.season} • ${ep.extension.uppercase()}", 
                                        color = Color.Gray,
                                        style = MaterialTheme.typography.bodySmall
                                    )
                                }
                                if (isCurrent) Icon(Icons.Default.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                Text("D-pad ↑/↓ to navigate · Enter to play · Back to close", color = Color.White.copy(alpha = 0.7f), style = MaterialTheme.typography.bodySmall)
            }
        }
    }
}

// ---------------------------- Helpers ----------------------------
private fun togglePlay(player: ExoPlayer) {
    if (player.isPlaying) player.pause() else player.play()
}

private fun seekBy(player: ExoPlayer, deltaMs: Long, duration: Long) {
    val target = (player.currentPosition + deltaMs).coerceIn(0L, max(0L, duration))
    player.seekTo(target)
}

private fun tryPlayNextEpisode(player: ExoPlayer, episodes: List<EpisodeInfo>, currentEpisodeIndex: Int, onChanged: (Int) -> Unit) {
    if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.lastIndex) {
        val newIndex = currentEpisodeIndex + 1
        playEpisode(player, episodes[newIndex]) {
            onChanged(newIndex)
        }
    } else {
        onChanged(currentEpisodeIndex)
    }
}
private fun tryPlayPrevEpisode(player: ExoPlayer, episodes: List<EpisodeInfo>, currentEpisodeIndex: Int, onChanged: (Int) -> Unit) {
    if (episodes.isNotEmpty() && currentEpisodeIndex > 0) {
        val newIndex = currentEpisodeIndex - 1
        playEpisode(player, episodes[newIndex]) {
            onChanged(newIndex)
        }
    } else {
        onChanged(currentEpisodeIndex)
    }
}

private fun adjustSpeed(player: ExoPlayer, delta: Float, onApplied: (Float) -> Unit) {
    val current = player.playbackParameters.speed
    val newSpeed = clampSpeed(current + delta)
    player.setSpeed(newSpeed)
    onApplied(newSpeed)
}

private fun formatTime(ms: Long): String {
    val total = (ms / 1000).coerceAtLeast(0)
    val h = total / 3600
    val m = (total % 3600) / 60
    val s = total % 60
    return if (h > 0) "%d:%02d:%02d".format(h, m, s) else "%02d:%02d".format(m, s)
}

private fun playEpisode(exoPlayer: ExoPlayer, episode: EpisodeInfo, onComplete: () -> Unit) {
    val mediaItem = MediaItem.fromUri(Uri.parse(episode.streamUrl))
    exoPlayer.setMediaItem(mediaItem)
    exoPlayer.prepare()
    exoPlayer.playWhenReady = true
    // keep current speed after prepare
    val currentSpeed = exoPlayer.playbackParameters.speed
    exoPlayer.setSpeed(currentSpeed)
    onComplete()
}

// Tracks helpers - using functions from TrackUtils.kt

// Data class
data class EpisodeInfo(
    val id: String,
    val title: String,
    val season: String,
    val extension: String,
    val streamUrl: String
)

// Hilt entrypoint
@dagger.hilt.EntryPoint
@dagger.hilt.InstallIn(dagger.hilt.components.SingletonComponent::class)
interface PlayerScreenEntryPoint {
    fun hydraApiService(): HydraApiService
    fun playbackProgressRepository(): PlaybackProgressRepository
}

// ---------------------------- Utilities ----------------------------
private fun android.content.Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    else -> runCatching {
        var ctx = this
        while (ctx is android.content.ContextWrapper) {
            if (ctx is Activity) return ctx
            ctx = ctx.baseContext
        }
        null
    }.getOrNull()
}

private fun setImmersive(activity: Activity, immersive: Boolean) {
    val window = activity.window
    WindowCompat.setDecorFitsSystemWindows(window, !immersive)
    val controller = WindowInsetsControllerCompat(window, window.decorView)
    if (immersive) {
        controller.hide(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
        controller.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    } else {
        controller.show(android.view.WindowInsets.Type.statusBars() or android.view.WindowInsets.Type.navigationBars())
    }
}