package `in`.rab.tsplex

abstract class DatabaseListFragment(private val idTable: String, private val orderBy: String) : ItemListFragment(mCache = false) {
    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()
        return ArrayList(SignDatabase(act).getSignsByIds(idTable, orderBy))
    }
}
