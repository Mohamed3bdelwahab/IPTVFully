package com.example.newiptv.ui.seriesinfo

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.TextView
import androidx.cardview.widget.CardView
import com.example.newiptv.R

class SeasonsAdapter(
    context: Context,
    private val seasons: List<String>
) : ArrayAdapter<String>(context, 0, seasons) {

    private var selectedIndex = -1

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = convertView ?: LayoutInflater.from(context)
            .inflate(R.layout.item_season, parent, false)

        val seasonCard = view.findViewById<CardView>(R.id.seasonCard)
        val seasonTitle = view.findViewById<TextView>(R.id.seasonText)

        seasonTitle.text = seasons[position]

        // Set focus change listener for scale animation
        seasonCard.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus) {
                selectedIndex = position
                v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                android.util.Log.d("SeasonsAdapter", "Season $position got FOCUS")
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
