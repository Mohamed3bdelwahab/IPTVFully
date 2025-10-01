package com.example.newiptv.ui.filter

import android.content.Context
import android.widget.ArrayAdapter
import android.widget.Spinner
import com.example.newiptv.data.db.entities.ItemEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking

/**
 * Manages multiple filter categories for series and movies
 * Supports genre, year, rating, and general sorting filters
 */
class FilterManager(private val context: Context) {
    
    // Filter state
    private val _filterState = MutableStateFlow(FilterState())
    val filterState: StateFlow<FilterState> = _filterState.asStateFlow()
    
    // Filter options - mutable lists for dynamic updates
    private val genreOptions = mutableListOf(
        "All Genres",
        "Action", "Adventure", "Animation", "Biography", "Comedy",
        "Crime", "Documentary", "Drama", "Family", "Fantasy",
        "Film-Noir", "History", "Horror", "Music", "Musical",
        "Mystery", "Romance", "Sci-Fi", "Sport", "Thriller",
        "War", "Western"
    )
    
    private val yearOptions = mutableListOf(
        "All Years",
        "2024", "2023", "2022", "2021", "2020",
        "2019", "2018", "2017", "2016", "2015",
        "2014", "2013", "2012", "2011", "2010",
        "2009", "2008", "2007", "2006", "2005",
        "2004", "2003", "2002", "2001", "2000",
        "1990s", "1980s", "1970s", "1960s", "1950s",
        "Before 1950"
    )
    
    private val ratingOptions = listOf(
        "All Ratings",
        "9+ (Excellent)", "8+ (Very Good)", "7+ (Good)",
        "6+ (Fair)", "5+ (Average)", "4+ (Below Average)",
        "3+ (Poor)", "2+ (Very Poor)", "1+ (Terrible)"
    )
    
    private val generalOptions = listOf(
        "Default",
        "A-Z (Alphabetical)",
        "Z-A (Reverse Alphabetical)",
        "Latest Release",
        "Oldest Release",
        "Rating (High to Low)",
        "Rating (Low to High)",
        "Year (Newest First)",
        "Year (Oldest First)",
        "Recently Added",
        "Most Popular"
    )
    
