package com.example.newiptv.ui.series

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.newiptv.R

class CategoryAdapter(
    context: Context,
    private val categories: List<String>
) : ArrayAdapter<String>(context, 0, categories) {

    private var selectedIndex = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_category, parent, false)

        val categoryCard = view.findViewById<CardView>(R.id.categoryCard)
        val categoryTitle = view.findViewById<TextView>(R.id.categoryText)

        categoryTitle.text = categories[position]

        // Set focus change listener for scale animation
        categoryCard.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                selectedIndex = position
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                android.util.Log.d("CategoryAdapter", "Category $position got FOCUS")
            } else {
                v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
            }
        }

        return view
    }

    fun setSelectedIndex(index: Int) {
        selectedIndex = index
        notifyDataSetChanged()
    }
}
