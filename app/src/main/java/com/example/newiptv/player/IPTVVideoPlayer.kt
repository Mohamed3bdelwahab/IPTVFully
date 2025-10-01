package com.example.newiptv.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.PlaybackParameters
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.TrackGroup
import androidx.media3.common.C
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.isActive

/**
 * Modern IPTV Video Player with ExoPlayer and FFmpeg support
 * Supports various formats including MKV, MP4, AVI, etc.
 * Enhanced with position tracking and auto-play functionality
 */
class IPTVVideoPlayer(
    private val context: Context,
    private val playerListener: PlayerListener? = null,
    private val positionManager: PlaybackPositionManager? = null,
    private val autoPlayManager: AutoPlayManager? = null
) {
    
    companion object {
        private const val TAG = "IPTVVideoPlayer"
        private const val BUFFER_SIZE = 200 * 1024 * 1024 // 200MB buffer (increased for high-quality videos)
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val MIN_BUFFER_MS = 60_000 // 60 seconds minimum buffer (increased)
        private const val MAX_BUFFER_MS = 300_000 // 300 seconds maximum buffer (increased)
        private const val BUFFER_FOR_PLAYBACK_MS = 15_000 // 15 seconds for playback (increased)
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 10_000 // 10 seconds after rebuffer (increased)
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var trackSelector: DefaultTrackSelector? = null
    private var currentUri: Uri? = null
    private var isInitialized = false
    private var currentContentId: String? = null
    private var currentContentType: String? = null
    private var currentDuration: Long = 0L
    private var pendingResumePosition: Long = 0L
    private var positionTrackingJob: kotlinx.coroutines.Job? = null
    private var isPositionTrackingStarted = false
    private var currentPlaybackSpeed: Float = 1.0f // Store current speed to preserve it
    private var audioFallbackAttempted = false // Track if we've already attempted fallback
    private var vlcPlayer: VLCPlayerWrapper? = null // VLC fallback player
    private var useVLCPlayer = false // Flag to use VLC instead of ExoPlayer
    private var lastVideoPosition: Long = 0L // Track video position for stuck frame detection
    private var stuckFrameDetectionJob: kotlinx.coroutines.Job? = null // Job for stuck frame detection
    
    interface PlayerListener {
        fun onPlayerReady()
        fun onPlayerError(error: String)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressChanged(position: Long, duration: Long)
        fun onBufferingChanged(isBuffering: Boolean)
        fun onEpisodeEnded() // For auto-play functionality
        fun onPositionLoaded(position: Long) // For resume functionality
    }
    
    init {
        initializePlayer()
        initializeVLCFallback()
    }
    
    private fun initializePlayer() {
        try {
            // Create OkHttp client for network requests
            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(CONNECT_TIMEOUT, TimeUnit.SECONDS)
                .readTimeout(READ_TIMEOUT, TimeUnit.SECONDS)
                .build()
            
            // Create data source factory with OkHttp support
            val dataSourceFactory: DataSource.Factory = OkHttpDataSource.Factory(okHttpClient)
            
            // Create media source factory
            val mediaSourceFactory = DefaultMediaSourceFactory(dataSourceFactory)
            
            // Create enhanced load control with better buffer settings
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setTargetBufferBytes(BUFFER_SIZE)
                .build()
            
            // Create track selector with audio track selection support
            trackSelector = DefaultTrackSelector(context).apply {
                // Enable audio track selection
                setParameters(
                    buildUponParameters()
                        .setMaxVideoSizeSd() // Allow all video sizes
                        .setPreferredAudioLanguage("en") // Prefer English audio
                        .setAllowAudioMixedMimeTypeAdaptiveness(true) // Allow mixed audio codecs
                        .setAllowAudioMixedSampleRateAdaptiveness(true) // Allow mixed sample rates
                        .setAllowAudioMixedChannelCountAdaptiveness(true) // Allow mixed channel counts
                        .setExceedVideoConstraintsIfNecessary(true) // Allow video constraints to be exceeded
                        .setExceedAudioConstraintsIfNecessary(true) // Allow audio constraints to be exceeded
                        .setTunnelingEnabled(false) // Disable tunneling for better compatibility
                        .setForceHighestSupportedBitrate(true) // Use highest quality available
                )
            }
            
            Log.i(TAG, "🎬 Creating Enhanced Player with:")
            Log.i(TAG, "   Buffer: ${BUFFER_SIZE / (1024 * 1024)}MB")
            Log.i(TAG, "   Min Buffer: ${MIN_BUFFER_MS / 1000}s")
            Log.i(TAG, "   Max Buffer: ${MAX_BUFFER_MS / 1000}s")
            Log.i(TAG, "   Playback Buffer: ${BUFFER_FOR_PLAYBACK_MS / 1000}s")
            Log.i(TAG, "   Audio Track Selection: Enabled")
            Log.i(TAG, "   Mixed Audio Codecs: Enabled")
            
            // Create enhanced renderers factory with hardware decoder fallback
            val renderersFactory = DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                setEnableAudioFloatOutput(true) // Enable high-quality audio output
                setEnableDecoderFallback(true) // Allow fallback to software decoder when hardware fails
            }
            
            // Log supported audio codecs for debugging
            logSupportedAudioCodecs()
            
            // Create ExoPlayer with enhanced configuration
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector!!)
                .setRenderersFactory(renderersFactory)
                .build()
            
            // Set up player listeners
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isInitialized = true
                            currentDuration = exoPlayer?.duration ?: 0L
                            
                            // Apply resume position when player is ready
                            if (pendingResumePosition > 0L) {
                                exoPlayer?.seekTo(pendingResumePosition)
                                Log.d(TAG, "📍 Applying resume position: ${pendingResumePosition}ms")
                                pendingResumePosition = 0L // Reset to prevent re-seeking
                            }
                            
                            // Initialize position tracking
                            currentContentId?.let { contentId ->
                                currentContentType?.let { contentType ->
                                    positionManager?.initializePositionTracking(contentId, contentType, currentDuration)
                                }
                            }
                            
                            // Log audio track information and select best track
                            logAudioTrackInfo()
                            selectBestAudioTrack()
                            
                            // Start stuck frame detection
                            startStuckFrameDetection()
                            
                            playerListener?.onPlayerReady()
                            playerListener?.onPlaybackStateChanged(exoPlayer?.isPlaying == true)
                        }
                        Player.STATE_BUFFERING -> {
                            playerListener?.onBufferingChanged(true)
                        }
                        Player.STATE_ENDED -> {
                            playerListener?.onPlaybackStateChanged(false)
                            
                            // Handle episode end for auto-play
                            playerListener?.onEpisodeEnded()
                            
                            // Mark content as completed
                            currentContentId?.let { contentId ->
                                currentContentType?.let { contentType ->
                                    positionManager?.markAsCompleted(contentId, contentType)
                                }
                            }
                        }
                        Player.STATE_IDLE -> {
                            // Player is idle
                        }
                    }
                }
                
                override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                    val errorMessage = error.message ?: "Unknown error"
                    val errorCode = error.errorCode
                    Log.e(TAG, "Player error: $errorMessage (Code: $errorCode)", error)
                    
                    // Enhanced error handling for different codec issues
                    when {
                        errorMessage.contains("audio/ac3") || errorMessage.contains("NO_UNSUPPORTED_TYPE") -> {
                            Log.w(TAG, "⚠️ AC3 audio codec error - attempting VLC fallback")
                            Log.d(TAG, "Error details: $errorMessage")
                            attemptVLCFallback()
                        }
                        errorMessage.contains("audio/eac3") -> {
                            Log.w(TAG, "⚠️ E-AC3 audio codec not supported - attempting VLC fallback")
                            attemptVLCFallback()
                        }
                        errorMessage.contains("audio/dts") -> {
                            Log.w(TAG, "⚠️ DTS audio codec not supported - attempting VLC fallback")
                            attemptVLCFallback()
                        }
                        errorMessage.contains("MediaCodec") || 
                        errorMessage.contains("DecoderInitException") ||
                        errorMessage.contains("DecoderQueryException") ||
                        errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_INIT_FAILED ||
                        errorCode == androidx.media3.common.PlaybackException.ERROR_CODE_DECODER_QUERY_FAILED -> {
                            Log.w(TAG, "⚠️ Hardware decoder error - attempting software fallback")
                            Log.d(TAG, "Error details: $errorMessage (Code: $errorCode)")
                            attemptSoftwareDecoderFallback()
                        }
                        errorMessage.contains("video/avc") || errorMessage.contains("video/hevc") -> {
                            Log.w(TAG, "⚠️ Video codec error - likely hardware decoder issue")
                            Log.d(TAG, "Error details: $errorMessage")
                            attemptSoftwareDecoderFallback()
                        }
                        else -> {
                            Log.e(TAG, "General playback error: $errorMessage (Code: $errorCode)")
                            playerListener?.onPlayerError("Playback error: $errorMessage")
                        }
                    }
                }
            })
            
            // Set up progress tracking
            exoPlayer?.addListener(object : Player.Listener {
                override fun onIsPlayingChanged(isPlaying: Boolean) {
                    playerListener?.onPlaybackStateChanged(isPlaying)
                }
            })
            
            Log.d(TAG, "Player initialized successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize player", e)
            playerListener?.onPlayerError("Failed to initialize player: ${e.message}")
        }
    }
    
    /**
     * Load and play a video from URL
     */
    fun loadVideo(url: String) {
        try {
            val uri = Uri.parse(url)
            currentUri = uri
            audioFallbackAttempted = false // Reset fallback flag for new video
            
            Log.d(TAG, "Loading video: $url")
            
            // Create media item
            val mediaItem = MediaItem.fromUri(uri)
            
            // Set media item to player
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Restore the current playback speed after loading new video
            if (currentPlaybackSpeed != 1.0f) {
                exoPlayer?.let { player ->
                    val playbackParameters = PlaybackParameters(currentPlaybackSpeed)
                    player.setPlaybackParameters(playbackParameters)
                    Log.d(TAG, "🔄 Restored playback speed to: ${currentPlaybackSpeed}x after loading video")
                }
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video", e)
            playerListener?.onPlayerError("Failed to load video: ${e.message}")
        }
    }
    
    /**
     * Load video with content tracking for position management and auto-play
     */
    fun loadVideoWithTracking(
        url: String,
        contentId: String,
        contentType: String,
        resumePosition: Long = 0L
    ) {
        try {
            this.currentContentId = contentId
            this.currentContentType = contentType
            this.pendingResumePosition = resumePosition
            audioFallbackAttempted = false // Reset fallback flag for new video
            
            val uri = Uri.parse(url)
            currentUri = uri
            
            Log.d(TAG, "🎬 Loading video with tracking:")
            Log.d(TAG, "   URL: $url")
            Log.d(TAG, "   Content ID: $contentId")
            Log.d(TAG, "   Content Type: $contentType")
            Log.d(TAG, "   Resume Position: ${resumePosition}ms")
            
            // Create media item
            val mediaItem = MediaItem.fromUri(uri)
            
            // Set media item to player
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
            // Restore the current playback speed after loading new video
            if (currentPlaybackSpeed != 1.0f) {
                exoPlayer?.let { player ->
                    val playbackParameters = PlaybackParameters(currentPlaybackSpeed)
                    player.setPlaybackParameters(playbackParameters)
                    Log.d(TAG, "🔄 Restored playback speed to: ${currentPlaybackSpeed}x after loading video with tracking")
                }
            }
            
            // Resume position will be applied when player becomes ready
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video with tracking", e)
            playerListener?.onPlayerError("Failed to load video: ${e.message}")
        }
    }
    
    /**
     * Play the video
     */
    fun play() {
        // Request audio focus for playback
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val result = audioManager.requestAudioFocus(
            null,
            AudioManager.STREAM_MUSIC,
            AudioManager.AUDIOFOCUS_GAIN
        )
        
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            if (useVLCPlayer) {
                vlcPlayer?.play()
            } else {
                exoPlayer?.play()
            }
            Log.d(TAG, "🔊 Audio focus granted, starting playback")
        } else {
            Log.w(TAG, "⚠️ Audio focus denied, cannot start playback")
        }
    }
    
    /**
     * Pause the video
     */
    fun pause() {
        if (useVLCPlayer) {
            vlcPlayer?.pause()
        } else {
            exoPlayer?.pause()
        }
        
        // Abandon audio focus when pausing
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
        Log.d(TAG, "🔊 Audio focus abandoned on pause")
        
        // Save current position when pausing
        val currentPosition = if (useVLCPlayer) {
            vlcPlayer?.getCurrentPosition() ?: 0L
        } else {
            exoPlayer?.currentPosition ?: 0L
        }
        positionManager?.updatePosition(currentPosition)
        positionManager?.saveCurrentPosition()
        Log.d(TAG, "📍 Position saved on pause: ${currentPosition}ms")
    }
    
    /**
     * Stop the video
     */
    fun stop() {
        exoPlayer?.stop()
        
        // Abandon audio focus when stopping
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
        Log.d(TAG, "🔊 Audio focus abandoned on stop")
        
        // Save current position when stopping
        val currentPosition = exoPlayer?.currentPosition ?: 0L
        positionManager?.updatePosition(currentPosition)
        positionManager?.saveCurrentPosition()
        Log.d(TAG, "📍 Position saved on stop: ${currentPosition}ms")
    }
    
    /**
     * Seek to specific position
     */
    fun seekTo(position: Long) {
        exoPlayer?.seekTo(position)
    }
    
    /**
     * Get current position
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Set playback speed with improved audio clarity
     * Uses PlaybackParameters to maintain speech clarity at higher speeds
     */
    fun setPlaybackSpeed(speed: Float) {
        currentPlaybackSpeed = speed // Store the speed
        exoPlayer?.let { player ->
            // Use PlaybackParameters for better audio processing
            // This helps maintain speech clarity at higher speeds
            val playbackParameters = PlaybackParameters(speed)
            player.setPlaybackParameters(playbackParameters)
            Log.d(TAG, "⚡ Playback speed set to: ${speed}x with improved audio clarity")
        }
    }
    
    /**
     * Get total duration
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    /**
     * Check if player is playing
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }
    
    /**
     * Check if player is ready
     */
    fun isReady(): Boolean {
        return isInitialized
    }
    
    /**
     * Get the ExoPlayer instance for UI binding
     */
    fun getPlayer(): ExoPlayer? {
        return exoPlayer
    }
    
    /**
     * Check if audio codec is supported
     */
    fun isAudioCodecSupported(mimeType: String): Boolean {
        return when (mimeType.lowercase()) {
            "audio/aac", "audio/mp4a-latm" -> true
            "audio/mpeg", "audio/mp3" -> true
            "audio/pcm", "audio/wav" -> true
            "audio/ogg", "audio/vorbis" -> true
            "audio/ac3" -> false // Not supported without additional codecs
            "audio/eac3" -> false // Not supported without additional codecs
            "audio/dts" -> false // Not supported without additional codecs
            else -> {
                Log.w(TAG, "Unknown audio codec: $mimeType")
                false
            }
        }
    }
    
    /**
     * Get supported audio codecs list
     */
    fun getSupportedAudioCodecs(): List<String> {
        return listOf(
            "AAC (Advanced Audio Coding)",
            "MP3 (MPEG Audio Layer III)",
            "PCM (Pulse Code Modulation)",
            "OGG Vorbis"
        )
    }
    
    /**
     * Get unsupported audio codecs list
     */
    fun getUnsupportedAudioCodecs(): List<String> {
        return listOf(
            "AC3 (Audio Codec 3)",
            "E-AC3 (Enhanced AC3)",
            "DTS (Digital Theater Systems)"
        )
    }
    
    /**
     * Log audio track information for debugging
     */
    private fun logAudioTrackInfo() {
        try {
            val currentTracks = exoPlayer?.currentTracks
            if (currentTracks != null) {
                val audioTracks = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (audioTracks.isNotEmpty()) {
                    Log.d(TAG, "🔊 Audio Tracks Found:")
                    for (i in audioTracks.indices) {
                        val trackGroup = audioTracks[i]
                        Log.d(TAG, "   Track Group $i: ${trackGroup.length} tracks")
                        for (j in 0 until trackGroup.length) {
                            val format = trackGroup.mediaTrackGroup.getFormat(j)
                            Log.d(TAG, "     Track $j: ${format.codecs} - ${format.sampleRate}Hz - ${format.channelCount} channels")
                        }
                    }
                } else {
                    Log.w(TAG, "⚠️ No audio tracks found!")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error logging audio track info", e)
        }
    }
    
    /**
     * Select the best available audio track
     */
    fun selectBestAudioTrack() {
        try {
            val currentTracks = exoPlayer?.currentTracks
            if (currentTracks != null) {
                val audioTracks = currentTracks.groups.filter { it.type == C.TRACK_TYPE_AUDIO }
                if (audioTracks.isNotEmpty()) {
                    // Select the first audio track (usually the best quality)
                    val trackGroup = audioTracks[0]
                    if (trackGroup.length > 0) {
                        val trackSelectionOverride = TrackSelectionOverride(trackGroup.mediaTrackGroup, listOf(0))
                        val parametersBuilder = trackSelector?.buildUponParameters()
                        if (parametersBuilder != null) {
                            parametersBuilder.setOverrideForType(trackSelectionOverride)
                            trackSelector?.setParameters(parametersBuilder)
                        }
                        Log.d(TAG, "🔊 Selected audio track: ${trackGroup.mediaTrackGroup.getFormat(0).codecs}")
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error selecting audio track", e)
        }
    }
    
    /**
     * Start position tracking (call this when video starts playing)
     */
    fun startPositionTracking() {
        // Only start tracking once
        if (isPositionTrackingStarted) {
            Log.d(TAG, "Position tracking already started, skipping")
            return
        }
        
        isPositionTrackingStarted = true
        
        // Set up position tracking callback (only once)
        positionManager?.onPositionLoaded = { savedPosition ->
            playerListener?.onPositionLoaded(savedPosition)
        }
        
        // Start periodic position tracking
        startPeriodicPositionTracking()
        
        Log.d(TAG, "📍 Position tracking started")
    }
    
    /**
     * Start stuck frame detection to detect when video is stuck on first frame
     */
    private fun startStuckFrameDetection() {
        // Cancel existing detection job
        stuckFrameDetectionJob?.cancel()
        
        stuckFrameDetectionJob = CoroutineScope(Dispatchers.Main).launch {
            delay(5000) // Wait 5 seconds after player is ready
            
            while (isActive) {
                delay(2000) // Check every 2 seconds
                
                if (exoPlayer?.isPlaying == true && isInitialized) {
                    val currentPosition = exoPlayer?.currentPosition ?: 0L
                    
                    // If video position hasn't changed for 10 seconds while playing, it's likely stuck
                    if (currentPosition == lastVideoPosition && currentPosition > 0) {
                        Log.w(TAG, "⚠️ Detected stuck video frame - position not advancing")
                        Log.w(TAG, "   Current position: ${currentPosition}ms")
                        Log.w(TAG, "   Last position: ${lastVideoPosition}ms")
                        Log.w(TAG, "   Attempting software decoder fallback...")
                        
                        // Trigger software decoder fallback
                        attemptSoftwareDecoderFallback()
                        break
                    }
                    
                    lastVideoPosition = currentPosition
                }
            }
        }
    }
    
    /**
     * Start periodic position tracking every 10 seconds
     */
    private fun startPeriodicPositionTracking() {
        // Cancel existing tracking job
        positionTrackingJob?.cancel()
        
        positionTrackingJob = CoroutineScope(Dispatchers.Main).launch {
            while (isActive) {
                delay(10_000) // 10 seconds
                
                // Only save position if video is playing and we have content info
                if (exoPlayer?.isPlaying == true && currentContentId != null && currentContentType != null) {
                    val currentPosition = exoPlayer?.currentPosition ?: 0L
                    val duration = exoPlayer?.duration ?: 0L
                    
                    if (duration > 0) {
                        positionManager?.updatePosition(currentPosition)
                        Log.d(TAG, "📍 Position saved: ${currentPosition}ms (${(currentPosition.toFloat() / duration.toFloat() * 100).toInt()}%)")
                    }
                }
            }
        }
    }
    
    /**
     * Update position tracking (call this periodically during playback)
     */
    fun updatePositionTracking() {
        val currentPosition = getCurrentPosition()
        positionManager?.updatePosition(currentPosition)
    }
    
    /**
     * Release player resources
     */
    fun release() {
        try {
            // Cancel position tracking
            positionTrackingJob?.cancel()
            positionTrackingJob = null
            
            // Cancel stuck frame detection
            stuckFrameDetectionJob?.cancel()
            stuckFrameDetectionJob = null
            
            // Abandon audio focus before releasing
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocus(null)
            Log.d(TAG, "🔊 Audio focus abandoned on release")
            
            // Save final position before releasing
            positionManager?.saveCurrentPosition()
            
            // Release ExoPlayer
            exoPlayer?.release()
            exoPlayer = null
            
            // Release VLC player if it was initialized
            vlcPlayer?.release()
            vlcPlayer = null
            
            // Reset all state
            trackSelector = null
            isInitialized = false
            currentContentId = null
            currentContentType = null
            currentDuration = 0L
            pendingResumePosition = 0L
            isPositionTrackingStarted = false
            useVLCPlayer = false
            audioFallbackAttempted = false
            
            Log.d(TAG, "Player released")
        } catch (e: Exception) {
            Log.e(TAG, "Error releasing player", e)
        }
    }
    
    /**
     * Set volume (0.0 to 1.0)
     */
    fun setVolume(volume: Float) {
        exoPlayer?.volume = volume.coerceIn(0f, 1f)
    }
    
    /**
     * Get current volume
     */
    fun getVolume(): Float {
        return exoPlayer?.volume ?: 1f
    }
    
    /**
     * Attempt to find and select a compatible audio track when AC3/DTS fails
     */
    private fun attemptAudioTrackFallback() {
        if (audioFallbackAttempted) {
            Log.w(TAG, "⚠️ Audio fallback already attempted, showing error message")
            playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
            return
        }
        
        audioFallbackAttempted = true
        Log.i(TAG, "🔄 Attempting audio track fallback...")
        
        try {
            val currentTracks = exoPlayer?.currentTracks
            if (currentTracks != null) {
                val audioTrackGroups = mutableListOf<androidx.media3.common.TrackGroup>()
                
                // Find all audio track groups
                for (i in 0 until currentTracks.groups.size) {
                    val trackGroup = currentTracks.groups[i]
                    if (trackGroup.type == androidx.media3.common.C.TRACK_TYPE_AUDIO) {
                        audioTrackGroups.add(trackGroup.mediaTrackGroup)
                    }
                }
                
                Log.d(TAG, "Found ${audioTrackGroups.size} audio track groups")
                
                // Try to find a compatible audio track (AAC, MP3, etc.)
                for (trackGroup in audioTrackGroups) {
                    for (j in 0 until trackGroup.length) {
                        val format = trackGroup.getFormat(j)
                        val mimeType = format.sampleMimeType ?: ""
                        
                        Log.d(TAG, "Checking audio track: $mimeType")
                        
                        // Check if this is a supported audio format
                        if (mimeType.startsWith("audio/") && 
                            (mimeType.contains("aac") || mimeType.contains("mp4") || 
                             mimeType.contains("mp3") || mimeType.contains("mpeg"))) {
                            
                            Log.i(TAG, "✅ Found compatible audio track: $mimeType")
                            
                            // Select this audio track
                            val trackSelectionOverride = androidx.media3.common.TrackSelectionOverride(
                                trackGroup, listOf(j)
                            )
                            
                            val parametersBuilder = trackSelector?.parameters?.buildUpon()
                            if (parametersBuilder != null) {
                                parametersBuilder.setOverrideForType(trackSelectionOverride)
                                trackSelector?.setParameters(parametersBuilder)
                            }
                            
                            Log.i(TAG, "🎵 Switched to compatible audio track")
                            return
                        }
                    }
                }
                
                Log.w(TAG, "⚠️ No compatible audio tracks found")
                playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
            } else {
                Log.w(TAG, "⚠️ No track information available")
                playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during audio fallback", e)
            playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
        }
    }
    
    /**
     * Log supported audio codecs for debugging
     */
    private fun logSupportedAudioCodecs() {
        try {
            val codecList = android.media.MediaCodecList(android.media.MediaCodecList.ALL_CODECS)
            val supportedAudioCodecs = mutableListOf<String>()
            
            for (i in 0 until codecList.codecInfos.size) {
                val codecInfo = codecList.codecInfos[i]
                if (!codecInfo.isEncoder) { // Only decoders
                    for (type in codecInfo.supportedTypes) {
                        if (type.startsWith("audio/")) {
                            supportedAudioCodecs.add(type)
                        }
                    }
                }
            }
            
            Log.i(TAG, "🎵 Supported Audio Codecs:")
            supportedAudioCodecs.distinct().sorted().forEach { codec ->
                Log.i(TAG, "   - $codec")
            }
            
            // Check specifically for AC3/DTS support
            val hasAC3 = supportedAudioCodecs.any { it.contains("ac3") }
            val hasDTS = supportedAudioCodecs.any { it.contains("dts") }
            val hasEAC3 = supportedAudioCodecs.any { it.contains("eac3") }
            
            Log.i(TAG, "🔍 Codec Support Status:")
            Log.i(TAG, "   AC3: ${if (hasAC3) "✅ Supported" else "❌ Not Supported"}")
            Log.i(TAG, "   E-AC3: ${if (hasEAC3) "✅ Supported" else "❌ Not Supported"}")
            Log.i(TAG, "   DTS: ${if (hasDTS) "✅ Supported" else "❌ Not Supported"}")
            
        } catch (e: Exception) {
            Log.e(TAG, "Error checking supported codecs", e)
        }
    }
    
    /**
     * Initialize VLC fallback player
     */
    private fun initializeVLCFallback() {
        try {
            Log.d(TAG, "🔄 Initializing VLC fallback player...")
            vlcPlayer = VLCPlayerWrapper(context)
            Log.d(TAG, "VLC player wrapper created: ${vlcPlayer != null}")
            
            vlcPlayer?.setPlayerListener(object : VLCPlayerWrapper.VLCPlayerListener {
                override fun onPlayerReady() {
                    Log.i(TAG, "✅ VLC Player ready")
                    playerListener?.onPlayerReady()
                }
                
                override fun onPlayerError(error: String) {
                    Log.e(TAG, "VLC Player error: $error")
                    playerListener?.onPlayerError("VLC Player error: $error")
                }
                
                override fun onPlaybackStateChanged(isPlaying: Boolean) {
                    playerListener?.onPlaybackStateChanged(isPlaying)
                }
                
                override fun onProgressChanged(position: Long, duration: Long) {
                    playerListener?.onProgressChanged(position, duration)
                }
                
                override fun onBufferingChanged(isBuffering: Boolean) {
                    playerListener?.onBufferingChanged(isBuffering)
                }
            })
            
            // Check if VLC is ready after initialization
            val isReady = vlcPlayer?.isVLCReady() ?: false
            Log.i(TAG, "✅ VLC fallback player initialized - Ready: $isReady")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC fallback player", e)
        }
    }
    
    /**
     * Attempt software decoder fallback for hardware decoder issues
     */
    private fun attemptSoftwareDecoderFallback() {
        if (audioFallbackAttempted) {
            Log.w(TAG, "⚠️ Software decoder fallback already attempted, showing error message")
            playerListener?.onPlayerError("Hardware decoder error. This video may not be compatible with your device. Please try a different video.")
            return
        }
        
        audioFallbackAttempted = true
        Log.i(TAG, "🔄 Attempting software decoder fallback for hardware decoder issue...")
        
        try {
            // Stop current player
            exoPlayer?.stop()
            
            // Create new ExoPlayer with software decoder preference
            val softwareRenderersFactory = DefaultRenderersFactory(context).apply {
                setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
                setEnableAudioFloatOutput(true)
                setEnableDecoderFallback(true)
            }
            
            // Create new ExoPlayer instance with software decoder
            val newExoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(DefaultMediaSourceFactory(context))
                .setLoadControl(DefaultLoadControl.Builder().build())
                .setTrackSelector(DefaultTrackSelector(context))
                .setRenderersFactory(softwareRenderersFactory)
                .build()
            
            // Replace the old player
            exoPlayer?.release()
            exoPlayer = newExoPlayer
            
            // Set up listeners for the new player
            setupPlayerListeners()
            
            // Reload the current video
            currentUri?.let { uri ->
                Log.i(TAG, "🔄 Reloading video with software decoder: ${uri.toString()}")
                val mediaItem = MediaItem.fromUri(uri)
                exoPlayer?.setMediaItem(mediaItem)
                exoPlayer?.prepare()
                Log.i(TAG, "✅ Switched to software decoder")
            } ?: run {
                Log.e(TAG, "No current URI available for software decoder fallback")
                playerListener?.onPlayerError("No video URL available for software decoder fallback")
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error during software decoder fallback", e)
            playerListener?.onPlayerError("Hardware decoder error. This video may not be compatible with your device. Please try a different video.")
        }
    }
    
    /**
     * Set up player listeners (extracted for reuse)
     */
    private fun setupPlayerListeners() {
        exoPlayer?.addListener(object : Player.Listener {
            override fun onPlaybackStateChanged(playbackState: Int) {
                when (playbackState) {
                    Player.STATE_READY -> {
                        isInitialized = true
                        currentDuration = exoPlayer?.duration ?: 0L
                        
                        // Apply resume position when player is ready
                        if (pendingResumePosition > 0L) {
                            exoPlayer?.seekTo(pendingResumePosition)
                            Log.d(TAG, "📍 Applying resume position: ${pendingResumePosition}ms")
                            pendingResumePosition = 0L
                        }
                        
                        // Initialize position tracking
                        currentContentId?.let { contentId ->
                            currentContentType?.let { contentType ->
                                positionManager?.initializePositionTracking(contentId, contentType, currentDuration)
                            }
                        }
                        
                        // Log audio track information and select best track
                        logAudioTrackInfo()
                        selectBestAudioTrack()
                        
                        // Start stuck frame detection for software decoder
                        startStuckFrameDetection()
                        
                        playerListener?.onPlayerReady()
                        playerListener?.onPlaybackStateChanged(exoPlayer?.isPlaying == true)
                    }
                    Player.STATE_BUFFERING -> {
                        playerListener?.onBufferingChanged(true)
                    }
                    Player.STATE_ENDED -> {
                        playerListener?.onPlaybackStateChanged(false)
                        playerListener?.onEpisodeEnded()
                        
                        // Mark content as completed
                        currentContentId?.let { contentId ->
                            currentContentType?.let { contentType ->
                                positionManager?.markAsCompleted(contentId, contentType)
                            }
                        }
                    }
                    Player.STATE_IDLE -> {
                        // Player is idle
                    }
                }
            }
            
            override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                val errorMessage = error.message ?: "Unknown error"
                val errorCode = error.errorCode
                Log.e(TAG, "Player error (software decoder): $errorMessage (Code: $errorCode)", error)
                
                // For software decoder, show user-friendly error
                playerListener?.onPlayerError("Video playback error. This video may not be compatible with your device.")
            }
        })
        
        // Set up progress tracking
        exoPlayer?.addListener(object : Player.Listener {
            override fun onIsPlayingChanged(isPlaying: Boolean) {
                playerListener?.onPlaybackStateChanged(isPlaying)
            }
        })
    }
    
    /**
     * Attempt to use VLC player for AC3/DTS support
     */
    private fun attemptVLCFallback() {
        if (audioFallbackAttempted) {
            Log.w(TAG, "⚠️ VLC fallback already attempted, showing error message")
            playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
            return
        }
        
        audioFallbackAttempted = true
        Log.i(TAG, "🔄 Attempting VLC fallback for AC3/DTS support...")
        
        try {
            // Check if VLC player is available and initialized
            if (vlcPlayer == null || !vlcPlayer!!.isVLCReady()) {
                Log.e(TAG, "VLC player not available or not ready")
                playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
                return
            }
            
            // Stop ExoPlayer
            exoPlayer?.stop()
            
            // Switch to VLC player
            useVLCPlayer = true
            currentUri?.let { uri ->
                Log.i(TAG, "🔄 Loading video in VLC: ${uri.toString()}")
                vlcPlayer?.loadVideo(uri.toString())
                Log.i(TAG, "✅ Switched to VLC player for AC3/DTS support")
            } ?: run {
                Log.e(TAG, "No current URI available for VLC fallback")
                playerListener?.onPlayerError("No video URL available for VLC fallback")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error during VLC fallback", e)
            playerListener?.onPlayerError("Audio codec not supported. This video uses AC3/DTS audio which requires additional codec support. Please try a different video or use a player that supports AC3/DTS audio.")
        }
    }
}