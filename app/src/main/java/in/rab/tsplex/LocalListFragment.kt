package `in`.rab.tsplex

abstract class LocalListFragment(val idTable: String, val orderBy: String) : SignListFragment() {
    override fun getSigns(): ArrayList<Sign> {
        return SignDatabase(activity).getSignsByIds(idTable, orderBy)
    }
}