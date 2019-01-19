package `in`.rab.tsplex

abstract class DatabaseListFragment(private val idTable: String, private val orderBy: String) : SignListFragment() {
    override fun getSigns(): ArrayList<Item> {
        val act = activity ?: return java.util.ArrayList()
        return ArrayList(SignDatabase(act).getSignsByIds(idTable, orderBy))
    }
}
