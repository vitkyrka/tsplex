package `in`.rab.tsplex

import Topics
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
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
import kotlinx.android.synthetic.main.exo_playback_control_view.*
import kotlinx.android.synthetic.main.fragment_sign_description.*
import java.util.regex.Pattern


class SignDescriptionFragment : FragmentVisibilityNotifier, Fragment() {
    private var mListener: OnTopicClickListener? = null
    private var mVideo: String? = null
    private var mWord: String? = null
    private var mDescription: String? = null
    private var mComment: String? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mTopic1: Int = 0
    private var mTopic2: Int = 0
    private var mId: Int = 0
    private var mVideoTask: AsyncTask<Lexikon, Void, String?>? = null
    private var mControllerVisible: Boolean = false
    private var mSpeed: Float = 0.0f
    private var mTranscriptionUrl: String ?= null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            mId = args.getInt(ARG_ID)
            mWord = args.getString(ARG_WORD)
            mDescription = args.getString(ARG_DESC)
            mComment = args.getString(ARG_COMMENT)
            mTranscriptionUrl = args.getString(ARG_TRANSCRIPTION)
            mTopic1 = args.getInt(ARG_TOPIC1)
            mTopic2 = args.getInt(ARG_TOPIC2)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mSimpleExoPlayerView = view.findViewById<SimpleExoPlayerView>(R.id.exoPlayerView)

        val listener = View.OnClickListener {
            val speed = when (it.id) {
                R.id.exo_050x -> 0.50f
                R.id.exo_075x -> 0.75f
                else -> 1.0f
            }

            mSpeed = speed;
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

        val desc = mDescription!!.split("//").joinToString("//<br>")
        @Suppress("DEPRECATION")
        view.findViewById<TextView>(R.id.textView).text = Html.fromHtml(desc)
        val idButton = view.findViewById<Button>(R.id.id)
        idButton.text = "%05d".format(mId)
        idButton.setOnClickListener(object : View.OnClickListener {
            override fun onClick(v: View?) {
                val b: Button = v as Button
                val url = "https://teckensprakslexikon.su.se/ord/" + b.text
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
        })

        wordText.text = mWord;
        if (mComment!!.isNotEmpty()) {
            commentText.text = mComment;
            commentTitle.visibility = VISIBLE
            commentText.visibility = VISIBLE
        }

        Glide.with(this).load(mTranscriptionUrl).into(transcriptionImage)

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

        val exo = mSimpleExoPlayer ?: return
        val settings = activity!!.getSharedPreferences("in.rab.tsplex", 0).edit()

        if (mSpeed != 0.0f) {
            settings.putFloat("signPlaybackSpeed", mSpeed)
        }

        settings.putInt("signRepeatMode", exo.repeatMode)
        settings.apply()

        mSimpleExoPlayerView?.player = null
        exo.release()
        mSimpleExoPlayer = null
    }

    fun playVideo() {
        if (context == null) {
            return
        }

        val lexikon = Lexikon.getInstance(context!!)
        val videoSource = ExtractorMediaSource(Uri.parse(mVideo),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)

        val settings = activity!!.getSharedPreferences("in.rab.tsplex", 0)
        val speed = settings.getFloat("signPlaybackSpeed", 0.75f)

        exo_speed.clearCheck()

        when (speed) {
            0.50f -> exo_050x.isChecked = true
            0.75f -> exo_075x.isChecked = true
            else -> exo_100x.isChecked = true
        }

        mSpeed = speed;

        mSimpleExoPlayer?.prepare(videoSource)
        mSimpleExoPlayerView?.player = mSimpleExoPlayer
        mSimpleExoPlayer?.repeatMode = settings.getInt("signRepeatMode", Player.REPEAT_MODE_ALL)
        mSimpleExoPlayer?.playbackParameters = PlaybackParameters(speed, 1f)
        mSimpleExoPlayer?.playWhenReady = true
    }

    private fun isOnline(): Boolean {
        val conman = context?.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager?
        val networkInfo = conman?.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }

    fun showError() {
        loadingProgress.visibility = GONE
        mSimpleExoPlayerView?.visibility = GONE

        val msg: String = if (isOnline()) {
            getString(R.string.fail_video_play)
        } else {
            getString(R.string.fail_offline)
        }

        Toast.makeText(activity, msg, Toast.LENGTH_LONG).show()
    }

    private inner class GetVideoUrlTask : AsyncTask<Lexikon, Void, String?>() {
        override fun doInBackground(vararg params: Lexikon): String? {
            val lexikon = params[0]

            for (trial in 0..1) {
                val page = lexikon.getSignPage(mId, trial == 1) ?: return null

                var pattern = Pattern.compile("\"([^\"]+\\.mp4)\"")
                val matcher = pattern.matcher(page)
                if (!matcher.find()) {
                    if (trial == 0) {
                        continue
                    }

                    return null
                }

                val url = "https://teckensprakslexikon.su.se/" + matcher.group(1)
                if (!lexikon.cacheVideo(url)) {
                    if (trial == 0 && lexikon.isDeadLink(url)) {
                        continue
                    }

                    return null
                }

                return url
            }

            return null
        }

        override fun onPostExecute(video: String?) {
            if (video == null) {
                showError()
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
                showError()
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
            mVideoTask = GetVideoUrlTask().execute(Lexikon.getInstance(activity!!))
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
        private val ARG_WORD = "word"
        private val ARG_DESC = "desc"
        private val ARG_COMMENT = "comment"
        private val ARG_TOPIC1 = "topic1"
        private val ARG_TOPIC2 = "topic2"
        private val ARG_TRANSCRIPTION = "transcription"

        fun newInstance(sign: Sign): SignDescriptionFragment {
            val fragment = SignDescriptionFragment()
            val args = Bundle()
            val desc = StringBuilder(sign.description)

            args.putInt(ARG_ID, sign.id)
            args.putString(ARG_DESC, desc.toString())
            args.putString(ARG_WORD, sign.word)
            args.putString(ARG_COMMENT, sign.comment)
            args.putString(ARG_TRANSCRIPTION, sign.getTranscriptionUrl())
            args.putInt(ARG_TOPIC1, sign.topic1)
            args.putInt(ARG_TOPIC2, sign.topic2)

            fragment.arguments = args
            return fragment
        }
    }
}
