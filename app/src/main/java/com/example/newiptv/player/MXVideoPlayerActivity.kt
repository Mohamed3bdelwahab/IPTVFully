package com.example.newiptv.player

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.example.newiptv.R
import com.example.newiptv.player.PlaybackPositionManager
import com.example.newiptv.repository.SettingsRepository
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.database.AppSettingsDao

/**
 * MX Video Player Activity - Uses MX Player as default with ExoPlayer fallback
 * This provides better codec support and hardware decoder compatibility
 */
class MXVideoPlayerActivity : AppCompatActivity(), MXPlayerIntegration.MXPlayerListener {
    
    companion object {
        private const val TAG = "MXVideoPlayerActivity"
        
        // Intent extras
        const val EXTRA_VIDEO_URL = "video_url"
        const val EXTRA_VIDEO_TITLE = "video_title"
        const val EXTRA_CONTENT_ID = "content_id"
        const val EXTRA_CONTENT_TYPE = "content_type"
        const val EXTRA_PLAYLIST_URLS = "playlist_urls"
        const val EXTRA_START_POSITION = "start_position"
        const val EXTRA_USE_MX_PLAYER = "use_mx_player"
        
        /**
         * Static method to launch video with MX Player
         */
        fun launchVideo(
            context: android.content.Context,
            videoUrl: String,
            videoTitle: String? = null,
            contentId: String? = null,
            contentType: String? = null,
            startPosition: Long = 0L,
            useMXPlayer: Boolean = true
        ) {
            val intent = Intent(context, MXVideoPlayerActivity::class.java).apply {
                putExtra(EXTRA_VIDEO_URL, videoUrl)
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_CONTENT_ID, contentId)
                putExtra(EXTRA_CONTENT_TYPE, contentType)
                putExtra(EXTRA_START_POSITION, startPosition)
                putExtra(EXTRA_USE_MX_PLAYER, useMXPlayer)
            }
            context.startActivity(intent)
        }
        
