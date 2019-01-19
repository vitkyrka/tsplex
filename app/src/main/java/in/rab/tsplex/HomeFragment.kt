package `in`.rab.tsplex

import kotlin.math.min

class HomeFragment : ItemListFragment() {
    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()

        val history = SignDatabase(act).getSignsByIds("history",
                "history.date DESC")
        val favorites = SignDatabase(act).getSignsByIds("favorites",
                "signs.sv ASC")

        val signs = ArrayList<Item>()

        signs.add(Header(getString(R.string.title_history)))
        signs.addAll(history.subList(0, min(2, history.size)))

        signs.add(Header(getString(R.string.title_favorites)))
        signs.addAll(favorites.subList(0, min(2, favorites.size)))

        return signs
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}