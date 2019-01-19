package `in`.rab.tsplex

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import com.bumptech.glide.Glide


abstract class SignListFragment : FragmentVisibilityNotifier, Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private var recylerView: RecyclerView? = null
    private var mState: Parcelable? = null
    private var mTask: AsyncTask<Void, Void, ArrayList<Item>>? = null
    private var mZoom = 1.0f
    private var mSigns: ArrayList<Item> = arrayListOf()

    protected abstract fun getSigns(): ArrayList<Item>

    private inner class DatabaseTask : AsyncTask<Void, Void, ArrayList<Item>>() {
        override fun doInBackground(vararg params: Void): ArrayList<Item> {
            return getSigns()
        }

        override fun onPostExecute(signs: ArrayList<Item>) {
            mSigns = signs
            loadList()
        }
    }

    fun loadList() {
        val recyler = recylerView
        if (context == null || recyler == null || !isAdded) {
            return
        }

        val dpToPixels = context!!.resources.displayMetrics.density
        val width = (100 * mZoom * dpToPixels + 0.5f).toInt()
        val height = (90 * mZoom * dpToPixels + 0.5f).toInt()
        val params = FrameLayout.LayoutParams(width, height)

        val layout = recyler.layoutManager
        if (layout != null) {
            (layout as GridAutofitLayoutManager).setColumnWidth(width)
        }

        val adapter = SignRecyclerViewAdapter(mSigns, mListener,
                Glide.with(this@SignListFragment), params)

        if (recyler.adapter != null) {
            recyler.swapAdapter(adapter, true)
        } else {
            recyler.adapter = adapter
        }

        if (mState != null) {
            recyler.layoutManager?.onRestoreInstanceState(mState)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_sign_list, container, false)

        if (view is RecyclerView) {
            val context = view.getContext()

            val layoutManager = GridAutofitLayoutManager(context, 1)
            val decoration = DividerItemDecoration(getContext(), layoutManager.orientation)

            view.addItemDecoration(decoration)
            view.layoutManager = layoutManager
            recylerView = view

            val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
                override fun onScale(detector: ScaleGestureDetector?): Boolean {
                    if (mSigns.size == 0) {
                        return true
                    }

                    mZoom *= detector!!.scaleFactor
                    mZoom = Math.max(0.1f, Math.min(mZoom, 5.0f))
                    loadList()
                    return true
                }
            })

            view.setOnTouchListener { _, event ->
                scaleGestureDetector.onTouchEvent(event)
                false
            }
        }
        return view
    }

    override fun onPause() {
        super.onPause()

        mTask?.cancel(true)
        mTask = null
        mState = recylerView?.layoutManager?.onSaveInstanceState()

        val settings = activity?.getSharedPreferences("in.rab.tsplex", 0)?.edit()
        settings?.putFloat("imageZoom", mZoom)
        settings?.apply()
    }

    override fun onResume() {
        super.onResume()

        val settings = activity?.getSharedPreferences("in.rab.tsplex", 0)
        if (settings != null) {
            mZoom = settings.getFloat("imageZoom", 1f)
        }

        mTask = DatabaseTask().execute()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Sign)
        fun onListFragmentInteraction(item: Example)
    }
}
