package `in`.rab.tsplex

import Topics
import android.content.Context
import android.content.Intent
import android.graphics.Typeface
import android.net.ConnectivityManager
import android.net.Uri
import android.os.Bundle
import android.support.v4.app.Fragment
import android.text.Html
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
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
import java.lang.IllegalStateException


class SignDescriptionFragment : FragmentVisibilityNotifier, Fragment() {
    private var mListener: OnTopicClickListener? = null
    private var mWord: String? = null
    private var mVideo: String? = null
    private var mDescription: String? = null
    private var mComment: String? = null
    private var mSimpleExoPlayerView: SimpleExoPlayerView? = null
    private var mSimpleExoPlayer: SimpleExoPlayer? = null
    private var mTopic1: Int = 0
    private var mTopic2: Int = 0
    private var mId: Int = 0
    private var mControllerVisible: Boolean = false
    private var mSpeed: Float = 0.0f
    private var mTranscriptionUrl: String? = null
    private var mExamples: ArrayList<Example>? = null
    private var mPosition = 0
    private var mScrollPos = 0
    private var mAdapter: ArrayAdapter<Example>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            mId = args.getInt(ARG_ID)
            mWord = args.getString(ARG_WORD)
            mDescription = args.getString(ARG_DESC)
            mVideo = args.getString(ARG_VIDEO)
            mComment = args.getString(ARG_COMMENT)
            mTranscriptionUrl = args.getString(ARG_TRANSCRIPTION)
            mTopic1 = args.getInt(ARG_TOPIC1)
            mTopic2 = args.getInt(ARG_TOPIC2)
            mExamples = args.getParcelableArrayList(ARG_EXAMPLES)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        try {
            outState.putInt("scrollPos", scrollView.scrollY)
        } catch (e: IllegalStateException) {
            // java.lang.IllegalStateException: scrollView must not be null
        }
        outState.putInt("videoPosition", mPosition)
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_sign_description, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        mSimpleExoPlayerView = view.findViewById(R.id.exoPlayerView)

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

        var desc = mDescription!!.split("//").joinToString("//<br>")
        if (!desc.endsWith('.')) {
            desc = "$desc."
        }

        @Suppress("DEPRECATION")
        view.findViewById<TextView>(R.id.textView).text = Html.fromHtml(desc)
        val idButton = view.findViewById<Button>(R.id.id)
        idButton.text = "[%05d]".format(mId)
        idButton.setOnClickListener { v ->
            val b: Button = v as Button
            // The slash at the end ensures that it doesn't match our app links filter
            val url = "https://teckensprakslexikon.su.se/ord/%05d/".format(mId)
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        }

        wordText.text = mWord
        if (mComment!!.isNotEmpty()) {
            commentText.text = mComment
            commentTitle.visibility = VISIBLE
            commentText.visibility = VISIBLE
        }

        if (mTopic1 != 0 && Topics.names[mTopic1]?.startsWith("Ospec") == false) {
            topics.visibility = VISIBLE

            var button = view.findViewById<Button>(R.id.topic1)
            button.text = Topics.names[mTopic1]
            button.setOnClickListener { mListener!!.onTopicClick(mTopic1) }
            button.visibility = VISIBLE

            if (mTopic2 != 0) {
                button = view.findViewById(R.id.topic2)
                button.text = Topics.names[mTopic2]
                button.setOnClickListener { mListener!!.onTopicClick(mTopic2) }
                button.visibility = VISIBLE

                topics.text = getString(R.string.topics)
            }
        }

        textView.setOnClickListener {
            if (transcriptionImage.visibility == VISIBLE) {
                transcriptionImage.visibility = GONE
                textView.maxLines = 3
            } else {
                Glide.with(this).load(mTranscriptionUrl).into(transcriptionImage)
                transcriptionImage.visibility = VISIBLE
                textView.maxLines = 100
            }
        }

        val videoExample = arrayListOf(Example(mVideo!!, mWord!!))
        val adapter = ArrayAdapter(activity!!,
                R.layout.item_video, videoExample + mExamples!!)

        mAdapter = adapter

