package com.example.newiptv.ui.series

import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import com.bumptech.glide.Glide
import com.example.newiptv.R
import com.example.newiptv.data.db.entities.ItemEntity

class SeriesCardPresenter : Presenter() {

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = ImageCardView(parent.context).apply {
            isFocusable = true
            isFocusableInTouchMode = true
            setMainImageDimensions(200, 300)
        }
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val series = item as ItemEntity
        val cardView = viewHolder.view as ImageCardView

        // Set title with rating and year
        val year = series.releaseDate?.split("-")?.firstOrNull() ?: ""
        val rating = series.rating5Based?.toString() ?: series.rating ?: "N/A"
        cardView.titleText = series.name
        cardView.contentText = "Rating: $rating | Year: $year"

        // Load cover image
        if (!series.cover.isNullOrEmpty()) {
            Glide.with(cardView.context)
                .load(series.cover)
                .placeholder(R.drawable.placeholder_image)
                .error(R.drawable.placeholder_image)
                .into(cardView.mainImageView)
        } else {
            cardView.mainImageView.setImageResource(R.drawable.placeholder_image)
        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.mainImageView.setImageDrawable(null)
    }
}
