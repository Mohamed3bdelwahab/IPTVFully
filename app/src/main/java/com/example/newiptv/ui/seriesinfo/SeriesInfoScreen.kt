package com.example.newiptv.ui.seriesinfo

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.widget.ListView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.EpisodeEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.player.VideoPlayerActivity
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class SeriesInfoScreen : AppCompatActivity() {

    private lateinit var seasonListView: ListView
    private lateinit var episodeRecyclerView: RecyclerView
    private lateinit var seasonAdapter: SeasonsAdapter
    private lateinit var episodeAdapter: EpisodesAdapter
    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView

    private lateinit var repository: TvRepository
    private var seriesId: String = ""
    private var seriesName: String = ""

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
        seasonListView = findViewById(R.id.seasonsListView)
        episodeRecyclerView = findViewById(R.id.episodesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
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
        
        // Add season selection listener for TV remote navigation
        seasonListView.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("SeriesInfoScreen", "=== ON SEASON SELECTED CALLED ===")
                android.util.Log.d("SeriesInfoScreen", "Season Position: $position")
                
                val seasonNumber = position + 1 // Assuming seasons start from 1
                android.util.Log.d("SeriesInfoScreen", "Season Number: $seasonNumber")
                android.util.Log.d("SeriesInfoScreen", "Series ID: $seriesId")
                
                // Always load episodes when season selection changes
                loadEpisodesForSeason(seasonNumber)
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                android.util.Log.d("SeriesInfoScreen", "=== NO SEASON SELECTED ===")
            }
        })

        episodeRecyclerView.apply {
            layoutManager = LinearLayoutManager(this@SeriesInfoScreen)
            adapter = episodeAdapter
        }
    }

    private fun loadSeasonsAndEpisodes() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            try {
                repository.loadEpisodesWithSync("series", seriesId).collectLatest { result ->
                    result.fold(
                        onSuccess = { episodes ->
                            loadingText.visibility = View.GONE

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

