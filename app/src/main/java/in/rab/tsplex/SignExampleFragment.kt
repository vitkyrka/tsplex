package `in`.rab.tsplex

import android.net.Uri
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
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
import kotlinx.android.synthetic.main.fragment_signexample.*
import java.util.*

class SignExampleFragment : FragmentVisibilityNotifier, ListFragment() {
    private var mExamples: ArrayList<Example>? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mExamples = arguments.getParcelableArrayList(ARG_EXAMPLES)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_signexample, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        if (mExamples?.size!! <= 10) {
            filterText.visibility = GONE
        }

        val adapter = ArrayAdapter(activity,
                android.R.layout.simple_list_item_1, mExamples!!)
        listView.adapter = adapter

        filterText.addTextChangedListener(object : TextWatcher {
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                mSimpleExoPlayerView?.visibility = GONE
                adapter.filter.filter(s)
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) =
                    Unit

            override fun afterTextChanged(s: Editable?) = Unit
        })

        filterText.setOnFocusChangeListener { v, hasFocus ->
            if (hasFocus)
                mSimpleExoPlayerView?.visibility = GONE
        }

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

        if (mPosition < 0 && mExamples!!.size == 1) {
            mPosition = 0
        }

        if (mPosition >= 0) {
            val dataSourceFactory = DefaultDataSourceFactory(context,
                    Util.getUserAgent(context, "in.rab.tsplex"), null)
            val extractorsFactory = DefaultExtractorsFactory()
            val example = listView.adapter?.getItem(mPosition) as Example
            val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                    dataSourceFactory, extractorsFactory, null, null)
            mSimpleExoPlayer?.prepare(videoSource)
        }

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
        val example = l?.adapter?.getItem(position) as Example
        val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                dataSourceFactory, extractorsFactory, null, null)

        mPosition = position
        mSimpleExoPlayer?.prepare(videoSource)
    }

    companion object {
        private val ARG_EXAMPLES = "examples"

        fun newInstance(examples: ArrayList<Example>): SignExampleFragment {
            val fragment = SignExampleFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_EXAMPLES, examples)
            fragment.arguments = args
            return fragment
        }
    }
}
