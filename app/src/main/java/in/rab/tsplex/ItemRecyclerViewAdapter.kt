package `in`.rab.tsplex

import android.annotation.SuppressLint
import android.support.v7.widget.RecyclerView
import android.text.Html
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
import android.os.Build
import android.text.Spanned



class ItemRecyclerViewAdapter(private val mSigns: List<Item>,
                              private val mListener: ItemListFragment.OnListFragmentInteractionListener?,
                              private val mGlide: RequestManager,
                              private val mLayoutParams: FrameLayout.LayoutParams) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(viewType, parent, false)

        return when (viewType) {
            R.layout.fragment_sign -> SignViewHolder(view, viewType)
            R.layout.item_header -> HeaderViewHolder(view, viewType)
            R.layout.item_search -> SearchViewHolder(view, viewType)
            else -> ExampleViewHolder(view, viewType)
        }
    }

    @SuppressLint("SetTextI18n")
    private fun bindSign(holder: SignViewHolder, sign: Sign) {
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

    private fun bindExample(holder: ExampleViewHolder, example: Example) {
        holder.mIdView.text = fromHtml("$example <em>(<strong>${example.signWord}</strong>)</em>")

        holder.mView.setOnClickListener {
            mListener?.onListFragmentInteraction(example)
        }
    }

    private fun bindHeader(holder: HeaderViewHolder, header: Header) {
        holder.mIdView.text = header.toString()
    }

    private fun bindSearch(holder: SearchViewHolder, search: Search) {
        holder.mView.findViewById<EditText>(R.id.homeSearchView)?.setOnClickListener {
            mListener?.onListFragmentInteraction(search)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val item = mSigns[position]

        when (getItemViewType(position)) {
            R.layout.fragment_sign -> bindSign(holder as SignViewHolder, item as Sign)
            R.layout.item_example -> bindExample(holder as ExampleViewHolder, item as Example)
            R.layout.item_header -> bindHeader(holder as HeaderViewHolder, item as Header)
            R.layout.item_search -> bindSearch(holder as SearchViewHolder, item as Search)
        }
    }

    override fun getItemCount(): Int {
        return mSigns.size
    }

    override fun getItemViewType(position: Int): Int {
        val item = mSigns[position]

        return if (item is Sign) {
            R.layout.fragment_sign
        } else if (item is Example) {
            R.layout.item_example
        } else if (item is Search) {
            R.layout.item_search
        } else {
            R.layout.item_header
        }
    }

    fun getSpanSize(position: Int): Int {
        return when (getItemViewType(position)) {
            R.layout.item_example -> 2
            R.layout.item_header -> 2
            R.layout.item_search -> 2
            else -> 1
        }
    }

    inner class SignViewHolder(val mView: View, val mViewType: Int) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
        val mImages: ViewFlipper = mView.findViewById(R.id.images)
        val mTranscriptionText: TextView = mView.findViewById(R.id.transcriptionText)
        val mExamplesCountText: TextView = mView.findViewById(R.id.examplesCountText)
        private val imageViewIds = intArrayOf(R.id.image1, R.id.image2, R.id.image3, R.id.image4)
        val imageViews: Array<ImageView> = Array(imageViewIds.size) { i ->
            mView.findViewById<ImageView>(imageViewIds[i])
        }
    }

    inner class ExampleViewHolder(val mView: View, val mViewType: Int) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
    }

    inner class HeaderViewHolder(val mView: View, val mViewType: Int) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById(R.id.id)
    }

    inner class SearchViewHolder(val mView: View, val mViewType: Int) : RecyclerView.ViewHolder(mView)
}
