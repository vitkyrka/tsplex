package `in`.rab.tsplex

import Topics
import android.content.Context
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import kotlinx.android.synthetic.main.fragment_sign_description.*
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException
import java.util.regex.Pattern


class SignDescriptionFragment : FragmentVisibilityNotifier, Fragment() {
    private var mListener: OnTopicClickListener? = null
    private var mVideo: String? = null
    private var mDescription: String? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mTopic1: Int = 0
    private var mTopic2: Int = 0
    private var mId: Int = 0
    private var mVideoTask: AsyncTask<OkHttpClient, Void, String?>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (arguments != null) {
            mId = arguments.getInt(ARG_ID)
            mDescription = arguments.getString(ARG_DESC)
            mTopic1 = arguments.getInt(ARG_TOPIC1)
            mTopic2 = arguments.getInt(ARG_TOPIC2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        return inflater!!.inflate(R.layout.fragment_sign_description, container, false)
    }

    override fun onViewCreated(view: View?, savedInstanceState: Bundle?) {
        if (view == null) {
            return
        }

        mSimpleExoPlayerView = view.findViewById<SimpleExoPlayerView>(R.id.exoPlayerView)
        mSimpleExoPlayerView!!.setOnTouchListener { _, _ ->
            if (mSimpleExoPlayer != null) {
                mSimpleExoPlayer!!.playWhenReady = !mSimpleExoPlayer!!.playWhenReady
            }

            true
        }

        view.findViewById<TextView>(R.id.textView).text = Html.fromHtml(mDescription)


        if (mTopic1 != 0) {
            view.findViewById<TextView>(R.id.topics).visibility = VISIBLE

            var button = view.findViewById<Button>(R.id.topic1)
            button.text = Topics.names[mTopic1]
            button.setOnClickListener(object : View.OnClickListener {
                override fun onClick(v: View?) {
                    mListener!!.onTopicClick(mTopic1)
                }
            })
            button.visibility = VISIBLE

            if (mTopic2 != 0) {
                button = view.findViewById<Button>(R.id.topic2)
                button.text = Topics.names[mTopic2]
                button.setOnClickListener(object : View.OnClickListener {
                    override fun onClick(v: View?) {
                        mListener!!.onTopicClick(mTopic2)
                    }
                })
                button.visibility = VISIBLE
            }
        }
    }

    override fun onPause() {
        super.onPause()

        mVideoTask?.cancel(true)
        mVideoTask = null

        mSimpleExoPlayer?.release()
        mSimpleExoPlayer = null
    }

    fun playVideo() {
        if (context == null) {
            return
        }

        val dataSourceFactory = OkHttpDataSourceFactory(LexikonClient.getInstance(context),
                Util.getUserAgent(context, "in.rab.tsplex"), null)
        val extractorsFactory = DefaultExtractorsFactory()
        val videoSource = ExtractorMediaSource(Uri.parse(mVideo),
                dataSourceFactory, extractorsFactory, null, null)

        mSimpleExoPlayer?.prepare(videoSource)
        mSimpleExoPlayerView?.player = mSimpleExoPlayer
        mSimpleExoPlayer?.repeatMode = Player.REPEAT_MODE_ALL
        mSimpleExoPlayer?.playbackParameters = PlaybackParameters(0.7.toFloat(), 0f)
        mSimpleExoPlayer?.playWhenReady = true
    }

    fun isOnline(): Boolean {
        val conman = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkInfo = conman?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    private inner class GetVideoUrlTask : AsyncTask<OkHttpClient, Void, String?>() {
        override fun doInBackground(vararg params: OkHttpClient): String? {
            val number = "%05d".format(mId)
            val client = params[0]
            val request = Request.Builder()
                    .url("http://teckensprakslexikon.su.se/ord/" + number)
                    .build()

            val page = try {
                val response = client.newCall(request).execute();
                response.body()?.string()
            } catch (e: IOException) {
                return null
            } ?: return null

            val pattern = Pattern.compile("file: \"(.*mp4)")
            val matcher = pattern.matcher(page)
            if (!matcher.find()) {
                return null
            }

            return "http://teckensprakslexikon.su.se/" + matcher.group(1)
        }

        override fun onPostExecute(video: String?) {
            if (video == null) {
                loadingProgress.visibility = GONE

                if (activity != null) {
                    val msg: String = if (isOnline()) {
                        getString(R.string.fail_video_play)
                    } else {
                        getString(R.string.fail_offline)
                    }

                    Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
                }

                return
            }

            mVideo = video
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
            }

            override fun onTracksChanged(trackGroups: TrackGroupArray?, trackSelections: TrackSelectionArray?) {
            }

            override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters?) {
            }

            override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
                if (playbackState == Player.STATE_READY) {
                    mSimpleExoPlayerView?.visibility = VISIBLE
                    loadingProgress.visibility = GONE
                }
            }

        })

        if (mVideo == null) {
            loadingProgress.visibility = VISIBLE
            mVideoTask = GetVideoUrlTask().execute(LexikonClient.getInstance(activity))
        } else {
            playVideo()
        }
    }

    override fun onShow() {
        mSimpleExoPlayer?.playWhenReady = true;
    }

    override fun onHide() {
        mSimpleExoPlayer?.playWhenReady = false;
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnTopicClickListener) {
            mListener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnTopicClickListener {
        fun onTopicClick(topic: Int)
    }

    companion object {
        private val ARG_ID = "id"
        private val ARG_DESC = "desc"
        private val ARG_TOPIC1 = "topic1"
        private val ARG_TOPIC2 = "topic2"

        fun newInstance(sign: Sign): SignDescriptionFragment {
            val fragment = SignDescriptionFragment()
            val args = Bundle()
            val desc = StringBuilder(sign.description)

            args.putInt(ARG_ID, sign.id)
            args.putString(ARG_DESC, desc.toString())
            args.putInt(ARG_TOPIC1, sign.topic1)
            args.putInt(ARG_TOPIC2, sign.topic2)

            fragment.arguments = args
            return fragment
        }
    }
}
