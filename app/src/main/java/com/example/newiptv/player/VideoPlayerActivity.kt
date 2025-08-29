package com.example.newiptv.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.media3.ui.PlayerView
import com.example.newiptv.R
import com.example.newiptv.databinding.ActivityVideoPlayerBinding
import java.util.concurrent.TimeUnit

/**
 * Video Player Activity with MX Player-like UI
 * Supports fullscreen, custom controls, and TV compatibility
 */
class VideoPlayerActivity : AppCompatActivity(), IPTVVideoPlayer.PlayerListener {
    
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var videoPlayer: IPTVVideoPlayer
    private lateinit var tvRemoteHandler: TVRemoteHandler
    private var isFullscreen = false
    private var isControlsVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        private const val CONTROLS_HIDE_DELAY = 3000L // 3 seconds
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // 1 second
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Set immersive mode
        setupImmersiveMode()
        
        binding = ActivityVideoPlayerBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupPlayer()
        setupTVRemote()
        setupControls()
        setupGestures()
        setupAutoHide()
        loadVideo()
    }
    
    private fun setupPlayer() {
        // Initialize video player
        videoPlayer = IPTVVideoPlayer(this, this)
        
        // Bind ExoPlayer to PlayerView
        videoPlayer.getPlayer()?.let { player ->
            binding.playerView.player = player
        }
        
        // Set player view settings
        binding.playerView.useController = false // We'll use custom controls
        binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
    }
    
    private fun setupTVRemote() {
        // Initialize TV remote handler
        tvRemoteHandler = TVRemoteHandler(
            context = this,
            exoPlayer = videoPlayer.getPlayer(),
            onPlayPause = {
                if (videoPlayer.isPlaying()) {
                    videoPlayer.pause()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_play)
                } else {
                    videoPlayer.play()
                    binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
                }
                showControlsTemporarily()
            },
            onSeek = { seekAmount ->
                if (videoPlayer.isReady()) {
                    val currentPosition = videoPlayer.getCurrentPosition()
                    val duration = videoPlayer.getDuration()
                    val newPosition = (currentPosition + seekAmount).coerceIn(0, duration)
                    videoPlayer.seekTo(newPosition)
                    updateTimeDisplay(newPosition, duration)
                    showControlsTemporarily()
                }
            },
            onSpeedChange = { newSpeed ->
                // Speed change callback - can be implemented for UI updates
                showControlsTemporarily()
            },
            onNextEpisode = {
                // Next episode callback - can be implemented for playlist navigation
                showControlsTemporarily()
            },
            onPrevEpisode = {
                // Previous episode callback - can be implemented for playlist navigation
                showControlsTemporarily()
            },
            onShowPlaylist = {
                // Show playlist callback - can be implemented for playlist UI
                showControlsTemporarily()
            },
            onShowSpeedMenu = {
                // Show speed menu callback - can be implemented for speed control UI
                showControlsTemporarily()
            },
            onShowSettings = {
                // Show settings callback - can be implemented for settings UI
                showControlsTemporarily()
            },
            onBack = {
                onBackPressed()
            },
            onClose = {
                onBackPressed()
            }
        )
    }
    
    private fun setupControls() {
        // Play/Pause button
        binding.btnPlayPause.setOnClickListener {
            if (videoPlayer.isPlaying()) {
                videoPlayer.pause()
                binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            } else {
                videoPlayer.play()
                binding.btnPlayPause.setImageResource(R.drawable.ic_pause)
            }
            showControlsTemporarily()
        }
        
        // Fullscreen button
        binding.btnFullscreen.setOnClickListener {
            toggleFullscreen()
        }
        
        // Back button
        binding.btnBack.setOnClickListener {
            onBackPressed()
        }
        
        // Seek bar
        binding.seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser && videoPlayer.isReady()) {
                    val duration = videoPlayer.getDuration()
                    val position = (progress * duration) / 100
                    videoPlayer.seekTo(position)
                    updateTimeDisplay(position, duration)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                handler.removeCallbacks(hideControlsRunnable)
            }
            
            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                showControlsTemporarily()
            }
        })
        
        // Volume controls
        binding.btnVolume.setOnClickListener {
            toggleVolumeControls()
        }
        
        binding.volumeSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                if (fromUser) {
                    val volume = progress / 100f
                    videoPlayer.setVolume(volume)
                    updateVolumeIcon(volume)
                }
            }
            
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })
        
        // Initialize volume
        binding.volumeSeekBar.progress = (videoPlayer.getVolume() * 100).toInt()
        updateVolumeIcon(videoPlayer.getVolume())
    }
    
    private fun setupGestures() {
        // Player view click to toggle controls with auto-hide
        binding.playerView.setOnClickListener {
            if (isControlsVisible) {
                hideControls()
            } else {
                showControlsTemporarily()
            }
        }
        
        // Touch gestures for seeking and control visibility
        binding.playerView.setOnTouchListener { _, event ->
            when (event.action) {
                android.view.MotionEvent.ACTION_DOWN -> {
                    // Show controls on touch
                    showControlsTemporarily()
                    true
                }
                else -> false
            }
        }
    }
    
    private fun loadVideo() {
        val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        
        if (videoUrl != null) {
            binding.tvTitle.text = videoTitle ?: "Video Player"
            videoPlayer.loadVideo(videoUrl)
        } else {
            // Load test video
            val testUrl = "http://aws85485.amazonedge.net/series/moh7amed819/150730/172237.mkv"
            binding.tvTitle.text = "Test Video (MKV)"
            videoPlayer.loadVideo(testUrl)
        }
    }
    
    private fun showControls() {
        if (!isControlsVisible) {
            isControlsVisible = true
            binding.controlsContainer.visibility = View.VISIBLE
            binding.topControls.visibility = View.VISIBLE
        }
    }
    
    private fun hideControls() {
        if (isControlsVisible) {
            isControlsVisible = false
            binding.controlsContainer.visibility = View.GONE
            binding.topControls.visibility = View.GONE
            binding.volumeControls.visibility = View.GONE
        }
    }
    
    private fun toggleFullscreen() {
        isFullscreen = !isFullscreen
        
        if (isFullscreen) {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen_exit)
        } else {
            requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
            binding.btnFullscreen.setImageResource(R.drawable.ic_fullscreen)
        }
    }
    
    private fun toggleVolumeControls() {
        if (binding.volumeControls.visibility == View.VISIBLE) {
            binding.volumeControls.visibility = View.GONE
        } else {
            binding.volumeControls.visibility = View.VISIBLE
            showControlsTemporarily()
        }
    }
    
    private fun updateTimeDisplay(position: Long, duration: Long) {
        val positionText = formatTime(position)
        val durationText = formatTime(duration)
        binding.tvTime.text = "$positionText / $durationText"
    }
    
    private fun updateVolumeIcon(volume: Float) {
        val iconRes = when {
            volume == 0f -> R.drawable.ic_volume_off
            volume < 0.5f -> R.drawable.ic_volume_down
            else -> R.drawable.ic_volume_up
        }
        binding.btnVolume.setImageResource(iconRes)
    }
    
    private fun formatTime(timeMs: Long): String {
        val hours = TimeUnit.MILLISECONDS.toHours(timeMs)
        val minutes = TimeUnit.MILLISECONDS.toMinutes(timeMs) % 60
        val seconds = TimeUnit.MILLISECONDS.toSeconds(timeMs) % 60
        
        return if (hours > 0) {
            String.format("%d:%02d:%02d", hours, minutes, seconds)
            } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }
    
    private fun updateProgress() {
        if (videoPlayer.isReady()) {
            val position = videoPlayer.getCurrentPosition()
            val duration = videoPlayer.getDuration()
            
            if (duration > 0) {
                val progress = ((position * 100) / duration).toInt()
                binding.seekBar.progress = progress
                updateTimeDisplay(position, duration)
            }
        }
        
        // Schedule next update
        handler.postDelayed({ updateProgress() }, PROGRESS_UPDATE_INTERVAL)
    }
    
    // IPTVVideoPlayer.PlayerListener implementations
    override fun onPlayerReady() {
        runOnUiThread {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            binding.progressBar.visibility = View.GONE
            updateProgress()
        }
    }
    
    override fun onPlayerError(error: String) {
        runOnUiThread {
            binding.progressBar.visibility = View.GONE
            // Show error message
            binding.tvError.text = error
            binding.tvError.visibility = View.VISIBLE
        }
    }
    
    override fun onPlaybackStateChanged(isPlaying: Boolean) {
        runOnUiThread {
            binding.btnPlayPause.setImageResource(
                if (isPlaying) R.drawable.ic_pause else R.drawable.ic_play
            )
        }
    }
    
    override fun onProgressChanged(position: Long, duration: Long) {
        // Handled by updateProgress()
    }
    
    override fun onBufferingChanged(isBuffering: Boolean) {
        runOnUiThread {
            binding.progressBar.visibility = if (isBuffering) View.VISIBLE else View.GONE
        }
    }
    
    override fun onResume() {
        super.onResume()
        videoPlayer.play()
    }
    
    override fun onPause() {
        super.onPause()
        videoPlayer.pause()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(hideControlsRunnable)
        tvRemoteHandler.destroy()
        videoPlayer.release()
    }
    
    override fun onBackPressed() {
        if (isFullscreen) {
            toggleFullscreen()
        } else {
            super.onBackPressed()
        }
    }
    
    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle TV remote key events
        event?.let { keyEvent ->
            if (tvRemoteHandler.handleKeyEvent(keyEvent)) {
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }
    
    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        // Handle TV remote key events
        event?.let { keyEvent ->
            if (tvRemoteHandler.handleKeyEvent(keyEvent)) {
                return true
            }
        }
        return super.onKeyUp(keyCode, event)
    }
    
    // Immersive Mode and Auto-Hide Methods
    private fun setupImmersiveMode() {
        // Hide system bars for immersive experience
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        )
        
        // Set fullscreen flags
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
    }
    
    private fun setupAutoHide() {
        // Auto-hide controls after inactivity
        binding.root.setOnSystemUiVisibilityChangeListener { visibility ->
            if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                // System bars are visible, hide them again
                setupImmersiveMode()
            }
        }
    }
    
    private fun showControlsTemporarily() {
        // Show controls and schedule auto-hide
        showControls()
        scheduleControlsHide()
    }
    
    private fun scheduleControlsHide() {
        // Cancel existing hide task
        handler.removeCallbacks(hideControlsRunnable)
        
        // Schedule new hide task
        handler.postDelayed(hideControlsRunnable, CONTROLS_HIDE_DELAY)
    }
    

    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-enter immersive mode when window gains focus
            setupImmersiveMode()
        }
    }
}
