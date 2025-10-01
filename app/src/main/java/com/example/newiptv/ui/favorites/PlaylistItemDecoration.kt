package com.example.newiptv.ui.favorites

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

/**
 * Item decoration for playlist grid spacing
 */
class PlaylistItemDecoration(private val spacing: Int) : RecyclerView.ItemDecoration() {

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        val position = parent.getChildAdapterPosition(view)
        val spanCount = (parent.layoutManager as? androidx.recyclerview.widget.GridLayoutManager)?.spanCount ?: 2
        
        val column = position % spanCount
        
        // Apply spacing
        outRect.left = if (column == 0) spacing else spacing / 2
        outRect.right = if (column == spanCount - 1) spacing else spacing / 2
        outRect.top = spacing
        outRect.bottom = spacing
    }
}
