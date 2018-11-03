package `in`.rab.tsplex

import android.provider.BaseColumns

class OrdbokenContract {

    abstract class HistoryEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "history"
            const val COLUMN_NAME_ID = "id"
            const val COLUMN_NAME_DATE = "date"
        }
    }

    abstract class FavoritesEntry : BaseColumns {
        companion object {
            const val TABLE_NAME = "favorites"
            const val COLUMN_NAME_ID = "id"
            const val COLUMN_NAME_DATE = "date"
        }
    }
}
