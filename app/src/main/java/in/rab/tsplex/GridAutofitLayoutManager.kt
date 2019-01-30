package `in`.rab.tsplex

import android.content.Context
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.max


class GridAutofitLayoutManager(context: Context?, spanCount: Int) : androidx.recyclerview.widget.GridLayoutManager(context, spanCount) {
    private var mColumnWidth: Int = 0
    private var mRefit = false

    fun setColumnWidth(width: Int) {
        if (width > 0 && width != mColumnWidth) {
            mRefit = true
            mColumnWidth = width
        }
    }

    override fun onLayoutChildren(recycler: androidx.recyclerview.widget.RecyclerView.Recycler?, state: androidx.recyclerview.widget.RecyclerView.State) {
        if (mRefit && mColumnWidth > 0) {
            val totalSpace: Int = if (orientation == androidx.recyclerview.widget.LinearLayoutManager.VERTICAL) {
                width - paddingRight - paddingLeft
            } else {
                height - paddingTop - paddingBottom
            }

            spanCount = max(1, totalSpace / mColumnWidth)
            mRefit = false
        }

        super.onLayoutChildren(recycler, state)
    }
}
