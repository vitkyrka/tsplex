package `in`.rab.tsplex

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import androidx.fragment.app.Fragment
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import android.view.*
import android.view.View.GONE
import android.view.View.VISIBLE
import android.widget.FrameLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import kotlinx.android.synthetic.main.exoplayer_preview_control.*
import kotlinx.android.synthetic.main.fragment_sign_list.*


abstract class ItemListFragment(private val mCache: Boolean = true) : FragmentVisibilityNotifier, androidx.fragment.app.Fragment(), androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener,
        ItemRecyclerViewAdapter.OnItemPlayHandler {
    private var mListener: OnListFragmentInteractionListener? = null
    private var recylerView: androidx.recyclerview.widget.RecyclerView? = null
    private var mState: Parcelable? = null
    private var mTask: AsyncTask<Void, Void, List<Item>>? = null
    private var mZoom = 1.0f
    protected var mItems: List<Item> = arrayListOf()
    protected val PREFS_NAME = "in.rab.tsplex"
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    protected var mPreviewPosition: Int = -1

    protected abstract fun getSigns(): List<Item>

    protected inner class DatabaseTask : AsyncTask<Void, Void, List<Item>>() {
        override fun doInBackground(vararg params: Void): List<Item> {
            return getSigns()
        }

        override fun onPostExecute(signs: List<Item>) {
            mItems = signs
            swipeLayout.isRefreshing = false
            loadList()
        }
    }

    protected fun update() {
        mTask?.cancel(true)
        mTask = DatabaseTask().execute()
    }

    override fun onRefresh() {
        update()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putInt("videoPosition", mPreviewPosition)
    }

    private fun isOnline(): Boolean {
        val conman = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkInfo = conman?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun showVideoError() {
        loadingProgress.visibility = GONE
        exoPlayerView.visibility = GONE

        val msg: String = if (isOnline()) {
            getString(R.string.fail_video_play)
        } else {
            getString(R.string.fail_offline)
        }

        loadingError.text = msg
        loadingError.visibility = VISIBLE
    }

    fun playVideo(title: String, video: String, position: Int) {
        val lexikon = context?.let { Lexikon.getInstance(it) } ?: return

        loadingProgress.visibility = VISIBLE
        loadingError.visibility = GONE

        (recylerView?.adapter as? ItemRecyclerViewAdapter)?.setSelected(position)

        var exoPlayer = mSimpleExoPlayer
        if (exoPlayer == null) {
            val trackSelector = DefaultTrackSelector(AdaptiveTrackSelection.Factory(DefaultBandwidthMeter()))
            exoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)?.apply {
                exoPlayerView?.player = this

                addListener(object : Player.EventListener {
                    override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
                    }

                    override fun onRepeatModeChanged(repeatMode: Int) {
                    }

                    override fun onLoadingChanged(isLoading: Boolean) {
                    }

                    override fun onPositionDiscontinuity() {
                    }

                    override fun onPlayerError(error: ExoPlaybackException?) {
                        showVideoError()
                    }

                    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    }

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            exoPlayerView?.visibility = View.VISIBLE

                            val position = mPreviewPosition
                            if (position > 0) {
                                (recylerView?.layoutManager as? GridAutofitLayoutManager?)?.scrollToPosition(mPreviewPosition)
                            }

                            loadingProgress.visibility = View.GONE

//                            if (mScrollPos != 0) {
//                                val scrollPos = mScrollPos
//                                scrollView.post { scrollView.smoothScrollTo(0, scrollPos) }
//                                mScrollPos = 0
//                                mClicked = true
//                            } else if (mPosition != 0 && mSelectedExample != -1 && !mClicked) {
//                                scrollView.post {
//                                    scrollView.requestChildFocus(videoGroup, videoGroup.findViewById(videoGroup.checkedRadioButtonId))
//                                }
//                            }
                        }
                    }

                })
            } ?: return

            mSimpleExoPlayer = exoPlayer
        }

        exoPlayerNext.visibility = if (position + 1 < mItems.size) {
            VISIBLE
        } else {
            GONE
        }
        exoPlayerPrevious.visibility = if (position - 1 >= 0) {
            VISIBLE
        } else {
            GONE
        }

        mPreviewPosition = position

        val videoSource = ExtractorMediaSource(Uri.parse("https://teckensprakslexikon.su.se/$video"),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)
        exoPlayer.playWhenReady = true
        exoPlayer.prepare(videoSource)

        exoPlayerTitle.text = title
    }

    override fun onItemPlay(item: Sign, position: Int) {
        playVideo(item.word, item.video, position)
    }

    override fun onItemPlay(item: Example, position: Int) {
        playVideo(item.toString(), item.video, position)
    }

    private fun playItem(position: Int) {
        if (position < 0 || position >= mItems.size) {
            return
        }

        val item = try {
            mItems[position]
        } catch (e: Exception) {
            return
        }


        (recylerView?.layoutManager as? GridAutofitLayoutManager?)?.scrollToPosition(position)

        if (item is Sign) {
            onItemPlay(item, position)
        } else if (item is Description) {
            onItemPlay(item.mSign, position)
        } else if (item is Example) {
            onItemPlay(item, position)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        exoPlayerClose.setOnClickListener {
            mPreviewPosition = -1
            mSimpleExoPlayer?.playWhenReady = false
            exoPlayerView?.visibility = GONE

            (recylerView?.adapter as? ItemRecyclerViewAdapter)?.setSelected(-1)
        }

        exoPlayerOpenNew.setOnClickListener {
            val position = mPreviewPosition

            if (position < 0 || position >= mItems.size) {
                return@setOnClickListener
            }

            val item = mItems[position]

            if (item is Sign) {
                mListener!!.onListFragmentInteraction(item)
            } else if (item is Example) {
                mListener!!.onListFragmentInteraction(item)
            }
        }

        exoPlayerNext.setOnClickListener {
            playItem(mPreviewPosition + 1)
        }

        exoPlayerPrevious.setOnClickListener {
            playItem(mPreviewPosition - 1)
        }

        savedInstanceState?.let {
            mPreviewPosition = it.getInt("videoPosition", mPreviewPosition)
        }

        val grand = playerGrandParent
        val player = playerParent

        grand.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
            private var mHeight = 0

            override fun onGlobalLayout() {
                val height = grand.measuredHeight

                if (height > mHeight) {
                    mHeight = height
                    player.maxHeight = height * 2 / 3
                }
            }
        })
    }

    fun loadList() {
        val recyler = recylerView
        if (context == null || recyler == null || !isAdded) {
            return
        }

        mListener?.onLoadList(mItems)

        val dpToPixels = context!!.resources.displayMetrics.density
        val width = (100 * mZoom * dpToPixels + 0.5f).toInt()
        val height = (90 * mZoom * dpToPixels + 0.5f).toInt()
        val params = FrameLayout.LayoutParams(width, height)

        val layout = recyler.layoutManager
        if (layout != null) {
            (layout as GridAutofitLayoutManager).setColumnWidth(width)
        }

        val adapter = ItemRecyclerViewAdapter(this, mItems, mListener,
                Glide.with(this@ItemListFragment), params)

        if (layout != null) {
            val autoFitLayout = (layout as GridAutofitLayoutManager)
            autoFitLayout.spanSizeLookup = object : androidx.recyclerview.widget.GridLayoutManager.SpanSizeLookup() {
                override fun getSpanSize(position: Int): Int {
                    val size = adapter.getSpanSize(position)

                    return if (size == 2) {
                        autoFitLayout.spanCount
                    } else {
                        size
                    }
                }
            }
        }

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

        view?.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeLayout)?.apply {
            setOnRefreshListener(this@ItemListFragment)
            isEnabled = false
        }

        val recycler = view.findViewById<androidx.recyclerview.widget.RecyclerView>(R.id.list)
        val context = recycler.getContext()

        val layoutManager = GridAutofitLayoutManager(context, 1)
        val decoration = androidx.recyclerview.widget.DividerItemDecoration(getContext(), layoutManager.orientation)

        // view.addItemDecoration(decoration)
        recycler.layoutManager = layoutManager
        recylerView = recycler

        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (mItems.isEmpty()) {
                    return true
                }

                mZoom *= detector!!.scaleFactor
                mZoom = Math.max(0.1f, Math.min(mZoom, 5.0f))
                loadList()
                return true
            }
        })

        recycler.setOnTouchListener { _, event ->
            scaleGestureDetector.onTouchEvent(event)
            false
        }

        return view
    }

    override fun onPause() {
        super.onPause()

        mTask?.cancel(true)
        mTask = null
        mState = recylerView?.layoutManager?.onSaveInstanceState()

        mSimpleExoPlayer?.let {
            mSimpleExoPlayerView?.player = null
            it.release()
            mSimpleExoPlayer = null
        }

        val settings = activity?.getSharedPreferences(PREFS_NAME, 0)?.edit()
        settings?.putFloat("imageZoom", mZoom)
        settings?.apply()
    }

    override fun onResume() {
        super.onResume()

        val settings = activity?.getSharedPreferences(PREFS_NAME, 0)
        if (settings != null) {
            mZoom = settings.getFloat("imageZoom", 1f)
        }

        if (!mCache || mItems.isEmpty()) {
            mTask = DatabaseTask().execute()
        } else {
            loadList()
        }
        playItem(mPreviewPosition)
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
        fun onLoadList(items: List<Item>)
        fun onListFragmentInteraction(item: Sign)
        fun onListFragmentInteraction(item: Example)
        fun onListFragmentInteraction(item: Topic)
        fun onExampleSearchClick(example: Example)
    }
}
