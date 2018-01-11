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
import com.google.android.exoplayer2.ext.okhttp.OkHttpDataSourceFactory
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.AdaptiveTrackSelection
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionArray
import com.google.android.exoplayer2.ui.SimpleExoPlayerView
import com.google.android.exoplayer2.upstream.DefaultBandwidthMeter
import com.google.android.exoplayer2.util.Util
import kotlinx.android.synthetic.main.fragment_signexample.*
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import java.util.*
import java.util.regex.Pattern

class SignExampleFragment : FragmentVisibilityNotifier, ListFragment() {
    private var mExamples: ArrayList<Example>? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mPosition = -1
    private var mId: Int = 0
    private var mTask: AsyncTask<OkHttpClient, Void, ArrayList<Example>>? = null

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

    private inner class GetExamplesTask : AsyncTask<OkHttpClient, Void, ArrayList<Example>>() {
        override fun doInBackground(vararg params: OkHttpClient): ArrayList<Example>? {
            val examples: ArrayList<Example> = ArrayList()
            val number = "%05d".format(mId)
            val client = params[0]
            val request = Request.Builder()
                    .url("http://teckensprakslexikon.su.se/ord/" + number)
                    .build()

            var response: Response? = null
            val page = try {
                response = client.newCall(request).execute();
                response.body()?.string()
            } catch (e: IOException) {
                return examples
            } finally {
                response?.body()?.close()
            } ?: return examples

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

            if (videos.size != descs.size) {
                return examples
            }

            (0 until videos.size).mapTo(examples) {
                Example("http://teckensprakslexikon.su.se/" + videos[it], descs[it])
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
            mTask = GetExamplesTask().execute(LexikonClient.getInstance(activity))
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
            val dataSourceFactory = OkHttpDataSourceFactory(LexikonClient.getInstance(context),
                    Util.getUserAgent(context, "in.rab.tsplex"), null)
            val extractorsFactory = DefaultExtractorsFactory()
            val example = listView.adapter?.getItem(mPosition) as Example
            val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                    dataSourceFactory, extractorsFactory, null, null)
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
        val dataSourceFactory = OkHttpDataSourceFactory(LexikonClient.getInstance(context),
                Util.getUserAgent(context, "in.rab.tsplex"), null)
        val extractorsFactory = DefaultExtractorsFactory()
        val example = l?.adapter?.getItem(position) as Example
        val videoSource = ExtractorMediaSource(Uri.parse(example.video),
                dataSourceFactory, extractorsFactory, null, null)

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
