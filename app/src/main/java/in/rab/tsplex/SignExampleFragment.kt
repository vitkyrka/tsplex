package `in`.rab.tsplex

import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.Toast
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import kotlinx.android.synthetic.main.fragment_signexample.*
import java.util.regex.Pattern

class SignExampleFragment : FragmentVisibilityNotifier, ListFragment() {
    private var mExamples: ArrayList<Example>? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPosition = -1
    private var mId: Int = 0
    private var mTask: AsyncTask<Lexikon, Void, ArrayList<Example>>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            mId = arguments.getInt(ARG_ID)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_signexample, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mSimpleExoPlayerView = view!!.findViewById<SimpleExoPlayerView>(R.id.exoPlayerView)
        mSimpleExoPlayerView!!.setOnTouchListener { v, event ->
            if (mSimpleExoPlayer != null) {
                mSimpleExoPlayer!!.playWhenReady = !mSimpleExoPlayer!!.playWhenReady
            }

            true
        }
    }

    private inner class UncachePage : AsyncTask<Lexikon, Void, Void>() {
        override fun doInBackground(vararg params: Lexikon): Void? {
            val example = mExamples?.get(mPosition) ?: return null
            val lexikon = params[0]

            if (lexikon.isDeadLink(example.video)) {
                // The parsing will only happen when this sign is opened the next time
                lexikon.getSignPage(mId, true)
            }

            return null
        }
    }

    private inner class GetExamplesTask : AsyncTask<Lexikon, Void, ArrayList<Example>>() {
        override fun doInBackground(vararg params: Lexikon): ArrayList<Example>? {
            var examples: ArrayList<Example> = ArrayList()
            val lexikon = params[0]

            retryloop@ for (trial in 0..1) {
                examples = ArrayList()
                val page = lexikon.getSignPage(mId, trial == 1) ?: return examples
                val videos: ArrayList<String> = ArrayList()

                var pattern = Pattern.compile("file: \"(.*mp4)")
                var matcher = pattern.matcher(page)

                // The first video is not an example
                matcher.find()

                while (matcher.find()) {
                    val video = matcher.group(1)

                    if (!video.contains("-slow")) {
                        videos.add(video)
                    }
                }

                pattern = Pattern.compile(">Exempel .*?\"text\">(.*?)</span>", Pattern.DOTALL)
                matcher = pattern.matcher(page)
                val descs: ArrayList<String> = ArrayList()

                while (matcher.find()) {
                    descs.add(matcher.group(1))
                }

                if (videos.size == 0 || videos.size != descs.size) {
                    if (trial == 0) {
                        continue
                    }

                    break
                }

                for (i in 0 until videos.size) {
                    var url = "http://teckensprakslexikon.su.se/" + videos[i]
                    examples.add(Example(url, descs[i]))
                }

                break
            }

            return examples
        }

        override fun onPostExecute(examples: ArrayList<Example>) {
            mExamples = examples
            playVideo()
        }
    }

    override fun onPause() {
        super.onPause()

        mTask?.cancel(true)
        mTask = null

        mSimpleExoPlayer?.release()
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
            UncachePage().execute(Lexikon.getInstance(activity))
            getString(R.string.fail_video_play)
        } else {
            getString(R.string.fail_offline)
        }

        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
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

        mSimpleExoPlayerView!!.player = mSimpleExoPlayer
        mSimpleExoPlayer!!.repeatMode = Player.REPEAT_MODE_ALL
        mSimpleExoPlayer!!.playbackParameters = PlaybackParameters(0.7.toFloat(), 0f)
        mSimpleExoPlayer!!.playWhenReady = true

        if (mExamples == null) {
            mTask = GetExamplesTask().execute(Lexikon.getInstance(activity))
        } else {
            playVideo()
        }
    }

    fun playVideo() {
        if (listView.adapter == null) {
            val adapter = ArrayAdapter(activity,
                    android.R.layout.simple_list_item_1, mExamples!!)
            listView.adapter = adapter
        }

        loadingProgress.visibility = GONE
        listView.visibility = VISIBLE

        if (mPosition < 0 && mExamples!!.size == 1) {
            mPosition = 0
        }

        if (mPosition >= 0) {
            val example = listView.adapter?.getItem(mPosition) as Example
            val lexikon = Lexikon.getInstance(context)
            val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                    lexikon.dataSourceFactory, lexikon.extractorsFactory,
                    null, null)
            mSimpleExoPlayer?.prepare(videoSource)
        }
    }

    override fun onShow() {
        mSimpleExoPlayer?.playWhenReady = true;
    }

    override fun onHide() {
        mSimpleExoPlayer?.playWhenReady = false;
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val example = l?.adapter?.getItem(position) as Example
        val lexikon = Lexikon.getInstance(context)
        val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)

        mPosition = position
        mSimpleExoPlayer?.prepare(videoSource)
    }

    companion object {
        private val ARG_ID = "id"

        fun newInstance(sign: Sign): SignExampleFragment {
            val fragment = SignExampleFragment()
            val args = Bundle()

            args.putInt(ARG_ID, sign.id)

            fragment.arguments = args
            return fragment
        }
    }
}
