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
import com.example.newiptv.ui.movieinfo.MovieInfoScreen
import com.example.newiptv.ui.series.CategoryAdapter
import com.example.newiptv.ui.series.SeriesAdapter
import com.example.newiptv.utils.KeyEventLogger
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class MoviesScreen : AppCompatActivity() {

    private lateinit var categoryListView: ListView
    private lateinit var moviesRecyclerView: RecyclerView
    private lateinit var moviesAdapter: SeriesAdapter

    private lateinit var loadingText: TextView
    private lateinit var errorText: TextView
    private lateinit var filterSpinner: Spinner

    private var selectedCategoryIndex = 0
    private var selectedMovieIndex = 0
    private var isInCategoryPanel = true
    
    // Filter options
    private val filterOptions = listOf(
        "Default",
        "A-Z",
        "Z-A", 
        "Latest",
        "Oldest",
        "Rating (High to Low)",
        "Rating (Low to High)",
        "Year (Newest)",
        "Year (Oldest)"
    )

     private lateinit var repository: TvRepository
    private var categories: List<CategoryEntity> = emptyList()
    private var currentMovies: List<ItemEntity> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_movies_screen)

        val database = DatabaseProvider.getDatabase(this)
        repository = TvRepository(database)

        initializeViews()
        setupCategoryList()
        setupMoviesGrid()
        setupTVRemoteNavigation()
        
        loadCategories()
    }

    private fun initializeViews() {
        categoryListView = findViewById(R.id.categoryListView)
        moviesRecyclerView = findViewById(R.id.moviesRecyclerView)
        loadingText = findViewById(R.id.loadingText)
        errorText = findViewById(R.id.errorText)
        filterSpinner = findViewById(R.id.filterSpinner)
        
        // Set initial focus to category panel
        categoryListView.requestFocus()
        isInCategoryPanel = true
        selectedCategoryIndex = 0
        selectedMovieIndex = 0
        
        // Setup filter spinner
        setupFilterSpinner()
        
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

    private fun setupFilterSpinner() {
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, filterOptions)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        filterSpinner.adapter = adapter
        
        filterSpinner.setOnItemSelectedListener(object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                if (position > 0) { // Skip "Default"
                    applyFilter(filterOptions[position])
                }
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        })
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

    private fun setupTVRemoteNavigation() {
        // TV remote navigation is handled in dispatchKeyEvent
    }

    override fun dispatchKeyEvent(event: KeyEvent): Boolean {
        if (event.action == KeyEvent.ACTION_DOWN) {
            when (event.keyCode) {
                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if (!isInCategoryPanel) {
                        isInCategoryPanel = true
                        categoryListView.requestFocus()
                        KeyEventLogger.logNavigation("MoviesScreen", "LEFT", "Movies", "Category")
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if (isInCategoryPanel) {
                        isInCategoryPanel = false
                        moviesRecyclerView.requestFocus()
                        KeyEventLogger.logNavigation("MoviesScreen", "RIGHT", "Category", "Movies")
                        updateMoviesFocus()
                        return true
                    }
                }
                KeyEvent.KEYCODE_DPAD_UP -> {
                    KeyEventLogger.logNavigation("MoviesScreen", "UP", if (isInCategoryPanel) "Category" else "Movies")
                    if (isInCategoryPanel) {
                        navigateCategoryUp()
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
                        // Get the actually focused movie from RecyclerView
                        val focusedView = moviesRecyclerView.focusedChild
                        if (focusedView != null) {
                            val position = moviesRecyclerView.getChildAdapterPosition(focusedView)
                            if (position != RecyclerView.NO_POSITION) {
                                val selectedMovie = moviesAdapter.getSeriesAt(position)
                                KeyEventLogger.logItemSelection("MoviesScreen", "Movie", position, 
                                    selectedMovie?.name ?: "Unknown")
                                selectedMovieIndex = position
                                selectCurrentMovie()
                            }
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
        val spanCount = 3
        if (selectedMovieIndex - spanCount >= 0) {
            selectedMovieIndex -= spanCount
            updateMoviesFocus()
            ensureCorrectPanelFocus()
            KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
        }
    }

    private fun navigateMovieDown() {
        val spanCount = 3
        if (selectedMovieIndex + spanCount < currentMovies.size) {
            selectedMovieIndex += spanCount
            updateMoviesFocus()
            ensureCorrectPanelFocus()
            KeyEventLogger.logFocusChange("MoviesScreen", "Movies", selectedMovieIndex)
        }
    }

    private fun updateCategorySelection() {
        categoryListView.setSelection(selectedCategoryIndex)
        updateCategoryVisualSelection()
    }

    private fun updateMoviesFocus() {
        if (selectedMovieIndex < currentMovies.size) {
            moviesRecyclerView.scrollToPosition(selectedMovieIndex)
            // Focus will be handled by the adapter
        }
    }

    private fun ensureCorrectPanelFocus() {
        if (isInCategoryPanel) {
            categoryListView.requestFocus()
        } else {
            moviesRecyclerView.requestFocus()
        }
    }

    private fun updateCategoryVisualSelection() {
        categoryListView.setSelection(selectedCategoryIndex)
    }

    private fun loadCategories() {
        loadingText.visibility = View.VISIBLE
        errorText.visibility = View.GONE

                lifecycleScope.launch {
            repository.loadCategoriesWithSync("movie").collectLatest { result ->
                result.fold(
                    onSuccess = { categoryList ->
                        categories = categoryList
                        val categoryAdapter = CategoryAdapter(this@MoviesScreen, categoryList)
                        categoryListView.adapter = categoryAdapter
                        
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
            repository.loadItemsWithSync("movie", categoryId).collectLatest { result ->
                result.fold(
                    onSuccess = { moviesList ->
                        currentMovies = moviesList
                        android.util.Log.d("MoviesScreen", "=== MOVIES LOADED SUCCESSFULLY ===")
                        android.util.Log.d("MoviesScreen", "Category ID: $categoryId")
                        android.util.Log.d("MoviesScreen", "Total Movies Loaded: ${moviesList.size}")
                        
                        moviesAdapter.updateSeries(moviesList)
                        selectedMovieIndex = 0
                        loadingText.visibility = View.GONE
                        
                        // Update category count for current category
                        if (selectedCategoryIndex < categories.size) {
                            updateCategoryCount(selectedCategoryIndex, moviesList.size)
                        }
                    },
                    onFailure = { exception ->
                        loadingText.visibility = View.GONE
                        errorText.text = "Failed to load movies: ${exception.message}"
                        errorText.visibility = View.VISIBLE
                        android.util.Log.e("MoviesScreen", "=== FAILED TO LOAD MOVIES ===")
                        android.util.Log.e("MoviesScreen", "Category ID: $categoryId")
                        android.util.Log.e("MoviesScreen", "Error: ${exception.message}")
                        exception.printStackTrace()
                    }
                )
            }
        }
    }

    private fun updateCategoryCount(categoryIndex: Int, count: Int) {
        val adapter = categoryListView.adapter as? CategoryAdapter
        adapter?.updateCategoryCount(categoryIndex, count)
    }

    private fun applyFilter(filterType: String) {
        android.util.Log.d("MoviesScreen", "Applying filter: $filterType")
        
        val filteredList = when (filterType) {
            "A-Z" -> currentMovies.sortedBy { it.name }
            "Z-A" -> currentMovies.sortedByDescending { it.name }
            "Latest" -> currentMovies.sortedByDescending { it.releaseDate ?: "" }
            "Oldest" -> currentMovies.sortedBy { it.releaseDate ?: "" }
            "Rating (High to Low)" -> currentMovies.sortedByDescending { it.rating5Based ?: 0.0 }
            "Rating (Low to High)" -> currentMovies.sortedBy { it.rating5Based ?: 0.0 }
            "Year (Newest)" -> currentMovies.sortedByDescending { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            "Year (Oldest)" -> currentMovies.sortedBy { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            else -> currentMovies // Default - no sorting
        }
        
        moviesAdapter.updateSeries(filteredList)
        selectedMovieIndex = 0
        updateMoviesFocus()
        KeyEventLogger.logScreenEvent("MoviesScreen", "Applied filter: $filterType")
    }
}
