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
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Video Player Activity with MX Player-like UI
 * Supports fullscreen, custom controls, and TV compatibility
 */
class VideoPlayerActivity : AppCompatActivity(), IPTVVideoPlayer.PlayerListener {
    
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var videoPlayer: IPTVVideoPlayer
    private lateinit var tvRemoteHandler: TVRemoteHandler
    private var speedOverlayMenu: SpeedOverlayMenu? = null
    private var playlistOverlayMenu: PlaylistOverlayMenu? = null
    private var isFullscreen = false
    private var isControlsVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    
    // Episode navigation data
    private var seriesId: String? = null
    private var seasonNumber: Int = 1
    private var currentEpisodeIndex: Int = 0
    private var episodes: List<com.example.newiptv.data.db.entities.EpisodeEntity> = emptyList()
    
    companion object {
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_SEASON_NUMBER = "season_number"
        const val EXTRA_EPISODE_INDEX = "episode_index"
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
                // Next episode callback - implemented for playlist navigation
                playNextEpisode()
            },
            onPrevEpisode = {
                // Previous episode callback - implemented for playlist navigation
                playPreviousEpisode()
            },
            onShowPlaylist = {
                // Show playlist callback - implemented for playlist UI
                showPlaylistMenu()
            },
            onShowSpeedMenu = {
                // Show speed menu callback - implemented for speed control UI
                showSpeedMenu()
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
        
        // Extract episode navigation data
        seriesId = intent.getStringExtra(EXTRA_SERIES_ID)
        seasonNumber = intent.getIntExtra(EXTRA_SEASON_NUMBER, 1)
        currentEpisodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, 0)
        
        android.util.Log.d("VideoPlayerActivity", "=== LOADING VIDEO ===")
        android.util.Log.d("VideoPlayerActivity", "Series ID: $seriesId")
        android.util.Log.d("VideoPlayerActivity", "Season Number: $seasonNumber")
        android.util.Log.d("VideoPlayerActivity", "Episode Index: $currentEpisodeIndex")
        android.util.Log.d("VideoPlayerActivity", "Video URL: $videoUrl")
        android.util.Log.d("VideoPlayerActivity", "Video Title: $videoTitle")
        
