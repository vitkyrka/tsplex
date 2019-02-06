package `in`.rab.tsplex

import android.app.SearchManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.BaseColumns
import java.util.*

class SignDatabase(context: Context) {

    private val mSignColumns = arrayOf("signs.id", "sv", "signs.video", "signs.desc", "transcription", "comment", "slug", "images", "topic1", "topic2", "num_examples")
    private val mExampleColumns = arrayOf("examples.video", "examples.desc", "examples.id", "signs.sv")
    private val mExampleTables = "examples JOIN signs on examples.id == signs.id"
    private val mOpenHelper: SignDatabaseOpenHelper

    init {
        mOpenHelper = SignDatabaseOpenHelper(context)
    }

    private fun makeSign(cursor: Cursor): Sign {
        return Sign(cursor.getInt(0),
                cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5),
                cursor.getString(6),
                cursor.getInt(7),
                cursor.getInt(8),
                cursor.getInt(9),
                cursor.getInt(10))
    }

    private fun makeExample(cursor: Cursor): Example {
        return Example(cursor.getString(0),
                cursor.getString(1),
                cursor.getInt(2),
                cursor.getString(3))
    }

    fun getSign(id: Int): Sign? {
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())

        var builder = SQLiteQueryBuilder()
        builder.tables = "signs"

        var cursor: Cursor = builder.query(mOpenHelper.database, mSignColumns, selection, selectionArgs,
                null, null, null) ?: return null

        if (!cursor.moveToNext()) {
            return null
        }

        val sign = makeSign(cursor)

        cursor.close()

        builder = SQLiteQueryBuilder()
        builder.tables = mExampleTables
        val columns = mExampleColumns
        cursor = builder.query(mOpenHelper.database, columns, "examples.id = ?", selectionArgs,
                null, null, null)
        if (cursor == null) {
            return sign
        }

        while (cursor.moveToNext()) {
            sign.examples.add(makeExample(cursor))
        }

        cursor.close()
        return sign
    }

    private fun getRelatedSigns(id: Int, table: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val builder = SQLiteQueryBuilder()
        val selection = "id in (SELECT otherid FROM $table WHERE id = ?)"
        val selectionArgs = arrayOf(id.toString())

        builder.tables = "signs"

        val cursor = builder.query(getDatabase(), mSignColumns, selection, selectionArgs, null, null, null)
                ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    internal fun getSynonyms(id: Int): ArrayList<Sign> = getRelatedSigns(id, "synonyms")
    internal fun getHomonyms(id: Int): ArrayList<Sign> = getRelatedSigns(id, "homonyms")

    fun getSignsByIds(idTable: String, orderBy: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = mOpenHelper.database!!.rawQuery(
                "SELECT signs.id, sv, video, desc, transcription, comment, slug, images, topic1, topic2, num_examples FROM signs INNER JOIN " + idTable +
                        " ON signs.id = " + idTable + ".id ORDER BY " + orderBy, null)
                ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getHistory(): ArrayList<Sign> = getSignsByIds("history", "history.date DESC")
    fun getFavorites(): ArrayList<Sign> = getSignsByIds("favorites", "signs.sv ASC")

    fun isFavorite(id: Int): Boolean {
        val builder = SQLiteQueryBuilder()
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())

        builder.tables = "favorites"

        val cursor = builder.query(getDatabase(), null, selection, selectionArgs,
                null, null, null, "1") ?: return false

        val count = cursor.count
        cursor.close()

        return count > 0
    }

    fun addToFavorites(id: Int) {
        val values = ContentValues()

        values.put("id", id)
        values.put("date", Date().time)

        getDatabase()?.insert("favorites", "null", values)
    }

    fun removeFromFavorites(id: Int) {
        getDatabase()?.delete("favorites", "id = ?",  arrayOf(id.toString()))
    }

    private fun getSignsByDescription(query: String, columns: Array<String>, builder: SQLiteQueryBuilder): Cursor {
        val selection = "descsegs_signs.rowid IN (SELECT docid FROM descsegs WHERE descsegs.content MATCH ?)"
        val terms = query.split(" ").map { v -> "$v*" }
        val selectionArgs = arrayOf(terms.joinToString(" "))
        val sortOrder = "descsegs_signs.pos, descsegs_signs.len, sv"

        builder.tables = "signs JOIN descsegs_signs ON descsegs_signs.signid == signs.id"

        return builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, sortOrder)
    }

    private fun getSigns(query: String, columns: Array<String>, builder: SQLiteQueryBuilder): Cursor {
        val fixedQuery = query.trim().toLowerCase()
        val selection = "words_signs.rowid IN (SELECT docid FROM words WHERE words.content MATCH ?) OR signs.id == ?"
        val selectionArgs = arrayOf(fixedQuery.trim() + "*", fixedQuery)
        val groupBy = "signs.id"

        // The comment is usally says that the sign is not that common so prefer no comment.
        // A lower ID is also presumed to indicate a more important sign (since it was added
        // earlier), but perhaps this is not true.
        val sortOrder = "words_signs.len, length(comment), num_examples DESC, signs.id"

        builder.tables = "signs JOIN words_signs ON words_signs.signid == signs.id"

        val cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                groupBy, null, sortOrder)
        if (cursor.count > 0) {
            return cursor
        }

        return getSignsByDescription(fixedQuery, columns, builder)
    }

    fun search(query: String, columns: Array<String>): Cursor {
        val builder = SQLiteQueryBuilder()
        builder.setProjectionMap(buildColumnMap())
        return getSigns(query, columns, builder)
    }

    fun getRandomExamples(): ArrayList<Example> {
        var examples = ArrayList<Example>()
        val builder = SQLiteQueryBuilder()
        builder.tables = mExampleTables

        val columns = mExampleColumns
        val cursor = builder.query(mOpenHelper.database, columns, null, null,
                "examples.video", null,
                "RANDOM() LIMIT 2") ?: return examples

        while (cursor.moveToNext()) {
            examples.add(makeExample(cursor))
        }

        cursor.close()
        return examples
    }

    fun getSigns(query: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = getSigns(query, mSignColumns, SQLiteQueryBuilder())

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    private fun getMask(topic: Int): Int {
        var remaining = topic
        var mask = 0

        while (remaining > 0) {
            remaining = remaining shr 8
            mask = (mask shl 8) or 0xff
        }

        return mask
    }

    fun getExamples(query: String?): ArrayList<Example> {
        var examples = ArrayList<Example>()
        val builder = SQLiteQueryBuilder()
        builder.tables = mExampleTables

        var selection: String? = null
        var selectionArgs: Array<String>? = null

        if (query != null) {
            selection = "examples.desc LIKE ?"
            selectionArgs = arrayOf("%$query%")
        }

        val columns = mExampleColumns
        val cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                "examples.video", null, "examples.desc") ?: return examples

        while (cursor.moveToNext()) {
            examples.add(makeExample(cursor))
        }

        cursor.close()
        return examples
    }

    fun clearHistory() {
        mOpenHelper.database?.execSQL("DELETE from history")
    }

    fun removeAllBookmarks() {
        mOpenHelper.database?.execSQL("DELETE from favorites")
    }

    fun getExampleSigns(keyword: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val selection = "examples.desc LIKE ? OR examples.video LIKE ?"
        val like = "%$keyword%"
        val selectionArgs = arrayOf(like, like)
        val groupBy = "signs.id"

        val builder = SQLiteQueryBuilder()
        builder.tables = "signs JOIN examples ON examples.id == signs.id"

        val cursor: Cursor = builder.query(mOpenHelper.database, mSignColumns, selection, selectionArgs,
                groupBy, null, "sv") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getTopicSigns(topic: Int): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val mask = getMask(topic)

        val selection = StringBuilder()
                .append("(topic1 & ").append(mask).append(") = ").append(topic).append(" OR")
                .append("(topic2 & ").append(mask).append(") = ").append(topic)
                .toString()

        val builder = SQLiteQueryBuilder()
        builder.tables = "signs"

        val cursor: Cursor = builder.query(mOpenHelper.database, mSignColumns, selection, null,
                null, null, "sv") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getAll(columns: Array<String>): Cursor {
        val builder = SQLiteQueryBuilder()
        builder.tables = "signs"
        builder.setProjectionMap(buildColumnMap())

        return builder.query(mOpenHelper.database, columns, null, null,
                null, null, "sv")
    }

    fun close() {
        mOpenHelper.database?.let { it.close() }
    }

    fun getDatabase() = mOpenHelper.database

    private class SignDatabaseOpenHelper internal constructor(context: Context) : ShippedSQLiteOpenHelper(context, DATABASE_NAME, null, DATABASE_VERSION)

    companion object {
        private const val DATABASE_NAME = "signs.jet"
        const val DATABASE_VERSION = 31

        private fun buildColumnMap(): HashMap<String, String> {
            val map = HashMap<String, String>()

            map[BaseColumns._ID] = "signs.rowid AS " + BaseColumns._ID
            map["desc"] = "desc"
            map[SearchManager.SUGGEST_COLUMN_TEXT_1] = "sv AS " + SearchManager.SUGGEST_COLUMN_TEXT_1
            map[SearchManager.SUGGEST_COLUMN_QUERY] = "sv AS " + SearchManager.SUGGEST_COLUMN_QUERY
            map[SearchManager.SUGGEST_COLUMN_TEXT_2] = "desc AS " + SearchManager.SUGGEST_COLUMN_TEXT_2
            map[SearchManager.SUGGEST_COLUMN_INTENT_DATA] = "id AS " + SearchManager.SUGGEST_COLUMN_INTENT_DATA

            return map
        }
    }
}