package `in`.rab.tsplex

import android.provider.BaseColumns

class OrdbokenContract {

    abstract class HistoryEntry : BaseColumns {
        companion object {
            val TABLE_NAME = "history"
            val COLUMN_NAME_ID = "id"
            val COLUMN_NAME_DATE = "date"
        }
    }

    abstract class FavoritesEntry : BaseColumns {
        companion object {
            val TABLE_NAME = "favorites"
            val COLUMN_NAME_ID = "id"
            val COLUMN_NAME_DATE = "date"
        }
    }
}
