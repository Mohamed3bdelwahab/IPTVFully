package com.example.newiptv.ui.series

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.DatabaseProvider
import com.example.newiptv.data.db.entities.CategoryEntity
import com.example.newiptv.data.db.entities.ItemEntity
import com.example.newiptv.data.repository.TvRepository
import com.example.newiptv.data.repository.WatchHistoryRepository
import com.example.newiptv.ui.seriesinfo.SeriesInfoScreen
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Job
import com.example.newiptv.ui.series.SeriesAdapter
import com.example.newiptv.ui.series.CategoryAdapter
import com.example.newiptv.ui.filter.FilterManager
import com.example.newiptv.ui.filter.FilterTVRemoteHandler
import com.example.newiptv.utils.KeyEventLogger
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout

class SeriesScreen : AppCompatActivity() {

    // Content type - can be "series" or "movies"
    private var contentType: String = "series"
    private var screenTitle: String = "Series"
    
    private lateinit var categoryListView: ListView
    private lateinit var seriesRecyclerView: RecyclerView
    private lateinit var seriesAdapter: SeriesAdapter
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
    private var selectedSeriesIndex = 0
    private var isInCategoryPanel = true
    private var currentFocusIndex = 0

    private lateinit var repository: TvRepository
    private lateinit var watchHistoryRepository: WatchHistoryRepository
    private var categories: List<CategoryEntity> = emptyList()
    private var currentSeries: List<ItemEntity> = emptyList()
    private var allSeries: List<ItemEntity> = emptyList() // Store all series for filtering

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_series_screen)

        // Get content type from intent (default to "series")
        contentType = intent.getStringExtra("content_type") ?: "series"
        screenTitle = when (contentType) {
            "movies" -> "Movies"
            else -> "Series"
        }
        
        // Update screen title
        title = screenTitle

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)
        watchHistoryRepository = WatchHistoryRepository(database)
        
        // Initialize filter manager
        filterManager = FilterManager(this)

        initializeViews()
        setupCategoryList()
        setupSeriesGrid()
        setupFilterSystem()
        setupSearch()
        setupTVRemoteNavigation()
        
        // Log drawable resources for debugging
        logDrawableResources()
        
        loadCategories()
    }

    private fun initializeViews() {
        categoryListView = findViewById(R.id.categoryListView)
        seriesRecyclerView = findViewById(R.id.seriesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        searchEditText = findViewById(R.id.searchEditText)
        
        // Initialize filter views
        filterRow = findViewById(R.id.filterRow)
        genreFilterSpinner = findViewById(R.id.genreFilterSpinner)
        yearFilterSpinner = findViewById(R.id.yearFilterSpinner)
        ratingFilterSpinner = findViewById(R.id.ratingFilterSpinner)
        generalFilterSpinner = findViewById(R.id.generalFilterSpinner)
        searchEditText = findViewById(R.id.searchEditText)
        clearFiltersButton = findViewById(R.id.clearFiltersButton)
        
        // Update titles based on content type
        findViewById<TextView>(R.id.contentTitleText).text = screenTitle
        findViewById<TextView>(R.id.categoriesTitleText).text = "${screenTitle} Categories"
        
        // Set initial focus to category panel
        categoryListView.requestFocus()
        isInCategoryPanel = true
        selectedCategoryIndex = 0
        selectedSeriesIndex = 0
        
        // Add focus change listener to category list
        categoryListView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isInCategoryPanel = true
                KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
            }
        }
        
        // Ensure proper focus handling
        KeyEventLogger.logScreenEvent("SeriesScreen", "Views initialized")
        KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
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
        categoryListView.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                android.util.Log.d("SeriesScreen", "=== ON ITEM SELECTED CALLED ===")
                android.util.Log.d("SeriesScreen", "Position: $position, Selected Index: $selectedCategoryIndex")
                android.util.Log.d("SeriesScreen", "Categories size: ${categories.size}")
                
                if (position < categories.size) {
                    val category = categories[position]
                    android.util.Log.d("SeriesScreen", "Category: ${category.categoryName} (ID: ${category.categoryId})")
                    
                    // Only update selection index, don't load series automatically
                    if (position != selectedCategoryIndex) {
                        selectedCategoryIndex = position
                        android.util.Log.d("SeriesScreen", "=== CATEGORY FOCUS CHANGED (TV REMOTE) ===")
                        android.util.Log.d("SeriesScreen", "New Position: $position")
                        android.util.Log.d("SeriesScreen", "Category ID: ${category.categoryId}")

                        // ✅ Only update visual selection, don't load series yet
                        updateCategorySelection()
                    } else {
                        android.util.Log.d("SeriesScreen", "Same position, no change needed")
                    }
                } else {
                    android.util.Log.e("SeriesScreen", "Invalid position: $position, categories size: ${categories.size}")
                }
            }

            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                android.util.Log.d("SeriesScreen", "=== NOTHING SELECTED ===")
            }
                })
        
        // Add click listener as backup for mouse clicks
        categoryListView.setOnItemClickListener { _, _, position, _ ->
            android.util.Log.d("SeriesScreen", "=== CATEGORY CLICKED (MOUSE) ===")
            android.util.Log.d("SeriesScreen", "Position: $position")
            
            if (position < categories.size) {
                selectedCategoryIndex = position
                val category = categories[position]
                android.util.Log.d("SeriesScreen", "Category Name: ${category.categoryName}")
                android.util.Log.d("SeriesScreen", "Category ID: ${category.categoryId}")
                loadSeriesForCategory(category.categoryId)
                updateCategorySelection()
            }
        }
        

    }
    
    private fun applyFilters() {
        if (allSeries.isNotEmpty()) {
            // Run filtering on background thread to avoid blocking UI
            lifecycleScope.launch(kotlinx.coroutines.Dispatchers.Default) {
                try {
                    val filteredSeries = filterManager.applyFilters(allSeries)
                    
                    // Update UI on main thread
                    withContext(kotlinx.coroutines.Dispatchers.Main) {
                        seriesAdapter.updateSeries(filteredSeries)
                        android.util.Log.d("SeriesScreen", "Applied filters: ${filterManager.getFilterSummary()}, showing ${filteredSeries.size} series")
                    }
                } catch (e: Exception) {
                    android.util.Log.e("SeriesScreen", "Error applying filters", e)
                }
            }
        }
    }

    private fun setupSeriesGrid() {
        seriesAdapter = SeriesAdapter { series ->
            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
            val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                putExtra("series_name", series.name)
                putExtra("series_category", categoryName)
                putExtra("series_id", series.itemId)
                putExtra("content_type", contentType)
            }
            startActivity(intent)
        }

        seriesRecyclerView.apply {
            layoutManager = GridLayoutManager(this@SeriesScreen, 3)
            adapter = seriesAdapter
        }
        
        // Add focus change listener to RecyclerView to automatically update panel state
        seriesRecyclerView.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
            if (hasFocus) {
                isInCategoryPanel = false
                KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
            }
        }
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
                        loadSeriesForCategory(categories[selectedCategoryIndex].categoryId)
                    }
                }
            }
        })
    }

    private fun performSearch(query: String) {
        lifecycleScope.launch {
            try {
                repository.searchContentByType(contentType, query).collectLatest { results ->
                    allSeries = results
                    filterManager.updateFilterOptions(results)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    applyFilters()
                    selectedSeriesIndex = 0
                    updateSeriesFocus()
                    
                    android.util.Log.d("SeriesScreen", "🔍 Search results: ${results.size} ${contentType}")
                    
                    // Log first few results for debugging
                    results.take(3).forEach { item ->
                        android.util.Log.d("SeriesScreen", "   Result: ${item.name} - ID: ${item.itemId}")
                    }
                    
                    if (results.isEmpty()) {
                        // Log some sample data to help debug
                        logSampleData()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesScreen", "❌ Search failed", e)
            }
        }
    }
    
    private fun logSampleData() {
        lifecycleScope.launch {
            try {
                val database = DatabaseProvider.getDatabase(this@SeriesScreen)
                val sampleItems = database.itemDao().getAllItemsByTypeSync(contentType).take(5)
                
                android.util.Log.d("SeriesScreen", "🔍 Sample ${contentType} in Database:")
                sampleItems.forEach { item ->
                    android.util.Log.d("SeriesScreen", "   ${contentType.replaceFirstChar { it.uppercase() }}: '${item.name}' - ID: ${item.itemId}")
                }
            } catch (e: Exception) {
                android.util.Log.e("SeriesScreen", "❌ Failed to log sample data", e)
            }
        }
    }

    private fun setupTVRemoteNavigation() {
        categoryListView.requestFocus()
        isInCategoryPanel = true
    }
    
    /**
     * Ensures focus stays in the correct panel and updates panel state
     */
    private fun ensureCorrectPanelFocus() {
        if (isInCategoryPanel) {
            if (!categoryListView.hasFocus()) {
                categoryListView.requestFocus()
            }
        } else {
            if (!seriesRecyclerView.hasFocus()) {
                seriesRecyclerView.requestFocus()
            }
        }
    }
    
    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        val additionalInfo = "Panel: ${if (isInCategoryPanel) "Category" else "Series"}, " +
                           "CategoryIndex: $selectedCategoryIndex, " +
                           "SeriesIndex: $selectedSeriesIndex"
        
        KeyEventLogger.logKeyEvent("SeriesScreen", event, additionalInfo)
        
        // Check if search field has focus
        if (searchEditText.hasFocus()) {
            if (event.action == KeyEvent.ACTION_DOWN) {
                when (event.keyCode) {
                    KeyEvent.KEYCODE_DPAD_DOWN -> {
                        // Move to filter UI
                        filterTVRemoteHandler.setFocusToElement(0)
                        android.util.Log.d("SeriesScreen", "DPAD_DOWN from search: Moved to filter UI")
                        return true
                    }
                    KeyEvent.KEYCODE_DPAD_LEFT -> {
                        // Move to category list
                        categoryListView.requestFocus()
                        android.util.Log.d("SeriesScreen", "DPAD_LEFT from search: Moved to category list")
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
                    if (!isInCategoryPanel && selectedSeriesIndex % 3 == 0) {
                        isInCategoryPanel = true
                        categoryListView.requestFocus()
                        android.util.Log.d("SeriesScreen", "DPAD_LEFT: Moved to category panel")
                        return true
                    }
                }
                                        KeyEvent.KEYCODE_DPAD_RIGHT -> {
                            if (isInCategoryPanel) {
                                isInCategoryPanel = false
                                seriesRecyclerView.requestFocus()
                                KeyEventLogger.logNavigation("SeriesScreen", "RIGHT", "Category", "Series")
                                // Ensure series focus is properly set
                                seriesRecyclerView.postDelayed({
                                    updateSeriesFocus()
                                }, 100)
                                return true
                            }
                        }
                                        KeyEvent.KEYCODE_DPAD_UP -> {
                            KeyEventLogger.logNavigation("SeriesScreen", "UP", if (isInCategoryPanel) "Category" else "Series")
                            if (isInCategoryPanel) {
                                // If at top of category list, move to search field first, then filter UI
                                if (selectedCategoryIndex == 0) {
                                    searchEditText.requestFocus()
                                    android.util.Log.d("SeriesScreen", "DPAD_UP: Moved to search field")
                                    return true
                                } else {
                                    navigateCategoryUp()
                                }
                            } else {
                                navigateSeriesUp()
                            }
                            return true
                        }
                        KeyEvent.KEYCODE_DPAD_DOWN -> {
                            KeyEventLogger.logNavigation("SeriesScreen", "DOWN", if (isInCategoryPanel) "Category" else "Series")
                            if (isInCategoryPanel) {
                                navigateCategoryDown()
                            } else {
                                navigateSeriesDown()
                            }
                            return true
                        }
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    if (isInCategoryPanel) {
                        KeyEventLogger.logItemSelection("SeriesScreen", "Category", selectedCategoryIndex, 
                            if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else "Unknown")
                        selectCurrentCategory()
                    } else {
                        // Get the actually focused series from RecyclerView instead of using our counter
                        val focusedView = seriesRecyclerView.focusedChild
                        if (focusedView != null) {
                            val position = seriesRecyclerView.getChildAdapterPosition(focusedView)
                            if (position != RecyclerView.NO_POSITION) {
                                val selectedSeries = seriesAdapter.getSeriesAt(position)
                                KeyEventLogger.logItemSelection("SeriesScreen", "Series", position, 
                                    selectedSeries?.name ?: "Unknown")
                                // Update our counter to match the actual focus
                                selectedSeriesIndex = position
                                selectCurrentSeries()
                            } else {
                                // Fallback to our counter if position not found
                                val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
                                KeyEventLogger.logItemSelection("SeriesScreen", "Series", selectedSeriesIndex, 
                                    series?.name ?: "Unknown")
                                selectCurrentSeries()
                            }
                        } else {
                            // Fallback to our counter if no focused child
                            val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
                            KeyEventLogger.logItemSelection("SeriesScreen", "Series", selectedSeriesIndex, 
                                series?.name ?: "Unknown")
                            selectCurrentSeries()
                        }
                    }
                    return true
                }
                KeyEvent.KEYCODE_BACK -> {
                    android.util.Log.d("SeriesScreen", "BACK key pressed - finishing activity")
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

    private fun updateCategorySelection() {
        categoryListView.setSelection(selectedCategoryIndex)
        updateCategoryVisualSelection()
    }
    
    private fun navigateCategoryUp() {
        if (selectedCategoryIndex > 0) {
            selectedCategoryIndex--
            updateCategorySelection()
            ensureCorrectPanelFocus() // Ensure we stay in category panel
            KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
        }
    }
    
    private fun navigateCategoryDown() {
        if (selectedCategoryIndex < categories.size - 1) {
            selectedCategoryIndex++
            updateCategorySelection()
            ensureCorrectPanelFocus() // Ensure we stay in category panel
            KeyEventLogger.logFocusChange("SeriesScreen", "Category", selectedCategoryIndex)
        }
    }
    
    private fun navigateSeriesUp() {
        val spanCount = 3 // same as GridLayoutManager spanCount
        val currentPosition = getCurrentFocusedSeriesPosition()
        if (currentPosition - spanCount >= 0) {
            selectedSeriesIndex = currentPosition - spanCount
            updateSeriesFocus()
            ensureCorrectPanelFocus() // Ensure we stay in series panel
            KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
        } else {
            // Already in top row
            KeyEventLogger.logError("SeriesScreen", "Cannot navigate UP", "Top row reached")
        }
    }

    private fun navigateSeriesDown() {
        val spanCount = 3
        val totalSeries = seriesAdapter.itemCount
        val currentPosition = getCurrentFocusedSeriesPosition()
        if (currentPosition + spanCount < totalSeries) {
            selectedSeriesIndex = currentPosition + spanCount
            updateSeriesFocus()
            ensureCorrectPanelFocus() // Ensure we stay in series panel
            KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
        } else {
            // Bottom row handling
            KeyEventLogger.logError("SeriesScreen", "Cannot navigate DOWN", "Bottom row reached")
        }
    }

    
    private fun updateSeriesFocus() {
        try {
            // Use fast scroll instead of smooth scroll for better performance
            seriesRecyclerView.scrollToPosition(selectedSeriesIndex)
            
            // Set focus immediately without delays
            val viewHolder = seriesRecyclerView.findViewHolderForAdapterPosition(selectedSeriesIndex)
            if (viewHolder != null) {
                viewHolder.itemView.requestFocus()
                KeyEventLogger.logFocusChange("SeriesScreen", "Series", selectedSeriesIndex)
            }
        } catch (e: Exception) {
            KeyEventLogger.logError("SeriesScreen", "Error updating series focus", e.message ?: "Unknown error")
        }
    }
    
    private fun selectCurrentCategory() {
        if (selectedCategoryIndex < categories.size) {
            val category = categories[selectedCategoryIndex]
            KeyEventLogger.logDataLoading("SeriesScreen", "Series", "Category Selection")
            loadSeriesForCategory(category.categoryId)
        }
    }
    
    private fun selectCurrentSeries() {
        val series = seriesAdapter.getSeriesAt(selectedSeriesIndex)
        if (series != null) {
            android.util.Log.d("SeriesScreen", "Selecting series: ${series.name} at index: $selectedSeriesIndex")
            val categoryName = if (selectedCategoryIndex < categories.size) categories[selectedCategoryIndex].categoryName else ""
            val intent = Intent(this, SeriesInfoScreen::class.java).apply {
                putExtra("series_name", series.name)
                putExtra("series_category", categoryName)
                putExtra("series_id", series.itemId)
                putExtra("content_type", contentType)
            }
            startActivity(intent)
        }
    }

    private fun loadCategories() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

        lifecycleScope.launch {
            repository.loadCategoriesWithSync(contentType).collectLatest { result ->
                result.fold(
                    onSuccess = { categoryList ->
                        // Add "All" category at the beginning with total count
                        val totalCount = repository.getTotalItemCountByType(contentType)
                        val allCategory = CategoryEntity(
                            categoryId = "all",
                            categoryName = "All",
                            parentId = -1,
                            type = contentType
                        )
                        
                        // Add "Recent Watched" category
                        val recentWatchedCategory = CategoryEntity(
                            categoryId = "recent_watched",
                            categoryName = "Recent Watched",
                            parentId = -1,
                            type = contentType
                        )
                        
                        // Keep original category names without counts
                        val categoriesWithCounts = categoryList
                        
                        categories = listOf(allCategory, recentWatchedCategory) + categoriesWithCounts
                        val categoryAdapter = CategoryAdapter(this@SeriesScreen, categories)
                        categoryListView.adapter = categoryAdapter
                        
                        // Set counts for each category
                        categoryAdapter.updateCategoryCount(0, totalCount) // "All" category
                        categoryAdapter.updateCategoryCount(1, 0) // "Recent Watched" category (will be updated when loaded)
                        
                        // Set counts for regular categories
                        categoryList.forEachIndexed { index, category ->
                            val count = repository.getItemCountByCategory(category.categoryId, contentType)
                            categoryAdapter.updateCategoryCount(index + 2, count) // +2 because of "All" and "Recent Watched"
                        }
                        
                        loadingText.visibility = View.GONE

                        if (categories.isNotEmpty()) {
                            loadSeriesForCategory(categories[0].categoryId)
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

    private fun loadSeriesForCategory(categoryId: String) {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE
        
        android.util.Log.d("SeriesScreen", "=== LOADING ${screenTitle.uppercase()} FOR CATEGORY ===")
        android.util.Log.d("SeriesScreen", "Category ID: $categoryId")
        android.util.Log.d("SeriesScreen", "Selected Category Index: $selectedCategoryIndex")

        lifecycleScope.launch {
            if (categoryId == "all") {
                // Load ALL series/movies
                try {
                    val database = DatabaseProvider.getDatabase(this@SeriesScreen)
                    val allItems = database.itemDao().getAllItemsByTypeSync(contentType)
                    
                    allSeries = allItems
                    android.util.Log.d("SeriesScreen", "=== ALL ${screenTitle.uppercase()} LOADED FROM DATABASE ===")
                    android.util.Log.d("SeriesScreen", "Total ${screenTitle} Loaded: ${allItems.size}")
                    
                    // Log first few items to see their data
                    allItems.take(3).forEach { item ->
                        android.util.Log.d("SeriesScreen", "${screenTitle.replaceFirstChar { it.uppercase() }}: ${item.name}, ID: ${item.itemId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(allItems)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded series
                    applyFilters()
                    selectedSeriesIndex = 0
                    loadingText.visibility = View.GONE
                    
                } catch (exception: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load all ${contentType}: ${exception.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("SeriesScreen", "=== FAILED TO LOAD ALL ${screenTitle.uppercase()} ===")
                    android.util.Log.e("SeriesScreen", "Error: ${exception.message}")
                    exception.printStackTrace()
                }
            } else if (categoryId == "recent_watched") {
                // Load recent watched content
                try {
                    val recentHistory = watchHistoryRepository.getRecentHistoryByType(contentType, 20)
                    val seriesList = recentHistory.map { history ->
                        watchHistoryRepository.convertToItemEntity(history)
                    }
                    
                    allSeries = seriesList
                    android.util.Log.d("SeriesScreen", "=== RECENT WATCHED LOADED SUCCESSFULLY ===")
                    android.util.Log.d("SeriesScreen", "Total Recent Watched: ${seriesList.size}")
                    
                    // Log first few items to see their data
                    seriesList.take(3).forEach { item ->
                        android.util.Log.d("SeriesScreen", "Recent Watched: ${item.name}, ID: ${item.itemId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(seriesList)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded series
                    applyFilters()
                    selectedSeriesIndex = 0
                    loadingText.visibility = View.GONE
                    
                    // Update category count for current category
                    updateCategoryCount(selectedCategoryIndex, seriesList.size)
                } catch (e: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load recent watched: ${e.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("SeriesScreen", "=== FAILED TO LOAD RECENT WATCHED ===")
                    android.util.Log.e("SeriesScreen", "Error: ${e.message}")
                    e.printStackTrace()
                }
            } else {
                // Load regular category content from pre-loaded database (NO API CALLS!)
                try {
                    val database = DatabaseProvider.getDatabase(this@SeriesScreen)
                    val seriesList = database.itemDao().getItemsByCategorySync(categoryId, contentType)
                    
                    allSeries = seriesList // Store all series for filtering
                    android.util.Log.d("SeriesScreen", "=== ${screenTitle.uppercase()} LOADED FROM DATABASE ===")
                    android.util.Log.d("SeriesScreen", "Category ID: $categoryId")
                    android.util.Log.d("SeriesScreen", "Total ${screenTitle} Loaded: ${seriesList.size}")
                    
                    // Log first few items to see their data
                    seriesList.take(3).forEach { item ->
                        android.util.Log.d("SeriesScreen", "${screenTitle.replaceFirstChar { it.uppercase() }}: ${item.name}, ID: ${item.itemId}, Category: ${item.categoryId}")
                    }
                    
                    // Update filter options with dynamic data from database
                    filterManager.updateFilterOptions(seriesList)
                    filterManager.refreshSpinnerAdapters(
                        genreFilterSpinner,
                        yearFilterSpinner,
                        ratingFilterSpinner,
                        generalFilterSpinner
                    )
                    
                    // Apply filters to the loaded series
                    applyFilters()
                    selectedSeriesIndex = 0
                    loadingText.visibility = View.GONE
                    
                    // Update category count for current category
                    updateCategoryCount(selectedCategoryIndex, seriesList.size)
                    
                } catch (exception: Exception) {
                    loadingText.visibility = View.GONE
                    errorText.text = "Failed to load ${contentType}: ${exception.message}"
                    errorText.visibility = View.VISIBLE
                    android.util.Log.e("SeriesScreen", "=== FAILED TO LOAD ${screenTitle.uppercase()} ===")
                    android.util.Log.e("SeriesScreen", "Category ID: $categoryId")
                    android.util.Log.e("SeriesScreen", "Error: ${exception.message}")
                    exception.printStackTrace()
                }
            }
        }
    }

    private fun updateCategoryCount(categoryIndex: Int, count: Int) {
        val adapter = categoryListView.adapter as? CategoryAdapter
        adapter?.updateCategoryCount(categoryIndex, count)
    }
    
    private fun updateCategoryVisualSelection() {
        // No need for manual visual selection - the focus background handles it automatically
        // Just ensure the correct category is selected in the ListView
        categoryListView.setSelection(selectedCategoryIndex)
    }
    
    /**
     * Gets the currently focused series position from RecyclerView
     * This ensures we always get the actual focus position, not our counter
     */
    private fun getCurrentFocusedSeriesPosition(): Int {
        val focusedView = seriesRecyclerView.focusedChild
        if (focusedView != null) {
            val position = seriesRecyclerView.getChildAdapterPosition(focusedView)
            if (position != RecyclerView.NO_POSITION) {
                // Update our counter to match the actual focus
                selectedSeriesIndex = position
                return position
            }
        }
        // Fallback to our counter if no focused child or position not found
        return selectedSeriesIndex
    }

    /**
     * Logs all drawable resources and their states for debugging
     */
    private fun logDrawableResources() {
        android.util.Log.d("SeriesScreen", "🎨 === DRAWABLE RESOURCES DEBUG ===")
        
        try {
            // Log the focus background drawable
            val focusBackground = getDrawable(R.drawable.category_focus_background)
            android.util.Log.d("SeriesScreen", "   🎯 Focus Background Drawable: $focusBackground")
            
            // Log the count badge background
            val countBadgeBackground = getDrawable(R.drawable.count_badge_background)
            android.util.Log.d("SeriesScreen", "   🔢 Count Badge Background: $countBadgeBackground")
            
            // Log the category focus glow
            val categoryFocusGlow = getDrawable(R.drawable.category_focus_glow)
            android.util.Log.d("SeriesScreen", "   ✨ Category Focus Glow: $categoryFocusGlow")
            
            // Log color resources
            val categoryFocusColor = getColor(R.color.category_focus_color)
            val categoryDefaultBg = getColor(R.color.category_default_bg)
            val accentColor = getColor(R.color.accent_color)
            
            android.util.Log.d("SeriesScreen", "   🎨 Category Focus Color: #${String.format("%06X", 0xFFFFFF and categoryFocusColor)}")
            android.util.Log.d("SeriesScreen", "   🎨 Category Default BG: #${String.format("%06X", 0xFFFFFF and categoryDefaultBg)}")
            android.util.Log.d("SeriesScreen", "   🎨 Accent Color: #${String.format("%06X", 0xFFFFFF and accentColor)}")
            
            // Log the current theme
            android.util.Log.d("SeriesScreen", "   🎭 Current Theme: ${theme.toString()}")
            
        } catch (e: Exception) {
            android.util.Log.e("SeriesScreen", "Error logging drawable resources: ${e.message}")
            e.printStackTrace()
        }
    }
}
