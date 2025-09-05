package com.example.newiptv.player

import android.content.Context
import android.media.AudioManager
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.trackselection.DefaultTrackSelector
import androidx.media3.exoplayer.trackselection.TrackSelector
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
        private const val BUFFER_SIZE = 100 * 1024 * 1024 // 100MB buffer (increased)
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        private const val MIN_BUFFER_MS = 30_000 // 30 seconds minimum buffer
        private const val MAX_BUFFER_MS = 120_000 // 120 seconds maximum buffer
        private const val BUFFER_FOR_PLAYBACK_MS = 10_000 // 10 seconds for playback
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000 // 5 seconds after rebuffer
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
            
            // Create ExoPlayer with enhanced configuration
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .setTrackSelector(trackSelector!!)
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
                    val errorMessage = "Playback error: ${error.message}"
                    Log.e(TAG, errorMessage, error)
                    playerListener?.onPlayerError(errorMessage)
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
            
            Log.d(TAG, "Loading video: $url")
            
            // Create media item
            val mediaItem = MediaItem.fromUri(uri)
            
            // Set media item to player
            exoPlayer?.setMediaItem(mediaItem)
            exoPlayer?.prepare()
            
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
            exoPlayer?.play()
            Log.d(TAG, "🔊 Audio focus granted, starting playback")
        } else {
            Log.w(TAG, "⚠️ Audio focus denied, cannot start playback")
        }
    }
    
    /**
     * Pause the video
     */
    fun pause() {
        exoPlayer?.pause()
        
        // Abandon audio focus when pausing
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.abandonAudioFocus(null)
        Log.d(TAG, "🔊 Audio focus abandoned on pause")
        
        // Save current position when pausing
        val currentPosition = exoPlayer?.currentPosition ?: 0L
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
                        parametersBuilder?.setOverrideForType(trackSelectionOverride)
                        trackSelector?.setParameters(parametersBuilder)
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
            
            // Abandon audio focus before releasing
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager.abandonAudioFocus(null)
            Log.d(TAG, "🔊 Audio focus abandoned on release")
            
            // Save final position before releasing
            positionManager?.saveCurrentPosition()
            
            exoPlayer?.release()
            exoPlayer = null
            trackSelector = null
            isInitialized = false
            currentContentId = null
            currentContentType = null
            currentDuration = 0L
            pendingResumePosition = 0L
            isPositionTrackingStarted = false
            
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
}