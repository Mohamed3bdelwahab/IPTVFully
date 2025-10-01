package com.example.newiptv.player

import android.content.pm.ActivityInfo
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.media3.ui.PlayerView
import com.example.newiptv.R
import com.example.newiptv.databinding.ActivityVideoPlayerBinding
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.repository.WatchHistoryRepository
import com.example.newiptv.database.AppSettingsDao
import com.example.newiptv.repository.SettingsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

/**
 * Video Player Activity with MX Player-like UI
 * Supports fullscreen, custom controls, and TV compatibility
 */
class VideoPlayerActivity : AppCompatActivity(), IPTVVideoPlayer.PlayerListener {
    
    companion object {
        private const val TAG = "VideoPlayerActivity"
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_SERIES_ID = "series_id"
        const val EXTRA_MOVIE_ID = "movie_id"
        const val EXTRA_SEASON_NUMBER = "season_number"
        const val EXTRA_EPISODE_INDEX = "episode_index"
        private const val CONTROLS_HIDE_DELAY = 3000L // 3 seconds
        private const val PROGRESS_UPDATE_INTERVAL = 1000L // 1 second
        private const val REQUEST_OVERLAY_PERMISSION = 1001
    }
    
    private lateinit var binding: ActivityVideoPlayerBinding
    private lateinit var videoPlayer: IPTVVideoPlayer
    private lateinit var tvRemoteHandler: TVRemoteHandler
    private lateinit var mxPlayerIntegration: MXPlayerIntegration
    private var speedOverlayMenu: SpeedOverlayMenu? = null
    private var playlistOverlayMenu: PlaylistOverlayMenu? = null
    private var isFullscreen = false
    private var isControlsVisible = true
    private val handler = Handler(Looper.getMainLooper())
    private val hideControlsRunnable = Runnable { hideControls() }
    
    // MX Player settings
    private var useMXPlayerAsDefault = true
    private var mxPlayerLaunched = false
    
    // Position tracking and auto-play managers
    private lateinit var positionManager: PlaybackPositionManager
    private lateinit var autoPlayManager: AutoPlayManager
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private var hasResumedFromPosition = false
    
    // Episode navigation data
    private var seriesId: String? = null
    private var seasonNumber: Int = 1
    private var currentEpisodeIndex: Int = 0
    private var episodes: List<com.example.newiptv.data.db.entities.EpisodeEntity> = emptyList()
    
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check and request overlay permission
        checkOverlayPermission()
        
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
        // Initialize position tracking and auto-play managers
        val database = DatabaseProvider.getDatabase(this)
        positionManager = PlaybackPositionManager(database)
        autoPlayManager = AutoPlayManager()
        
        // Initialize settings repository
        val settingsDao = database.appSettingsDao()
        settingsRepository = SettingsRepository(settingsDao)
        watchHistoryRepository = WatchHistoryRepository(database)
        
        // Initialize MX Player integration
        mxPlayerIntegration = MXPlayerIntegration(this, object : MXPlayerIntegration.MXPlayerListener {
            override fun onMXPlayerLaunchSuccess() {
                Log.d(TAG, "✅ MX Player launched successfully")
                mxPlayerLaunched = true
                // Close this activity since MX Player is handling playback
            finish()
            }
            
            override fun onMXPlayerLaunchFailed(error: String) {
                Log.w(TAG, "⚠️ MX Player launch failed: $error")
                // Fallback to ExoPlayer
                initializeExoPlayer()
            }
            
            override fun onMXPlayerNotInstalled() {
                Log.w(TAG, "⚠️ MX Player not installed")
                // Fallback to ExoPlayer
                initializeExoPlayer()
            }
        })
        
        // Initialize video player with managers (ExoPlayer as fallback)
        videoPlayer = IPTVVideoPlayer(this, this, positionManager, autoPlayManager)
        
        // Bind ExoPlayer to PlayerView
        videoPlayer.getPlayer()?.let { player ->
            binding.playerView.player = player
        }
        
