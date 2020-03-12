package `in`.rab.tsplex

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Build
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.ContextCompat
import com.bumptech.glide.Priority
import com.bumptech.glide.RequestManager
import com.bumptech.glide.request.RequestOptions
import kotlin.math.min


class ItemRecyclerViewAdapter(private val mPlayHandler: OnItemPlayHandler,
                              private val mSigns: List<Item>,
                              private val mListener: ItemListFragment.OnListFragmentInteractionListener?,
                              private val mGlide: RequestManager,
                              private val mLayoutParams: FrameLayout.LayoutParams) : androidx.recyclerview.widget.RecyclerView.Adapter<androidx.recyclerview.widget.RecyclerView.ViewHolder>() {
    private var selectedPosition = -1

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): androidx.recyclerview.widget.RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.fragment_sign -> SignViewHolder(view, viewType)
            R.layout.item_description -> DescriptionViewHolder(view, viewType)
            R.layout.item_header -> HeaderViewHolder(view, viewType)
            R.layout.item_topic -> TopicViewHolder(view, viewType)
            R.layout.item_folder -> FolderViewHolder(view, viewType)
            else -> ExampleViewHolder(view, viewType)
        }
    }

    internal fun setSelected(position: Int) {
        if (selectedPosition >= 0 && selectedPosition < mSigns.size) {
            notifyItemChanged(selectedPosition)
        }

        if (position < 0 || position >= mSigns.size) {
            selectedPosition = -1
            return
        }

        selectedPosition = position
        notifyItemChanged(position)
    }

    @SuppressLint("SetTextI18n")
    private fun bindSign(holder: SignViewHolder, sign: Sign, position: Int) {
        holder.mIdView.text = sign.word

        if (sign.examplesCount > 0) {
            holder.mExamplesCountText.text = sign.examplesCount.toString()
            holder.mExamplesCountText.visibility = VISIBLE
        } else {
            holder.mExamplesCountText.visibility = GONE
        }


        val urls = sign.getImageUrls()

        holder.mImages.stopFlipping()
        holder.mImages.displayedChild = 0

        val realCount = sign.transcription.codePointCount(0, sign.transcription.length)
        val numShow = 15

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

        holder.mPlayButton.setOnClickListener {
            mPlayHandler.onItemPlay(sign, position)
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
            mGlide.load(urls[urls.size - 1]).apply(lowOptions).into(holder.imageViews[i])
        }

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(sign)
        }
    }

    private fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    private fun bindExample(holder: ExampleViewHolder, example: Example, position: Int) {
        holder.mIdView.text = fromHtml("$example <em>(<strong>${example.signWord}</strong>)</em>")

        holder.mIdView.setOnClickListener {
            mPlayHandler.onExampleClick(example, position)
        }

        holder.mPlayButton.setOnClickListener {
            mPlayHandler.onItemPlay(example, position)
        }

        holder.mExampleSearch.setOnClickListener {
            mListener?.onExampleSearchClick(example)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindDescription(holder: DescriptionViewHolder, description: Description, position: Int) {
        val sign = description.mSign

        with(holder) {
            mWordText.text = sign.word
            mTranscriptionText.text = sign.transcription

            val desc = sign.description.split("//").joinToString("//<br>")
            mTextView.text = Html.fromHtml(if (!desc.endsWith('.')) {
                "$desc."
            } else {
                desc
            })

            itemView.setOnClickListener {
                mPlayHandler.onItemPlay(sign, position)
            }

            if (sign.comment.isNotEmpty()) {
                mCommentText.text = "${sign.comment}."
                mCommentText.visibility = VISIBLE
            } else {
                mCommentText.visibility = GONE
            }
        }
    }

    private fun bindHeader(holder: HeaderViewHolder, header: Header) {
        holder.mIdView.text = header.toString()
    }

    private fun bindTopic(holder: TopicViewHolder, topic: Topic) {
        holder.mIdView.text = topic.toString()

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(topic)
        }
    }

    private fun bindFolder(holder: FolderViewHolder, folder: Folder) {
        holder.mIdView.text = folder.toString()

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(folder)
        }

        holder.mView.setOnLongClickListener {
            mListener?.onItemLongClick(folder) ?: false
        }
    }

    override fun onBindViewHolder(holder: androidx.recyclerview.widget.RecyclerView.ViewHolder, position: Int) {
        val item = mSigns[position]

        holder.itemView.setBackgroundColor(if (position == selectedPosition) {
            ContextCompat.getColor(holder.itemView.context, R.color.selected)
        } else {
            Color.TRANSPARENT
        })

        when (getItemViewType(position)) {
            R.layout.fragment_sign -> bindSign(holder as SignViewHolder, item as Sign, position)
            R.layout.item_example -> bindExample(holder as ExampleViewHolder, item as Example, position)
            R.layout.item_description -> bindDescription(holder as DescriptionViewHolder, item as Description, position)
            R.layout.item_header -> bindHeader(holder as HeaderViewHolder, item as Header)
            R.layout.item_topic -> bindTopic(holder as TopicViewHolder, item as Topic)
            R.layout.item_folder -> bindFolder(holder as FolderViewHolder, item as Folder)
        }
    }

    override fun getItemCount(): Int {
        return mSigns.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = mSigns[position]

        return when (item) {
            is Description -> R.layout.item_description
            is Sign -> R.layout.fragment_sign
            is Example -> R.layout.item_example
            is Topic -> R.layout.item_topic
            is Folder -> R.layout.item_folder
            else -> R.layout.item_header
        }
    }

    fun getSpanSize(position: Int): Int {
        if (position >= mSigns.size) {
            return 1
        }

        return when (getItemViewType(position)) {
            R.layout.fragment_sign -> 1
            else -> 2
        }
    }

    inner class SignViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
        val mImages: ViewFlipper = mView.findViewById(R.id.images)
        val mTranscriptionText: TextView = mView.findViewById(R.id.transcriptionText)
        val mExamplesCountText: TextView = mView.findViewById(R.id.examplesCountText)
        val mPlayButton: ImageButton = mView.findViewById(R.id.playButton)
        private val imageViewIds = intArrayOf(R.id.image1, R.id.image2, R.id.image3, R.id.image4)
        val imageViews: Array<ImageView> = Array(imageViewIds.size) { i ->
            mView.findViewById<ImageView>(imageViewIds[i])
        }
    }

    inner class ExampleViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
        val mPlayButton: ImageButton = mView.findViewById(R.id.playButton)
        val mExampleSearch: ImageButton = mView.findViewById(R.id.exampleSearch)
    }


    inner class DescriptionViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mWordText: TextView = mView.findViewById(R.id.wordText)
        val mWordWrapper: LinearLayout = mView.findViewById(R.id.wordWrapper)
        val mTranscriptionText: TextView = mView.findViewById(R.id.transcriptionText)
        val mTextView: TextView = mView.findViewById(R.id.textView)
        val mCommentText: TextView = mView.findViewById(R.id.commentText)
    }

    inner class HeaderViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
    }

    inner class TopicViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
    }

    inner class FolderViewHolder(val mView: View, val mViewType: Int) : androidx.recyclerview.widget.RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
    }

    interface OnItemPlayHandler {
        fun onItemPlay(item: Sign, position: Int)
        fun onItemPlay(item: Example, position: Int)
        fun onExampleClick(item: Example, position: Int)
    }
}
