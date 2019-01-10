package `in`.rab.tsplex

import android.annotation.SuppressLint
import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.Player.REPEAT_MODE_ALL
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.fragment_signexample.*

class ExampleListFragment : FragmentVisibilityNotifier, ListFragment() {
    private var mExamples: ArrayList<Example>? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPosition = -1
    private var mTask: AsyncTask<Void, Void, ArrayList<Example>>? = null
    private var mControllerVisible: Boolean = false
    private var mSpeed: Float = 0.0f

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_signexample, container, false)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSimpleExoPlayerView = exoPlayerView

        val listener = View.OnClickListener {
            val speed = when (it.id) {
                R.id.exo_050x -> 0.50f
                R.id.exo_075x -> 0.75f
                else -> 1.0f
            }

            mSpeed = speed
            mSimpleExoPlayer?.playbackParameters = PlaybackParameters(speed, 1f)
        }

        exo_050x.setOnClickListener(listener)
        exo_075x.setOnClickListener(listener)
        exo_100x.setOnClickListener(listener)

        exoPlayerView.setControllerVisibilityListener { it -> mControllerVisible = it == VISIBLE }
        exoPlayerView.setOnTouchListener { v, event ->
            val exoView = v as SimpleExoPlayerView
            if (event.action == MotionEvent.ACTION_DOWN) {
                if (mControllerVisible) {
                    exoView.hideController()
                } else {
                    exoView.showController()
                }
            }
            true
        }

        listView.setOnItemLongClickListener { parent, _, position, _ ->
            val l = parent as ListView
            val example = l.adapter?.getItem(position) as Example

            val q = Uri.parse(example.video).lastPathSegment
            val intent = Intent(activity, SearchActivity::class.java)
            intent.action = Intent.ACTION_SEARCH
            intent.putExtra(Intent.EXTRA_TITLE, example.toString())
            intent.putExtra(SearchManager.QUERY, "ex:$q")

            startActivity(intent)
            false
        }
    }

    override fun onPause() {
        super.onPause()

        mTask?.cancel(true)
        mTask = null

        val exo = mSimpleExoPlayer ?: return
        val settings = activity?.getSharedPreferences("in.rab.tsplex", 0)?.edit()

        if (mSpeed != 0.0f) {
            settings?.putFloat("examplePlaybackSpeed", mSpeed)
        }

        settings?.putInt("exampleRepeatMode", exo.repeatMode)
        settings?.apply()

        mSimpleExoPlayerView?.player = null
        exo.release()
        mSimpleExoPlayer = null
    }

    private fun isOnline(): Boolean {
        val conman = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkInfo = conman?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun showError() {
        mSimpleExoPlayerView?.visibility = GONE

        val msg: String = if (isOnline()) {
            getString(R.string.fail_video_play)
        } else {
            getString(R.string.fail_offline)
        }

        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }

    private inner class DatabaseTask : AsyncTask<Void, Void, ArrayList<Example>>() {
        override fun doInBackground(vararg params: Void): ArrayList<Example> {
            val act = activity ?: return ArrayList<Example>()

            return SignDatabase(act).getExamples()
        }

        override fun onPostExecute(signs: ArrayList<Example>) {
            mExamples = signs
            playVideo()
        }
    }

    override fun onResume() {
        super.onResume()

        val bandwidthMeter = DefaultBandwidthMeter()
        val videoTrackSelectionFactory = AdaptiveTrackSelection.Factory(bandwidthMeter)
        val trackSelector = DefaultTrackSelector(videoTrackSelectionFactory)
        mSimpleExoPlayer = ExoPlayerFactory.newSimpleInstance(context, trackSelector)

        mSimpleExoPlayer?.addListener(object : Player.EventListener {
            override fun onTimelineChanged(timeline: Timeline?, manifest: Any?) {
            }

            override fun onRepeatModeChanged(repeatMode: Int) {
            }

            override fun onLoadingChanged(isLoading: Boolean) {
            }

            override fun onPositionDiscontinuity() {
            }

            override fun onPlayerError(error: ExoPlaybackException?) {
                showError()
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    mSimpleExoPlayerView?.visibility = View.VISIBLE
                }
            }

        })

        val settings = activity!!.getSharedPreferences("in.rab.tsplex", 0)
        val speed = settings.getFloat("signPlaybackSpeed", 0.75f)

        exo_speed.clearCheck()

        when (speed) {
            0.50f -> exo_050x.isChecked = true
            0.75f -> exo_075x.isChecked = true
            else -> exo_100x.isChecked = true
        }

        mSpeed = speed

        mSimpleExoPlayerView!!.player = mSimpleExoPlayer
        mSimpleExoPlayer!!.repeatMode = settings.getInt("signRepeatMode", REPEAT_MODE_ALL)
        mSimpleExoPlayer!!.playbackParameters = PlaybackParameters(speed, 1f)
        mSimpleExoPlayer!!.playWhenReady = true



        if (mExamples == null) {
            mTask = DatabaseTask().execute()
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        if (listView.adapter == null) {
            val adapter = ArrayAdapter(activity!!,
                    android.R.layout.simple_list_item_1, mExamples!!)
            listView.adapter = adapter
        }

        loadingProgress.visibility = GONE
        listView.visibility = VISIBLE

        if (mPosition >= 0) {
            val example = listView.adapter?.getItem(mPosition) as Example
            val lexikon = Lexikon.getInstance(context!!)
            val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                    lexikon.dataSourceFactory, lexikon.extractorsFactory,
                    null, null)
            mSimpleExoPlayer?.prepare(videoSource)
        }
    }

    override fun onShow() {
        mSimpleExoPlayer?.playWhenReady = true
    }

    override fun onHide() {
        mSimpleExoPlayer?.playWhenReady = false
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val example = l?.adapter?.getItem(position) as Example
        val lexikon = Lexikon.getInstance(context!!)
        val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)

        mPosition = position
        mSimpleExoPlayer?.prepare(videoSource)
    }

    companion object {
        fun newInstance(): ExampleListFragment {
            return ExampleListFragment()
        }
    }
}
