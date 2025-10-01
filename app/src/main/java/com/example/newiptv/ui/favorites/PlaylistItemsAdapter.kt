package com.example.newiptv.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.FavoritePlaylistItemEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying playlist items in a list
 */
class PlaylistItemsAdapter(
    private val onItemClick: (FavoritePlaylistItemEntity) -> Unit,
    private val onItemLongClick: (FavoritePlaylistItemEntity) -> Unit
) : ListAdapter<FavoritePlaylistItemEntity, PlaylistItemsAdapter.PlaylistItemViewHolder>(PlaylistItemDiffCallback()) {

    private var focusedPosition = 0

    fun getItemAt(position: Int): FavoritePlaylistItemEntity? {
        return if (position >= 0 && position < itemCount) {
            getItem(position)
        } else {
            null
        }
    }

    fun setFocusedPosition(position: Int) {
        val oldPosition = focusedPosition
        focusedPosition = position
        notifyItemChanged(oldPosition)
        notifyItemChanged(focusedPosition)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistItemViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist_content, parent, false)
        return PlaylistItemViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistItemViewHolder, position: Int) {
        val item = getItem(position)
        holder.bind(item, position == focusedPosition)
    }

    inner class PlaylistItemViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val contentIcon: ImageView = itemView.findViewById(R.id.contentIcon)
        private val contentCover: ImageView = itemView.findViewById(R.id.contentCover)
        private val contentTitle: TextView = itemView.findViewById(R.id.contentTitle)
        private val contentType: TextView = itemView.findViewById(R.id.contentType)
        private val addedDate: TextView = itemView.findViewById(R.id.addedDate)

        fun bind(item: FavoritePlaylistItemEntity, isFocused: Boolean) {
            contentTitle.text = item.title
            
            // Set content type
            val typeText = when (item.contentType) {
                "movie" -> "Movie"
                "series" -> "Series"
                "episode" -> "Episode ${item.episodeNumber}"
                else -> item.contentType.capitalize()
            }
            contentType.text = typeText
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            addedDate.text = "Added ${dateFormat.format(Date(item.addedDate))}"
            
            // Set icon based on content type
            val iconRes = when (item.contentType) {
                "movie" -> R.drawable.ic_movie
                "series" -> R.drawable.ic_tv
                "episode" -> R.drawable.ic_play_circle
                else -> R.drawable.ic_play_circle
            }
            contentIcon.setImageResource(iconRes)
            
            // Load cover image if available
            if (!item.cover.isNullOrEmpty()) {
                contentCover.visibility = View.VISIBLE
                Glide.with(itemView.context)
                    .load(item.cover)
                    .placeholder(R.drawable.ic_image_placeholder)
                    .error(R.drawable.ic_image_placeholder)
                    .into(contentCover)
            } else {
                contentCover.visibility = View.GONE
            }
            
            // Set focus state
            itemView.isSelected = isFocused
            if (isFocused) {
                itemView.background = itemView.context.getDrawable(R.drawable.item_focused_background)
            } else {
                itemView.background = itemView.context.getDrawable(R.drawable.item_default_background)
            }
            
            // Set click listeners
            itemView.setOnClickListener {
                onItemClick(item)
            }
            
            itemView.setOnLongClickListener {
                onItemLongClick(item)
                true
            }
        }
    }

    class PlaylistItemDiffCallback : DiffUtil.ItemCallback<FavoritePlaylistItemEntity>() {
        override fun areItemsTheSame(oldItem: FavoritePlaylistItemEntity, newItem: FavoritePlaylistItemEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoritePlaylistItemEntity, newItem: FavoritePlaylistItemEntity): Boolean {
            return oldItem == newItem
        }
    }
}
