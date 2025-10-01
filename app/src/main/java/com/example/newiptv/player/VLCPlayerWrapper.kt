package com.example.newiptv.player

import android.content.Context
import android.net.Uri
import android.util.Log
import android.view.Surface
import org.videolan.libvlc.LibVLC
import org.videolan.libvlc.Media
import org.videolan.libvlc.MediaPlayer
import org.videolan.libvlc.interfaces.IVLCVout
import java.util.*

/**
 * VLC Player Wrapper for AC3/DTS audio support
 * This provides a fallback when ExoPlayer cannot handle certain audio codecs
 */
class VLCPlayerWrapper(private val context: Context) {
    
    companion object {
        private const val TAG = "VLCPlayerWrapper"
    }
    
    private var libVLC: LibVLC? = null
    private var mediaPlayer: MediaPlayer? = null
    private var currentSurface: Surface? = null
    private var isInitialized = false
    private var initializationAttempted = false
    
    interface VLCPlayerListener {
        fun onPlayerReady()
        fun onPlayerError(error: String)
        fun onPlaybackStateChanged(isPlaying: Boolean)
        fun onProgressChanged(position: Long, duration: Long)
        fun onBufferingChanged(isBuffering: Boolean)
    }
    
    private var playerListener: VLCPlayerListener? = null
    
    init {
        // Initialize VLC immediately to ensure it's ready when needed
        Log.d(TAG, "VLCPlayerWrapper created, initializing VLC...")
        initializeVLC()
    }
    
    private fun initializeVLC(): Boolean {
        if (initializationAttempted) {
            return isInitialized
        }
        
        initializationAttempted = true
        
        try {
            Log.d(TAG, "VLC: Starting initialization...")
            
            val options = ArrayList<String>()
            options.add("--aout=opensles") // Use OpenSL ES for better audio support
            options.add("--audio-resampler=soxr") // High-quality audio resampling
            options.add("--avcodec-skiploopfilter=4") // Skip loop filter for better performance
            options.add("--avcodec-skip-frame=0") // Don't skip frames
            options.add("--avcodec-skip-idct=0") // Don't skip IDCT
            options.add("--avcodec-fast") // Fast decoding
            options.add("--no-audio-display") // Don't display audio info
            options.add("--no-video-title-show") // Don't show video title
            options.add("--no-snapshot-preview") // Don't show snapshot preview
            options.add("--live-caching=300") // Live stream caching
            options.add("--network-caching=1000") // Network caching
            options.add("--clock-jitter=0") // Clock jitter
            options.add("--clock-synchro=0") // Clock synchronization
            
            Log.d(TAG, "VLC: Initializing LibVLC with ${options.size} options...")
            
            // Try to create LibVLC instance
            libVLC = try {
                LibVLC(context, options)
            } catch (e: Exception) {
                Log.e(TAG, "VLC: Exception during LibVLC creation", e)
                playerListener?.onPlayerError("Failed to initialize VLC library: ${e.message}")
                return false
            }
            
            if (libVLC == null) {
                Log.e(TAG, "VLC: Failed to create LibVLC instance - libVLC is null")
                playerListener?.onPlayerError("Failed to initialize VLC library - LibVLC instance is null")
                return false
            }
            
            Log.d(TAG, "VLC: LibVLC instance created successfully")
            
            // Try to create MediaPlayer instance
            mediaPlayer = try {
                MediaPlayer(libVLC)
            } catch (e: Exception) {
                Log.e(TAG, "VLC: Exception during MediaPlayer creation", e)
                playerListener?.onPlayerError("Failed to initialize VLC MediaPlayer: ${e.message}")
                return false
            }
            
            if (mediaPlayer == null) {
                Log.e(TAG, "VLC: Failed to create MediaPlayer instance - mediaPlayer is null")
                playerListener?.onPlayerError("Failed to initialize VLC MediaPlayer - MediaPlayer instance is null")
                return false
            }
            
            Log.d(TAG, "VLC: MediaPlayer instance created successfully")
            
            // Set up event listeners
            mediaPlayer?.setEventListener { event ->
                when (event.type) {
                    MediaPlayer.Event.Opening -> {
                        Log.d(TAG, "VLC: Opening media")
                        playerListener?.onBufferingChanged(true)
                    }
                    MediaPlayer.Event.Buffering -> {
                        val buffering = event.buffering
                        Log.d(TAG, "VLC: Buffering $buffering%")
                        playerListener?.onBufferingChanged(buffering < 100)
                    }
                    MediaPlayer.Event.Playing -> {
                        Log.d(TAG, "VLC: Playing")
                        isInitialized = true
                        playerListener?.onPlaybackStateChanged(true)
                        playerListener?.onBufferingChanged(false)
                        playerListener?.onPlayerReady()
                    }
                    MediaPlayer.Event.Paused -> {
                        Log.d(TAG, "VLC: Paused")
                        playerListener?.onPlaybackStateChanged(false)
                    }
                    MediaPlayer.Event.Stopped -> {
                        Log.d(TAG, "VLC: Stopped")
                        playerListener?.onPlaybackStateChanged(false)
                    }
                    MediaPlayer.Event.EncounteredError -> {
                        Log.e(TAG, "VLC: Error occurred")
                        playerListener?.onPlayerError("VLC Player error: ${event.type}")
                    }
                    MediaPlayer.Event.EndReached -> {
                        Log.d(TAG, "VLC: End reached")
                        playerListener?.onPlaybackStateChanged(false)
                    }
                }
            }
            
            Log.i(TAG, "✅ VLC Player initialized successfully")
            return true
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize VLC Player", e)
            playerListener?.onPlayerError("Failed to initialize VLC Player: ${e.message}")
            return false
        }
    }
    
