package com.example.newiptv.player

import android.content.Context
import android.view.KeyEvent
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.common.PlaybackParameters
import android.util.Log

/**
 * TV Remote Handler for IPTV Video Players
 * Supports native ExoPlayer
 * Based on comprehensive TV remote documentation
 */
class TVRemoteHandler(
    private val context: Context,
    private val exoPlayer: ExoPlayer? = null,
    private val onPlayPause: (() -> Unit)? = null,
    private val onSeek: ((Long) -> Unit)? = null,
    private val onSpeedChange: ((Float) -> Unit)? = null,
    private val onNextEpisode: (() -> Unit)? = null,
    private val onPrevEpisode: (() -> Unit)? = null,
    private val onShowPlaylist: (() -> Unit)? = null,
    private val onShowSpeedMenu: (() -> Unit)? = null,
    private val onShowSettings: (() -> Unit)? = null,
    private val onBack: (() -> Unit)? = null,
    private val onClose: (() -> Unit)? = null
) {
    
    companion object {
        private const val TAG = "TVRemoteHandler"
        private const val SEEK_FORWARD_AMOUNT = 10_000L // 10 seconds
        private const val SEEK_BACKWARD_AMOUNT = 10_000L // 10 seconds
        private const val FAST_SEEK_FORWARD_AMOUNT = 30_000L // 30 seconds
        private const val FAST_SEEK_BACKWARD_AMOUNT = 30_000L // 30 seconds
        private const val SPEED_INCREMENT = 0.25f
        private const val MIN_SPEED = 0.25f
        private const val MAX_SPEED = 3.0f
    }
    
    // Speed presets mapping
    private val speedPresets = listOf(0.5f, 0.75f, 1.0f, 1.25f, 1.5f, 1.75f, 2.0f)
    
    // Speed overlay menu
    private var speedOverlayMenu: SpeedOverlayMenu? = null
    private var playlistOverlayMenu: PlaylistOverlayMenu? = null
    
    // Store current speed to preserve it across video loads
    private var currentPlaybackSpeed: Float = 1.0f
    
    /**
     * Handle key events from TV remote
     * @param keyEvent The key event to handle
     * @return true if the key event was handled, false otherwise
     */
    fun handleKeyEvent(keyEvent: KeyEvent): Boolean {
        if (keyEvent.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        Log.d(TAG, "Handling key event: ${keyEvent.keyCode}")
        
        // If speed menu is visible, handle it first
        if (speedOverlayMenu?.isMenuVisible() == true) {
            return speedOverlayMenu?.handleKeyEvent(keyEvent) ?: false
        }
        
        // If playlist menu is visible, handle it first
        if (playlistOverlayMenu?.isMenuVisible() == true) {
            return playlistOverlayMenu?.handleKeyEvent(keyEvent) ?: false
        }
        
        return when (keyEvent.keyCode) {
            // Play/Pause Controls
            KeyEvent.KEYCODE_DPAD_CENTER,
            KeyEvent.KEYCODE_ENTER,
            KeyEvent.KEYCODE_NUMPAD_ENTER,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_SPACE -> {
                handlePlayPause()
                true
            }
            
            // Seeking Controls
            KeyEvent.KEYCODE_DPAD_RIGHT,
            KeyEvent.KEYCODE_L -> {
                handleSeek(SEEK_FORWARD_AMOUNT)
                true
            }
            KeyEvent.KEYCODE_DPAD_LEFT,
            KeyEvent.KEYCODE_J -> {
                handleSeek(-SEEK_BACKWARD_AMOUNT)
                true
            }
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
            KeyEvent.KEYCODE_R -> {
                handleSeek(FAST_SEEK_FORWARD_AMOUNT)
                true
            }
            KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_U -> {
                handleSeek(-FAST_SEEK_BACKWARD_AMOUNT)
                true
            }
            
            // Episode Navigation
            KeyEvent.KEYCODE_MEDIA_NEXT -> {
                handleNextEpisode()
                true
            }
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
                handlePrevEpisode()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_UP -> {
                handleNextEpisode()
                true
            }
            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                handlePrevEpisode()
                true
            }
            
            // Speed Controls
            KeyEvent.KEYCODE_NUMPAD_ADD,
            KeyEvent.KEYCODE_PLUS,
            KeyEvent.KEYCODE_EQUALS -> {
                handleSpeedChange(SPEED_INCREMENT)
                true
            }
            KeyEvent.KEYCODE_NUMPAD_SUBTRACT,
            KeyEvent.KEYCODE_MINUS -> {
                handleSpeedChange(-SPEED_INCREMENT)
                true
            }
            
            // Speed Presets (Number Keys 0-6)
            in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_6 -> {
                handleSpeedPreset(keyEvent.keyCode - KeyEvent.KEYCODE_0)
                true
            }
            
            // Menu Controls
            KeyEvent.KEYCODE_MENU -> {
                handleShowPlaylist()
                true
            }
            KeyEvent.KEYCODE_INFO -> {
                handleShowSpeedMenu()
                true
            }
            KeyEvent.KEYCODE_SETTINGS -> {
                handleShowSettings()
                true
            }
            
            // Navigation & Exit
            KeyEvent.KEYCODE_BACK -> {
                handleBack()
                true
            }
            KeyEvent.KEYCODE_ESCAPE -> {
                handleClose()
                true
            }
            
            else -> false
        }
    }
    
    /**
     * Handle play/pause toggle
     */
    private fun handlePlayPause() {
        Log.d(TAG, "Handling play/pause")
        
        if (exoPlayer != null) {
            // Native ExoPlayer
            if (exoPlayer.isPlaying) {
                exoPlayer.pause()
            } else {
                exoPlayer.play()
            }
        } else {
            // Callback for other player types
            onPlayPause?.invoke()
        }
    }
    
    /**
     * Handle seeking
     * @param seekAmount Amount to seek in milliseconds (positive for forward, negative for backward)
     */
    private fun handleSeek(seekAmount: Long) {
        Log.d(TAG, "Handling seek: $seekAmount ms")
        
        if (exoPlayer != null) {
            // Native ExoPlayer
            val currentPosition = exoPlayer.currentPosition
            val duration = exoPlayer.duration
            
            // Check if duration is valid (not -9223372036854775807)
            if (duration > 0) {
                val newPosition = (currentPosition + seekAmount).coerceIn(0, duration)
                exoPlayer.seekTo(newPosition)
            } else {
                // Duration not available yet, just seek relative to current position
                val newPosition = maxOf(0, currentPosition + seekAmount)
                exoPlayer.seekTo(newPosition)
            }
        } else {
            // Callback for other player types
            onSeek?.invoke(seekAmount)
        }
    }
    
    /**
     * Handle next episode
     */
    private fun handleNextEpisode() {
        Log.d(TAG, "Handling next episode")
        onNextEpisode?.invoke()
    }
    
    /**
     * Handle previous episode
     */
    private fun handlePrevEpisode() {
        Log.d(TAG, "Handling previous episode")
        onPrevEpisode?.invoke()
    }
    
    /**
     * Handle speed change
     * @param speedDelta Speed change amount (positive for increase, negative for decrease)
     */
    private fun handleSpeedChange(speedDelta: Float) {
        Log.d(TAG, "Handling speed change: $speedDelta")
        
        if (exoPlayer != null) {
            // Native ExoPlayer with improved audio clarity
            val currentSpeed = exoPlayer.playbackParameters.speed
            val newSpeed = (currentSpeed + speedDelta).coerceIn(MIN_SPEED, MAX_SPEED)
            currentPlaybackSpeed = newSpeed // Store the new speed
            // Use PlaybackParameters for better audio processing
            val playbackParameters = PlaybackParameters(newSpeed)
            exoPlayer.setPlaybackParameters(playbackParameters)
            Log.d(TAG, "⚡ Speed changed to: ${newSpeed}x with improved audio clarity")
            onSpeedChange?.invoke(newSpeed)
        } else {
            // Callback for other player types
            onSpeedChange?.invoke(speedDelta)
        }
    }
    
    /**
     * Handle speed preset selection
     * @param presetIndex Index of the speed preset (0-6)
     */
    private fun handleSpeedPreset(presetIndex: Int) {
        if (presetIndex in speedPresets.indices) {
            val speed = speedPresets[presetIndex]
            Log.d(TAG, "Handling speed preset: $speed (index: $presetIndex)")
            
            if (exoPlayer != null) {
                // Native ExoPlayer with improved audio clarity
                currentPlaybackSpeed = speed // Store the new speed
                val playbackParameters = PlaybackParameters(speed)
                exoPlayer.setPlaybackParameters(playbackParameters)
                Log.d(TAG, "⚡ Speed preset set to: ${speed}x with improved audio clarity")
                onSpeedChange?.invoke(speed)
            } else {
                // Callback for other player types
                onSpeedChange?.invoke(speed)
            }
        }
    }
    
    /**
     * Handle show playlist
     */
    private fun handleShowPlaylist() {
        Log.d(TAG, "Handling show playlist")
        onShowPlaylist?.invoke()
    }
    
    /**
     * Handle show speed menu
     */
    private fun handleShowSpeedMenu() {
        Log.d(TAG, "Handling show speed menu")
        
        // Create and show speed overlay menu
        if (speedOverlayMenu == null) {
            speedOverlayMenu = SpeedOverlayMenu(
                context = context,
                exoPlayer = exoPlayer,
                onSpeedChanged = { newSpeed ->
                    onSpeedChange?.invoke(newSpeed)
                },
                onClose = {
                    speedOverlayMenu = null
                }
            )
        }
        
        speedOverlayMenu?.show()
        onShowSpeedMenu?.invoke()
    }
    
    /**
     * Handle show settings
     */
    private fun handleShowSettings() {
        Log.d(TAG, "Handling show settings")
        onShowSettings?.invoke()
    }
    
    /**
     * Handle back button
     */
    private fun handleBack() {
        Log.d(TAG, "Handling back")
        onBack?.invoke()
    }
    
    /**
     * Handle close/escape
     */
    private fun handleClose() {
        Log.d(TAG, "Handling close")
        onClose?.invoke()
    }
    
    /**
     * Get current playback speed
     * @return Current playback speed
     */
    fun getCurrentSpeed(): Float {
        return currentPlaybackSpeed
    }
    
    /**
     * Set current playback speed (for initialization)
     */
    fun setCurrentSpeed(speed: Float) {
        currentPlaybackSpeed = speed
    }
    
    /**
     * Get current position
     * @return Current playback position in milliseconds
     */
    fun getCurrentPosition(): Long {
        return exoPlayer?.currentPosition ?: 0L
    }
    
    /**
     * Get total duration
     * @return Total duration in milliseconds
     */
    fun getDuration(): Long {
        return exoPlayer?.duration ?: 0L
    }
    
    /**
     * Check if player is playing
     * @return true if playing, false otherwise
     */
    fun isPlaying(): Boolean {
        return exoPlayer?.isPlaying == true
    }
    
    /**
     * Get speed presets
     * @return List of available speed presets
     */
    fun getSpeedPresets(): List<Float> {
        return speedPresets
    }
    
    /**
     * Set playlist overlay menu reference
     */
    fun setPlaylistOverlayMenu(menu: PlaylistOverlayMenu?) {
        playlistOverlayMenu = menu
    }
    
    /**
     * Clean up resources
     */
    fun destroy() {
        speedOverlayMenu?.destroy()
        speedOverlayMenu = null
        playlistOverlayMenu?.destroy()
        playlistOverlayMenu = null
    }
}
