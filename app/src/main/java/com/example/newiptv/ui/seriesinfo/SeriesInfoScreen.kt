package com.example.newiptv.ui.seriesinfo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.view.View
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import android.widget.Button
import android.widget.ImageButton
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import android.widget.ListView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.EpisodeEntity
import com.example.newiptv.data.db.entities.InfoEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.data.repository.WatchHistoryRepository
import com.example.newiptv.data.repository.FavoritePlaylistRepository
import com.example.newiptv.data.db.entities.FavoritePlaylistEntity
import com.example.newiptv.ui.favorites.PlaylistDialogAdapter
import com.example.newiptv.player.VideoPlayerActivity
import com.example.newiptv.player.MXPlayerIntegration
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel

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
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private lateinit var favoritePlaylistRepository: FavoritePlaylistRepository
    private lateinit var database: com.example.newiptv.data.db.AppDatabase
    private var contentType: String = "series"
    private var seriesId: String = ""
    private var seriesName: String = ""
    private var selectedSeasonIndex = 0
    private var selectedEpisodeIndex = 0
    private var isInSeasonPanel = true
    private var currentFocusIndex = 0
    private var isFavoriteButtonFocused = false
    private var isPlayAllButtonFocused = false
    
    // Backdrop animation properties
    private var backdropUrls: List<String> = emptyList()
    private var currentBackdropIndex = 0
    private val backdropAnimationDuration = 3000L // 3 seconds

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_info_screen)

        database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)
        watchHistoryRepository = WatchHistoryRepository(database)
        favoritePlaylistRepository = FavoritePlaylistRepository(database.favoritePlaylistDao())
        
        // Initialize default playlists if needed
        lifecycleScope.launch {
            favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
        }

        contentType = intent.getStringExtra("content_type") ?: "series"
        seriesId = intent.getStringExtra("series_id") ?: ""
        seriesName = intent.getStringExtra("series_name") ?: ""

        // Update title based on content type
        title = when (contentType) {
            "movies" -> "Movie Info"
            else -> "Series Info"
        }

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
        
        // Initialize favorite button
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)
        favoriteButton.setOnClickListener {
            showFavoriteDialog()
        }
        
        // Make favorite button focusable for TV remote
        favoriteButton.isFocusable = true
        favoriteButton.isFocusableInTouchMode = true
        
        // Add focus change listener for visual feedback
        favoriteButton.setOnFocusChangeListener { _, hasFocus ->
            isFavoriteButtonFocused = hasFocus
            if (hasFocus) {
                android.util.Log.d("SeriesInfoScreen", "🎯 Favorite button focused")
                favoriteButton.background = getDrawable(R.drawable.button_focused_background)
            } else {
                favoriteButton.background = getDrawable(R.drawable.button_primary_background)
            }
        }
        
        // Initialize play all button
        val playAllButton = findViewById<Button>(R.id.playAllButton)
        playAllButton.setOnClickListener {
            playAllEpisodes()
        }
        
        // Make play all button focusable for TV remote
        playAllButton.isFocusable = true
        playAllButton.isFocusableInTouchMode = true
        
        // Add focus change listener for visual feedback
        playAllButton.setOnFocusChangeListener { _, hasFocus ->
            isPlayAllButtonFocused = hasFocus
            if (hasFocus) {
                android.util.Log.d("SeriesInfoScreen", "🎯 Play All button focused")
                playAllButton.background = getDrawable(R.drawable.button_focused_background)
            } else {
                playAllButton.background = getDrawable(R.drawable.button_primary_background)
            }
        }
        
        // Set initial focus to season panel
        seasonListView.requestFocus()
        isInSeasonPanel = true
        
        // Update panel titles based on content type
        if (contentType == "movies") {
            findViewById<TextView>(R.id.seasonsTitleText).text = "Movie Details"
            findViewById<TextView>(R.id.episodesTitleText).text = "Movie Info"
            loadingText.text = "Loading movies..."
        }
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
                // Check if activity is still valid
                if (isFinishing || isDestroyed || backdropUrls.isEmpty()) return
                
                try {
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
                    
                    // Schedule next animation only if activity is still valid
                    if (!isFinishing && !isDestroyed) {
                        backdropImageView.postDelayed(this, backdropAnimationDuration)
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SeriesInfoScreen", "Error in backdrop animation: ${e.message}")
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
        if (contentType == "movies") {
            // For movies, just show total count
            val countText = "Total Movies: ${episodes.size}"
            seriesCountView.text = countText
        } else {
            // For series, show seasons and episodes
            val totalSeasons = episodes.map { it.season }.distinct().size
            val totalEpisodes = episodes.size
            val countText = "Seasons: $totalSeasons | Episodes: $totalEpisodes"
            seriesCountView.text = countText
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val additionalInfo = "Panel: ${if (isInSeasonPanel) "Season" else "Episode"}, " +
                           "SeasonIndex: $selectedSeasonIndex, " +
                           "EpisodeIndex: $selectedEpisodeIndex"
        
        KeyEventLogger.logKeyEvent("SeriesInfoScreen", event, additionalInfo)
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (isFavoriteButtonFocused) {
                        // Move to season panel (left side)
                        moveToSeasonPanel()
                        return true
                    } else if (isPlayAllButtonFocused) {
                        // Move to favorite button
                        moveToFavoriteButton()
                        return true
                    } else if (!isInSeasonPanel) {
                        isInSeasonPanel = true
                        seasonListView.requestFocus()
                        android.util.Log.d("SeriesInfoScreen", "DPAD_LEFT: Moved to season panel")
                        return true
                    } else {
                        // In season panel - navigate left within seasons
                        navigateSeasonUp()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isFavoriteButtonFocused) {
                        // Move to play all button
                        moveToPlayAllButton()
                        return true
                    } else if (isPlayAllButtonFocused) {
                        // Move to episode panel (right side)
                        isInSeasonPanel = false
                        episodeRecyclerView.requestFocus()
                        KeyEventLogger.logNavigation("SeriesInfoScreen", "RIGHT", "PlayAll", "Episode")
                        // Ensure episode focus is properly set
                        episodeRecyclerView.postDelayed({
                            updateEpisodeFocus()
                        }, 100)
                        return true
                    } else if (isInSeasonPanel) {
                        isInSeasonPanel = false
                        episodeRecyclerView.requestFocus()
                        KeyEventLogger.logNavigation("SeriesInfoScreen", "RIGHT", "Season", "Episode")
                        // Ensure episode focus is properly set
                        episodeRecyclerView.postDelayed({
                            updateEpisodeFocus()
                        }, 100)
                        return true
                    } else {
                        // In episode panel - navigate right within episodes
                        navigateEpisodeDown()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (isFavoriteButtonFocused) {
                        // Move to last season or last episode
                        if (isInSeasonPanel) {
                            val seasonsCount = seasonAdapter.count
                            selectedSeasonIndex = seasonsCount - 1
                            updateSeasonSelection()
                            moveToSeasonPanel()
                        } else {
                            val episodesCount = episodeAdapter.itemCount
                            selectedEpisodeIndex = episodesCount - 1
                            updateEpisodeFocus()
                        }
                        return true
                    } else if (isPlayAllButtonFocused) {
                        // Move to favorite button
                        moveToFavoriteButton()
                        return true
                    } else {
                        KeyEventLogger.logNavigation("SeriesInfoScreen", "UP", if (isInSeasonPanel) "Season" else "Episode")
                        if (isInSeasonPanel) {
                            navigateSeasonUp()
                        } else {
                            navigateEpisodeUp()
                        }
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (isFavoriteButtonFocused) {
                        // Move to play all button
                        moveToPlayAllButton()
                        return true
                    } else if (isPlayAllButtonFocused) {
                        // Stay on play all button (or could move to episodes if needed)
                        return true
                    } else if (isInSeasonPanel) {
                        // Check if we're at the last season
                        val seasonsCount = seasonAdapter.count
                        if (selectedSeasonIndex >= seasonsCount - 1) {
                            // Move to favorite button from last season
                            moveToFavoriteButton()
                            return true
                        } else {
                            navigateSeasonDown()
                        }
                    } else {
                        // In episode panel - check if we're at the last episode
                        val episodesCount = episodeAdapter.itemCount
                        if (selectedEpisodeIndex >= episodesCount - 1) {
                            // Move to favorite button from last episode
                            moveToFavoriteButton()
                            return true
                        } else {
                            navigateEpisodeDown()
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isFavoriteButtonFocused) {
                        android.util.Log.d("SeriesInfoScreen", "✅ Favorite button selected")
                        showFavoriteDialog()
                        return true
                    } else if (isPlayAllButtonFocused) {
                        android.util.Log.d("SeriesInfoScreen", "✅ Play All button selected")
                        playAllEpisodes()
                        return true
                    } else if (isInSeasonPanel) {
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
    
    private fun moveToFavoriteButton() {
        isFavoriteButtonFocused = true
        isPlayAllButtonFocused = false
        findViewById<ImageButton>(R.id.favoriteButton).requestFocus()
        android.util.Log.d("SeriesInfoScreen", "🎯 Moved focus to Favorite button")
    }
    
    private fun moveToPlayAllButton() {
        isFavoriteButtonFocused = false
        isPlayAllButtonFocused = true
        findViewById<Button>(R.id.playAllButton).requestFocus()
        android.util.Log.d("SeriesInfoScreen", "🎯 Moved focus to Play All button")
    }
    
    private fun moveToSeasonPanel() {
        isFavoriteButtonFocused = false
        isPlayAllButtonFocused = false
        seasonListView.requestFocus()
        android.util.Log.d("SeriesInfoScreen", "🎯 Moved focus to Season panel")
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
                val info = repository.getInfo(seriesId, contentType)
                if (info != null) {
                    updateSeriesInfo(info)
                }
                
                // Load episodes
                repository.loadEpisodesWithSync(contentType, seriesId).collectLatest { result ->
                    result.fold(
                        onSuccess = { episodes ->
                            loadingText.visibility = View.GONE

                            // Update seasons/episodes count
                            updateSeasonsEpisodesCount(episodes)

                            if (contentType == "movies") {
                                // For movies, treat each movie as a "season" for UI consistency
                                val movieMap = episodes.groupBy { it.season }
                                val movieSeasons = movieMap.keys.sorted().map { "Movie $it" }

                                seasonAdapter = SeasonsAdapter(this@SeriesInfoScreen, movieSeasons)
                                seasonListView.adapter = seasonAdapter

                                // Auto-load first movie if available
                                if (movieSeasons.isNotEmpty()) {
                                    loadEpisodesForSeason(movieMap.keys.sorted().first(), movieMap)
                                }
                            } else {
                                // For series, group by season
                                val seasonMap = episodes.groupBy { it.season }
                                val seasons = seasonMap.keys.sorted().map { "Season $it" }

                                seasonAdapter = SeasonsAdapter(this@SeriesInfoScreen, seasons)
                                seasonListView.adapter = seasonAdapter

                                // Auto-load first season if available
                                if (seasons.isNotEmpty()) {
                                    loadEpisodesForSeason(seasonMap.keys.sorted().first(), seasonMap)
                                }
                            }
                        },
                        onFailure = { e ->
                            loadingText.visibility = View.GONE
                            val contentTypeText = if (contentType == "movies") "movies" else "episodes"
                        errorText.text = "Failed to load $contentTypeText: ${e.message}"
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
        val contentTypeText = if (contentType == "movies") "MOVIE" else "EPISODES FOR SEASON"
        android.util.Log.d("SeriesInfoScreen", "=== LOADING $contentTypeText ===")
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
                            val contentTypeText = if (contentType == "movies") "movies" else "episodes"
                            android.util.Log.w("SeriesInfoScreen", "⚠️ No $contentTypeText found for season $seasonNumber")
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
        val contentTypeText = if (contentType == "movies") "MOVIE" else "EPISODE"
        android.util.Log.d("SeriesInfoScreen", "=== PLAYING $contentTypeText ===")
        android.util.Log.d("SeriesInfoScreen", "${contentTypeText.capitalize()} Title: ${episode.title}")
        android.util.Log.d("SeriesInfoScreen", "${contentTypeText.capitalize()} Season: ${episode.season}")
        android.util.Log.d("SeriesInfoScreen", "${contentTypeText.capitalize()} DirectSource: ${episode.directSource}")
        android.util.Log.d("SeriesInfoScreen", "${if (contentType == "movies") "Movie" else "Series"} Name: $seriesName")
        android.util.Log.d("SeriesInfoScreen", "${if (contentType == "movies") "Movie" else "Series"} ID: $seriesId")
        
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
                
                // Log all episodes in the season
                currentSeasonEpisodes.forEachIndexed { index, ep ->
                    android.util.Log.d("SeriesInfoScreen", "Season episode $index: ${ep.title} (ID: ${ep.id})")
                }
                
                // Try MX Player playlist first, fallback to individual episode
                // Note: episodeIndex will be calculated inside playSeasonPlaylist after filtering
                if (playSeasonPlaylist(currentSeasonEpisodes, episode)) {
                    android.util.Log.d("SeriesInfoScreen", "✅ MX Player playlist launched successfully")
                } else {
                    android.util.Log.d("SeriesInfoScreen", "⚠️ MX Player not available, using fallback player")
                    
                    // Calculate episode index for fallback player
                    val episodeIndex = currentSeasonEpisodes.indexOfFirst { it.id == episode.id }
                    
                    val intent = Intent(this@SeriesInfoScreen, VideoPlayerActivity::class.java)
                    intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, episode.directSource)
                    intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "${seriesName} - ${episode.title}")
                    intent.putExtra(VideoPlayerActivity.EXTRA_SERIES_ID, seriesId)
                    intent.putExtra(VideoPlayerActivity.EXTRA_MOVIE_ID, episode.id.toString()) // Add episode ID
                    intent.putExtra(VideoPlayerActivity.EXTRA_SEASON_NUMBER, episode.season)
                    intent.putExtra(VideoPlayerActivity.EXTRA_EPISODE_INDEX, episodeIndex)
                    intent.putExtra("content_type", contentType)
                    
                    android.util.Log.d("SeriesInfoScreen", "Launching VideoPlayerActivity with:")
                    android.util.Log.d("SeriesInfoScreen", "  - Video URL: ${episode.directSource}")
                    android.util.Log.d("SeriesInfoScreen", "  - Series ID: $seriesId")
                    android.util.Log.d("SeriesInfoScreen", "  - Season Number: ${episode.season}")
                    android.util.Log.d("SeriesInfoScreen", "  - Episode Index: $episodeIndex")
                    
                    startActivity(intent)
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "Error getting episode index", e)
                e.printStackTrace()
                
                // Fallback: launch without navigation data
                val intent = Intent(this@SeriesInfoScreen, VideoPlayerActivity::class.java)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_URL, episode.directSource)
                intent.putExtra(VideoPlayerActivity.EXTRA_VIDEO_TITLE, "${seriesName} - ${episode.title}")
                intent.putExtra(VideoPlayerActivity.EXTRA_SERIES_ID, seriesId)
                intent.putExtra(VideoPlayerActivity.EXTRA_MOVIE_ID, episode.id.toString()) // Add episode ID
                intent.putExtra("content_type", contentType)
                startActivity(intent)
            }
        }
    }
    
    /**
     * Play entire season as playlist in MX Player with correct data types
     */
    private fun playSeasonPlaylist(episodes: List<EpisodeEntity>, startEpisode: EpisodeEntity): Boolean {
        android.util.Log.d("SeriesInfoScreen", "🎬 Attempting to launch season playlist in MX Player...")
        android.util.Log.d("SeriesInfoScreen", "Episodes count: ${episodes.size}")
        android.util.Log.d("SeriesInfoScreen", "Start episode: ${startEpisode.title}")
        
        // Check if MX Player is available
        val mxPlayerIntegration = MXPlayerIntegration(this, object : MXPlayerIntegration.MXPlayerListener {
            override fun onMXPlayerLaunchSuccess() {
                android.util.Log.d("SeriesInfoScreen", "✅ MX Player playlist launched successfully")
            }
            
            override fun onMXPlayerLaunchFailed(error: String) {
                android.util.Log.e("SeriesInfoScreen", "❌ MX Player playlist launch failed: $error")
            }
            
            override fun onMXPlayerNotInstalled() {
                android.util.Log.w("SeriesInfoScreen", "⚠️ MX Player not installed")
            }
        })
        
        if (!mxPlayerIntegration.isMXPlayerInstalled()) {
            android.util.Log.w("SeriesInfoScreen", "⚠️ MX Player not installed, cannot launch playlist")
            return false
        }
        
        // Prepare video URLs and names, filtering out episodes with empty URLs
        val validEpisodes = episodes.filter { episode ->
            if (episode.directSource.isNullOrEmpty()) {
                android.util.Log.w("SeriesInfoScreen", "⚠️ Skipping episode with empty URL: ${episode.title}")
                false
            } else {
                true
            }
        }
        
        val videoUrls = validEpisodes.map { it.directSource!! }
        val videoNames = validEpisodes.map { it.title ?: "Episode ${it.episodeNum}" }
        
        if (videoUrls.isEmpty()) {
            android.util.Log.e("SeriesInfoScreen", "❌ No valid video URLs found for playlist")
            return false
        }
        
        android.util.Log.d("SeriesInfoScreen", "Valid episodes for playlist: ${videoUrls.size}")
        
        // Calculate the correct start index after filtering
        val startIndex = validEpisodes.indexOfFirst { it.id == startEpisode.id }
        if (startIndex == -1) {
            android.util.Log.e("SeriesInfoScreen", "❌ Start episode not found in valid episodes list")
            return false
        }
        
        android.util.Log.d("SeriesInfoScreen", "🎯 Corrected start index: $startIndex (after filtering)")
        
        // 🔹 Add to watch history BEFORE launching MX Player playlist
        addToWatchHistoryForPlaylist(validEpisodes, startIndex)
        
        // Launch season playlist using the working method with correct data types
        val success = mxPlayerIntegration.launchSeasonPlaylist(
            videoUrls = videoUrls,
            episodeNames = videoNames,
            title = "${seriesName} - Season ${episodes.firstOrNull()?.season ?: 1}",
            startIndex = startIndex.coerceIn(0, videoUrls.size - 1),
            decodeMode = MXPlayerIntegration.DECODE_MODE_AUTO
        )
        
        if (success) {
            android.util.Log.d("SeriesInfoScreen", "✅ Season playlist launched in MX Player")
            android.widget.Toast.makeText(this, "Playing season in MX Player!", android.widget.Toast.LENGTH_SHORT).show()
        } else {
            android.util.Log.e("SeriesInfoScreen", "❌ Failed to launch season playlist")
        }
        
        return success
    }
    
    /**
     * Add watch history for playlist episodes
     */
    private fun addToWatchHistoryForPlaylist(episodes: List<EpisodeEntity>, startIndex: Int) {
        lifecycleScope.launch {
            try {
                val startEpisode = episodes.getOrNull(startIndex) ?: return@launch
                
                // Add the starting episode to watch history
                watchHistoryRepository.addToHistory(
                    contentId = startEpisode.id.toString(),
                    contentType = "episode",
                    title = startEpisode.title ?: "Episode ${startEpisode.episodeNum}",
                    cover = null,
                    streamUrl = startEpisode.directSource,
                    categoryId = null,
                    categoryName = null,
                    seriesId = seriesId,
                    seasonNumber = startEpisode.season,
                    episodeNumber = startEpisode.episodeNum,
                    watchDuration = 0L,
                    totalDuration = 0L,
                    watchPercentage = 0f,
                    isCompleted = false,
                    resumePosition = 0L
                )
                
                android.util.Log.d("SeriesInfoScreen", "✅ Added episode to watch history: ${startEpisode.title}")
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Failed to add episode to watch history", e)
            }
        }
    }
    
    private fun showFavoriteDialog() {
        lifecycleScope.launch {
            try {
                android.util.Log.d("SeriesInfoScreen", "🔍 Getting playlists for favorite dialog...")
                
                // Ensure default playlists are created first
                favoritePlaylistRepository.createDefaultPlaylistsIfNeeded()
                
                val playlists = favoritePlaylistRepository.getAllPlaylistsSync()
                android.util.Log.d("SeriesInfoScreen", "📋 Found ${playlists.size} playlists: ${playlists.map { it.name }}")
                
                runOnUiThread {
                    showCustomFavoriteDialog(playlists)
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Failed to show favorite dialog", e)
                e.printStackTrace()
            }
        }
    }
    
    private fun showCustomFavoriteDialog(playlists: List<FavoritePlaylistEntity>) {
        android.util.Log.d("SeriesInfoScreen", "🎨 Creating custom favorite dialog...")
        
        // Create custom dialog
        val dialog = android.app.Dialog(this)
        dialog.setContentView(R.layout.dialog_favorite_playlist)
        dialog.setCancelable(true)
        
        // Get dialog views
        val dialogTitle = dialog.findViewById<android.widget.TextView>(R.id.dialogTitle)
        val dialogMessage = dialog.findViewById<android.widget.TextView>(R.id.dialogMessage)
        val createNewPlaylistButton = dialog.findViewById<android.widget.Button>(R.id.createNewPlaylistButton)
        val playlistRecyclerView = dialog.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.playlistRecyclerView)
        val cancelButton = dialog.findViewById<android.widget.Button>(R.id.cancelButton)
        
        // Set dialog content
        dialogTitle.text = "Add to Favorites"
        dialogMessage.text = "Choose a playlist to add '${seriesName}' to:"
        
        // Setup create new playlist button
        createNewPlaylistButton.setOnClickListener {
            android.util.Log.d("SeriesInfoScreen", "➕ Create new playlist button clicked")
            dialog.dismiss()
            showCreatePlaylistDialog()
        }
        
        // Setup playlist RecyclerView
        playlistRecyclerView.layoutManager = androidx.recyclerview.widget.LinearLayoutManager(this)
        val adapter = PlaylistDialogAdapter(playlists) { playlist ->
            android.util.Log.d("SeriesInfoScreen", "📝 Adding to playlist: ${playlist.name}")
            dialog.dismiss()
            addToPlaylist(playlist.id)
        }
        playlistRecyclerView.adapter = adapter
        
        // Setup cancel button
        cancelButton.setOnClickListener {
            android.util.Log.d("SeriesInfoScreen", "❌ Dialog cancelled")
            dialog.dismiss()
        }
        
        // Setup TV remote navigation
        setupDialogNavigation(dialog, createNewPlaylistButton, playlistRecyclerView, cancelButton)
        
        // Show dialog
        dialog.show()
        
        // Set initial focus
        createNewPlaylistButton.requestFocus()
        
        android.util.Log.d("SeriesInfoScreen", "✅ Custom dialog created and shown with ${playlists.size} playlists")
    }
    
    private fun setupDialogNavigation(
        dialog: android.app.Dialog,
        createButton: android.widget.Button,
        recyclerView: androidx.recyclerview.widget.RecyclerView,
        cancelButton: android.widget.Button
    ) {
        // Handle key events for TV remote navigation
        dialog.setOnKeyListener { _, keyCode, event ->
            if (event.action == android.view.KeyEvent.ACTION_DOWN) {
                when (keyCode) {
                    android.view.KeyEvent.KEYCODE_DPAD_DOWN -> {
                        if (createButton.hasFocus()) {
                            recyclerView.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_DPAD_UP -> {
                        if (recyclerView.hasFocus()) {
                            createButton.requestFocus()
                            return@setOnKeyListener true
                        }
                    }
                    android.view.KeyEvent.KEYCODE_BACK -> {
                        dialog.dismiss()
                        return@setOnKeyListener true
                    }
                }
            }
            false
        }
    }
    
    private fun showCreatePlaylistDialog() {
        val input = android.widget.EditText(this).apply {
            hint = "Enter playlist name"
            setPadding(32, 16, 32, 16)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Create New Playlist")
            .setMessage("Enter a name for your new playlist:")
            .setView(input)
            .setPositiveButton("Create") { _, _ ->
                val name = input.text.toString().trim()
                if (name.isNotEmpty()) {
                    createPlaylistAndAdd(name)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    private fun createPlaylistAndAdd(name: String) {
        lifecycleScope.launch {
            try {
                val playlist = favoritePlaylistRepository.createPlaylist(name)
                addToPlaylist(playlist.id)
                android.util.Log.d("SeriesInfoScreen", "✅ Created playlist and added series: $name")
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Failed to create playlist", e)
            }
        }
    }
    
    private fun addToPlaylist(playlistId: String) {
        lifecycleScope.launch {
            try {
                val success = favoritePlaylistRepository.addItemToPlaylist(
                    playlistId = playlistId,
                    contentId = seriesId,
                    contentType = "series",
                    title = seriesName,
                    cover = null,
                    streamUrl = null,
                    seriesId = seriesId,
                    seasonNumber = null,
                    episodeNumber = null
                )
                
                if (success) {
                    android.util.Log.d("SeriesInfoScreen", "✅ Added series to playlist: $seriesName")
                    // Show success message and change heart color
                    runOnUiThread {
                        android.widget.Toast.makeText(this@SeriesInfoScreen, "Added to favorites!", android.widget.Toast.LENGTH_SHORT).show()
                        updateFavoriteButtonState(true)
                    }
                } else {
                    android.util.Log.d("SeriesInfoScreen", "⚠️ Series already in playlist")
                    runOnUiThread {
                        android.widget.Toast.makeText(this@SeriesInfoScreen, "Already in favorites!", android.widget.Toast.LENGTH_SHORT).show()
                        updateFavoriteButtonState(true)
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Failed to add to playlist", e)
            }
        }
    }
    
    private fun updateFavoriteButtonState(isInFavorites: Boolean) {
        val favoriteButton = findViewById<ImageButton>(R.id.favoriteButton)
        if (isInFavorites) {
            // Change to filled heart (red)
            favoriteButton.setImageResource(R.drawable.ic_favorite_filled)
            favoriteButton.setColorFilter(getColor(R.color.red))
        } else {
            // Change to border heart (white)
            favoriteButton.setImageResource(R.drawable.ic_favorite_border)
            favoriteButton.setColorFilter(getColor(R.color.white))
        }
    }
    
    private fun playAllEpisodes() {
        lifecycleScope.launch {
            try {
                val episodes = database.episodeDao().getEpisodesSync(seriesId)
                if (episodes.isNotEmpty()) {
                    // For "Play All", start with the first episode
                    playSeasonPlaylist(episodes, episodes.first())
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesInfoScreen", "❌ Failed to play all episodes", e)
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Cleanup resources
        lifecycleScope.cancel()
        
        // Stop backdrop animation
        try {
            backdropImageView.clearAnimation()
            backdropImageView.removeCallbacks(null)
        } catch (e: Exception) {
            android.util.Log.e("SeriesInfoScreen", "Error cleaning up backdrop animation: ${e.message}")
        }
    }
}