    fun setPlayerListener(listener: VLCPlayerListener) {
        this.playerListener = listener
    }
    
    fun setSurface(surface: Surface) {
        currentSurface = surface
        try {
            if (!ensureVLCReady()) return
            
            val vlcVout = mediaPlayer?.vlcVout
            vlcVout?.setVideoSurface(surface, null)
            vlcVout?.attachViews()
            Log.d(TAG, "VLC: Surface attached")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set VLC surface", e)
        }
    }
    
    fun loadVideo(url: String) {
        try {
            Log.d(TAG, "VLC: Loading video: $url")
            
            // Ensure VLC is initialized before attempting to load video
            if (!ensureVLCReady()) {
                Log.e(TAG, "VLC not ready, cannot load video")
                playerListener?.onPlayerError("VLC player not ready")
                return
            }
            
            // Double-check that we have valid instances
            if (libVLC == null) {
                Log.e(TAG, "VLC: libVLC is null during loadVideo")
                playerListener?.onPlayerError("VLC library not initialized")
                return
            }
            
            if (mediaPlayer == null) {
                Log.e(TAG, "VLC: mediaPlayer is null during loadVideo")
                playerListener?.onPlayerError("VLC MediaPlayer not initialized")
                return
            }
            
            // Create Media object with proper error handling
            val media = try {
                Media(libVLC, Uri.parse(url))
            } catch (e: Exception) {
                Log.e(TAG, "VLC: Failed to create Media object", e)
                playerListener?.onPlayerError("Failed to create VLC Media object: ${e.message}")
                return
            }
            
            if (media == null) {
                Log.e(TAG, "VLC: Media object is null")
                playerListener?.onPlayerError("Failed to create VLC Media object")
                return
            }
            
            // Set media to player
            mediaPlayer?.media = media
            media.release()
            Log.d(TAG, "VLC: Video loaded successfully")
            
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load video in VLC", e)
            playerListener?.onPlayerError("Failed to load video: ${e.message}")
        }
    }
    
    private fun ensureVLCReady(): Boolean {
        if (!isInitialized && !initializationAttempted) {
            return initializeVLC()
        }
        return isInitialized
    }
    
    fun play() {
        try {
            if (!ensureVLCReady()) {
                Log.e(TAG, "VLC not ready for play")
                return
            }
            mediaPlayer?.play()
            Log.d(TAG, "VLC: Play command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play VLC", e)
        }
    }
    
    fun pause() {
        try {
            if (!ensureVLCReady()) {
                Log.e(TAG, "VLC not ready for pause")
                return
            }
            mediaPlayer?.pause()
            Log.d(TAG, "VLC: Pause command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to pause VLC", e)
        }
    }
    
    fun stop() {
        try {
            if (!ensureVLCReady()) {
                Log.e(TAG, "VLC not ready for stop")
                return
            }
            mediaPlayer?.stop()
            Log.d(TAG, "VLC: Stop command sent")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to stop VLC", e)
        }
    }
    
    fun seekTo(position: Long) {
        try {
            if (!ensureVLCReady()) {
                Log.e(TAG, "VLC not ready for seek")
                return
            }
            mediaPlayer?.time = position
            Log.d(TAG, "VLC: Seek to $position")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to seek VLC", e)
        }
    }
    
    fun getCurrentPosition(): Long {
        return try {
            if (!ensureVLCReady()) return 0L
            mediaPlayer?.time ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VLC position", e)
            0L
        }
    }
    
    fun getDuration(): Long {
        return try {
            if (!ensureVLCReady()) return 0L
            mediaPlayer?.length ?: 0L
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VLC duration", e)
            0L
        }
    }
    
    fun setVolume(volume: Float) {
        try {
            if (!ensureVLCReady()) return
            mediaPlayer?.volume = (volume * 100).toInt().coerceIn(0, 100)
            Log.d(TAG, "VLC: Volume set to ${(volume * 100).toInt()}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to set VLC volume", e)
        }
    }
    
    fun getVolume(): Float {
        return try {
            if (!ensureVLCReady()) return 1f
            (mediaPlayer?.volume ?: 100) / 100f
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VLC volume", e)
            1f
        }
    }
    
    fun isPlaying(): Boolean {
        return try {
            if (!ensureVLCReady()) return false
            mediaPlayer?.isPlaying ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Failed to get VLC playing state", e)
            false
        }
    }
    
    fun release() {
        try {
            currentSurface?.let { surface ->
                mediaPlayer?.vlcVout?.detachViews()
            }
            mediaPlayer?.stop()
            mediaPlayer?.release()
            libVLC?.release()
            mediaPlayer = null
            libVLC = null
            currentSurface = null
            isInitialized = false
            initializationAttempted = false
            Log.d(TAG, "VLC: Released")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to release VLC", e)
        }
    }
    
    fun isInitialized(): Boolean = isInitialized
    
    fun isVLCReady(): Boolean {
        val ready = ensureVLCReady() && libVLC != null && mediaPlayer != null
        Log.d(TAG, "VLC Ready Check: initialized=$isInitialized, libVLC=${libVLC != null}, mediaPlayer=${mediaPlayer != null}, ready=$ready")
        return ready
    }
}
