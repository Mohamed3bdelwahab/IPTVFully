package com.example.newiptv.ui.series

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.CategoryEntity

class CategoryAdapter(
    context: Context,
    private val categories: List<CategoryEntity>
) : ArrayAdapter<CategoryEntity>(context, 0, categories) {

    private var selectedIndex = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_category, parent, false)

        val categoryCard = view.findViewById<CardView>(R.id.categoryCard)
        val categoryTitle = view.findViewById<TextView>(R.id.categoryText)
        val categoryCount = view.findViewById<TextView>(R.id.categoryCount)

        val category = categories[position]
        categoryTitle.text = category.categoryName
        
        // Set series count - just the number
        val count = categoryCounts[position] ?: 0
        categoryCount.text = count.toString()

        // Set focus change listener for scale animation and color logging
        categoryCard.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                selectedIndex = position
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                
                // Log focus state and colors
                val cardBackground = v.background
                val cardElevation = categoryCard.cardElevation
                val cardRadius = categoryCard.radius
                
                // Log text colors
                val categoryTextColor = categoryTitle.currentTextColor
                val countTextColor = categoryCount.currentTextColor
                
                android.util.Log.d("CategoryAdapter", "🎯 Category $position got FOCUS")
                android.util.Log.d("CategoryAdapter", "   📱 Card Background: $cardBackground")
                android.util.Log.d("CategoryAdapter", "   📏 Card Elevation: $cardElevation")
                android.util.Log.d("CategoryAdapter", "   🔵 Card Radius: $cardRadius")
                android.util.Log.d("CategoryAdapter", "   🎨 Category Name: ${category.categoryName}")
                android.util.Log.d("CategoryAdapter", "   🔢 Series Count: $count")
                android.util.Log.d("CategoryAdapter", "   🟢 Category Text Color: #${String.format("%06X", 0xFFFFFF and categoryTextColor)}")
                android.util.Log.d("CategoryAdapter", "   🔢 Count Text Color: #${String.format("%06X", 0xFFFFFF and countTextColor)}")
                
                // Force background refresh
                v.invalidate()
                
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                
                // Log unfocus state
                val cardBackground = v.background
                val categoryTextColor = categoryTitle.currentTextColor
                val countTextColor = categoryCount.currentTextColor
                
                android.util.Log.d("CategoryAdapter", "❌ Category $position lost FOCUS")
                android.util.Log.d("CategoryAdapter", "   📱 Card Background: $cardBackground")
                android.util.Log.d("CategoryAdapter", "   🟢 Category Text Color: #${String.format("%06X", 0xFFFFFF and categoryTextColor)}")
                android.util.Log.d("CategoryAdapter", "   🔢 Count Text Color: #${String.format("%06X", 0xFFFFFF and countTextColor)}")
            }
        }

        return view
    }

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
        notifyDataSetChanged()
    }
    
    private val categoryCounts = mutableMapOf<Int, Int>()
    
    fun updateCategoryCount(position: Int, count: Int) {
        categoryCounts[position] = count
        notifyDataSetChanged()
    }
}
