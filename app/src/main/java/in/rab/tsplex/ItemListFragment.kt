package `in`.rab.tsplex

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Build
import android.os.Bundle
import android.os.Parcelable
import android.speech.tts.TextToSpeech
import android.view.*
import android.view.View.*
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.Toast
import androidx.preference.PreferenceManager
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
import java.util.*


abstract class ItemListFragment(protected var mCache: Boolean = true, private val mEmptyText: Int = 0) : FragmentVisibilityNotifier, androidx.fragment.app.Fragment(), androidx.swiperefreshlayout.widget.SwipeRefreshLayout.OnRefreshListener,
        ItemRecyclerViewAdapter.OnItemPlayHandler {
    private var mListener: OnListFragmentInteractionListener? = null
    private var recylerView: androidx.recyclerview.widget.RecyclerView? = null
    private var mState: Parcelable? = null
    private var mTask: AsyncTask<Void, Void, List<Item>>? = null
    protected var mItems: List<Item> = arrayListOf()
    protected val PREFS_NAME = "in.rab.tsplex"
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    protected var mPreviewPosition: Int = -1
    private var mRepeatMode: Int = Player.REPEAT_MODE_ALL
    private var mSpeed: Float = 0.75f
    private var mScreenWidthDp = 320f
    private var mZoom = 1.5f
    private var mPlayerBaseHeight = 0
    private var mVideoZoom = 1f
    private var mPreviousVisible = INVISIBLE
    private var mNextVisible = INVISIBLE
    protected var mCloseVisible = VISIBLE
    protected var mOpenNewVisible = VISIBLE
    protected abstract fun getSigns(): List<Item>
    private var ttsEnabled = false
    private var tts: TextToSpeech? = null
    private var ttsInitialized = false

    protected inner class DatabaseTask : AsyncTask<Void, Void, List<Item>>() {
        override fun doInBackground(vararg params: Void): List<Item> {
            return getSigns()
        }

        override fun onPostExecute(signs: List<Item>) {
            loadingProgress.hide()
            mItems = signs
            swipeLayout.isRefreshing = false


            if (mEmptyText != 0) {
                if (signs.isEmpty()) {
                    emptyList.visibility = VISIBLE
                } else {
                    emptyList.visibility = GONE
                }
            }

            loadList()
        }
    }

    protected fun update(showProgress: Boolean = true) {
        mTask?.cancel(true)
        if (showProgress) {
            loadingProgress.show()
        }
        mTask = DatabaseTask().execute()
    }

    override fun onRefresh() {
        update(false)
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
        loadingProgress.hide()
        playerParent.visibility = GONE

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

        loadingProgress.show()
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
                        if (repeatMode != mRepeatMode) {
                            mRepeatMode = repeatMode

                            getSharedPreferences()?.edit()?.apply {
                                putInt("signRepeatMode", repeatMode)
                                apply()
                            }
                        }
                    }

                    override fun onLoadingChanged(isLoading: Boolean) {
                    }

                    override fun onPositionDiscontinuity() {
                    }

                    override fun onPlayerError(error: ExoPlaybackException?) {
                        mListener?.onVideoFetchEnd()
                        showVideoError()
                    }

                    override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
                    }

                    override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
                    }

                    override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                        if (playbackState == Player.STATE_READY) {
                            playerParent?.visibility = View.VISIBLE

                            mListener?.onVideoFetchEnd()

                            val position = mPreviewPosition
                            if (position > 0) {
                                (recylerView?.layoutManager as? GridAutofitLayoutManager?)?.scrollToPosition(mPreviewPosition)
                            }

                            loadingProgress?.hide()

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

        mNextVisible = if (nextPlayablePosition(position, mItems) < mItems.size) {
            VISIBLE
        } else {
            INVISIBLE
        }
        mPreviousVisible = if (prevPlayablePosition(position, mItems) >= 0) {
            VISIBLE
        } else {
            INVISIBLE
        }

        if (exoPlayerExtraControls.visibility != VISIBLE) {
            exoPlayerNext.visibility = mNextVisible
            exoPlayerPrevious.visibility = mPreviousVisible
        }

        mPreviewPosition = position
        val video = "https://teckensprakslexikon.su.se/$video"

        mListener?.onVideoFetchStart(video)

        val videoSource = ExtractorMediaSource(Uri.parse(video),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)

        exoPlayer.apply {
            playbackParameters = PlaybackParameters(mSpeed, 1f)
            repeatMode = mRepeatMode
            playWhenReady = true
            prepare(videoSource)
        }

        exoPlayerTitle.text = title
    }

    private fun speakTts(tts: TextToSpeech?, what: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            tts?.speak(what, TextToSpeech.QUEUE_FLUSH, null, what)
        } else {
            tts?.speak(what, TextToSpeech.QUEUE_FLUSH, null)
        }
    }

    private fun speak(what: String) {
        if (!ttsEnabled) return

        if (ttsInitialized) {
            speakTts(tts, what)
            return
        }

        tts = TextToSpeech(context, object : TextToSpeech.OnInitListener {
            override fun onInit(status: Int) {
                if (status != TextToSpeech.SUCCESS) {
                    Toast.makeText(context, R.string.fail_tts, Toast.LENGTH_SHORT).show()
                    return
                }

                tts?.language = Locale("sv")
                ttsInitialized = true
                speakTts(tts, what)
            }
        })
    }

    override fun onItemPlay(item: Sign, position: Int) {
        speak(item.word)
        mListener?.onItemPlay(item)
        playVideo(item.word, item.video, position)
    }

    override fun onItemPlay(item: Example, position: Int) {
        speak(item.toString())
        mListener?.onItemPlay(item)
        playVideo("", item.video, position)
    }

    override fun onItemPlay(item: Explanation, position: Int) {
        speak(item.toString())
        mListener?.onItemPlay(item)
        playVideo("", item.video, position)
    }

    override fun onExampleClick(item: Example, position: Int) {
        mListener?.onListFragmentInteraction(item)
    }

    private fun nextPlayablePosition(position: Int, items: List<Item>): Int {
        val baseIndex = position + 1

        if (baseIndex >= items.size) {
            return items.size
        }

        for ((index, item) in items.subList(baseIndex, items.size).withIndex()) {
            if (isPlayable(item))
                return baseIndex + index
        }

        return items.size
    }

    private fun prevPlayablePosition(position: Int, items: List<Item>): Int {
        val baseIndex = position - 1

        if (baseIndex < 0) {
            return baseIndex
        }

        for ((index, item) in items.subList(0, baseIndex + 1).reversed().withIndex()) {
            if (isPlayable(item))
                return baseIndex - index
        }

        return -1
    }

    private fun isPlayable(item: Item): Boolean {
        return when (item) {
            is Sign -> true
            is Description -> true
            is Example -> true
            is Explanation -> true
            else -> false
        }
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

        when (item) {
            is Sign -> {
                onItemPlay(item, position)
            }
            is Description -> {
                onItemPlay(item.mSign, position)
            }
            is Example -> {
                onItemPlay(item, position)
            }
            is Explanation -> {
                onItemPlay(item, position)
            }
        }
    }

    fun getSharedPreferences(): SharedPreferences? {
        return activity?.getSharedPreferences("in.rab.tsplex", 0)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mEmptyText != 0) {
            emptyListText.text = getString(mEmptyText)
        }

        val listener = View.OnClickListener {
            val speed = when (it.id) {
                R.id.exo_050x -> 0.50f
                R.id.exo_075x -> 0.75f
                else -> 1.0f
            }

            mSpeed = speed
            mSimpleExoPlayer?.playbackParameters = PlaybackParameters(speed, 1f)

            getSharedPreferences()?.edit()?.apply {
                putFloat("signPlaybackSpeed", speed)
                apply()
            }
        }

        exo_050x.setOnClickListener(listener)
        exo_075x.setOnClickListener(listener)
        exo_100x.setOnClickListener(listener)

        exoPlayerClose.setOnClickListener {
            mPreviewPosition = -1
            mSimpleExoPlayer?.playWhenReady = false
            playerParent?.visibility = GONE

            (recylerView?.adapter as? ItemRecyclerViewAdapter)?.setSelected(-1)
        }

        val videoScaleDetector = ScaleGestureDetector(context!!, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (mPlayerBaseHeight == 0) {
                    return true
                }

                mVideoZoom *= detector.scaleFactor
                mVideoZoom = mVideoZoom.coerceIn(0.75f, 1.5f)

                val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                        (mPlayerBaseHeight * mVideoZoom).toInt())
                playerParent?.layoutParams = params

                activity?.getSharedPreferences(PREFS_NAME, 0)?.edit()?.apply {
                    putFloat(VIDEO_ZOOM_PREF, mVideoZoom)
                    apply()
                }

                return true
            }
        })

        exoPlayerView.setOnTouchListener { _, event ->
            videoScaleDetector.onTouchEvent(event)
            if (event.action == MotionEvent.ACTION_DOWN) {
                val visible = exoPlayerExtraControls.visibility

                exoPlayerExtraControls.visibility = if (visible == VISIBLE) {
                    GONE
                } else {
                    VISIBLE
                }

                if (visible == VISIBLE) {
                    exoPlayerTitle.visibility = visible
                    exoPlayerNext.visibility = mNextVisible
                    exoPlayerPrevious.visibility = mPreviousVisible
                    exoPlayerClose.visibility = mCloseVisible
                    exoPlayerOpenNew.visibility = mOpenNewVisible
                } else {
                    exoPlayerTitle.visibility = GONE
                    exoPlayerNext.visibility = GONE
                    exoPlayerPrevious.visibility = GONE
                    exoPlayerClose.visibility = GONE
                    exoPlayerOpenNew.visibility = GONE
                }
            }
            true
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
            playItem(nextPlayablePosition(mPreviewPosition, mItems))
        }

        exoPlayerPrevious.setOnClickListener {
            playItem(prevPlayablePosition(mPreviewPosition, mItems))
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

                    player.maxHeight = height * 3 / 4
                    mPlayerBaseHeight = height / 2

                    val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                            (mPlayerBaseHeight * mVideoZoom).toInt())
                    player.layoutParams = params
                }
            }
        })

        context?.resources?.displayMetrics?.let {
            mScreenWidthDp = (it.widthPixels / it.density)
            mZoom = Math.min(mScreenWidthDp / (SIGN_WIDTH_DP * 2), 2f)
        }
    }

    protected fun getNumSignColumns(): Int {
        return Math.max(1, Math.floor((mScreenWidthDp / (SIGN_WIDTH_DP * mZoom)).toDouble()).toInt())
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
        val context = recycler.context

        val layoutManager = GridAutofitLayoutManager(context, 1)
        // val decoration = androidx.recyclerview.widget.DividerItemDecoration(getContext(), layoutManager.orientation)
        // view.addItemDecoration(decoration)
        recycler.layoutManager = layoutManager
        recylerView = recycler

        val scaleGestureDetector = ScaleGestureDetector(context, object : ScaleGestureDetector.SimpleOnScaleGestureListener() {
            override fun onScale(detector: ScaleGestureDetector): Boolean {
                if (mItems.isEmpty()) {
                    return true
                }

                mZoom *= detector.scaleFactor
                mZoom = Math.max(MIN_ZOOM, Math.min(mZoom, MAX_ZOOM))
                loadList()

                activity?.getSharedPreferences(PREFS_NAME, 0)?.edit()?.apply {
                    putFloat(ZOOM_PREF, mZoom)
                    apply()
                }

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

        tts?.stop()
        tts?.shutdown()
        tts = null
        ttsInitialized = false

        mTask?.cancel(true)
        mTask = null
        mState = recylerView?.layoutManager?.onSaveInstanceState()

        mSimpleExoPlayer?.let {
            mSimpleExoPlayerView?.player = null
            it.release()
            mSimpleExoPlayer = null
        }

    }

    override fun onResume() {
        super.onResume()

        getSharedPreferences()?.let {
            mZoom = it.getFloat(ZOOM_PREF, mZoom)
            mVideoZoom = it.getFloat(VIDEO_ZOOM_PREF, mVideoZoom)
            mRepeatMode = it.getInt("signRepeatMode", mRepeatMode)
            mSpeed = it.getFloat("signPlaybackSpeed", mSpeed)

            exo_speed.clearCheck()

            when (mSpeed) {
                0.50f -> exo_050x.isChecked = true
                0.75f -> exo_075x.isChecked = true
                else -> exo_100x.isChecked = true
            }

        }

        val sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context)
        ttsEnabled = sharedPreferences.getBoolean("tts", false)

        if (!mCache || mItems.isEmpty()) {
            update()
        } else {
            loadList()
        }
        playItem(mPreviewPosition)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException("$context must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnListFragmentInteractionListener {
        fun onItemPlay(item: Item)
        fun onVideoFetchStart(video: String)
        fun onVideoFetchEnd()
        fun onLoadList(items: List<Item>)
        fun onListFragmentInteraction(item: Sign)
        fun onListFragmentInteraction(item: Example)
        fun onListFragmentInteraction(item: Topic)
        fun onListFragmentInteraction(item: Folder)
        fun onItemLongClick(item: Folder): Boolean
        fun onExampleSearchClick(example: Example)
        fun onFindSimilarClick(item: Sign)
    }

    companion object {
        private const val MIN_ZOOM = 1f
        private const val MAX_ZOOM = 5f
        private const val SIGN_WIDTH_DP = 100f + 5
        private const val ZOOM_PREF = "imageZoom2"
        private const val VIDEO_ZOOM_PREF = "videoZoom"
    }
}
