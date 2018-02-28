package `in`.rab.tsplex

import android.os.Bundle

class ArrayListFragment : SignListFragment() {
    private var signs: ArrayList<Sign> = ArrayList<Sign>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            signs = arguments.getParcelableArrayList(ARG_SIGNS)
        }
    }

    override fun getSigns(): ArrayList<Sign> {
        return signs
    }

    companion object {
        private val ARG_SIGNS = "signs"

        fun newInstance(signs: ArrayList<Sign>): ArrayListFragment {
            val fragment = ArrayListFragment()
            val args = Bundle()
            args.putParcelableArrayList(ARG_SIGNS, signs)
            fragment.arguments = args
            return fragment
        }
    }
}
