package `in`.rab.tsplex

import android.content.Context
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.DividerItemDecoration
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.bumptech.glide.Glide

class SignFragment : FragmentVisibilityNotifier, Fragment() {
    private var signs: ArrayList<Sign> = ArrayList<Sign>()
    private var mListener: SignListFragment.OnListFragmentInteractionListener? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            signs = arguments.getParcelableArrayList(ARG_SIGNS)
        }
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
            view.adapter = SignRecyclerViewAdapter(signs, mListener, Glide.with(this));
        }
        return view
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)
        if (context is SignListFragment.OnListFragmentInteractionListener) {
            mListener = context
        } else {
            throw RuntimeException(context!!.toString() + " must implement OnListFragmentInteractionListener")
        }
    }

    override fun onDetach() {
        super.onDetach()
        mListener = null
    }

    companion object {
        private val ARG_SIGNS = "signs"

        fun newInstance(signs: ArrayList<Sign>): SignFragment {
            val fragment = SignFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_SIGNS, signs)
            fragment.arguments = args
            return fragment
        }
    }
}
