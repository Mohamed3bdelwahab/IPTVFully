package com.example.newiptv.ui.search

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import com.example.newiptv.ui.search.GlobalSearchAdapter
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * Global search screen that searches across ALL series and movies
 */
class GlobalSearchScreen : AppCompatActivity() {
    
    private lateinit var searchEditText: EditText
    private lateinit var resultsRecyclerView: RecyclerView
    private lateinit var resultsAdapter: GlobalSearchAdapter
    private lateinit var noResultsText: TextView
    private lateinit var searchHintText: TextView
    
    private lateinit var repository: TvRepository
    private var searchResults = mutableListOf<ItemEntity>()
    private var selectedIndex = 0
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_global_search)
        
        setupViews()
        setupRecyclerView()
        setupRepository()
        setupSearchListener()
        
        // Set initial focus to search box
        searchEditText.requestFocus()
    }
    
    private fun setupViews() {
        searchEditText = findViewById(R.id.searchEditText)
        resultsRecyclerView = findViewById(R.id.resultsRecyclerView)
        noResultsText = findViewById(R.id.noResultsText)
        searchHintText = findViewById(R.id.searchHintText)
        
        // Set hint text
        searchHintText.text = "Search across all series and movies..."
    }
    
    private fun setupRecyclerView() {
        resultsAdapter = GlobalSearchAdapter { item ->
            openItemDetails(item)
        }
        
        resultsRecyclerView.apply {
            layoutManager = GridLayoutManager(this@GlobalSearchScreen, 3)
            adapter = resultsAdapter
            isFocusable = true
            isFocusableInTouchMode = true
        }
    }
    
    private fun setupRepository() {
        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)
    }
    
    private fun setupSearchListener() {
        // Debounced search to prevent keyboard issues
        var searchJob: kotlinx.coroutines.Job? = null
        
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                val query = s?.toString()?.trim() ?: ""
                
                // Cancel previous search job
                searchJob?.cancel()
                
                if (query.length >= 2) {
                    // Start searching after 2 characters with delay to prevent keyboard issues
                    searchJob = lifecycleScope.launch {
                        kotlinx.coroutines.delay(300) // 300ms delay
                        if (query == searchEditText.text.toString().trim()) {
                            performSearch()
                        }
                    }
                } else if (query.isEmpty()) {
                    clearResults()
                }
            }
        })
        
        searchEditText.setOnEditorActionListener { _, _, _ ->
            performSearch()
            true
        }
    }
    
    private fun performSearch() {
        val query = searchEditText.text.toString().trim()
        
        if (query.isEmpty()) {
            clearResults()
            return
        }
        
        Log.d("GlobalSearch", "🔍 Searching for: '$query'")
        
        lifecycleScope.launch {
            try {
                repository.searchAllContent(query).collectLatest { results ->
                    searchResults.clear()
                    searchResults.addAll(results)
                    
                    Log.d("GlobalSearch", "📊 Found ${results.size} results")
                    
                    // Log search results breakdown
                    val seriesCount = results.count { it.type == "series" }
                    val moviesCount = results.count { it.type == "movie" }
                    Log.d("GlobalSearch", "📺 Series: $seriesCount, 🎬 Movies: $moviesCount")
                    
                    // Log first few results for debugging
                    results.take(5).forEach { item ->
                        Log.d("GlobalSearch", "   Result: ${item.name} (${item.type}) - ID: ${item.itemId}")
                    }
                    
                    if (results.isEmpty()) {
                        showNoResults()
                        // Log some sample data to help debug
                        logSampleData()
                    } else {
                        showResults()
                        resultsAdapter.updateResults(results)
                        selectedIndex = 0
                        updateFocus()
                    }
                }
            } catch (e: Exception) {
                Log.e("GlobalSearch", "❌ Search failed", e)
                showNoResults()
            }
        }
    }
    
    private fun logSampleData() {
        lifecycleScope.launch {
            try {
                // Get some sample data to help debug
                val database = DatabaseProvider.getDatabase(this@GlobalSearchScreen)
                val sampleMovies = database.itemDao().getAllItemsByTypeSync("movie").take(5)
                val sampleSeries = database.itemDao().getAllItemsByTypeSync("series").take(5)
                
                Log.d("GlobalSearch", "🔍 Sample Movies in Database:")
                sampleMovies.forEach { movie ->
                    Log.d("GlobalSearch", "   Movie: '${movie.name}' - ID: ${movie.itemId}")
                }
                
                Log.d("GlobalSearch", "🔍 Sample Series in Database:")
                sampleSeries.forEach { series ->
                    Log.d("GlobalSearch", "   Series: '${series.name}' - ID: ${series.itemId}")
                }
            } catch (e: Exception) {
                Log.e("GlobalSearch", "❌ Failed to log sample data", e)
            }
        }
    }
    
    private fun clearResults() {
        searchResults.clear()
        resultsAdapter.updateResults(emptyList())
        showSearchHint()
    }
    
    private fun showResults() {
        resultsRecyclerView.visibility = View.VISIBLE
        noResultsText.visibility = View.GONE
        searchHintText.visibility = View.GONE
    }
    
    private fun showNoResults() {
        resultsRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.VISIBLE
        searchHintText.visibility = View.GONE
        noResultsText.text = "No results found for '${searchEditText.text}'"
    }
    
    private fun showSearchHint() {
        resultsRecyclerView.visibility = View.GONE
        noResultsText.visibility = View.GONE
        searchHintText.visibility = View.VISIBLE
    }
    
    private fun updateFocus() {
        if (searchResults.isNotEmpty() && selectedIndex < searchResults.size) {
            resultsRecyclerView.smoothScrollToPosition(selectedIndex)
            resultsAdapter.setFocusedPosition(selectedIndex)
        }
    }
    
    private fun openItemDetails(item: ItemEntity) {
        Log.d("GlobalSearch", "🎬 Opening details for: ${item.name} (${item.type})")
        
        val intent = when (item.type) {
            "series" -> Intent(this, SeriesInfoScreen::class.java).apply {
                putExtra("seriesId", item.itemId)
                putExtra("seriesName", item.name)
            }
            "movie" -> Intent(this, MovieInfoScreen::class.java).apply {
                putExtra("movieId", item.itemId)
                putExtra("movieName", item.name)
            }
            else -> return
        }
        
        startActivity(intent)
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> {
                    if (searchResults.isNotEmpty()) {
                        selectedIndex = (selectedIndex - 3).coerceAtLeast(0)
                        updateFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if (searchResults.isNotEmpty()) {
                        selectedIndex = (selectedIndex + 3).coerceAtMost(searchResults.size - 1)
                        updateFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (searchResults.isNotEmpty() && selectedIndex > 0) {
                        selectedIndex--
                        updateFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (searchResults.isNotEmpty() && selectedIndex < searchResults.size - 1) {
                        selectedIndex++
                        updateFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (searchResults.isNotEmpty() && selectedIndex < searchResults.size) {
                        openItemDetails(searchResults[selectedIndex])
                        return true
                    }
                }
                KeyEvent.KEYCODE_BACK -> {
                    if (searchEditText.hasFocus()) {
                        finish()
                        return true
                    } else {
                        searchEditText.requestFocus()
                        return true
                    }
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }
}
