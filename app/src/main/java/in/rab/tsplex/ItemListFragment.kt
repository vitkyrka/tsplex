package `in`.rab.tsplex

import android.content.Context
import android.net.Uri
import android.opengl.Visibility
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.provider.ContactsContract
import android.support.v4.app.Fragment
import android.support.v4.widget.SwipeRefreshLayout
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.GridLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.ScaleGestureDetector
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.FrameLayout
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


abstract class ItemListFragment : FragmentVisibilityNotifier, Fragment(), SwipeRefreshLayout.OnRefreshListener,
        ItemRecyclerViewAdapter.OnItemPlayHandler {
    private var mListener: OnListFragmentInteractionListener? = null
    private var recylerView: RecyclerView? = null
    private var mState: Parcelable? = null
    private var mTask: AsyncTask<Void, Void, List<Item>>? = null
    private var mZoom = 1.0f
    private var mSigns: List<Item> = arrayListOf()
    protected val PREFS_NAME = "in.rab.tsplex"
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPreviewPosition: Int = -1

    protected abstract fun getSigns(): List<Item>

    protected inner class DatabaseTask : AsyncTask<Void, Void, List<Item>>() {
        override fun doInBackground(vararg params: Void): List<Item> {
            return getSigns()
        }

        override fun onPostExecute(signs: List<Item>) {
            mSigns = signs
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

    fun playVideo(title: String, video: String, position: Int) {
        val lexikon = context?.let { Lexikon.getInstance(it) } ?: return

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
                        //showError()
                    }

                    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    }

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            exoPlayerView?.visibility = View.VISIBLE
                            //loadingProgress.visibility = View.GONE

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

        exoPlayerNext.visibility = if (position + 1 < mSigns.size) {
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
        if (position < 0 || position >= mSigns.size) {
            return
        }

        val item = try {
            mSigns[position]
        } catch (e: Exception) {
            return
        }

        if (item is Sign) {
            onItemPlay(item, position)
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
        }

        exoPlayerOpenNew.setOnClickListener {
            val position = mPreviewPosition

            if (position < 0 || position >= mSigns.size) {
                return@setOnClickListener
            }

            val item = mSigns[position]

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

        val adapter = ItemRecyclerViewAdapter(this, mSigns, mListener,
                Glide.with(this@ItemListFragment), params)

        if (layout != null) {
            val autoFitLayout = (layout as GridAutofitLayoutManager)
            autoFitLayout.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
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

        view?.findViewById<SwipeRefreshLayout>(R.id.swipeLayout)?.apply {
            setOnRefreshListener(this@ItemListFragment)
            isEnabled = false
        }

        val recycler = view.findViewById<RecyclerView>(R.id.list)
        val context = recycler.getContext()

        val layoutManager = GridAutofitLayoutManager(context, 1)
        val decoration = DividerItemDecoration(getContext(), layoutManager.orientation)

        // view.addItemDecoration(decoration)
        recycler.layoutManager = layoutManager
        recylerView = recycler

        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector?): Boolean {
                if (mSigns.isEmpty()) {
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

        mTask = DatabaseTask().execute()
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
        fun onListFragmentInteraction(item: Sign)
        fun onListFragmentInteraction(item: Example)
        fun onListFragmentInteraction(item: Search)
        fun onListFragmentInteraction(item: Topic)
        fun onExampleSearchClick(example: Example)
    }
}
