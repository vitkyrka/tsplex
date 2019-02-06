package `in`.rab.tsplex

class FavoritesFragment : ItemListFragment(mCache = false) {
    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()
        return ArrayList(SignDatabase(act).getFavorites())
    }

    companion object {
        fun newInstance() = FavoritesFragment()
    }
}