package `in`.rab.tsplex

import android.content.Context
import android.os.AsyncTask
import android.os.Bundle
import android.os.Parcelable
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide

abstract class SignListFragment : FragmentVisibilityNotifier, Fragment() {
    private var mListener: OnListFragmentInteractionListener? = null
    private var recylerView: RecyclerView? = null
    private var mState: Parcelable? = null
    private var mTask: AsyncTask<Void, Void, ArrayList<Sign>>? = null

    protected abstract fun getSigns(): ArrayList<Sign>

    private inner class DatabaseTask : AsyncTask<Void, Void, ArrayList<Sign>>() {
        override fun doInBackground(vararg params: Void): ArrayList<Sign> {
            return getSigns()
        }

        override fun onPostExecute(signs: ArrayList<Sign>) {
            if (!isAdded)
                return;

            recylerView!!.adapter = SignRecyclerViewAdapter(signs, mListener,
                    Glide.with(this@SignListFragment));

            if (mState != null) {
                recylerView!!.layoutManager?.onRestoreInstanceState(mState)
            }
        }
    }

    override fun onResume() {
        super.onResume()

        mTask = DatabaseTask().execute()
    }

    override fun onCreateView(inflater: LayoutInflater?, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        val view = inflater!!.inflate(R.layout.fragment_sign_list, container, false)

        if (view is RecyclerView) {
            val context = view.getContext()
            val layoutManager = LinearLayoutManager(context);
            val decoration = DividerItemDecoration(getContext(), layoutManager.orientation)

            view.addItemDecoration(decoration)
            view.layoutManager = layoutManager
            recylerView = view
        }
        return view
    }

    override fun onPause() {
        super.onPause()

        mTask?.cancel(true)
        mTask = null
        mState = recylerView?.layoutManager?.onSaveInstanceState()
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    interface OnListFragmentInteractionListener {
        fun onListFragmentInteraction(item: Sign)
    }
}
