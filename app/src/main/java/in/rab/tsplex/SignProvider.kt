package `in`.rab.tsplex

import android.app.SearchManager
import android.content.ContentProvider
import android.content.ContentValues
import android.content.UriMatcher
import android.database.Cursor
import android.net.Uri
import android.provider.BaseColumns

class SignProvider : ContentProvider() {

    private var mDatabase: SignDatabase? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    override fun getType(uri: Uri): String? {
        return null
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? {
        return null
    }

    override fun onCreate(): Boolean {
        mDatabase = SignDatabase.getInstance(context!!)
        return true
    }

    override fun query(uri: Uri, projection: Array<String>?, selection: String?, selectionArgs: Array<String>?,
                       sortOrder: String?): Cursor? {
        val columns = arrayOf(BaseColumns._ID, SearchManager.SUGGEST_COLUMN_TEXT_1,
                SearchManager.SUGGEST_COLUMN_TEXT_2,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA,
                SearchManager.SUGGEST_COLUMN_QUERY)

        when (sUriMatcher.match(uri)) {
            MATCH_ALL -> return mDatabase!!.getAll(columns)

            MATCH_ID -> return null

            MATCH_SUGGEST -> return mDatabase!!.search(uri.lastPathSegment!!, columns)
        }

        return null
    }

    override fun update(uri: Uri, values: ContentValues?, selection: String?, selectionArgs: Array<String>?): Int {
        return 0
    }

    companion object {
        private val sUriMatcher = buildUriMatcher()

        private const val MATCH_ALL = 1
        private const val MATCH_ID = 2
        private const val MATCH_SUGGEST = 3

        private fun buildUriMatcher(): UriMatcher {
            val matcher = UriMatcher(UriMatcher.NO_MATCH)

            matcher.addURI("in.rab.tsplex.SignProvider", SearchManager.SUGGEST_URI_PATH_QUERY + "/*",
                    MATCH_SUGGEST)

            return matcher
        }
    }

}
