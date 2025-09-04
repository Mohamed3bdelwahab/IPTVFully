package com.example.newiptv.player

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.DefaultLoadControl
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.okhttp.OkHttpDataSource
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/**
 * Modern IPTV Video Player with ExoPlayer and FFmpeg support
 * Supports various formats including MKV, MP4, AVI, etc.
 */
class IPTVVideoPlayer(
    private val context: Context,
    private val playerListener: PlayerListener? = null
) {
    
    companion object {
        private const val TAG = "IPTVVideoPlayer"
        private const val BUFFER_SIZE = 50 * 1024 * 1024 // 50MB buffer
        private const val CONNECT_TIMEOUT = 30L
        private const val READ_TIMEOUT = 30L
        
        // Enhanced buffer configuration
        private const val MIN_BUFFER_MS = 30_000 // 30 seconds minimum buffer
        private const val MAX_BUFFER_MS = 120_000 // 120 seconds maximum buffer (increased)
        private const val BUFFER_FOR_PLAYBACK_MS = 2_500 // 2.5 seconds for playback start
        private const val BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS = 5_000 // 5 seconds after rebuffer
    }
    
    private var exoPlayer: ExoPlayer? = null
    private var currentUri: Uri? = null
    private var isInitialized = false
    
    interface PlayerListener {
        fun onPlayerReady()
        fun onPlayerError(error: String)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressChanged(position: Long, duration: Long)
        fun onBufferingChanged(isBuffering: Boolean)
        fun onVideoEnded()
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
            
            // Create enhanced load control for better buffering
            val loadControl = DefaultLoadControl.Builder()
                .setBufferDurationsMs(
                    MIN_BUFFER_MS,
                    MAX_BUFFER_MS,
                    BUFFER_FOR_PLAYBACK_MS,
                    BUFFER_FOR_PLAYBACK_AFTER_REBUFFER_MS
                )
                .setTargetBufferBytes(BUFFER_SIZE)
                .setPrioritizeTimeOverSizeThresholds(true)
                .build()
            
            // Create ExoPlayer with enhanced configuration
            exoPlayer = ExoPlayer.Builder(context)
                .setMediaSourceFactory(mediaSourceFactory)
                .setLoadControl(loadControl)
                .build()
            
            Log.d(TAG, "ExoPlayer initialized with enhanced buffer: ${BUFFER_SIZE / (1024 * 1024)}MB, " +
                    "Min buffer: ${MIN_BUFFER_MS / 1000}s, Max buffer: ${MAX_BUFFER_MS / 1000}s")
            
            // Set up player listeners
            exoPlayer?.addListener(object : Player.Listener {
                override fun onPlaybackStateChanged(playbackState: Int) {
                    when (playbackState) {
                        Player.STATE_READY -> {
                            isInitialized = true
                            playerListener?.onPlayerReady()
                            playerListener?.onPlaybackStateChanged(exoPlayer?.isPlaying == true)
                        }
                        Player.STATE_BUFFERING -> {
                            playerListener?.onBufferingChanged(true)
                        }
                        Player.STATE_ENDED -> {
                            playerListener?.onPlaybackStateChanged(false)
                            playerListener?.onVideoEnded()
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
     * Play the video
     */
    fun play() {
        exoPlayer?.play()
    }
    
    /**
     * Pause the video
     */
    fun pause() {
        exoPlayer?.pause()
    }
    
    /**
     * Stop the video
     */
    fun stop() {
        exoPlayer?.stop()
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
     * Release player resources
     */
    fun release() {
        try {
            exoPlayer?.release()
            exoPlayer = null
            isInitialized = false
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