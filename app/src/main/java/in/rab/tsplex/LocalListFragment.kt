package `in`.rab.tsplex

import android.database.Cursor
import android.database.sqlite.SQLiteDatabase

abstract class LocalListFragment(val idTable: String, val orderBy: String) : SignListFragment() {
    override fun getSigns(): ArrayList<Sign> {
        return SignDatabase(activity).getSignsByIds(idTable, orderBy)
    }
}