    /**
     * Update filter options based on actual data from database
     * This runs on background thread to avoid blocking UI
     */
    fun updateFilterOptions(items: List<ItemEntity>) {
        // Run on background thread to avoid blocking UI
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
            try {
                // Generate dynamic year options from actual data
                val years = items.mapNotNull { item ->
                    item.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull()
                }.distinct().sortedDescending()
                
                val dynamicYearOptions = mutableListOf("All Years")
                
                // Add individual years (last 10 years)
                years.take(10).forEach { year ->
                    dynamicYearOptions.add(year.toString())
                }
                
                // Add decades for older content
                val decades = years.filter { it < years.take(10).lastOrNull() ?: 0 }
                    .map { (it / 10) * 10 }
                    .distinct()
                    .sortedDescending()
                
                decades.forEach { decade ->
                    dynamicYearOptions.add("${decade}s")
                }
                
                // Add "Before 1950" if there are very old items
                if (years.any { it < 1950 }) {
                    dynamicYearOptions.add("Before 1950")
                }
                
                // Generate dynamic genre options from actual data
                val genres = items.mapNotNull { item ->
                    item.genre?.split(",")?.map { it.trim() }
                }.flatten().distinct().sorted()
                
                val dynamicGenreOptions = mutableListOf("All Genres")
                dynamicGenreOptions.addAll(genres)
                
                // Update options on main thread
                kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                    // Update year options
                    yearOptions.clear()
                    yearOptions.addAll(dynamicYearOptions)
                    
                    // Update genre options
                    genreOptions.clear()
                    genreOptions.addAll(dynamicGenreOptions)
                }
            } catch (e: Exception) {
                android.util.Log.e("FilterManager", "Error updating filter options", e)
            }
        }
    }
    
    /**
     * Refresh spinner adapters after updating filter options
     */
    fun refreshSpinnerAdapters(
        genreSpinner: Spinner,
        yearSpinner: Spinner,
        ratingSpinner: Spinner,
        generalSpinner: Spinner
    ) {
        // Refresh genre adapter
        val genreAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, genreOptions)
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genreSpinner.adapter = genreAdapter
        
        // Refresh year adapter
        val yearAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, yearOptions)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        
        // Reset selections to "All" options
        genreSpinner.setSelection(0)
        yearSpinner.setSelection(0)
    }
    
    /**
     * Setup filter spinners with adapters
     */
    fun setupFilterSpinners(
        genreSpinner: Spinner,
        yearSpinner: Spinner,
        ratingSpinner: Spinner,
        generalSpinner: Spinner
    ) {
        // Genre filter
        val genreAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, genreOptions)
        genreAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        genreSpinner.adapter = genreAdapter
        
        // Year filter
        val yearAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, yearOptions)
        yearAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        yearSpinner.adapter = yearAdapter
        
        // Rating filter
        val ratingAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, ratingOptions)
        ratingAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        ratingSpinner.adapter = ratingAdapter
        
        // General filter
        val generalAdapter = ArrayAdapter(context, android.R.layout.simple_spinner_item, generalOptions)
        generalAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        generalSpinner.adapter = generalAdapter
        
        // Set up listeners
        genreSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFilterState { copy(selectedGenre = genreOptions[position]) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        yearSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFilterState { copy(selectedYear = yearOptions[position]) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        ratingSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFilterState { copy(selectedRating = ratingOptions[position]) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
        
        generalSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: android.view.View?, position: Int, id: Long) {
                updateFilterState { copy(selectedGeneral = generalOptions[position]) }
            }
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {}
        }
    }
    
    /**
     * Apply filters to a list of items
     * This runs on background thread to avoid blocking UI
     */
    fun applyFilters(items: List<ItemEntity>): List<ItemEntity> {
        val currentState = _filterState.value
        
        // For small lists, apply filters synchronously to avoid coroutine overhead
        if (items.size < 100) {
            return applyFiltersSync(items, currentState)
        }
        
        // For large lists, run on background thread
        return runBlocking {
            withContext(Dispatchers.Default) {
                applyFiltersSync(items, currentState)
            }
        }
    }
    
    /**
     * Synchronous filter application (internal method)
     */
    private fun applyFiltersSync(items: List<ItemEntity>, currentState: FilterState): List<ItemEntity> {
        var filteredItems = items.toList()
        
        // Apply search filter first
        if (currentState.searchQuery.isNotEmpty()) {
            val searchLower = currentState.searchQuery.lowercase()
            filteredItems = filteredItems.filter { item ->
                item.name.lowercase().contains(searchLower) ||
                item.cast?.lowercase()?.contains(searchLower) == true ||
                item.director?.lowercase()?.contains(searchLower) == true ||
                item.genre?.lowercase()?.contains(searchLower) == true ||
                item.plot?.lowercase()?.contains(searchLower) == true
            }
        }
        
        // Apply genre filter
        if (currentState.selectedGenre != "All Genres") {
            filteredItems = filteredItems.filter { item ->
                item.genre?.contains(currentState.selectedGenre, ignoreCase = true) == true
            }
        }
        
        // Apply year filter
        if (currentState.selectedYear != "All Years") {
            filteredItems = filteredItems.filter { item ->
                val itemYear = item.releaseDate?.split("-")?.firstOrNull() ?: ""
                when {
                    // Check if it's a specific year (4 digits)
                    currentState.selectedYear.matches(Regex("\\d{4}")) -> itemYear == currentState.selectedYear
                    // Check if it's a decade (ends with 's')
                    currentState.selectedYear.endsWith("s") -> {
                        val decade = currentState.selectedYear.dropLast(1)
                        itemYear.startsWith(decade)
                    }
                    // Check if it's "Before 1950"
                    currentState.selectedYear == "Before 1950" -> {
                        itemYear.isNotEmpty() && itemYear.toIntOrNull()?.let { it < 1950 } == true
                    }
                    else -> true
                }
            }
        }
        
        // Apply rating filter
        if (currentState.selectedRating != "All Ratings") {
            filteredItems = filteredItems.filter { item ->
                val rating = item.rating?.toFloatOrNull() ?: 0f
                when (currentState.selectedRating) {
                    "9+ (Excellent)" -> rating >= 9.0f
                    "8+ (Very Good)" -> rating >= 8.0f
                    "7+ (Good)" -> rating >= 7.0f
                    "6+ (Fair)" -> rating >= 6.0f
                    "5+ (Average)" -> rating >= 5.0f
                    "4+ (Below Average)" -> rating >= 4.0f
                    "3+ (Poor)" -> rating >= 3.0f
                    "2+ (Very Poor)" -> rating >= 2.0f
                    "1+ (Terrible)" -> rating >= 1.0f
                    else -> true
                }
            }
        }
        
        // Apply general sorting
        filteredItems = when (currentState.selectedGeneral) {
            "A-Z (Alphabetical)" -> filteredItems.sortedBy { it.name }
            "Z-A (Reverse Alphabetical)" -> filteredItems.sortedByDescending { it.name }
            "Latest Release" -> filteredItems.sortedByDescending { it.releaseDate ?: "" }
            "Oldest Release" -> filteredItems.sortedBy { it.releaseDate ?: "" }
            "Rating (High to Low)" -> filteredItems.sortedByDescending { it.rating?.toFloatOrNull() ?: 0f }
            "Rating (Low to High)" -> filteredItems.sortedBy { it.rating?.toFloatOrNull() ?: 0f }
            "Year (Newest First)" -> filteredItems.sortedByDescending { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            "Year (Oldest First)" -> filteredItems.sortedBy { 
                it.releaseDate?.split("-")?.firstOrNull()?.toIntOrNull() ?: 0 
            }
            "Recently Added" -> filteredItems.sortedByDescending { it.lastModified ?: "" }
            "Most Popular" -> filteredItems.sortedByDescending { it.rating?.toFloatOrNull() ?: 0f }
            else -> filteredItems // Default - no sorting
        }
        
        return filteredItems
    }
    
    /**
     * Update search query
     */
    fun updateSearchQuery(query: String) {
        updateFilterState { copy(searchQuery = query) }
    }
    
    /**
     * Clear all filters
     */
    fun clearAllFilters() {
        updateFilterState { 
            FilterState() // Reset to default state
        }
    }
    
    /**
     * Check if any filters are active
     */
    fun hasActiveFilters(): Boolean {
        val state = _filterState.value
        return state.selectedGenre != "All Genres" ||
               state.selectedYear != "All Years" ||
               state.selectedRating != "All Ratings" ||
               state.selectedGeneral != "Default" ||
               state.searchQuery.isNotEmpty()
    }
    
    /**
     * Get current filter summary
     */
    fun getFilterSummary(): String {
        val state = _filterState.value
        val activeFilters = mutableListOf<String>()
        
        if (state.searchQuery.isNotEmpty()) {
            activeFilters.add("Search: \"${state.searchQuery}\"")
        }
        if (state.selectedGenre != "All Genres") {
            activeFilters.add("Genre: ${state.selectedGenre}")
        }
        if (state.selectedYear != "All Years") {
            activeFilters.add("Year: ${state.selectedYear}")
        }
        if (state.selectedRating != "All Ratings") {
            activeFilters.add("Rating: ${state.selectedRating}")
        }
        if (state.selectedGeneral != "Default") {
            activeFilters.add("Sort: ${state.selectedGeneral}")
        }
        
        return if (activeFilters.isEmpty()) {
            "No filters applied"
        } else {
            activeFilters.joinToString(", ")
        }
    }
    
    private fun updateFilterState(update: FilterState.() -> FilterState) {
        _filterState.value = _filterState.value.update()
    }
}

/**
 * Data class representing the current filter state
 */
data class FilterState(
    val selectedGenre: String = "All Genres",
    val selectedYear: String = "All Years",
    val selectedRating: String = "All Ratings",
    val selectedGeneral: String = "Default",
    val searchQuery: String = ""
)
