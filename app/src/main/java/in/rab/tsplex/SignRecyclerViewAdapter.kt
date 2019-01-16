package `in`.rab.tsplex

import android.annotation.SuppressLint
import android.opengl.Visibility
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import kotlin.math.min

class SignRecyclerViewAdapter(private val mSigns: List<Sign>,
                              private val mExamples: List<Example>,
                              private val mListener: SignListFragment.OnListFragmentInteractionListener?,
                              private val mGlide: RequestManager,
                              private val mLayoutParams: FrameLayout.LayoutParams) : RecyclerView.Adapter<SignRecyclerViewAdapter.ViewHolder>() {

    constructor(signs: List<Sign>, listener: SignListFragment.OnListFragmentInteractionListener?,
                glide: RequestManager, layoutParams: FrameLayout.LayoutParams) :
            this(signs, ArrayList<Example>(), listener, glide, layoutParams)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)
        return ViewHolder(view, viewType)
    }

    @SuppressLint("SetTextI18n")
    private fun bindSign(holder: ViewHolder, position: Int) {
        val sign = mSigns[position]
        holder.mItem = sign
        holder.mIdView.text = sign.word

        val urls = sign.getImageUrls()

        holder.mImages.stopFlipping()
        holder.mImages.displayedChild = 0

        val realCount = sign.transcription.codePointCount(0, sign.transcription.length)
        val numShow = 20

        if (realCount > numShow) {
            val cutOffset = sign.transcription.offsetByCodePoints(0, min(numShow, realCount))
            holder.mTranscriptionText.text = sign.transcription.substring(0, cutOffset) + "…"
        } else {
            holder.mTranscriptionText.text = sign.transcription
        }

        for (view in holder.imageViews) {
            view.layoutParams = mLayoutParams
        }

        val lowOptions = RequestOptions.priorityOf(Priority.LOW)
        val highOptions = RequestOptions.priorityOf(Priority.HIGH)

        if (urls.size > 1) {
            holder.mTranscriptionText.setOnClickListener {
                if (holder.mImages.isFlipping) {
                    holder.mImages.stopFlipping()
                    holder.mImages.displayedChild = 0
                } else {
                    holder.mImages.startFlipping()
                }
            }
        } else {
            holder.mTranscriptionText.setOnClickListener(null)
        }

        for ((i, url) in urls.withIndex()) {
            if (i >= holder.imageViews.size) {
                break
            }

            val options = if (i == 0) {
                highOptions
            } else {
                lowOptions
            }

            mGlide.load(url).apply(options).into(holder.imageViews[i])
        }

        for (i in urls.size until holder.imageViews.size) {
            mGlide.load(urls[i % urls.size]).apply(lowOptions).into(holder.imageViews[i])
        }

        holder.mView.setOnClickListener {
            val item = holder.mItem as Sign?

            if (item != null) {
                mListener?.onListFragmentInteraction(item)
            }
        }
    }

    private fun bindExample(holder: ViewHolder, position: Int) {
        val example = mExamples[position]
        holder.mItem = example
        holder.mIdView.text = example.toString()

        holder.mView.setOnClickListener {
            val item = holder.mItem as Example?

            if (item != null) {
                mListener?.onListFragmentInteraction(item)
            }
        }
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        when (holder.mViewType) {
            R.layout.fragment_sign -> bindSign(holder, position)
            R.layout.item_example -> bindExample(holder, position - mSigns.size)
        }
    }

    override fun getItemCount(): Int {
        return mSigns.size + mExamples.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (position < mSigns.size) {
            R.layout.fragment_sign
        } else {
            R.layout.item_example
        }
    }

    inner class ViewHolder(val mView: View, val mViewType: Int) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
        val mImages: ViewFlipper = mView.findViewById(R.id.images)
        val mTranscriptionText: TextView = mView.findViewById(R.id.transcriptionText)
        private val imageViewIds = intArrayOf(R.id.image1, R.id.image2, R.id.image3, R.id.image4)
        val imageViews: Array<ImageView> = Array(imageViewIds.size) { i ->
            mView.findViewById<ImageView>(imageViewIds[i])
        }
        var mItem: Any? = null
    }
}