        fun launchPlaylist(
            context: android.content.Context,
            playlistUrls: List<String>,
            videoTitle: String? = null,
            contentId: String? = null,
            contentType: String? = null,
            startPosition: Long = 0L,
            useMXPlayer: Boolean = true
        ) {
            val intent = Intent(context, MXVideoPlayerActivity::class.java).apply {
                putStringArrayListExtra(EXTRA_PLAYLIST_URLS, ArrayList(playlistUrls))
                putExtra(EXTRA_VIDEO_TITLE, videoTitle)
                putExtra(EXTRA_CONTENT_ID, contentId)
                putExtra(EXTRA_CONTENT_TYPE, contentType)
                putExtra(EXTRA_START_POSITION, startPosition)
                putExtra(EXTRA_USE_MX_PLAYER, useMXPlayer)
            }
            context.startActivity(intent)
        }
    }
    
    private lateinit var mxPlayerIntegration: MXPlayerIntegration
    private lateinit var exoVideoPlayer: IPTVVideoPlayer
    private lateinit var settingsRepository: SettingsRepository
    private lateinit var positionManager: PlaybackPositionManager
    
    // Video data
    private var videoUrl: String? = null
    private var videoTitle: String? = null
    private var contentId: String? = null
    private var contentType: String? = null
    private var playlistUrls: ArrayList<String>? = null
    private var startPosition: Long = 0L
    private var useMXPlayer: Boolean = true
    
    // State tracking
    private var mxPlayerLaunched = false
    private var fallbackToExoPlayer = false
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_video_player)
        
        Log.i(TAG, "🎬 MX Video Player Activity created")
        
        // Initialize components
        initializeComponents()
        
        // Get video data from intent
        extractVideoData()
        
        // Launch video playback
        launchVideoPlayback()
    }
    
    /**
     * Initialize all components
     */
    private fun initializeComponents() {
        // Initialize MX Player integration
        mxPlayerIntegration = MXPlayerIntegration(this, this)
        
        // Initialize ExoPlayer as fallback
        exoVideoPlayer = IPTVVideoPlayer(this, object : IPTVVideoPlayer.PlayerListener {
            override fun onPlayerReady() {
                Log.d(TAG, "✅ ExoPlayer ready")
            }
            
            override fun onPlayerError(error: String) {
                Log.e(TAG, "❌ ExoPlayer error: $error")
                Toast.makeText(this@MXVideoPlayerActivity, "Playback error: $error", Toast.LENGTH_LONG).show()
            }
            
            override fun onPlaybackStateChanged(isPlaying: Boolean) {
                Log.d(TAG, "🎵 Playback state: ${if (isPlaying) "Playing" else "Paused"}")
            }
            
            override fun onBufferingChanged(isBuffering: Boolean) {
                Log.d(TAG, "⏳ Buffering: $isBuffering")
            }
            
            override fun onEpisodeEnded() {
                Log.d(TAG, "🏁 Episode ended")
                finish()
            }
            
            override fun onProgressChanged(position: Long, duration: Long) {
                // Handle progress updates if needed
            }
            
            override fun onPositionLoaded(position: Long) {
                // Handle position loaded if needed
            }
        }, positionManager, null)
        
        // Initialize settings repository
        val database = DatabaseProvider.getDatabase(this)
        val settingsDao = database.appSettingsDao()
        settingsRepository = SettingsRepository(settingsDao)
        
        // Initialize position manager
        positionManager = PlaybackPositionManager(database)
        
        Log.d(TAG, "✅ Components initialized")
    }
    
    /**
     * Extract video data from intent
     */
    private fun extractVideoData() {
        videoUrl = intent.getStringExtra(EXTRA_VIDEO_URL)
        videoTitle = intent.getStringExtra(EXTRA_VIDEO_TITLE)
        contentId = intent.getStringExtra(EXTRA_CONTENT_ID)
        contentType = intent.getStringExtra(EXTRA_CONTENT_TYPE)
        playlistUrls = intent.getStringArrayListExtra(EXTRA_PLAYLIST_URLS)
        startPosition = intent.getLongExtra(EXTRA_START_POSITION, 0L)
        useMXPlayer = intent.getBooleanExtra(EXTRA_USE_MX_PLAYER, true)
        
        Log.i(TAG, "📹 Video data extracted:")
        Log.i(TAG, "   URL: $videoUrl")
        Log.i(TAG, "   Title: $videoTitle")
        Log.i(TAG, "   Content ID: $contentId")
        Log.i(TAG, "   Content Type: $contentType")
        Log.i(TAG, "   Playlist: ${playlistUrls?.size ?: 0} videos")
        Log.i(TAG, "   Start Position: ${startPosition}ms")
        Log.i(TAG, "   Use MX Player: $useMXPlayer")
    }
    
    /**
     * Launch video playback
     */
    private fun launchVideoPlayback() {
        if (useMXPlayer && mxPlayerIntegration.isMXPlayerInstalled()) {
            launchMXPlayer()
        } else {
            Log.i(TAG, "🔄 Using ExoPlayer (MX Player not available or disabled)")
            launchExoPlayer()
        }
    }
    
    /**
     * Launch MX Player
     */
    private fun launchMXPlayer() {
        Log.i(TAG, "🚀 Launching MX Player...")
        
        val success = if (playlistUrls != null && playlistUrls!!.isNotEmpty()) {
            // Launch playlist
            mxPlayerIntegration.launchPlaylist(
                videoUrls = playlistUrls!!,
                title = videoTitle,
                startIndex = 0,
                startPosition = startPosition,
                decodeMode = MXPlayerIntegration.DECODE_MODE_AUTO
            )
        } else {
            // Launch single video
            videoUrl?.let { url ->
                mxPlayerIntegration.launchVideo(
                    videoUrl = url,
                    title = videoTitle,
                    startPosition = startPosition,
                    decodeMode = MXPlayerIntegration.DECODE_MODE_AUTO
                )
            } ?: false
        }
        
        if (success) {
            mxPlayerLaunched = true
            Log.i(TAG, "✅ MX Player launched successfully")
        } else {
            Log.w(TAG, "⚠️ MX Player launch failed, falling back to ExoPlayer")
            fallbackToExoPlayer()
        }
    }
    
    /**
     * Launch ExoPlayer as fallback
     */
    private fun launchExoPlayer() {
        Log.i(TAG, "🔄 Launching ExoPlayer fallback...")
        
        fallbackToExoPlayer = true
        
        // ExoPlayer listener is already set up in constructor
        
        // Load video
        videoUrl?.let { url ->
            exoVideoPlayer.loadVideoWithTracking(
                url = url,
                contentId = contentId ?: "unknown",
                contentType = contentType ?: "movie",
                resumePosition = startPosition
            )
        }
        
        Log.i(TAG, "✅ ExoPlayer fallback launched")
    }
    
    /**
     * Fallback to ExoPlayer when MX Player fails
     */
    private fun fallbackToExoPlayer() {
        if (!fallbackToExoPlayer) {
            Log.i(TAG, "🔄 Falling back to ExoPlayer...")
            launchExoPlayer()
        }
    }
    
    // MX Player Integration Listener Implementation
    
    override fun onMXPlayerLaunchSuccess() {
        Log.i(TAG, "✅ MX Player launched successfully")
        mxPlayerLaunched = true
        
        // Show toast to user
        Toast.makeText(this, "Opening in MX Player...", Toast.LENGTH_SHORT).show()
        
        // Close this activity since MX Player is handling playback
        finish()
    }
    
    override fun onMXPlayerLaunchFailed(error: String) {
        Log.e(TAG, "❌ MX Player launch failed: $error")
        
        // Fallback to ExoPlayer
        fallbackToExoPlayer()
    }
    
    override fun onMXPlayerNotInstalled() {
        Log.w(TAG, "⚠️ MX Player not installed")
        
        // Show installation prompt
        mxPlayerIntegration.showMXPlayerInstallationPrompt()
        
        // Fallback to ExoPlayer
        fallbackToExoPlayer()
    }
    
    override fun onBackPressed() {
        if (fallbackToExoPlayer) {
            // If using ExoPlayer, handle back press normally
            super.onBackPressed()
        } else {
            // If MX Player was launched, just finish this activity
            Log.d(TAG, "🔙 Back pressed - finishing activity")
            finish()
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        
        // Clean up ExoPlayer if it was used
        if (fallbackToExoPlayer) {
            exoVideoPlayer.release()
        }
        
        Log.d(TAG, "🧹 Activity destroyed")
    }
    
}
