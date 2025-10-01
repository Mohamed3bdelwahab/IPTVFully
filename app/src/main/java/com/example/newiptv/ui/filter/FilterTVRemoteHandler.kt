package com.example.newiptv.ui.filter

import android.view.KeyEvent
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import com.example.newiptv.utils.KeyEventLogger

/**
 * Handles TV remote navigation for filter UI elements
 * Provides D-Pad navigation between filter spinners and clear button
 */
class FilterTVRemoteHandler(
    private val genreSpinner: Spinner,
    private val yearSpinner: Spinner,
    private val ratingSpinner: Spinner,
    private val generalSpinner: Spinner,
    private val searchEditText: EditText,
    private val clearButton: Button
) {
    
    private var currentFocusIndex = 0
    private val focusableElements = listOf(
        genreSpinner,
        yearSpinner,
        ratingSpinner,
        generalSpinner,
        searchEditText,
        clearButton
    )
    
    private val elementNames = listOf(
        "Genre Filter",
        "Year Filter", 
        "Rating Filter",
        "General Filter",
        "Search Field",
        "Clear Filters"
    )
    
    init {
        setupFocusHandling()
    }
    
    private fun setupFocusHandling() {
        // Set up focus change listeners for all elements
        focusableElements.forEachIndexed { index, element ->
            element.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    currentFocusIndex = index
                    KeyEventLogger.logFocusChange("FilterTVRemoteHandler", elementNames[index], index)
                }
            }
        }
        
        // Set initial focus to first element
        focusableElements[0].requestFocus()
    }
    
    /**
     * Handle TV remote key events for filter navigation
     */
    fun handleKeyEvent(event: KeyEvent): Boolean {
        if (event.action != KeyEvent.ACTION_DOWN) {
            return false
        }
        
        return when (event.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> {
                moveFocusLeft()
                true
            }
            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                moveFocusRight()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP -> {
                // Don't open dropdowns with Up/Down - only navigate between filter elements
                false
            }
            KeyEvent.KEYCODE_DPAD_DOWN -> {
                // Don't open dropdowns with Up/Down - only navigate between filter elements
                false
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                // Only Enter/Center opens dropdowns or activates buttons
                activateCurrentElement()
                true
            }
            KeyEvent.KEYCODE_DPAD_UP, KeyEvent.KEYCODE_DPAD_DOWN -> {
                // For search field, allow text editing with Up/Down
                if (currentFocusIndex == 4) { // Search field index
                    // Let the EditText handle text editing
                    false
                } else {
                    false
                }
            }
            else -> false
        }
    }
    
    private fun moveFocusLeft() {
        if (currentFocusIndex > 0) {
            currentFocusIndex--
            focusableElements[currentFocusIndex].requestFocus()
            KeyEventLogger.logNavigation("FilterTVRemoteHandler", "LEFT", 
                elementNames[currentFocusIndex + 1], elementNames[currentFocusIndex])
        } else {
            // At first element, signal to move back to category list
            clearFilterFocus()
            KeyEventLogger.logNavigation("FilterTVRemoteHandler", "LEFT", 
                elementNames[currentFocusIndex], "Category List")
        }
    }
    
    private fun moveFocusRight() {
        if (currentFocusIndex < focusableElements.size - 1) {
            currentFocusIndex++
            focusableElements[currentFocusIndex].requestFocus()
            KeyEventLogger.logNavigation("FilterTVRemoteHandler", "RIGHT", 
                elementNames[currentFocusIndex - 1], elementNames[currentFocusIndex])
        }
    }
    
    private fun openSpinnerDropdown() {
        val currentSpinner = focusableElements[currentFocusIndex] as Spinner
        currentSpinner.performClick()
        KeyEventLogger.logScreenEvent("FilterTVRemoteHandler", "Opened ${elementNames[currentFocusIndex]} dropdown")
    }
    
    private fun activateCurrentElement() {
        val currentElement = focusableElements[currentFocusIndex]
        when (currentElement) {
            is Spinner -> {
                currentElement.performClick()
                KeyEventLogger.logScreenEvent("FilterTVRemoteHandler", "Activated ${elementNames[currentFocusIndex]}")
            }
            is EditText -> {
                currentElement.requestFocus()
                KeyEventLogger.logScreenEvent("FilterTVRemoteHandler", "Focused ${elementNames[currentFocusIndex]}")
            }
            is Button -> {
                currentElement.performClick()
                KeyEventLogger.logScreenEvent("FilterTVRemoteHandler", "Clicked ${elementNames[currentFocusIndex]}")
            }
        }
    }
    
    /**
     * Get current focused element name
     */
    fun getCurrentFocusName(): String {
        return if (currentFocusIndex in elementNames.indices) {
            elementNames[currentFocusIndex]
        } else {
            "Unknown"
        }
    }
    
    /**
     * Set focus to a specific element
     */
    fun setFocusToElement(index: Int) {
        if (index in focusableElements.indices) {
            currentFocusIndex = index
            focusableElements[index].requestFocus()
            KeyEventLogger.logFocusChange("FilterTVRemoteHandler", elementNames[index], index)
        }
    }
    
    /**
     * Check if any filter element has focus
     */
    fun hasFilterFocus(): Boolean {
        return focusableElements.any { it.hasFocus() }
    }
    
    /**
     * Move focus away from filter elements
     */
    fun clearFilterFocus() {
        focusableElements.forEach { it.clearFocus() }
        currentFocusIndex = 0
    }
}
