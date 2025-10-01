package com.example.newiptv.ui.movies

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.CategoryEntity
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.data.repository.WatchHistoryRepository
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.series.CategoryAdapter
import com.example.newiptv.ui.series.SeriesAdapter
import com.example.newiptv.ui.filter.FilterManager
import com.example.newiptv.ui.filter.FilterTVRemoteHandler
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class MoviesScreen : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var moviesRecyclerView: RecyclerView
    private lateinit var moviesAdapter: SeriesAdapter

    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    
    // New filter system
    private lateinit var filterManager: FilterManager
    private lateinit var filterTVRemoteHandler: FilterTVRemoteHandler
    private lateinit var genreFilterSpinner: Spinner
    private lateinit var yearFilterSpinner: Spinner
    private lateinit var ratingFilterSpinner: Spinner
    private lateinit var generalFilterSpinner: Spinner
    private lateinit var searchEditText: EditText
    private lateinit var clearFiltersButton: Button
    private lateinit var filterRow: LinearLayout

    private var selectedCategoryIndex = 0
    private var selectedMovieIndex = 0
    private var isInCategoryPanel = true

    private lateinit var repository: TvRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private var categories: List<CategoryEntity> = emptyList()
    private var currentMovies: List<ItemEntity> = emptyList()
    private var allMovies: List<ItemEntity> = emptyList() // Store all movies for filtering

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movies_screen)

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)
        watchHistoryRepository = WatchHistoryRepository(database)
        
        // Initialize filter manager
        filterManager = FilterManager(this)

        initializeViews()
        setupCategoryList()
        setupMoviesGrid()
        setupFilterSystem()
        setupSearch()
        setupTVRemoteNavigation()
        
        loadCategories()
    }

    private fun initializeViews() {
        categoryListView = findViewById(R.id.categoryListView)
        moviesRecyclerView = findViewById(R.id.moviesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        searchEditText = findViewById(R.id.searchEditText)
        
        // Initialize filter views
        filterRow = findViewById(R.id.filterRow)
        genreFilterSpinner = findViewById(R.id.genreFilterSpinner)
        yearFilterSpinner = findViewById(R.id.yearFilterSpinner)
        ratingFilterSpinner = findViewById(R.id.ratingFilterSpinner)
        generalFilterSpinner = findViewById(R.id.generalFilterSpinner)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)
        
        // Set initial focus to category panel
        categoryListView.requestFocus()
        isInCategoryPanel = true
        selectedCategoryIndex = 0
        selectedMovieIndex = 0
        
        // Add focus change listener to category list
        categoryListView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isInCategoryPanel = true
                KeyEventLogger.logFocusChange("MoviesScreen", "Category", selectedCategoryIndex)
            }
        }
        
        // Add focus change listener to movies grid
        moviesRecyclerView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isInCategoryPanel = false
                KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
            }
        }
    }

    private fun setupFilterSystem() {
        // Setup filter spinners with the filter manager
        filterManager.setupFilterSpinners(
            genreFilterSpinner,
            yearFilterSpinner,
            ratingFilterSpinner,
            generalFilterSpinner
        )
        
        // Setup search field with debounce to prevent too many filter updates
        var searchJob: kotlinx.coroutines.Job? = null
        searchEditText.addTextChangedListener(object : android.text.TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            override fun afterTextChanged(s: android.text.Editable?) {
                // Cancel previous search job
                searchJob?.cancel()
                
                // Start new search job with debounce
                searchJob = lifecycleScope.launch {
                    kotlinx.coroutines.delay(300) // 300ms debounce
                    filterManager.updateSearchQuery(s?.toString() ?: "")
                }
            }
        })
        
        // Setup clear filters button
        clearFiltersButton.setOnClickListener {
            filterManager.clearAllFilters()
            searchEditText.setText("")
            applyFilters()
        }
        
        // Initialize filter TV remote handler
        filterTVRemoteHandler = FilterTVRemoteHandler(
            genreFilterSpinner,
            yearFilterSpinner,
            ratingFilterSpinner,
            generalFilterSpinner,
            searchEditText,
            clearFiltersButton
        )
        
        // Observe filter state changes
        lifecycleScope.launch {
            filterManager.filterState.collect { filterState ->
                applyFilters()
            }
        }
    }

    private fun setupCategoryList() {
        categoryListView.setOnItemClickListener { _, _, position, _ ->
            selectedCategoryIndex = position
            selectCurrentCategory()
        }
    }

    private fun setupMoviesGrid() {
        // Create adapter without click listener initially
        moviesAdapter = SeriesAdapter { movie ->
            // This will be set up after categories are loaded
        }
        
        moviesRecyclerView.layoutManager = GridLayoutManager(this, 3)
        moviesRecyclerView.adapter = moviesAdapter
    }

    private fun setupSearch() {
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
                            performSearch(query)
                        }
                    }
                } else if (query.isEmpty()) {
                    // Clear search and show current category
                    if (categories.isNotEmpty()) {
                        loadMoviesForCategory(categories[selectedCategoryIndex].categoryId)
                    }
                }
            }
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                repository.searchContentByType("movie", query).collectLatest { results ->
                    allMovies = results
                    filterManager.updateFilterOptions(results)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    applyFilters()
                    selectedMovieIndex = 0
                    updateMoviesFocus()
                    
                    android.util.Log.d("MoviesScreen", "🔍 Search results: ${results.size} movies")
                    
                    // Log first few results for debugging
                    results.take(3).forEach { item ->
                        android.util.Log.d("MoviesScreen", "   Result: ${item.name} - ID: ${item.itemId}")
                    }
                    
                    if (results.isEmpty()) {
                        // Log some sample data to help debug
                        logSampleData()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("MoviesScreen", "❌ Search failed", e)
            }
        }
    }
    
    private fun logSampleData() {
        lifecycleScope.launch {
            try {
                val database = DatabaseProvider.getDatabase(this@MoviesScreen)
                val sampleMovies = database.itemDao().getAllItemsByTypeSync("movie").take(5)
                
                android.util.Log.d("MoviesScreen", "🔍 Sample Movies in Database:")
                sampleMovies.forEach { movie ->
                    android.util.Log.d("MoviesScreen", "   Movie: '${movie.name}' - ID: ${movie.itemId}")
                }
            } catch (e: Exception) {
                android.util.Log.e("MoviesScreen", "❌ Failed to log sample data", e)
            }
        }
    }

    private fun setupTVRemoteNavigation() {
        // TV remote navigation is handled in dispatchKeyEvent
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        // Check if search field has focus
        if (searchEditText.hasFocus()) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Move to filter UI
                        filterTVRemoteHandler.setFocusToElement(0)
                        android.util.Log.d("MoviesScreen", "DPAD_DOWN from search: Moved to filter UI")
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Move to category list
                        categoryListView.requestFocus()
                        android.util.Log.d("MoviesScreen", "DPAD_LEFT from search: Moved to category list")
                        return true
                    }
                }
            }
            return super.dispatchKeyEvent(event)
        }
        
        // Check if filter UI has focus first
        if (filterTVRemoteHandler.hasFilterFocus()) {
            if (filterTVRemoteHandler.handleKeyEvent(event)) {
                return true
            }
            // If filter focus was cleared, move back to category list
            if (!filterTVRemoteHandler.hasFilterFocus() && event.keyCode == KeyEvent.KEYCODE_DPAD_LEFT) {
                categoryListView.requestFocus()
                return true
            }
        }
        
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isInCategoryPanel && selectedMovieIndex % 3 == 0) {
                        isInCategoryPanel = true
                        categoryListView.requestFocus()
                        android.util.Log.d("MoviesScreen", "DPAD_LEFT: Moved to category panel")
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isInCategoryPanel) {
                        isInCategoryPanel = false
                        moviesRecyclerView.requestFocus()
                        KeyEventLogger.logNavigation("MoviesScreen", "RIGHT", "Category", "Movies")
                        // Ensure movies focus is properly set
                        moviesRecyclerView.postDelayed({
                            updateMoviesFocus()
                        }, 100)
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    KeyEventLogger.logNavigation("MoviesScreen", "UP", if (isInCategoryPanel) "Category" else "Movies")
                    if (isInCategoryPanel) {
                        // If at top of category list, move to search field first, then filter UI
                        if (selectedCategoryIndex == 0) {
                            searchEditText.requestFocus()
                            android.util.Log.d("MoviesScreen", "DPAD_UP: Moved to search field")
                            return true
                        } else {
                            navigateCategoryUp()
                        }
                    } else {
                        navigateMovieUp()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    KeyEventLogger.logNavigation("MoviesScreen", "DOWN", if (isInCategoryPanel) "Category" else "Movies")
                    if (isInCategoryPanel) {
                        navigateCategoryDown()
                    } else {
                        navigateMovieDown()
                    }
                    return true
                }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInCategoryPanel) {
                        KeyEventLogger.logItemSelection("MoviesScreen", "Category", selectedCategoryIndex, 
                            if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else "Unknown")
                        selectCurrentCategory()
                    } else {
                        // Get the actually focused movie from RecyclerView instead of using our counter
                        val focusedView = moviesRecyclerView.focusedChild
                        if (focusedView != null) {
                            val position = moviesRecyclerView.getChildAdapterPosition(focusedView)
                            if (position != RecyclerView.NO_POSITION) {
                                val selectedMovie = moviesAdapter.getSeriesAt(position)
                                KeyEventLogger.logItemSelection("MoviesScreen", "Movie", position, 
                                    selectedMovie?.name ?: "Unknown")
                                // Update our counter to match the actual focus
                                selectedMovieIndex = position
                                selectCurrentMovie()
                            } else {
                                // Fallback to our counter if position not found
                                val movie = moviesAdapter.getSeriesAt(selectedMovieIndex)
                                KeyEventLogger.logItemSelection("MoviesScreen", "Movie", selectedMovieIndex, 
                                    movie?.name ?: "Unknown")
                                selectCurrentMovie()
                            }
                        } else {
                            // Fallback to our counter if no focused child
                            val movie = moviesAdapter.getSeriesAt(selectedMovieIndex)
                            KeyEventLogger.logItemSelection("MoviesScreen", "Movie", selectedMovieIndex, 
                                movie?.name ?: "Unknown")
                            selectCurrentMovie()
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    android.util.Log.d("MoviesScreen", "BACK key pressed - finishing activity")
                    finish()
                    return true
                }
            }
        }
        
        return super.dispatchKeyEvent(event)
    }

    private fun selectCurrentCategory() {
        if (selectedCategoryIndex < categories.size) {
            val category = categories[selectedCategoryIndex]
            loadMoviesForCategory(category.categoryId)
            updateCategoryVisualSelection()
        }
    }

    private fun selectCurrentMovie() {
        val movie = moviesAdapter.getSeriesAt(selectedMovieIndex)
        if (movie != null) {
            // Launch MovieInfoScreen - same pattern as Series screen
            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
            val intent = Intent(this, MovieInfoScreen::class.java).apply {
                putExtra("movie_id", movie.itemId)
                putExtra("movie_name", movie.name)
                putExtra("movie_category", categoryName)
            }
            startActivity(intent)
        }
    }

    private fun navigateCategoryUp() {
        if (selectedCategoryIndex > 0) {
            selectedCategoryIndex--
            updateCategorySelection()
            ensureCorrectPanelFocus()
            KeyEventLogger.logFocusChange("MoviesScreen", "Category", selectedCategoryIndex)
        }
    }

    private fun navigateCategoryDown() {
        if (selectedCategoryIndex < categories.size - 1) {
            selectedCategoryIndex++
            updateCategorySelection()
            ensureCorrectPanelFocus()
            KeyEventLogger.logFocusChange("MoviesScreen", "Category", selectedCategoryIndex)
        }
    }

    private fun navigateMovieUp() {
        val spanCount = 3 // same as GridLayoutManager spanCount
        val currentPosition = getCurrentFocusedMoviePosition()
        if (currentPosition - spanCount >= 0) {
            selectedMovieIndex = currentPosition - spanCount
            updateMoviesFocus()
            ensureCorrectPanelFocus() // Ensure we stay in movies panel
            KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
        } else {
            // Already in top row
            KeyEventLogger.logError("MoviesScreen", "Cannot navigate UP", "Top row reached")
        }
    }

    private fun navigateMovieDown() {
        val spanCount = 3
        val totalMovies = moviesAdapter.itemCount
        val currentPosition = getCurrentFocusedMoviePosition()
        if (currentPosition + spanCount < totalMovies) {
            selectedMovieIndex = currentPosition + spanCount
            updateMoviesFocus()
            ensureCorrectPanelFocus() // Ensure we stay in movies panel
            KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
        } else {
            // Bottom row handling
            KeyEventLogger.logError("MoviesScreen", "Cannot navigate DOWN", "Bottom row reached")
        }
    }

    private fun updateCategorySelection() {
        categoryListView.setSelection(selectedCategoryIndex)
        updateCategoryVisualSelection()
    }

    private fun updateMoviesFocus() {
        try {
            // Use fast scroll instead of smooth scroll for better performance
            moviesRecyclerView.scrollToPosition(selectedMovieIndex)
            
            // Set focus immediately without delays
            val viewHolder = moviesRecyclerView.findViewHolderForAdapterPosition(selectedMovieIndex)
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus()
                KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
            }
        } catch (e: Exception) {
            KeyEventLogger.logError("MoviesScreen", "Error updating movies focus", e.message ?: "Unknown error")
        }
    }

    /**
     * Gets the currently focused movie position from RecyclerView
     * This ensures we always get the actual focus position, not our counter
     */
    private fun getCurrentFocusedMoviePosition(): Int {
        val focusedView = moviesRecyclerView.focusedChild
        if (focusedView != null) {
            val position = moviesRecyclerView.getChildAdapterPosition(focusedView)
            if (position != RecyclerView.NO_POSITION) {
                // Update our counter to match the actual focus
                selectedMovieIndex = position
                return position
            }
        }
        // Fallback to our counter if no focused child or position not found
        return selectedMovieIndex
    }

    private fun ensureCorrectPanelFocus() {
        if (isInCategoryPanel) {
            if (!categoryListView.hasFocus()) {
                categoryListView.requestFocus()
            }
        } else {
            if (!moviesRecyclerView.hasFocus()) {
                moviesRecyclerView.requestFocus()
            }
        }
    }

    private fun updateCategoryVisualSelection() {
        categoryListView.setSelection(selectedCategoryIndex)
    }
    
    private fun updateCategoryCount(categoryIndex: Int, count: Int) {
        val adapter = categoryListView.adapter as? CategoryAdapter
        adapter?.updateCategoryCount(categoryIndex, count)
    }

    private fun loadCategories() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

                lifecycleScope.launch {
            repository.loadCategoriesWithSync("movie").collectLatest { result ->
                result.fold(
                    onSuccess = { categoryList ->
                        // Add "All" category at the beginning with total count
                        val totalCount = repository.getTotalItemCountByType("movie")
                        val allCategory = CategoryEntity(
                            categoryId = "all",
                            categoryName = "All",
                            parentId = -1,
                            type = "movie"
                        )
                        
                        // Add "Recent Watched" category
                        val recentWatchedCategory = CategoryEntity(
                            categoryId = "recent_watched",
                            categoryName = "Recent Watched",
                            parentId = -1,
                            type = "movie"
                        )
                        
                        // Keep original category names without counts
                        val categoriesWithCounts = categoryList
                        
                        categories = listOf(allCategory, recentWatchedCategory) + categoriesWithCounts
                        val categoryAdapter = CategoryAdapter(this@MoviesScreen, categories)
                        categoryListView.adapter = categoryAdapter
                        
                        // Set counts for each category
                        categoryAdapter.updateCategoryCount(0, totalCount) // "All" category
                        categoryAdapter.updateCategoryCount(1, 0) // "Recent Watched" category (will be updated when loaded)
                        
                        // Set counts for regular categories
                        categoryList.forEachIndexed { index, category ->
                            val count = repository.getItemCountByCategory(category.categoryId, "movie")
                            categoryAdapter.updateCategoryCount(index + 2, count) // +2 because of "All" and "Recent Watched"
                        }
                        
                        // Set up the movies adapter click listener now that categories are loaded
                        moviesAdapter = SeriesAdapter { movie ->
                            // Same pattern as Series screen
                            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
                            val intent = Intent(this@MoviesScreen, MovieInfoScreen::class.java).apply {
                                putExtra("movie_id", movie.itemId)
                                putExtra("movie_name", movie.name)
                                putExtra("movie_category", categoryName)
                            }
                            startActivity(intent)
                        }
                        moviesRecyclerView.adapter = moviesAdapter
                        
                        loadingText.visibility = View.GONE

                        if (categoryList.isNotEmpty()) {
                            loadMoviesForCategory(categoryList[0].categoryId)
                        }
                    },
                    onFailure = { exception ->
                        loadingText.visibility = View.GONE
                        errorText.text = "Failed to load categories: ${exception.message}"
                        errorText.visibility = View.VISIBLE
                    }
                )
            }
        }
    }

    private fun loadMoviesForCategory(categoryId: String) {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        
        android.util.Log.d("MoviesScreen", "=== LOADING MOVIES FOR CATEGORY ===")
        android.util.Log.d("MoviesScreen", "Category ID: $categoryId")
        android.util.Log.d("MoviesScreen", "Selected Category Index: $selectedCategoryIndex")

        lifecycleScope.launch {
            if (categoryId == "all") {
                // Load ALL movies
                try {
                    val database = DatabaseProvider.getDatabase(this@MoviesScreen)
                    val allItems = database.itemDao().getAllItemsByTypeSync("movie")
                    
                    allMovies = allItems
                    android.util.Log.d("MoviesScreen", "=== ALL MOVIES LOADED FROM DATABASE ===")
                    android.util.Log.d("MoviesScreen", "Total Movies Loaded: ${allItems.size}")
                    
                    // Log first few items to see their data
                    allItems.take(3).forEach { item ->
                        android.util.Log.d("MoviesScreen", "Movie: ${item.name}, ID: ${item.itemId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(allItems)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded movies
                    applyFilters()
                    selectedMovieIndex = 0
                    loadingText.visibility = View.GONE
                    
                } catch (exception: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load all movies: ${exception.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("MoviesScreen", "=== FAILED TO LOAD ALL MOVIES ===")
                    android.util.Log.e("MoviesScreen", "Error: ${exception.message}")
                    exception.printStackTrace()
                }
            } else if (categoryId == "recent_watched") {
                // Load recent watched content
                try {
                    val recentHistory = watchHistoryRepository.getRecentHistoryByType("movie", 20)
                    val moviesList = recentHistory.map { history ->
                        watchHistoryRepository.convertToItemEntity(history)
                    }
                    
                    allMovies = moviesList
                    android.util.Log.d("MoviesScreen", "=== RECENT WATCHED LOADED SUCCESSFULLY ===")
                    android.util.Log.d("MoviesScreen", "Total Recent Watched: ${moviesList.size}")
                    
                    // Log first few items to see their data
                    moviesList.take(3).forEach { item ->
                        android.util.Log.d("MoviesScreen", "Recent Watched: ${item.name}, ID: ${item.itemId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(moviesList)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded movies
                    applyFilters()
                    selectedMovieIndex = 0
                    loadingText.visibility = View.GONE
                    
                    // Update category count for current category
                    if (selectedCategoryIndex < categories.size) {
                        updateCategoryCount(selectedCategoryIndex, moviesList.size)
                    }
                } catch (e: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load recent watched: ${e.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("MoviesScreen", "=== FAILED TO LOAD RECENT WATCHED ===")
                    android.util.Log.e("MoviesScreen", "Error: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                // Load regular category content from pre-loaded database (NO API CALLS!)
                try {
                    val database = DatabaseProvider.getDatabase(this@MoviesScreen)
                    val moviesList = database.itemDao().getItemsByCategorySync(categoryId, "movie")
                    
                    allMovies = moviesList // Store all movies for filtering
                    android.util.Log.d("MoviesScreen", "=== MOVIES LOADED FROM DATABASE ===")
                    android.util.Log.d("MoviesScreen", "Category ID: $categoryId")
                    android.util.Log.d("MoviesScreen", "Total Movies Loaded: ${moviesList.size}")
                    
                    // Log first few items to see their data
                    moviesList.take(3).forEach { item ->
                        android.util.Log.d("MoviesScreen", "Movie: ${item.name}, ID: ${item.itemId}, Category: ${item.categoryId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(moviesList)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded movies
                    applyFilters()
                    selectedMovieIndex = 0
                    loadingText.visibility = View.GONE
                    
                    // Update category count for current category
                    if (selectedCategoryIndex < categories.size) {
                        updateCategoryCount(selectedCategoryIndex, moviesList.size)
                    }
                    
                } catch (exception: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load movies: ${exception.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("MoviesScreen", "=== FAILED TO LOAD MOVIES ===")
                    android.util.Log.e("MoviesScreen", "Category ID: $categoryId")
                    android.util.Log.e("MoviesScreen", "Error: ${exception.message}")
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun applyFilters() {
        if (allMovies.isNotEmpty()) {
            // Run filtering on background thread to avoid blocking UI
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val filteredMovies = filterManager.applyFilters(allMovies)
                    
                    // Update UI on main thread
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        moviesAdapter.updateSeries(filteredMovies)
                        selectedMovieIndex = 0
                        updateMoviesFocus()
                        android.util.Log.d("MoviesScreen", "Applied filters: ${filterManager.getFilterSummary()}, showing ${filteredMovies.size} movies")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("MoviesScreen", "Error applying filters", e)
                }
            }
        }
    }
}