        if (videoUrl != null) {
            binding.tvTitle.text = videoTitle ?: "Video Player"
            videoPlayer.loadVideo(videoUrl)
            
            // Load episodes for navigation if series ID is available
            if (seriesId != null) {
                loadEpisodesForNavigation()
            }
        } else {
            // Load test video
            val testUrl = "http://aws85485.amazonedge.net/series/moh7amed819/150730/172237.mkv"
            binding.tvTitle.text = "Test Video (MKV)"
            videoPlayer.loadVideo(testUrl)
        }
    }
    
    private fun loadEpisodesForNavigation() {
        android.util.Log.d("VideoPlayerActivity", "=== LOADING EPISODES FOR NAVIGATION ===")
        android.util.Log.d("VideoPlayerActivity", "Series ID: $seriesId")
        android.util.Log.d("VideoPlayerActivity", "Season Number: $seasonNumber")
        
        if (seriesId.isNullOrEmpty()) {
            android.util.Log.e("VideoPlayerActivity", "Series ID is null or empty, cannot load episodes")
            return
        }
        
        // Use coroutine to load episodes from database
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val database = com.example.newiptv.data.db.DatabaseProvider.getDatabase(this@VideoPlayerActivity)
                val episodeDao = database.episodeDao()
                
                android.util.Log.d("VideoPlayerActivity", "Database and DAO initialized successfully")
                
                // Load all episodes for the series first
                val allEpisodes = episodeDao.getEpisodesSync(seriesId!!)
                android.util.Log.d("VideoPlayerActivity", "Total episodes loaded for series $seriesId: ${allEpisodes.size}")
                
                // Log all episodes to see what we have
                allEpisodes.forEach { episode ->
                    android.util.Log.d("VideoPlayerActivity", "All episodes - ID: ${episode.id}, Title: ${episode.title}, Season: ${episode.season}, Episode: ${episode.episodeNum}")
                }
                
                // Filter episodes for the current season
                val loadedEpisodes = allEpisodes
                    .filter { it.season == seasonNumber }
                    .sortedBy { it.episodeNum }
                
                android.util.Log.d("VideoPlayerActivity", "Filtered episodes for season $seasonNumber: ${loadedEpisodes.size}")
                
                // Log filtered episodes
                loadedEpisodes.forEach { episode ->
                    android.util.Log.d("VideoPlayerActivity", "Filtered episode - ID: ${episode.id}, Title: ${episode.title}, Season: ${episode.season}, Episode: ${episode.episodeNum}")
                }
                
                // Update UI on main thread
                runOnUiThread {
                    episodes = loadedEpisodes
                    android.util.Log.d("VideoPlayerActivity", "Episodes list updated with ${episodes.size} episodes")
                    
                    // Log final episode details
                    episodes.forEachIndexed { index, episode ->
                        android.util.Log.d("VideoPlayerActivity", "Final episode $index: ${episode.title} (Season ${episode.season}, Episode ${episode.episodeNum})")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerActivity", "Error loading episodes for navigation", e)
                e.printStackTrace()
            }
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
        speedOverlayMenu?.destroy()
        speedOverlayMenu = null
        playlistOverlayMenu?.destroy()
        playlistOverlayMenu = null
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
    
    private fun showSpeedMenu() {
        if (speedOverlayMenu == null) {
            speedOverlayMenu = SpeedOverlayMenu(
                context = this,
                exoPlayer = videoPlayer.getPlayer(),
                onSpeedChanged = { speed ->
                    // Speed changed callback
                    android.util.Log.d("VideoPlayerActivity", "Playback speed changed to: ${speed}x")
                },
                onClose = {
                    // Speed menu closed callback
                    speedOverlayMenu = null
                }
            )
        }
        speedOverlayMenu?.show()
    }
    
    private fun showPlaylistMenu() {
        android.util.Log.d("VideoPlayerActivity", "=== SHOWING PLAYLIST MENU ===")
        android.util.Log.d("VideoPlayerActivity", "Episodes available: ${episodes.size}")
        android.util.Log.d("VideoPlayerActivity", "Current episode index: $currentEpisodeIndex")
        
        if (episodes.isEmpty()) {
            android.util.Log.w("VideoPlayerActivity", "No episodes available for playlist")
            showControlsTemporarily()
            return
        }
        
        if (playlistOverlayMenu == null) {
            playlistOverlayMenu = PlaylistOverlayMenu(
                context = this,
                episodes = episodes,
                currentEpisodeIndex = currentEpisodeIndex,
                onEpisodeSelected = { episode, index ->
                    android.util.Log.d("VideoPlayerActivity", "Playlist episode selected: ${episode.title} at index $index")
                    currentEpisodeIndex = index
                    playEpisodeAtIndex(index)
                },
                onClose = {
                    // Playlist menu closed callback
                    playlistOverlayMenu = null
                }
            )
        }
        
        playlistOverlayMenu?.show()
    }
    
    private fun playNextEpisode() {
        android.util.Log.d("VideoPlayerActivity", "=== PLAYING NEXT EPISODE ===")
        android.util.Log.d("VideoPlayerActivity", "Current episode index: $currentEpisodeIndex")
        android.util.Log.d("VideoPlayerActivity", "Total episodes: ${episodes.size}")
        android.util.Log.d("VideoPlayerActivity", "Episodes list: ${episodes.map { it.title }}")
        
        if (episodes.isEmpty()) {
            android.util.Log.e("VideoPlayerActivity", "Episodes list is empty! Cannot play next episode")
            showControlsTemporarily()
            return
        }
        
        if (currentEpisodeIndex < episodes.size - 1) {
            currentEpisodeIndex++
            android.util.Log.d("VideoPlayerActivity", "Moving to next episode at index: $currentEpisodeIndex")
            playEpisodeAtIndex(currentEpisodeIndex)
        } else {
            android.util.Log.d("VideoPlayerActivity", "Already at last episode (index: $currentEpisodeIndex)")
            showControlsTemporarily()
        }
    }
    
    private fun playPreviousEpisode() {
        android.util.Log.d("VideoPlayerActivity", "=== PLAYING PREVIOUS EPISODE ===")
        android.util.Log.d("VideoPlayerActivity", "Current episode index: $currentEpisodeIndex")
        android.util.Log.d("VideoPlayerActivity", "Total episodes: ${episodes.size}")
        android.util.Log.d("VideoPlayerActivity", "Episodes list: ${episodes.map { it.title }}")
        
        if (episodes.isEmpty()) {
            android.util.Log.e("VideoPlayerActivity", "Episodes list is empty! Cannot play previous episode")
            showControlsTemporarily()
            return
        }
        
        if (currentEpisodeIndex > 0) {
            currentEpisodeIndex--
            android.util.Log.d("VideoPlayerActivity", "Moving to previous episode at index: $currentEpisodeIndex")
            playEpisodeAtIndex(currentEpisodeIndex)
        } else {
            android.util.Log.d("VideoPlayerActivity", "Already at first episode (index: $currentEpisodeIndex)")
            showControlsTemporarily()
        }
    }
    
    private fun playEpisodeAtIndex(index: Int) {
        if (index in episodes.indices) {
            val episode = episodes[index]
            android.util.Log.d("VideoPlayerActivity", "Playing episode at index $index: ${episode.title}")
            android.util.Log.d("VideoPlayerActivity", "Episode directSource: ${episode.directSource}")
            
            if (!episode.directSource.isNullOrEmpty()) {
                // Update title
                binding.tvTitle.text = episode.title
                
                // Load and play the episode
                videoPlayer.loadVideo(episode.directSource)
                
                // Show controls briefly
                showControlsTemporarily()
            } else {
                android.util.Log.e("VideoPlayerActivity", "Episode directSource is null or empty!")
                showControlsTemporarily()
            }
        } else {
            android.util.Log.e("VideoPlayerActivity", "Invalid episode index: $index")
            showControlsTemporarily()
        }
    }
    

    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-enter immersive mode when window gains focus
            setupImmersiveMode()
        }
    }
}
