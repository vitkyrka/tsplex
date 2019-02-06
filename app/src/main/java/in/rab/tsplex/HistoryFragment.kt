package `in`.rab.tsplex

class HistoryFragment : ItemListFragment(mCache = false) {
    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()
        return ArrayList(SignDatabase(act).getHistory())
    }

    companion object {
        fun newInstance() = HistoryFragment()
    }
}