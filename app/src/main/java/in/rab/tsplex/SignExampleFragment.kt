package `in`.rab.tsplex

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.exoplayer2.util.Util
import java.util.*

class SignExampleFragment : FragmentVisibilityNotifier, ListFragment() {
    private var mVideos: ArrayList<String>? = null
    private var mDescs: ArrayList<String>? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mVideos = arguments.getStringArrayList(ARG_VIDEOS)
            mDescs = arguments.getStringArrayList(ARG_DESCS)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_signexample, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(activity,
                android.R.layout.simple_list_item_1, mDescs!!)
        listView.adapter = adapter

        mSimpleExoPlayerView = view!!.findViewById<SimpleExoPlayerView>(R.id.exoPlayerView)
        mSimpleExoPlayerView!!.setOnTouchListener { v, event ->
            if (mSimpleExoPlayer != null) {
                mSimpleExoPlayer!!.playWhenReady = !mSimpleExoPlayer!!.playWhenReady
            }

            true
        }
    }

    override fun onPause() {
        super.onPause()

        mSimpleExoPlayer?.release()
        mSimpleExoPlayer = null
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

        val dataSourceFactory = DefaultDataSourceFactory(context,
                Util.getUserAgent(context, "in.rab.tsplex"), null)
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(Uri.parse(mVideos!![mPosition]),
                dataSourceFactory, extractorsFactory, null, null)
        mSimpleExoPlayer!!.prepare(videoSource)

        mSimpleExoPlayerView!!.player = mSimpleExoPlayer
        mSimpleExoPlayer!!.repeatMode = Player.REPEAT_MODE_ALL
        mSimpleExoPlayer!!.playbackParameters = PlaybackParameters(0.7.toFloat(), 0f)
        mSimpleExoPlayer!!.playWhenReady = true
    }

    override fun onShow() {
        mSimpleExoPlayer?.playWhenReady = true;
    }

    override fun onHide() {
        mSimpleExoPlayer?.playWhenReady = false;
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val dataSourceFactory = DefaultDataSourceFactory(context,
                Util.getUserAgent(activity, "yourApplicationName"), null)
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(Uri.parse(mVideos!![position]),
                dataSourceFactory, extractorsFactory, null, null)

        mPosition = position
        mSimpleExoPlayer?.prepare(videoSource)
    }

    companion object {
        private val ARG_VIDEOS = "videos"
        private val ARG_DESCS = "descs"

        fun newInstance(videos: ArrayList<String>,
                        descs: ArrayList<String>): SignExampleFragment {
            val fragment = SignExampleFragment()
            val args = Bundle()
            args.putStringArrayList(ARG_VIDEOS, videos)
            args.putStringArrayList(ARG_DESCS, descs)
            fragment.arguments = args
            return fragment
        }
    }
}
