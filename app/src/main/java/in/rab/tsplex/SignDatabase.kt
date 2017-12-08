package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.BaseColumns
import java.util.*

class SignDatabase(context: Context) {

    private val mOpenHelper: SignDatabaseOpenHelper

    init {
        mOpenHelper = SignDatabaseOpenHelper(context)
    }

    private fun query(selection: String?, selectionArgs: Array<String>?, columns: Array<String>): Cursor {
        val builder = SQLiteQueryBuilder()
        builder.tables = "signs"
        builder.setProjectionMap(buildColumnMap())

        return builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, "sv")
    }

    fun getSign(id: Int): Sign? {
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())
        var columns = arrayOf("sv", "video", "desc", "slug", "images", "topic1", "topic2")

        var builder = SQLiteQueryBuilder()
        builder.tables = "signs"

        var cursor: Cursor? = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, null) ?: return null

        cursor!!.moveToNext()
        val sign = Sign(id, cursor.getString(0), cursor.getString(1),
                cursor.getString(2), cursor.getString(3),
                cursor.getInt(4), cursor.getInt(5), cursor.getInt(6))

        builder = SQLiteQueryBuilder()
        builder.tables = "examples"
        columns = arrayOf("video", "desc")
        cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, null)
        if (cursor == null) {
            return sign
        }

        while (cursor.moveToNext()) {
            sign.examples.add(Example(cursor.getString(0), cursor.getString(1)))
        }

        return sign
    }

    fun getExamples(): Sign {
        val sign = Sign(0,  "", "", "", "", 0, 0, 0)

        val builder = SQLiteQueryBuilder()
        builder.tables = "examples"
        val columns = arrayOf("video", "desc")
        val cursor = builder.query(mOpenHelper.database, columns, null, null,
                "video", null, "desc") ?: return sign

        while (cursor.moveToNext()) {
            sign.examples.add(Example(cursor.getString(0), cursor.getString(1)))
        }

        cursor.close()
        return sign
    }

    internal fun getSynonyms(id: Int): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = mOpenHelper.database!!.rawQuery(
                "SELECT id, sv, video, desc, slug, images, topic1, topic2 FROM signs WHERE id in (SELECT otherid FROM synonyms WHERE id = ?)",
                arrayOf(id.toString())) ?: return signs

        while (cursor.moveToNext()) {
            signs.add(Sign(cursor.getInt(0), cursor.getString(1),
                    cursor.getString(2), cursor.getString(3),
                    cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), cursor.getInt(7)))
        }

        return signs
    }

    internal fun getHomonyms(id: Int): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = mOpenHelper.database!!.rawQuery(
                "SELECT id, sv, video, desc, slug, images, topic1, topic2 FROM signs WHERE id in (SELECT otherid FROM homonyms WHERE id = ?)",
                arrayOf(id.toString())) ?: return signs

        while (cursor.moveToNext()) {
            signs.add(Sign(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), cursor.getInt(7)))
        }

        return signs
    }

    fun getSignsByIds(idTable: String, orderBy: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = mOpenHelper.database!!.rawQuery(
                "SELECT signs.id, sv, video, desc, slug, images, topic1, topic2 FROM signs INNER JOIN " + idTable +
                        " ON signs.id = " + idTable + ".id ORDER BY " + orderBy, null) ?: return signs

        while (cursor.moveToNext()) {
            signs.add(Sign(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), cursor.getInt(7)))
        }

        cursor.close()

        return signs
    }

    fun getSigns(query: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val selection = "sv like ?"
        val selectionArgs = arrayOf(query + "%")
        val columns = arrayOf("id", "sv", "video", "desc", "slug", "images", "topic1", "topic2")

        val builder = SQLiteQueryBuilder()
        builder.tables = "signs"

        val cursor: Cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, "sv") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(Sign(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), cursor.getInt(7)))
        }

        return signs
    }

    private fun getMask(topic: Int): Int {
        var remaining = topic
        var mask = 0

        while (remaining > 0) {
            remaining = remaining shr 8;
            mask = (mask shl 8) or 0xff
        }

        return mask
    }

    fun getTopicSigns(topic: Int): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val mask = getMask(topic)
        val columns = arrayOf("id", "sv", "video", "desc", "slug", "images", "topic1", "topic2")

        val selection = StringBuilder()
                .append("(topic1 & ").append(mask).append(") = ").append(topic).append(" OR")
                .append("(topic2 & ").append(mask).append(") = ").append(topic)
                .toString()

        val builder = SQLiteQueryBuilder()
        builder.tables = "signs"

        val cursor: Cursor = builder.query(mOpenHelper.database, columns, selection, null,
                null, null, "sv") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(Sign(cursor.getInt(0), cursor.getString(1), cursor.getString(2),
                    cursor.getString(3), cursor.getString(4), cursor.getInt(5),
                    cursor.getInt(6), cursor.getInt(7)))
        }

        return signs
    }

    fun getAll(columns: Array<String>): Cursor {
        return query(null, null, columns)
    }

    fun search(query: String, columns: Array<String>): Cursor {
        val selection = "sv like ?"
        val selectionArgs = arrayOf(query + "%")

        return query(selection, selectionArgs, columns)
    }

    fun getDatabase() = mOpenHelper.database

    private class SignDatabaseOpenHelper internal constructor(context: Context) : ShippedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION) {
    }

    companion object {
        private val DATABASE_NAME = "signs.jet"
        private val DATABASE_VERSION = 18

        private fun buildColumnMap(): HashMap<String, String> {
            val map = HashMap<String, String>()

            map.put(BaseColumns._ID, "rowid AS " + BaseColumns._ID)
            map.put("desc", "desc")
            map.put(SearchManager.SUGGEST_COLUMN_TEXT_1,
                    "sv AS " + SearchManager.SUGGEST_COLUMN_TEXT_1)
            map.put(SearchManager.SUGGEST_COLUMN_QUERY, "sv AS " + SearchManager.SUGGEST_COLUMN_QUERY)
            map.put(SearchManager.SUGGEST_COLUMN_TEXT_2,
                    "desc AS " + SearchManager.SUGGEST_COLUMN_TEXT_2)
            map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA, "id AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA)

            return map
        }
    }
}