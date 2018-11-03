package `in`.rab.tsplex

abstract class DatabaseListFragment(val idTable: String, val orderBy: String) : SignListFragment() {
    override fun getSigns(): ArrayList<Sign> {
        val act = activity ?: return java.util.ArrayList<Sign>()
        return SignDatabase(act).getSignsByIds(idTable, orderBy)
    }
}
