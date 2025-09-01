package com.example.newiptv.ui.seriesinfo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ListView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.EpisodeEntity
import com.example.newiptv.data.db.entities.InfoEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.player.VideoPlayerActivity
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeriesInfoScreen : AppCompatActivity() {

    private lateinit var seasonListView: ListView
    private lateinit var episodeRecyclerView: RecyclerView
    private lateinit var seasonAdapter: SeasonsAdapter
    private lateinit var episodeAdapter: EpisodesAdapter
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    
    // New UI elements for enhanced series info
    private lateinit var backdropImageView: ImageView
    private lateinit var seriesTitleView: TextView
    private lateinit var seriesCategoryView: TextView
    private lateinit var seriesGenreView: TextView
    private lateinit var seriesReleaseDateView: TextView
    private lateinit var seriesRuntimeView: TextView
    private lateinit var seriesRatingView: TextView
    private lateinit var seriesCountView: TextView
    private lateinit var seriesDirectorView: TextView
    private lateinit var seriesCastView: TextView
    private lateinit var seriesDescriptionView: TextView

    private lateinit var repository: TvRepository
    private var seriesId: String = ""
    private var seriesName: String = ""
    private var selectedSeasonIndex = 0
    private var selectedEpisodeIndex = 0
    private var isInSeasonPanel = true
    private var currentFocusIndex = 0
    
    // Backdrop animation properties
    private var backdropUrls: List<String> = emptyList()
    private var currentBackdropIndex = 0
    private val backdropAnimationDuration = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_info_screen)

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)

        seriesId = intent.getStringExtra("series_id") ?: ""
        seriesName = intent.getStringExtra("series_name") ?: ""

        initializeViews()
        setupAdapters()
        loadSeasonsAndEpisodes()
    }

    private fun initializeViews() {
        // Existing views
        seasonListView = findViewById(R.id.seasonsListView)
        episodeRecyclerView = findViewById(R.id.episodesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        
        // New enhanced UI elements
        backdropImageView = findViewById(R.id.backdropImageView)
        seriesTitleView = findViewById(R.id.seriesTitleView)
        seriesCategoryView = findViewById(R.id.seriesCategoryView)
        seriesGenreView = findViewById(R.id.seriesGenreView)
        seriesReleaseDateView = findViewById(R.id.seriesReleaseDateView)
        seriesRuntimeView = findViewById(R.id.seriesRuntimeView)
        seriesRatingView = findViewById(R.id.seriesRatingView)
        seriesCountView = findViewById(R.id.seriesCountView)
        seriesDirectorView = findViewById(R.id.seriesDirectorView)
        seriesCastView = findViewById(R.id.seriesCastView)
        seriesDescriptionView = findViewById(R.id.seriesDescriptionView)
        
        // Set initial focus to season panel
        seasonListView.requestFocus()
        isInSeasonPanel = true
        selectedSeasonIndex = 0
        selectedEpisodeIndex = 0
        
        // Set initial series info
        seriesTitleView.text = seriesName
        seriesCategoryView.text = intent.getStringExtra("series_category") ?: ""
        
        // Ensure proper focus handling
        KeyEventLogger.logScreenEvent("SeriesInfoScreen", "Views initialized")
        KeyEventLogger.logFocusChange("SeriesInfoScreen", "Season", selectedSeasonIndex)
    }

    private fun setupAdapters() {
        seasonAdapter = SeasonsAdapter(this, emptyList())

        episodeAdapter = EpisodesAdapter { episode ->
            playEpisode(episode)
        }

        seasonListView.adapter = seasonAdapter
        
        // Add season selection listener
        seasonListView.setOnItemClickListener { _, _, position, _ ->
            android.util.Log.d("SeriesInfoScreen", "=== SEASON CLICKED (MOUSE) ===")
            android.util.Log.d("SeriesInfoScreen", "Season Position: $position")
            val seasonNumber = position + 1 // Assuming seasons start from 1
            android.util.Log.d("SeriesInfoScreen", "Season Number: $seasonNumber")
            loadEpisodesForSeason(seasonNumber)
        }
        
        // Add season selection listener for TV remote navigation (focus only, no auto-load)
        seasonListView.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("SeriesInfoScreen", "=== ON SEASON FOCUS CHANGED ===")
                android.util.Log.d("SeriesInfoScreen", "Season Position: $position")
                
                val seasonNumber = position + 1 // Assuming seasons start from 1
                android.util.Log.d("SeriesInfoScreen", "Season Number: $seasonNumber")
                android.util.Log.d("SeriesInfoScreen", "Series ID: $seriesId")
                
                // Only update selection index, don't load episodes automatically
                selectedSeasonIndex = position
                android.util.Log.d("SeriesInfoScreen", "Updated selectedSeasonIndex to: $selectedSeasonIndex")
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                android.util.Log.d("SeriesInfoScreen", "=== NO SEASON FOCUSED ===")
            }
        })

        episodeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SeriesInfoScreen)
            adapter = episodeAdapter
        }
        
        // Setup TV remote navigation
        setupTVRemoteNavigation()
    }
    
    private fun setupTVRemoteNavigation() {
        // Set initial focus to season panel
        seasonListView.requestFocus()
    }
    
    private fun startBackdropAnimation() {
        if (backdropUrls.isEmpty()) return
        
        val fadeInAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_in)
        val fadeOutAnimation = AnimationUtils.loadAnimation(this, android.R.anim.fade_out)
        
        fadeOutAnimation.duration = 500
        fadeInAnimation.duration = 500
        
        val runnable = object : Runnable {
            override fun run() {
                if (backdropUrls.isNotEmpty()) {
                    // Fade out current image
                    backdropImageView.startAnimation(fadeOutAnimation)
                    
                    // Load next backdrop image
                    val currentUrl = backdropUrls[currentBackdropIndex]
                    Glide.with(this@SeriesInfoScreen)
                        .load(currentUrl)
                        .placeholder(R.color.panel_background)
                        .error(R.color.panel_background)
                        .centerCrop()
                        .into(backdropImageView)
                    
                    // Fade in new image
                    backdropImageView.startAnimation(fadeInAnimation)
                    
                    // Move to next backdrop
                    currentBackdropIndex = (currentBackdropIndex + 1) % backdropUrls.size
                    
                    // Schedule next animation
                    backdropImageView.postDelayed(this, backdropAnimationDuration)
                }
            }
        }
        
        // Start the animation cycle
        backdropImageView.postDelayed(runnable, backdropAnimationDuration)
    }
    
    private fun updateSeriesInfo(info: InfoEntity) {
        // Update backdrop images
        backdropUrls = info.backdropPath?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        if (backdropUrls.isNotEmpty()) {
            startBackdropAnimation()
        }
        
        // Update series information
        seriesTitleView.text = info.name ?: seriesName
        seriesGenreView.text = "Genre: ${info.genre ?: "Unknown"}"
        seriesReleaseDateView.text = "Released: ${info.releaseDate ?: "Unknown"}"
        seriesRuntimeView.text = "Runtime: ${info.episodeRunTime ?: "0"} min"
        
        // Update rating with both rating types
        val rating5Based = info.rating5Based ?: 0.0
        val ratingString = info.rating ?: ""
        val ratingText = if (ratingString.isNotEmpty() && rating5Based > 0) {
            "Rating: ★ ${String.format("%.1f", rating5Based)} ($ratingString)"
        } else if (rating5Based > 0) {
            "Rating: ★ ${String.format("%.1f", rating5Based)}"
        } else if (ratingString.isNotEmpty()) {
            "Rating: ★ $ratingString"
        } else {
            "Rating: Not available"
        }
        seriesRatingView.text = ratingText
        
        // Update director
        seriesDirectorView.text = "Director: ${info.director ?: "Unknown"}"
        
        // Update cast (truncate if too long)
        val cast = info.cast ?: "Unknown"
        seriesCastView.text = if (cast.length > 60) {
            "Cast: ${cast.take(60)}..."
        } else {
            "Cast: $cast"
        }
        
        // Update plot/description
        seriesDescriptionView.text = info.plot ?: "No description available"
        
        KeyEventLogger.logScreenEvent("SeriesInfoScreen", "Series info updated")
    }
    
    private fun updateSeasonsEpisodesCount(episodes: List<EpisodeEntity>) {
        val totalSeasons = episodes.map { it.season }.distinct().size
        val totalEpisodes = episodes.size
        seriesCountView.text = "Seasons: $totalSeasons | Episodes: $totalEpisodes"
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val additionalInfo = "Panel: ${if (isInSeasonPanel) "Season" else "Episode"}, " +
                           "SeasonIndex: $selectedSeasonIndex, " +
                           "EpisodeIndex: $selectedEpisodeIndex"
        
        KeyEventLogger.logKeyEvent("SeriesInfoScreen", event, additionalInfo)
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isInSeasonPanel) {
                        isInSeasonPanel = true
                        seasonListView.requestFocus()
                        android.util.Log.d("SeriesInfoScreen", "DPAD_LEFT: Moved to season panel")
                        return true
                    }
                }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isInSeasonPanel) {
                                isInSeasonPanel = false
                                episodeRecyclerView.requestFocus()
                                KeyEventLogger.logNavigation("SeriesInfoScreen", "RIGHT", "Season", "Episode")
                                // Ensure episode focus is properly set
                                episodeRecyclerView.postDelayed({
                                    updateEpisodeFocus()
                                }, 100)
                                return true
                            }
                        }
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                            KeyEventLogger.logNavigation("SeriesInfoScreen", "UP", if (isInSeasonPanel) "Season" else "Episode")
                            if (isInSeasonPanel) {
                                navigateSeasonUp()
                            } else {
                                navigateEpisodeUp()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            KeyEventLogger.logNavigation("SeriesInfoScreen", "DOWN", if (isInSeasonPanel) "Season" else "Episode")
                            if (isInSeasonPanel) {
                                navigateSeasonDown()
                            } else {
                                navigateEpisodeDown()
                            }
                            return true
                        }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInSeasonPanel) {
                        val seasonNumber = selectedSeasonIndex + 1
                        KeyEventLogger.logItemSelection("SeriesInfoScreen", "Season", selectedSeasonIndex, "Season $seasonNumber")
                        selectCurrentSeason()
                    } else {
                        val episode = episodeAdapter.getEpisodeAt(selectedEpisodeIndex)
                        KeyEventLogger.logItemSelection("SeriesInfoScreen", "Episode", selectedEpisodeIndex, 
                            episode?.title ?: "Unknown")
                        selectCurrentEpisode()
                    }
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    android.util.Log.d("SeriesInfoScreen", "BACK key pressed - finishing activity")
                    finish()
                    return true
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
    
    private fun getKeyCodeName(keyCode: Int): String {
        return when (keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> "DPAD_LEFT"
            KeyEvent.KEYCODE_DPAD_RIGHT -> "DPAD_RIGHT"
            KeyEvent.KEYCODE_DPAD_UP -> "DPAD_UP"
            KeyEvent.KEYCODE_DPAD_DOWN -> "DPAD_DOWN"
            KeyEvent.KEYCODE_DPAD_CENTER -> "DPAD_CENTER"
            KeyEvent.KEYCODE_ENTER -> "ENTER"
            KeyEvent.KEYCODE_BACK -> "BACK"
            else -> "UNKNOWN($keyCode)"
        }
    }
    
    private fun navigateSeasonUp() {
        if (selectedSeasonIndex > 0) {
            selectedSeasonIndex--
            updateSeasonSelection()
            KeyEventLogger.logFocusChange("SeriesInfoScreen", "Season", selectedSeasonIndex)
        }
    }
    
    private fun navigateSeasonDown() {
        // Get seasons count from adapter
        val seasonsCount = seasonAdapter.count
        if (selectedSeasonIndex < seasonsCount - 1) {
            selectedSeasonIndex++
            updateSeasonSelection()
            KeyEventLogger.logFocusChange("SeriesInfoScreen", "Season", selectedSeasonIndex)
        }
    }
    
    private fun navigateEpisodeUp() {
        val episodesCount = episodeAdapter.itemCount
        if (selectedEpisodeIndex > 0) {
            selectedEpisodeIndex--
            updateEpisodeFocus()
            KeyEventLogger.logFocusChange("SeriesInfoScreen", "Episode", selectedEpisodeIndex)
            KeyEventLogger.logNavigation("SeriesInfoScreen", "UP", "Episode", "Episode")
        } else {
            KeyEventLogger.logError("SeriesInfoScreen", "Cannot navigate UP", "Already at first episode (index: $selectedEpisodeIndex)")
        }
    }
    
    private fun navigateEpisodeDown() {
        val episodesCount = episodeAdapter.itemCount
        if (selectedEpisodeIndex < episodesCount - 1) {
            selectedEpisodeIndex++
            updateEpisodeFocus()
            KeyEventLogger.logFocusChange("SeriesInfoScreen", "Episode", selectedEpisodeIndex)
            KeyEventLogger.logNavigation("SeriesInfoScreen", "DOWN", "Episode", "Episode")
        } else {
            KeyEventLogger.logError("SeriesInfoScreen", "Cannot navigate DOWN", "Already at last episode (index: $selectedEpisodeIndex, total: $episodesCount)")
        }
    }
    
    private fun updateSeasonSelection() {
        seasonListView.setSelection(selectedSeasonIndex)
    }
    
    private fun updateEpisodeFocus() {
        episodeRecyclerView.post {
            try {
                // Scroll to the selected position
                episodeRecyclerView.smoothScrollToPosition(selectedEpisodeIndex)
                
                // Wait a bit for scroll to complete, then set focus
                episodeRecyclerView.postDelayed({
                    val viewHolder = episodeRecyclerView.findViewHolderForAdapterPosition(selectedEpisodeIndex)
                    if (viewHolder != null) {
                        viewHolder.itemView.requestFocus()
                        KeyEventLogger.logFocusChange("SeriesInfoScreen", "Episode", selectedEpisodeIndex)
                    } else {
                        // If viewHolder is null, try to scroll again
                        episodeRecyclerView.scrollToPosition(selectedEpisodeIndex)
                        episodeRecyclerView.postDelayed({
                            val retryViewHolder = episodeRecyclerView.findViewHolderForAdapterPosition(selectedEpisodeIndex)
                            retryViewHolder?.itemView?.requestFocus()
                        }, 100)
                    }
                }, 150)
            } catch (e: Exception) {
                KeyEventLogger.logError("SeriesInfoScreen", "Error updating episode focus", e.message ?: "Unknown error")
            }
        }
    }
    
    private fun selectCurrentSeason() {
        val seasonNumber = selectedSeasonIndex + 1 // Assuming seasons start from 1
        android.util.Log.d("SeriesInfoScreen", "=== SEASON SELECTED (ENTER/CENTER) ===")
        android.util.Log.d("SeriesInfoScreen", "Selecting season: $seasonNumber at index: $selectedSeasonIndex")
        android.util.Log.d("SeriesInfoScreen", "Series ID: $seriesId")
        loadEpisodesForSeason(seasonNumber)
    }
    
    private fun selectCurrentEpisode() {
        val episode = episodeAdapter.getEpisodeAt(selectedEpisodeIndex)
        if (episode != null) {
            android.util.Log.d("SeriesInfoScreen", "=== EPISODE SELECTED (ENTER/CENTER) ===")
            android.util.Log.d("SeriesInfoScreen", "Selecting episode: ${episode.title} at index: $selectedEpisodeIndex")
            android.util.Log.d("SeriesInfoScreen", "Episode season: ${episode.season}")
            android.util.Log.d("SeriesInfoScreen", "Episode directSource: ${episode.directSource}")
            playEpisode(episode)
        } else {
            android.util.Log.e("SeriesInfoScreen", "Episode at index $selectedEpisodeIndex is null!")
        }
    }
    
    private fun loadSeasonsAndEpisodes() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                // Load series info first
                val info = repository.getInfo(seriesId, "series")
                if (info != null) {
                    updateSeriesInfo(info)
                }
                
                // Load episodes
                repository.loadEpisodesWithSync("series", seriesId).collectLatest { result ->
                    result.fold(
                        onSuccess = { episodes ->
                            loadingText.visibility = View.GONE

                            // Update seasons/episodes count
                            updateSeasonsEpisodesCount(episodes)

                            // Group by season
                            val seasonMap = episodes.groupBy { it.season }
                            val seasons = seasonMap.keys.sorted().map { "Season $it" }

                            seasonAdapter = SeasonsAdapter(this@SeriesInfoScreen, seasons)
                            seasonListView.adapter = seasonAdapter

                            // Auto-load first season if available
                            if (seasons.isNotEmpty()) {
                                loadEpisodesForSeason(seasonMap.keys.sorted().first(), seasonMap)
                            }
                        },
                        onFailure = { e ->
                            loadingText.visibility = View.GONE
                            errorText.text = "Failed to load episodes: ${e.message}"
                            errorText.visibility = View.VISIBLE
                            Log.e("SeriesInfoScreen", "Failed to load episodes", e)
                        }
                    )
                }
            } catch (e: Exception) {
                loadingText.visibility = View.GONE
                errorText.text = "Error: ${e.message}"
                errorText.visibility = View.VISIBLE
                Log.e("SeriesInfoScreen", "Exception fetching episodes", e)
            }
        }
    }

    private fun loadEpisodesForSeason(seasonNumber: Int, cached: Map<Int, List<EpisodeEntity>>? = null) {
        android.util.Log.d("SeriesInfoScreen", "=== LOADING EPISODES FOR SEASON ===")
        android.util.Log.d("SeriesInfoScreen", "Season Number: $seasonNumber")
        android.util.Log.d("SeriesInfoScreen", "Series ID: $seriesId")
        android.util.Log.d("SeriesInfoScreen", "Cached episodes available: ${cached != null}")
        
        lifecycleScope.launch {
            try {
                val episodes = cached?.get(seasonNumber)
                if (episodes != null) {
                    episodeAdapter.updateEpisodes(episodes)
                    android.util.Log.d("SeriesInfoScreen", "✅ Loaded ${episodes.size} episodes for season $seasonNumber (from cache)")
                    
                    // Log first few episodes to see their data
                    episodes.take(3).forEach { episode ->
                        android.util.Log.d("SeriesInfoScreen", "Episode: ${episode.title}, Season: ${episode.season}, DirectSource: ${episode.directSource}")
                    }
                } else {
                    android.util.Log.d("SeriesInfoScreen", "No cached episodes, loading from repository...")
                    // Load from repository if not cached
                    repository.getEpisodes(seriesId).collectLatest { episodeList ->
                        android.util.Log.d("SeriesInfoScreen", "Total episodes in DB: ${episodeList.size}")
                        
                        val seasonEpisodes = episodeList.filter { it.season == seasonNumber }
                        android.util.Log.d("SeriesInfoScreen", "Episodes for season $seasonNumber: ${seasonEpisodes.size}")
                        
                        episodeAdapter.updateEpisodes(seasonEpisodes)
                        android.util.Log.d("SeriesInfoScreen", "✅ Loaded ${seasonEpisodes.size} episodes for season $seasonNumber (from DB)")
                        
                        // Log first few episodes to see their data
                        seasonEpisodes.take(3).forEach { episode ->
                            android.util.Log.d("SeriesInfoScreen", "Episode: ${episode.title}, Season: ${episode.season}, DirectSource: ${episode.directSource}")
                        }
                        
                        if (seasonEpisodes.isEmpty()) {
                            android.util.Log.w("SeriesInfoScreen", "⚠️ No episodes found for season $seasonNumber")
                        }
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Error loading episodes for season $seasonNumber", e)
                e.printStackTrace()
            }
        }
    }

    private fun playEpisode(episode: EpisodeEntity) {
        android.util.Log.d("SeriesInfoScreen", "=== PLAYING EPISODE ===")
        android.util.Log.d("SeriesInfoScreen", "Episode Title: ${episode.title}")
        android.util.Log.d("SeriesInfoScreen", "Episode Season: ${episode.season}")
        android.util.Log.d("SeriesInfoScreen", "Episode DirectSource: ${episode.directSource}")
        android.util.Log.d("SeriesInfoScreen", "Series Name: $seriesName")
        android.util.Log.d("SeriesInfoScreen", "Series ID: $seriesId")
        
        // Check if URL is valid
        if (episode.directSource.isNullOrEmpty()) {
            android.util.Log.e("SeriesInfoScreen", "ERROR: Episode directSource is null or empty!")
            return
        }
        
        // Get current season episodes to find episode index
        lifecycleScope.launch {
            try {
                android.util.Log.d("SeriesInfoScreen", "Getting episodes from database for series: $seriesId")
                
                // Use the database directly to get episodes
                val database = com.example.newiptv.data.db.DatabaseProvider.getDatabase(this@SeriesInfoScreen)
                val episodeDao = database.episodeDao()
                
                val allEpisodes = episodeDao.getEpisodesSync(seriesId)
                android.util.Log.d("SeriesInfoScreen", "Total episodes in database: ${allEpisodes.size}")
                
                val currentSeasonEpisodes = allEpisodes
                    .filter { it.season == episode.season }
                    .sortedBy { it.episodeNum }
                
                android.util.Log.d("SeriesInfoScreen", "Episodes for season ${episode.season}: ${currentSeasonEpisodes.size}")
                
                val episodeIndex = currentSeasonEpisodes.indexOfFirst { it.id == episode.id }
                
                android.util.Log.d("SeriesInfoScreen", "Episode index in season: $episodeIndex")
                android.util.Log.d("SeriesInfoScreen", "Total episodes in season: ${currentSeasonEpisodes.size}")
                
                // Log all episodes in the season
                currentSeasonEpisodes.forEachIndexed { index, ep ->
                    android.util.Log.d("SeriesInfoScreen", "Season episode $index: ${ep.title} (ID: ${ep.id})")
                }
                
                val intent = Intent(this@SeriesInfoScreen, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, episode.directSource)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "${seriesName} - ${episode.title}")
                intent.putExtra(VideoPlayerActivity.EXTRA_SERIES_ID, seriesId)
                intent.putExtra(VideoPlayerActivity.EXTRA_SEASON_NUMBER, episode.season)
                intent.putExtra(VideoPlayerActivity.EXTRA_EPISODE_INDEX, episodeIndex)
                
                android.util.Log.d("SeriesInfoScreen", "Launching VideoPlayerActivity with:")
                android.util.Log.d("SeriesInfoScreen", "  - Video URL: ${episode.directSource}")
                android.util.Log.d("SeriesInfoScreen", "  - Series ID: $seriesId")
                android.util.Log.d("SeriesInfoScreen", "  - Season Number: ${episode.season}")
                android.util.Log.d("SeriesInfoScreen", "  - Episode Index: $episodeIndex")
                
                startActivity(intent)
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "Error getting episode index", e)
                e.printStackTrace()
                
                // Fallback: launch without navigation data
                val intent = Intent(this@SeriesInfoScreen, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, episode.directSource)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "${seriesName} - ${episode.title}")
                startActivity(intent)
            }
        }
    }
}

