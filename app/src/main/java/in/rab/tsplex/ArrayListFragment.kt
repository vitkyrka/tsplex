package `in`.rab.tsplex

import android.os.Bundle

class ArrayListFragment : ItemListFragment() {
    private var items: List<Item> = ArrayList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            items = args.getSerializable(ARG_ITEMS)!! as ArrayList<Item>
        }
    }

    override fun getSigns(): List<Item> {
        return items
    }

    companion object {
        private const val ARG_ITEMS = "items"

        fun newInstance(items: ArrayList<Item>): ArrayListFragment {
            val fragment = ArrayListFragment()
            val args = Bundle()
            args.putSerializable(ARG_ITEMS, items)
            fragment.arguments = args
            return fragment
        }
    }
}
