package `in`.rab.tsplex

import android.content.Context
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import kotlin.math.max


class GridAutofitLayoutManager(context: Context?, spanCount: Int) : GridLayoutManager(context, spanCount) {
    private var mColumnWidth: Int = 0
    private var mRefit = false

    fun setColumnWidth(width: Int) {
        if (width > 0 && width != mColumnWidth) {
            mRefit = true
            mColumnWidth = width
        }
    }

    override fun onLayoutChildren(recycler: RecyclerView.Recycler?, state: RecyclerView.State) {
        if (mRefit && mColumnWidth > 0) {
            val totalSpace: Int = if (orientation == LinearLayoutManager.VERTICAL) {
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
