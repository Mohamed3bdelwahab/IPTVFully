package com.example.newiptv.ui.history

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.WatchHistoryEntity
import java.text.SimpleDateFormat
import java.util.*

class HistoryAdapter(
    private val onItemClick: (WatchHistoryEntity) -> Unit
) : RecyclerView.Adapter<HistoryAdapter.HistoryViewHolder>() {

    private var historyItems: List<WatchHistoryEntity> = emptyList()
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy 'at' HH:mm", Locale.getDefault())

    fun updateHistory(newHistoryItems: List<WatchHistoryEntity>) {
        historyItems = newHistoryItems
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HistoryViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_history, parent, false)
        return HistoryViewHolder(view)
    }

    override fun onBindViewHolder(holder: HistoryViewHolder, position: Int) {
        val historyItem = historyItems[position]
        holder.bind(historyItem)
    }

    override fun getItemCount(): Int = historyItems.size

    fun getHistoryAt(position: Int): WatchHistoryEntity? {
        return if (position in 0 until historyItems.size) {
            historyItems[position]
        } else {
            null
        }
    }

    inner class HistoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val historyCard: CardView = itemView.findViewById(R.id.historyCard)
        private val historyImage: ImageView = itemView.findViewById(R.id.historyImage)
        private val historyTitle: TextView = itemView.findViewById(R.id.historyTitle)
        private val historyProgress: TextView = itemView.findViewById(R.id.historyProgress)
        private val historyDate: TextView = itemView.findViewById(R.id.historyDate)
        private val historyType: TextView = itemView.findViewById(R.id.historyType)

        fun bind(historyItem: WatchHistoryEntity) {
            // Set title
            historyTitle.text = historyItem.title

            // Set content type with icon
            val typeText = when (historyItem.contentType) {
                "movie" -> "🎬 Movie"
                "series" -> "📺 Series"
                "episode" -> "▶️ Episode"
                else -> "📹 Content"
            }
            historyType.text = typeText

            // Set progress information
            val progressText = if (historyItem.isCompleted) {
                "✅ Completed"
            } else if (historyItem.totalDuration > 0) {
                val progressPercent = (historyItem.watchPercentage * 100).toInt()
                "$progressPercent% watched"
            } else {
                "Watched"
            }
            historyProgress.text = progressText

            // Set date
            val dateText = dateFormat.format(Date(historyItem.lastWatched))
            historyDate.text = dateText

            // Load cover image
            if (!historyItem.cover.isNullOrEmpty()) {
                Glide.with(itemView.context)
                    .load(historyItem.cover)
                    .placeholder(R.drawable.placeholder_image)
                    .error(R.drawable.placeholder_image)
                    .into(historyImage)
            } else {
                historyImage.setImageResource(R.drawable.placeholder_image)
            }

            // Set click listener
            historyCard.setOnClickListener {
                onItemClick(historyItem)
            }

            // Set focus change listener for scale animation
            historyCard.setOnFocusChangeListener { v, hasFocus ->
                if (hasFocus) {
                    v.animate().scaleX(1.1f).scaleY(1.1f).setDuration(150).start()
                    android.util.Log.d("HistoryAdapter", "🎯 History item $adapterPosition got FOCUS")
                } else {
                    v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(150).start()
                    android.util.Log.d("HistoryAdapter", "❌ History item $adapterPosition lost FOCUS")
                }
            }
        }
    }
}
