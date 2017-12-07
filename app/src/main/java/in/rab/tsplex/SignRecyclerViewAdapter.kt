package `in`.rab.tsplex

import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import com.bumptech.glide.RequestManager

class SignRecyclerViewAdapter(private val mValues: List<Sign>,
                              private val mListener: SignListFragment.OnListFragmentInteractionListener?,
                              private val mGlide: RequestManager) : RecyclerView.Adapter<SignRecyclerViewAdapter.ViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.fragment_sign, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val sign = mValues[position]
        holder.mItem = sign
        holder.mIdView.text = sign.word

        val urls = sign.getImageUrls()

        for ((i, url) in sign.getImageUrls().withIndex()) {
            if (i >= holder.imageViews.size) {
                break;
            }

            mGlide.load(url).into(holder.imageViews[i])
            holder.imageViews[i].visibility = View.VISIBLE;
        }

        for (i in urls.size until holder.imageViews.size) {
            mGlide.clear(holder.imageViews[i])
            holder.imageViews[i].setImageDrawable(null)
            holder.imageViews[i].visibility = View.GONE;
        }

        holder.mView.setOnClickListener {
            var sign = holder.mItem

            if (sign != null) {
                mListener?.onListFragmentInteraction(sign)
            }
        }
    }

    override fun getItemCount(): Int {
        return mValues.size
    }

    inner class ViewHolder(val mView: View) : RecyclerView.ViewHolder(mView) {
        val mIdView: TextView = mView.findViewById<TextView>(R.id.id)
        private val imageViewIds = intArrayOf(R.id.image1, R.id.image2, R.id.image3);
        val imageViews: Array<ImageView> = Array(imageViewIds.size, { i ->
            mView.findViewById<ImageView>(imageViewIds[i])
        })
        var mItem: Sign? = null
    }
}