        // Set player view settings
        binding.playerView.useController = false // We'll use custom controls
        binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        
        // Load and apply settings
        loadAndApplySettings()
    }
    
    /**
     * Initialize ExoPlayer as fallback
     */
    private fun initializeExoPlayer() {
        Log.i(TAG, "🔄 Initializing ExoPlayer as fallback...")
        
        // Bind ExoPlayer to PlayerView
        videoPlayer.getPlayer()?.let { player ->
            binding.playerView.player = player
        }
        
        // Set player view settings
        binding.playerView.useController = false // We'll use custom controls
        binding.playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING)
        
        // Load and apply settings
        loadAndApplySettings()
        
        Log.i(TAG, "✅ ExoPlayer fallback initialized")
    }
    
    private fun loadAndApplySettings() {
        lifecycleScope.launch {
            try {
                // Initialize default settings if none exist
                settingsRepository.initializeDefaultSettings()
                
                // Load current settings
                val settings = settingsRepository.getSettingsSync()
                
                // Apply default playback speed
                val defaultSpeed = settings.defaultPlaybackSpeed
                videoPlayer.setPlaybackSpeed(defaultSpeed)
                // Sync speed with TV remote handler
                tvRemoteHandler.setCurrentSpeed(defaultSpeed)
                android.util.Log.d("VideoPlayerActivity", "🎯 Applied default playback speed: ${defaultSpeed}x")
                
                // Apply auto-play next setting
                autoPlayManager.setAutoPlayEnabled(settings.autoPlayNext)
                android.util.Log.d("VideoPlayerActivity", "🎯 Auto-play next: ${settings.autoPlayNext}")
                
                // Apply remember position setting
                positionManager.setRememberPositionEnabled(settings.rememberPosition)
                android.util.Log.d("VideoPlayerActivity", "🎯 Remember position: ${settings.rememberPosition}")
                
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerActivity", "Failed to load settings", e)
            }
        }
    }
    
    private fun savePlaybackSpeed(speed: Float) {
        lifecycleScope.launch {
            try {
                // Check if remember speed is enabled
                val settings = settingsRepository.getSettingsSync()
                if (settings.rememberPlaybackSpeed) {
                    settingsRepository.setDefaultPlaybackSpeed(speed)
                    android.util.Log.d("VideoPlayerActivity", "💾 Saved playback speed: ${speed}x")
                } else {
                    android.util.Log.d("VideoPlayerActivity", "💾 Speed not saved (remember disabled): ${speed}x")
                }
            } catch (e: Exception) {
                android.util.Log.e("VideoPlayerActivity", "Failed to save playback speed", e)
            }
        }
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
                // Speed change callback - save to settings and update UI
                android.util.Log.d("VideoPlayerActivity", "⚡ Speed changed to: ${newSpeed}x")
                // Sync speed with video player
                videoPlayer.setPlaybackSpeed(newSpeed)
                savePlaybackSpeed(newSpeed)
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
        
        // Set up episode navigation and menu buttons
        binding.btnPreviousEpisode.setOnClickListener {
            android.util.Log.d("VideoPlayerActivity", "🔄 Previous Episode button clicked")
            playPreviousEpisode()
        }
        
        binding.btnNextEpisode.setOnClickListener {
            android.util.Log.d("VideoPlayerActivity", "⏭️ Next Episode button clicked")
            playNextEpisode()
        }
        
        binding.btnSpeedMenu.setOnClickListener {
            android.util.Log.d("VideoPlayerActivity", "⚡ Speed Menu button clicked")
            showSpeedMenu()
        }
        
        binding.btnPlaylistMenu.setOnClickListener {
            android.util.Log.d("VideoPlayerActivity", "📋 Playlist Menu button clicked")
            showPlaylistMenu()
        }
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
            
            // Reset resume flag for new video
            hasResumedFromPosition = false
            
            // Try MX Player first if enabled and available
            Log.d(TAG, "🔍 MX Player check: useMXPlayerAsDefault=$useMXPlayerAsDefault")
            val isMXInstalled = mxPlayerIntegration.isMXPlayerInstalled()
            Log.d(TAG, "🔍 MX Player check: isMXPlayerInstalled=$isMXInstalled")
            
            if (useMXPlayerAsDefault && isMXInstalled) {
                Log.i(TAG, "🚀 Attempting to launch MX Player...")
                launchMXPlayer(videoUrl, videoTitle)
            } else {
                Log.i(TAG, "🔄 Using ExoPlayer (MX Player not available or disabled)")
                loadVideoWithExoPlayer(videoUrl, videoTitle)
            }
        } else {
            android.util.Log.e("VideoPlayerActivity", "No video URL provided")
        }
    }
    
    /**
     * Launch video in MX Player
     */
    private fun launchMXPlayer(videoUrl: String, videoTitle: String?) {
        // 🔹 Add to watch history BEFORE launching MX Player
        addToWatchHistory()
        
        // Get resume position if available
        val resumePosition = if (seriesId != null) {
            // For series, we'll get position after loading episodes
            0L
        } else {
            // For movies, get position immediately
            val contentId = videoTitle ?: "unknown"
            // Note: getSavedPosition is a suspend function, so we'll get position in coroutine
            0L // For now, start from beginning
        }
        
        Log.i(TAG, "🎬 Launching MX Player with resume position: ${resumePosition}ms")
        
        val success = mxPlayerIntegration.launchVideo(
            videoUrl = videoUrl,
            title = videoTitle,
            startPosition = resumePosition,
            decodeMode = MXPlayerIntegration.DECODE_MODE_AUTO
        )
        
        if (!success) {
            Log.w(TAG, "⚠️ MX Player launch failed, falling back to ExoPlayer")
            loadVideoWithExoPlayer(videoUrl, videoTitle)
        }
    }
    
    /**
     * Load video with ExoPlayer (fallback)
     */
    private fun loadVideoWithExoPlayer(videoUrl: String, videoTitle: String?) {
        // Load episodes first to get the specific episode ID
        if (seriesId != null) {
            loadEpisodesForNavigationAndVideo(videoUrl, videoTitle)
        } else {
            // For movies, use video title as content ID
            val contentType = "movie"
            val contentId = videoTitle ?: "unknown"
            
            // Load resume position before loading video
            lifecycleScope.launch {
                try {
                    val resumePosition = positionManager?.getSavedPosition(contentId, contentType) ?: 0L
                    android.util.Log.d("VideoPlayerActivity", "📍 Resume position loaded for movie: ${resumePosition}ms")
                    
                    // Load video with position tracking and resume position
                    videoPlayer.loadVideoWithTracking(videoUrl, contentId, contentType, resumePosition)
                    
                    // Start position tracking
                    videoPlayer.startPositionTracking()
        } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerActivity", "Failed to load resume position", e)
                    // Load video without resume position
                    videoPlayer.loadVideoWithTracking(videoUrl, contentId, contentType, 0L)
                    videoPlayer.startPositionTracking()
                }
            }
        }
    }
    
    private fun loadEpisodesForNavigationAndVideo(videoUrl: String, videoTitle: String?) {
        android.util.Log.d("VideoPlayerActivity", "=== LOADING EPISODES FOR NAVIGATION AND VIDEO ===")
        android.util.Log.d("VideoPlayerActivity", "Series ID: $seriesId")
        android.util.Log.d("VideoPlayerActivity", "Season Number: $seasonNumber")
        android.util.Log.d("VideoPlayerActivity", "Episode Index: $currentEpisodeIndex")
        
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
                    
                    // Get the current episode for position tracking
                    if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.size) {
                        val currentEpisode = episodes[currentEpisodeIndex]
                        android.util.Log.d("VideoPlayerActivity", "🎯 Current episode for position tracking: ${currentEpisode.title} (ID: ${currentEpisode.id})")
                        
                        // Use the specific episode ID for position tracking
                        val contentType = "episode"
                        val contentId = currentEpisode.id // Use specific episode ID instead of series ID
                        
                        // Load resume position for this specific episode
                        lifecycleScope.launch {
                            try {
                                val resumePosition = positionManager?.getSavedPosition(contentId, contentType) ?: 0L
                                android.util.Log.d("VideoPlayerActivity", "📍 Resume position loaded for episode ${currentEpisode.title}: ${resumePosition}ms")
                                
                                // Load video with position tracking and resume position
                                videoPlayer.loadVideoWithTracking(videoUrl, contentId, contentType, resumePosition)
                                
                                // Start position tracking
                                videoPlayer.startPositionTracking()
                                
                                // Initialize auto-play manager with episodes
                                autoPlayManager.initializeAutoPlay(seriesId!!, currentEpisode.id, episodes)
                                android.util.Log.d("VideoPlayerActivity", "🎬 Auto-play initialized for episode: ${currentEpisode.title}")
                                
                            } catch (e: Exception) {
                                android.util.Log.e("VideoPlayerActivity", "Failed to load resume position for episode", e)
                                // Load video without resume position
                                videoPlayer.loadVideoWithTracking(videoUrl, contentId, contentType, 0L)
                                videoPlayer.startPositionTracking()
                                
                                // Initialize auto-play manager
                                autoPlayManager.initializeAutoPlay(seriesId!!, currentEpisode.id, episodes)
                            }
                        }
                    } else {
                        android.util.Log.e("VideoPlayerActivity", "No episodes found or invalid episode index")
                        // Fallback to series ID if no episodes found
                        val contentType = "episode"
                        val contentId = seriesId!!
                        videoPlayer.loadVideoWithTracking(videoUrl, contentId, contentType, 0L)
                        videoPlayer.startPositionTracking()
                    }
                    
                    // Log final episode details
                    episodes.forEachIndexed { index, episode ->
                        android.util.Log.d("VideoPlayerActivity", "Final episode $index: ${episode.title} (Season ${episode.season}, Episode ${episode.episodeNum})")
                    }
                }
        } catch (e: Exception) {
                android.util.Log.e("VideoPlayerActivity", "Error loading episodes for navigation and video", e)
                e.printStackTrace()
            }
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
                    
                    // Initialize auto-play manager with episodes
                    if (episodes.isNotEmpty() && currentEpisodeIndex < episodes.size) {
                        val currentEpisode = episodes[currentEpisodeIndex]
                        autoPlayManager.initializeAutoPlay(seriesId!!, currentEpisode.id, episodes)
                        android.util.Log.d("VideoPlayerActivity", "🎬 Auto-play initialized for episode: ${currentEpisode.title}")
                    }
                    
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
                
                // Update watch history every 30 seconds
                if (position % 30000 < 1000) { // Every ~30 seconds
                    updateWatchHistory()
                }
            }
        }
        
        // Schedule next update
        handler.postDelayed({ updateProgress() }, PROGRESS_UPDATE_INTERVAL)
    }
    
    // Watch History Management
    private fun addToWatchHistory() {
        lifecycleScope.launch {
            try {
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: return@launch
                val videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE) ?: "Unknown Title"
                val contentType = intent.getStringExtra("content_type") ?: "movie"
                val seriesId = intent.getStringExtra(EXTRA_SERIES_ID)
                val movieId = intent.getStringExtra(EXTRA_MOVIE_ID)
                val seasonNumber = intent.getIntExtra(EXTRA_SEASON_NUMBER, 1)
                val episodeIndex = intent.getIntExtra(EXTRA_EPISODE_INDEX, 0)
                
                // Determine content type and ID - use proper DB IDs instead of hash codes
                val contentId = when (contentType) {
                    "episode" -> seriesId ?: videoUrl.hashCode().toString()
                    "series" -> seriesId ?: videoUrl.hashCode().toString()
                    "movie" -> movieId ?: videoUrl.hashCode().toString()
                    else -> movieId ?: videoUrl.hashCode().toString()
                }
                
                // Get current position and duration
                val currentPosition = videoPlayer?.getCurrentPosition() ?: 0L
                val duration = videoPlayer?.getDuration() ?: 0L
                val watchPercentage = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                
                watchHistoryRepository.addToHistory(
                    contentId = contentId,
                    contentType = contentType,
                    title = videoTitle,
                    cover = null, // Could be extracted from video metadata
                    streamUrl = videoUrl,
                    categoryId = null,
                    categoryName = null,
                    seriesId = seriesId,
                    seasonNumber = if (contentType == "episode") seasonNumber else null,
                    episodeNumber = if (contentType == "episode") episodeIndex + 1 else null,
                    watchDuration = currentPosition,
                    totalDuration = duration,
                    watchPercentage = watchPercentage,
                    isCompleted = watchPercentage >= 0.9f, // Consider 90%+ as completed
                    resumePosition = currentPosition
                )
                
                Log.d(TAG, "✅ Added to watch history: $videoTitle ($contentType)")
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to add to watch history", e)
            }
        }
    }
    
    private fun updateWatchHistory() {
        lifecycleScope.launch {
            try {
                val videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL) ?: return@launch
                val contentType = intent.getStringExtra("content_type") ?: "movie"
                val seriesId = intent.getStringExtra(EXTRA_SERIES_ID)
                val movieId = intent.getStringExtra(EXTRA_MOVIE_ID)
                
                val contentId = when (contentType) {
                    "episode" -> seriesId ?: videoUrl.hashCode().toString()
                    "series" -> seriesId ?: videoUrl.hashCode().toString()
                    "movie" -> movieId ?: videoUrl.hashCode().toString()
                    else -> movieId ?: videoUrl.hashCode().toString()
                }
                
                val currentPosition = videoPlayer?.getCurrentPosition() ?: 0L
                val duration = videoPlayer?.getDuration() ?: 0L
                val watchPercentage = if (duration > 0) currentPosition.toFloat() / duration.toFloat() else 0f
                
                watchHistoryRepository.updateWatchProgress(
                    contentId = contentId,
                    contentType = contentType,
                    watchDuration = currentPosition,
                    totalDuration = duration,
                    watchPercentage = watchPercentage,
                    isCompleted = watchPercentage >= 0.9f,
                    resumePosition = currentPosition
                )
            } catch (e: Exception) {
                Log.e(TAG, "❌ Failed to update watch history", e)
            }
        }
    }

    // IPTVVideoPlayer.PlayerListener implementations
    override fun onPlayerReady() {
        runOnUiThread {
            binding.btnPlayPause.setImageResource(R.drawable.ic_play)
            binding.progressBar.visibility = View.GONE
            updateProgress()
            
            // Add to watch history when player is ready
            addToWatchHistory()
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
    
    override fun onEpisodeEnded() {
        runOnUiThread {
            android.util.Log.d("VideoPlayerActivity", "🎬 Episode ended - checking auto-play")
            
            // Check if auto-play should trigger
            if (autoPlayManager.shouldAutoPlayNext()) {
                val nextEpisode = autoPlayManager.getNextEpisode()
                if (nextEpisode != null) {
                    android.util.Log.d("VideoPlayerActivity", "🎬 Auto-playing next episode: ${nextEpisode.title}")
                    // Load next episode
                    loadNextEpisode(nextEpisode)
                } else {
                    android.util.Log.d("VideoPlayerActivity", "🎬 No next episode available")
                }
            } else {
                android.util.Log.d("VideoPlayerActivity", "🎬 Auto-play disabled or no next episode")
            }
        }
    }
    
    override fun onPositionLoaded(position: Long) {
        runOnUiThread {
            android.util.Log.d("VideoPlayerActivity", "📍 Position loaded: ${position}ms")
            
            // Only resume once to prevent loops
            if (position > 0 && !hasResumedFromPosition) {
                hasResumedFromPosition = true
                // Show resume dialog or auto-resume
                showResumeDialog(position)
            }
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
        // If any overlay menus are open, close them first
        if (speedOverlayMenu?.isMenuVisible() == true) {
            speedOverlayMenu?.hide()
            return
        }
        if (playlistOverlayMenu?.isMenuVisible() == true) {
            playlistOverlayMenu?.hide()
            return
        }
        
        // Always exit the activity when back is pressed, regardless of fullscreen state
            super.onBackPressed()
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
        // Set fullscreen flags first
        window.setFlags(
            WindowManager.LayoutParams.FLAG_FULLSCREEN,
            WindowManager.LayoutParams.FLAG_FULLSCREEN
        )
        
        // Set landscape orientation for full screen experience
        requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE
        isFullscreen = true
        
        // Ensure the content extends to the edges
        window.statusBarColor = android.graphics.Color.TRANSPARENT
        window.navigationBarColor = android.graphics.Color.TRANSPARENT
        
        // Use modern immersive mode approach - but only after window is ready
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Android 11+ (API 30+) - delay until after onCreate
            window.decorView.post {
                try {
                    window.setDecorFitsSystemWindows(false)
                    window.insetsController?.let { controller ->
                        controller.hide(android.view.WindowInsets.Type.systemBars())
                        controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerActivity", "Error setting up modern immersive mode", e)
                    // Fallback to legacy method
                    setupLegacyImmersiveMode()
                }
            }
        } else {
            // Android 10 and below
            setupLegacyImmersiveMode()
        }
    }
    
    private fun setupLegacyImmersiveMode() {
        @Suppress("DEPRECATION")
        window.decorView.systemUiVisibility = (
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY or
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE or
            View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN or
            View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or
            View.SYSTEM_UI_FLAG_FULLSCREEN
        )
    }
    
    private fun setupAutoHide() {
        // Auto-hide controls after inactivity
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            // Modern approach for Android 11+ - delay until window is ready
            window.decorView.post {
                try {
                    window.insetsController?.let { controller ->
                        controller.systemBarsBehavior = android.view.WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                    }
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerActivity", "Error setting up auto-hide", e)
                }
            }
        } else {
            // Legacy approach for older Android versions
            @Suppress("DEPRECATION")
            binding.root.setOnSystemUiVisibilityChangeListener { visibility ->
                if (visibility and View.SYSTEM_UI_FLAG_FULLSCREEN == 0) {
                    // System bars are visible, hide them again
                    setupLegacyImmersiveMode()
                }
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
        // Check if overlay permission is granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                android.util.Log.w("VideoPlayerActivity", "❌ Cannot show speed menu: overlay permission not granted")
                android.widget.Toast.makeText(
                    this,
                    "Please grant overlay permission to use speed menu",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        
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
        // Check if overlay permission is granted
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                android.util.Log.w("VideoPlayerActivity", "❌ Cannot show playlist menu: overlay permission not granted")
                android.widget.Toast.makeText(
                    this,
                    "Please grant overlay permission to use playlist menu",
                    android.widget.Toast.LENGTH_SHORT
                ).show()
                return
            }
        }
        
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
                    tvRemoteHandler.setPlaylistOverlayMenu(null)
                }
            )
            
            // Set the playlist menu reference in TVRemoteHandler
            tvRemoteHandler.setPlaylistOverlayMenu(playlistOverlayMenu)
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
            // Save current episode position before switching
            saveCurrentEpisodePosition()
            
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
            // Save current episode position before switching
            saveCurrentEpisodePosition()
            
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
                
                // Update current episode index
                currentEpisodeIndex = index
                
                // Update auto-play manager with current episode
                autoPlayManager.updateCurrentEpisode(episode.id)
                
                // Load video with tracking using specific episode ID for resume position
                val contentType = "episode"
                val contentId = episode.id // Use specific episode ID for position tracking
                
                // Load resume position for this specific episode
                lifecycleScope.launch {
                    try {
                        val resumePosition = positionManager?.getSavedPosition(contentId, contentType) ?: 0L
                        android.util.Log.d("VideoPlayerActivity", "📍 Resume position loaded for episode ${episode.title}: ${resumePosition}ms")
                        
                        // Load video with position tracking and resume position
                        videoPlayer.loadVideoWithTracking(episode.directSource, contentId, contentType, resumePosition)
                        
                        // Start position tracking
                        videoPlayer.startPositionTracking()
                        
                    } catch (e: Exception) {
                        android.util.Log.e("VideoPlayerActivity", "Failed to load resume position for episode", e)
                        // Load video without resume position
                        videoPlayer.loadVideoWithTracking(episode.directSource, contentId, contentType, 0L)
                        videoPlayer.startPositionTracking()
                    }
                }
                
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
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // If playlist menu is visible, let it handle DPAD keys
        if (playlistOverlayMenu?.isMenuVisible() == true) {
            if (playlistOverlayMenu?.handleKeyEvent(event) == true) {
                return true
            }
        }
        
        // If speed menu is visible, let it handle keys
        if (speedOverlayMenu?.isMenuVisible() == true) {
            if (speedOverlayMenu?.handleKeyEvent(event) == true) {
                return true
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
    
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) {
            // Re-enter immersive mode when window gains focus
            // Use legacy method to avoid window initialization issues
            setupLegacyImmersiveMode()
        }
    }
    
    /**
     * Load next episode for auto-play
     */
    private fun loadNextEpisode(nextEpisode: com.example.newiptv.data.db.entities.EpisodeEntity) {
        android.util.Log.d("VideoPlayerActivity", "🎬 Loading next episode: ${nextEpisode.title}")
        
        if (!nextEpisode.directSource.isNullOrEmpty()) {
            // Update current episode index
            currentEpisodeIndex = episodes.indexOf(nextEpisode)
            autoPlayManager.updateCurrentEpisode(nextEpisode.id)
            
            // Update title
            binding.tvTitle.text = nextEpisode.title
            
            // Load video with tracking using specific episode ID
            val contentType = "episode"
            val contentId = nextEpisode.id // Use specific episode ID for position tracking
            
            // Load resume position for this specific episode
            lifecycleScope.launch {
                try {
                    val resumePosition = positionManager?.getSavedPosition(contentId, contentType) ?: 0L
                    android.util.Log.d("VideoPlayerActivity", "📍 Resume position loaded for next episode ${nextEpisode.title}: ${resumePosition}ms")
                    
                    // Load video with position tracking and resume position
                    videoPlayer.loadVideoWithTracking(nextEpisode.directSource, contentId, contentType, resumePosition)
                    
                    // Start position tracking
                    videoPlayer.startPositionTracking()
                    
                } catch (e: Exception) {
                    android.util.Log.e("VideoPlayerActivity", "Failed to load resume position for next episode", e)
                    // Load video without resume position
                    videoPlayer.loadVideoWithTracking(nextEpisode.directSource, contentId, contentType, 0L)
                    videoPlayer.startPositionTracking()
                }
            }
            
            // Show controls briefly
            showControlsTemporarily()
        } else {
            android.util.Log.e("VideoPlayerActivity", "Next episode directSource is null or empty!")
        }
    }
    
    /**
     * Show resume dialog when position is loaded
     */
    private fun showResumeDialog(position: Long) {
        // For now, just auto-resume from the position
        // In a full implementation, this would show a dialog asking the user
        android.util.Log.d("VideoPlayerActivity", "📍 Auto-resuming from position: ${position}ms")
        
        // Seek to the saved position
        videoPlayer.seekTo(position)
        
        // Show a brief message
        //binding.tvTitle.text = "Resuming from ${formatTime(position)}"
        //showControlsTemporarily()
    }
    
    /**
     * Save current episode position before switching episodes
     */
    private fun saveCurrentEpisodePosition() {
        try {
            if (episodes.isNotEmpty() && currentEpisodeIndex in episodes.indices) {
                val currentEpisode = episodes[currentEpisodeIndex]
                val currentPosition = videoPlayer.getCurrentPosition()
                
                if (currentPosition > 0) {
                    android.util.Log.d("VideoPlayerActivity", "💾 Saving position for episode ${currentEpisode.title}: ${currentPosition}ms")
                    
                    // Save position using the position manager
                    positionManager?.saveCurrentPosition()
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("VideoPlayerActivity", "Failed to save current episode position", e)
        }
    }
    
    /**
     * Check and request overlay permission for system alert windows
     */
    private fun checkOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (!android.provider.Settings.canDrawOverlays(this)) {
                android.util.Log.d("VideoPlayerActivity", "🔒 Overlay permission not granted, requesting...")
                requestOverlayPermission()
            } else {
                android.util.Log.d("VideoPlayerActivity", "✅ Overlay permission already granted")
            }
        }
    }
    
    /**
     * Request overlay permission from user
     */
    private fun requestOverlayPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            val intent = android.content.Intent(
                android.provider.Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                android.net.Uri.parse("package:$packageName")
            )
            startActivityForResult(intent, REQUEST_OVERLAY_PERMISSION)
        }
    }
    
    /**
     * Handle permission request result
     */
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: android.content.Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
                if (android.provider.Settings.canDrawOverlays(this)) {
                    android.util.Log.d("VideoPlayerActivity", "✅ Overlay permission granted by user")
                } else {
                    android.util.Log.w("VideoPlayerActivity", "❌ Overlay permission denied by user")
                    // Show a message to user about the permission being required
                    android.widget.Toast.makeText(
                        this,
                        "Overlay permission is required for speed and playlist menus. Please grant permission in settings.",
                        android.widget.Toast.LENGTH_LONG
                    ).show()
                }
            }
        }
    }

}
