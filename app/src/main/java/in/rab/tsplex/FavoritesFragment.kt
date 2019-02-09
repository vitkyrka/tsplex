package `in`.rab.tsplex

class FavoritesFragment : ItemListFragment(mCache = false, mEmptyText = R.string.no_bookmarks) {
    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()
        return ArrayList(SignDatabase.getInstance(act).getFavorites())
    }

    companion object {
        fun newInstance() = FavoritesFragment()
    }
}