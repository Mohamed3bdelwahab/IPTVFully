package com.example.newiptv.ui.favorites

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.FavoritePlaylistEntity
import java.text.SimpleDateFormat
import java.util.*

/**
 * Adapter for displaying favorite playlists in a grid
 */
class PlaylistAdapter(
    private val onPlaylistClick: (FavoritePlaylistEntity) -> Unit,
    private val onPlaylistLongClick: (FavoritePlaylistEntity) -> Unit
) : ListAdapter<FavoritePlaylistEntity, PlaylistAdapter.PlaylistViewHolder>(PlaylistDiffCallback()) {

    private var onItemClickListener: ((Int) -> Unit)? = null
    private var focusedPosition = 0

    fun setOnItemClickListener(listener: (Int) -> Unit) {
        onItemClickListener = listener
    }

    fun getItemAt(position: Int): FavoritePlaylistEntity? {
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

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PlaylistViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_playlist, parent, false)
        return PlaylistViewHolder(view)
    }

    override fun onBindViewHolder(holder: PlaylistViewHolder, position: Int) {
        val playlist = getItem(position)
        holder.bind(playlist, position == focusedPosition)
    }

    inner class PlaylistViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val playlistIcon: ImageView = itemView.findViewById(R.id.playlistIcon)
        private val playlistName: TextView = itemView.findViewById(R.id.playlistName)
        private val playlistItemCount: TextView = itemView.findViewById(R.id.playlistItemCount)
        private val playlistDate: TextView = itemView.findViewById(R.id.playlistDate)

        fun bind(playlist: FavoritePlaylistEntity, isFocused: Boolean) {
            playlistName.text = playlist.name
            playlistItemCount.text = "${playlist.itemCount} items"
            
            // Format date
            val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
            playlistDate.text = dateFormat.format(Date(playlist.createdDate))
            
            // Set icon based on item count
            if (playlist.itemCount > 0) {
                playlistIcon.setImageResource(R.drawable.ic_playlist_play)
                playlistIcon.setColorFilter(itemView.context.getColor(R.color.accent_color))
            } else {
                playlistIcon.setImageResource(R.drawable.ic_playlist_add)
                playlistIcon.setColorFilter(itemView.context.getColor(R.color.text_secondary))
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
                onPlaylistClick(playlist)
            }
            
            itemView.setOnLongClickListener {
                onPlaylistLongClick(playlist)
                true
            }
        }
    }

    class PlaylistDiffCallback : DiffUtil.ItemCallback<FavoritePlaylistEntity>() {
        override fun areItemsTheSame(oldItem: FavoritePlaylistEntity, newItem: FavoritePlaylistEntity): Boolean {
            return oldItem.id == newItem.id
        }

        override fun areContentsTheSame(oldItem: FavoritePlaylistEntity, newItem: FavoritePlaylistEntity): Boolean {
            return oldItem == newItem
        }
    }
}