        for (i in 0 until adapter.count) {
            val v = adapter.getView(i, null, videoGroup) as RadioButton
            v.id = i
            v.setOnCheckedChangeListener { buttonView, isChecked ->
                if (isChecked)
                    buttonView.typeface = Typeface.DEFAULT_BOLD
                else
                    buttonView.typeface = Typeface.DEFAULT
            }
            v.setOnClickListener {
                val example = adapter.getItem(i) as Example
                val lexikon = Lexikon.getInstance(context!!)
                val videoSource = ExtractorMediaSource(Uri.parse("https://teckensprakslexikon.su.se/" + example.video),
                        lexikon.dataSourceFactory, lexikon.extractorsFactory,
                        null, null)

                mPosition = i
                mSimpleExoPlayer?.prepare(videoSource)
            }
            if (i != 0) {
                v.setOnLongClickListener {
                    mListener?.onExampleLongClick(adapter.getItem(i)!!)
                    true
                }
            }
            if (i == 0) {
                v.isChecked = true
            }
            videoGroup.addView(v)
        }

        videoGroup.visibility = VISIBLE

        if (savedInstanceState != null) {
            mScrollPos = savedInstanceState.getInt("scrollPos")
            mPosition = savedInstanceState.getInt("videoPosition", -1)
        }
    }

    override fun onPause() {
        super.onPause()

        val exo = mSimpleExoPlayer ?: return
        val settings = activity!!.getSharedPreferences("in.rab.tsplex", 0).edit()

        if (mSpeed != 0.0f) {
            settings.putFloat("signPlaybackSpeed", mSpeed)
        }

        settings.putInt("signRepeatMode", exo.repeatMode)
        settings.apply()

        mScrollPos = scrollView.scrollY

        mSimpleExoPlayerView?.player = null
        exo.release()
        mSimpleExoPlayer = null

    }

    private fun playVideo() {
        if (context == null) {
            return
        }

        val video = mAdapter?.getItem(mPosition)?.video ?: return
        val r = videoGroup.getChildAt(mPosition) as RadioButton
        r.isChecked = true

        val lexikon = Lexikon.getInstance(context!!)
        val videoSource = ExtractorMediaSource(Uri.parse("https://teckensprakslexikon.su.se/$video"),
                lexikon.dataSourceFactory, lexikon.extractorsFactory,
                null, null)
        mSimpleExoPlayer?.prepare(videoSource)
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

                    if (mScrollPos != 0) {
                        val scrollPos = mScrollPos
                        scrollView.post { scrollView.smoothScrollTo(0, scrollPos) }
                        mScrollPos = 0
                    }
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

        mSimpleExoPlayerView?.player = mSimpleExoPlayer
        mSimpleExoPlayer?.repeatMode = settings.getInt("signRepeatMode", Player.REPEAT_MODE_ALL)
        mSimpleExoPlayer?.playbackParameters = PlaybackParameters(speed, 1f)
        mSimpleExoPlayer?.playWhenReady = true

        loadingProgress.visibility = VISIBLE
        playVideo()
    }

    override fun onShow() {
        mSimpleExoPlayer?.playWhenReady = true
    }

    override fun onHide() {
        mSimpleExoPlayer?.playWhenReady = false
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
        fun onExampleLongClick(example: Example)
    }

    companion object {
        private const val ARG_ID = "id"
        private const val ARG_WORD = "word"
        private const val ARG_DESC = "desc"
        private const val ARG_VIDEO = "video"
        private const val ARG_COMMENT = "comment"
        private const val ARG_TOPIC1 = "topic1"
        private const val ARG_TOPIC2 = "topic2"
        private const val ARG_TRANSCRIPTION = "transcription"
        private const val ARG_EXAMPLES = "examples"

        fun newInstance(sign: Sign): SignDescriptionFragment {
            val fragment = SignDescriptionFragment()
            val args = Bundle()
            val desc = StringBuilder(sign.description)

            args.putInt(ARG_ID, sign.id)
            args.putString(ARG_DESC, desc.toString())
            args.putString(ARG_WORD, sign.word)
            args.putString(ARG_VIDEO, sign.video)
            args.putString(ARG_COMMENT, sign.comment)
            args.putString(ARG_TRANSCRIPTION, sign.getTranscriptionUrl())
            args.putInt(ARG_TOPIC1, sign.topic1)
            args.putInt(ARG_TOPIC2, sign.topic2)
            args.putParcelableArrayList(ARG_EXAMPLES, sign.examples)

            fragment.arguments = args
            return fragment
        }
    }
}
