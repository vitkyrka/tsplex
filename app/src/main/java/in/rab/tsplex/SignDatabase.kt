package `in`.rab.tsplex

import android.app.SearchManager
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteQueryBuilder
import android.provider.BaseColumns
import android.util.Log
import java.util.*
import java.util.regex.Pattern

class SignDatabase(context: Context) {

    private val mSignColumns = arrayOf("DISTINCT signs.id", "sv", "signs.video", "signs.desc", "transcription", "comment", "slug", "images", "topic1", "topic2", "topic1extra", "topic2extra", "num_examples", "occurence")
    private val mExampleColumns = arrayOf("examples.video", "examples.desc", "examples.signid", "signs.sv")
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
                cursor.getLong(8),
                cursor.getLong(9),
                cursor.getString(10),
                cursor.getString(11),
                cursor.getInt(12),
                cursor.getInt(13))
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
        builder.tables = "explanations"
        cursor = builder.query(mOpenHelper.database, arrayOf("video", "desc"), selection, selectionArgs, null, null, null)
        if (cursor != null && cursor.moveToNext()) {
            sign.explanations.add(Explanation(cursor.getString(0), cursor.getString(1)))
        }
        cursor?.close()

        builder = SQLiteQueryBuilder()
        builder.tables = "examples JOIN examples_signs ON examples.rowid == examples_signs.exampleid JOIN signs ON signs.id == examples.signid"
        val columns = arrayOf("examples.video", "examples.desc", "examples.signid", "signs.sv")
        cursor = builder.query(mOpenHelper.database, columns, "examples_signs.signid = ?", selectionArgs,
                null, null, null, "100")
        if (cursor == null) {
            return sign
        }

        while (cursor.moveToNext()) {
            sign.examples.add(Example(cursor.getString(0),
                    cursor.getString(1),
                    cursor.getInt(2),
                    cursor.getString(3)))
        }

        cursor.close()
        return sign
    }

    private fun getRelatedSigns(id: Int, table: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val builder = SQLiteQueryBuilder()
        val selection = "id in (SELECT otherid FROM $table WHERE id = ?)"
        val selectionArgs = arrayOf(id.toString())
        val sortOrder = "occurence DESC, length(comment), num_examples DESC, signs.id"

        builder.tables = "signs"

        val cursor = builder.query(getDatabase(), mSignColumns, selection, selectionArgs, null, null, sortOrder)
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
                "SELECT signs.id, sv, video, desc, transcription, comment, slug, images, topic1, topic2, topic1extra, topic2extra, num_examples, occurence FROM signs INNER JOIN " + idTable +
                        " ON signs.id = " + idTable + ".id ORDER BY " + orderBy, null)
                ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getHistory(): ArrayList<Sign> = getSignsByIds("history", "history.date DESC")
    fun getFavorites(): ArrayList<Sign> = getSignsByIds("bookmarks", "signs.sv ASC")

    fun isFavorite(id: Int): Boolean {
        val builder = SQLiteQueryBuilder()
        val selection = "id = ?"
        val selectionArgs = arrayOf(id.toString())

        builder.tables = "bookmarks"

        val cursor = builder.query(getDatabase(), null, selection, selectionArgs,
                null, null, null, "1") ?: return false

        val count = cursor.count
        cursor.close()

        return count > 0
    }

    fun getFolderSigns(folderId: Int): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val builder = SQLiteQueryBuilder()
        builder.tables = "signs INNER JOIN bookmarks ON signs.id = bookmarks.id"
        val selection = "bookmarks.folderid = ?"
        val selectionArgs = arrayOf(folderId.toString())

        val cursor: Cursor = builder.query(mOpenHelper.database, mSignColumns, selection, selectionArgs,
                null, null, "signs.sv ASC") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getBookmarksFolders(): ArrayList<Folder> {
        val folders = ArrayList<Folder>()
        val cursor = getDatabase()?.query("folders", arrayOf("id", "name"), null, null, null, null, "name ASC")
                ?: return folders

        while (cursor.moveToNext()) {
            folders.add(Folder(cursor.getInt(0), cursor.getString(1)))
        }

        cursor.close()
        return folders
    }


    fun addBookmarksFolder(name: String) {
        val values = ContentValues()

        values.put("name", name)
        values.put("lastused", Date().time)

        getDatabase()?.insert("folders", "null", values)
    }

    fun removeBookmarksFolder(folderId: Int) {
        getDatabase()?.apply {
            delete("folders", "id = ?", arrayOf(folderId.toString()))
            delete("bookmarks", "folderId = ?", arrayOf(folderId.toString()))
        }
    }

    fun addToFavorites(id: Int, folderId: Int) {
        val values = ContentValues()

        values.put("id", id)
        values.put("date", Date().time)
        values.put("folderid", folderId)

        getDatabase()?.insert("bookmarks", "null", values)
    }

    fun removeFromFavorites(id: Int) {
        getDatabase()?.delete("bookmarks", "id = ?", arrayOf(id.toString()))
    }

    fun addToHistory(id: Int) {
        val values = ContentValues()

        values.put("id", id)
        values.put("date", Date().time)

        getDatabase()?.apply {
            insert("history", "null", values)

            delete("history",
                    "id IN (SELECT id FROM history ORDER BY date DESC LIMIT -1 OFFSET 50)",
                    null)
        }
    }

    private fun getSignsByDescription(query: String, columns: Array<String>, builder: SQLiteQueryBuilder, limit: String?): Cursor {
        val selection = "descsegs_signs.rowid IN (SELECT docid FROM descsegs WHERE descsegs.content MATCH ?)"
        val terms = query.split(" ").map { v -> "$v*" }
        val selectionArgs = arrayOf(terms.joinToString(" "))
        val sortOrder = "occurence DESC, length(comment), num_examples DESC, descsegs_signs.pos, descsegs_signs.len, sv"

        builder.tables = "signs JOIN descsegs_signs ON descsegs_signs.signid == signs.id"

        return builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                null, null, sortOrder, limit)
    }

    private fun getSigns(query: String, columns: Array<String>, builder: SQLiteQueryBuilder, limit: String?): Cursor {
        val fixedQuery = query.trim().toLowerCase()
        val selection = "words_signs.rowid IN (SELECT docid FROM words WHERE words.content MATCH ?) OR signs.id == ?"
        val selectionArgs = arrayOf(fixedQuery.trim() + "*", fixedQuery)
        val groupBy = "signs.id"

        // The comment is usally says that the sign is not that common so prefer no comment.
        // A lower ID is also presumed to indicate a more important sign (since it was added
        // earlier), but perhaps this is not true.
        val sortOrder = "words_signs.len, occurence DESC, length(comment), num_examples DESC, signs.id"

        builder.tables = "signs JOIN words_signs ON words_signs.signid == signs.id"

        val cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                groupBy, null, sortOrder, limit)
        if (cursor.count > 0) {
            return cursor
        }

        return getSignsByDescription(fixedQuery, columns, builder, limit)
    }

    private fun getNewTagsSignCountsFastPath(baseTagIds: List<List<Int>>, newTagIds: Array<Int>): HashMap<Int, Int> {
        val builder = SQLiteQueryBuilder()
        val selectionArgs = null
        val groupBy = "tagid"
        val columns = arrayOf("segs_tags.tagid, COUNT(DISTINCT signs_segs.signid)")
        val sortOrder = null
        val limit = null

        val selections = baseTagIds.map {
            val or = it.joinToString(",")
            "signs_segs.segid IN (SELECT segs_tags.segid FROM segs_tags WHERE segs_tags.tagid IN ($or))"
        } + arrayOf("tagid IN (${newTagIds.joinToString(",")})")

        val selection = selections.joinToString(" AND ")

        builder.tables = "signs_segs JOIN segs_tags ON signs_segs.segid == segs_tags.segid"

        val cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                groupBy, null, sortOrder, limit)

        val counts = hashMapOf<Int, Int>()

        while (cursor.moveToNext()) {
            counts[cursor.getInt(0)] = cursor.getInt(1)
        }

        cursor.close()
        return counts
    }

    fun getNewTagsSignCounts(baseTagIds: List<List<List<Int>>>, newTagIds: Array<Int>, segmentIndex: Int): HashMap<Int, Int> {
        if (baseTagIds.isEmpty()) {
            return hashMapOf()
        }
        if (baseTagIds.size == 1) {
            return getNewTagsSignCountsFastPath(baseTagIds[0], newTagIds)
        }

        val counts = hashMapOf<Int, Int>()

        val tmpBaseTags = baseTagIds.toMutableList()
        val baseSegmentTags = baseTagIds[segmentIndex]

        newTagIds.forEach {
            tmpBaseTags[segmentIndex] = baseSegmentTags + arrayListOf(arrayListOf(it))

            counts[it] = getSignsCountByTags(tmpBaseTags)
        }

        return counts
    }

    private fun getSignsByTagsQuery(tagIds: List<TagGroup>, columns: Array<String>, limit: String? = RESULTS_LIMIT): String {
        val builder = SQLiteQueryBuilder()
        val groupBy = "signs.id"
        val sortOrder = "occurence DESC, length(comment), num_examples DESC, signs.id"

        val selection = tagIds.joinToString(" OR ") { attr ->
            if (attr.isEmpty()) {
                "1=1"
            } else {
                attr.filter { it.isNotEmpty() }.joinToString(" AND ") { ids ->
                    val or = ids.joinToString(",")
                    "signs_segs.segid IN (SELECT segs_tags.segid FROM segs_tags WHERE segs_tags.tagid IN ($or))"
                }
            }
        }

        val havings = tagIds.filter { it.isNotEmpty() }.map { attr ->
            "SUM(CASE WHEN " + if (attr.isEmpty()) {
                "1=1"
            } else {
                attr.filter { it.isNotEmpty() }.joinToString(" AND ") { ids ->
                    val or = ids.joinToString(",")
                    "signs_segs.segid IN (SELECT segs_tags.segid FROM segs_tags WHERE segs_tags.tagid IN ($or))"
                }
            } + " THEN 1 ELSE 0 END) > 0"
        } + arrayOf("COUNT(segid) >= " + tagIds.size.toString())

        val having = havings.joinToString(" AND ")

        builder.tables = "signs JOIN signs_segs ON signs.id == signs_segs.signid"

        return builder.buildQuery(columns, selection, groupBy, having, sortOrder, limit)
    }

    private fun getSignsByTags(tagIds: List<TagGroup>, columns: Array<String>, limit: String? = RESULTS_LIMIT): Cursor {
        val query = getSignsByTagsQuery(tagIds, columns, limit)

        Log.i("foo", query)

        return getDatabase()!!.rawQuery(query, null)
    }


    fun getSignsByTags(tagIds: List<TagGroup>, limit: String? = RESULTS_LIMIT): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = getSignsByTags(tagIds, mSignColumns, limit)

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getSignsCountByTags(tagIds: List<TagGroup>): Int {
        var signs = 0
        val subQuery = getSignsByTagsQuery(tagIds, arrayOf("signs.id"), limit = null)
        val query = "SELECT COUNT(*) FROM (${subQuery})"

        Log.i("foo", "getSignsCountByTags: $query")

        val cursor = getDatabase()!!.rawQuery(query, null)

        while (cursor.moveToNext()) {
            signs = cursor.getInt(0)
        }

        cursor.close()
        return signs
    }

    fun getTags(id: Int): List<TagGroup> {
        val builder = SQLiteQueryBuilder()
        val selectionArgs = arrayOf(id.toString())
        val groupBy = null
        val limit = null
        val sortOrder = "signs_segs.rowid"
        val columns = arrayOf("segs_tags.segid, segs_tags.tagid")
        val selection = "signs_segs.signid = ?"

        builder.tables = "signs_segs JOIN segs_tags ON signs_segs.segid == segs_tags.segid"

        val cursor = builder.query(mOpenHelper.database, columns, selection, selectionArgs,
                groupBy, null, sortOrder, limit)

        var curSeg = -1
        var segTagIds = arrayListOf<TagList>()
        val tagIds = arrayListOf<TagGroup>()

        while (cursor.moveToNext()) {
            val seg = cursor.getInt(0)
            val tagId = cursor.getInt(1)

            if (seg == curSeg) {
                segTagIds.add(arrayListOf(tagId))
            } else {
                if (segTagIds.isNotEmpty()) {
                    tagIds.add(segTagIds)
                }

                segTagIds = arrayListOf(arrayListOf(tagId))
                curSeg = seg
            }
        }

        if (segTagIds.isNotEmpty()) {
            tagIds.add(segTagIds)
        }

        return tagIds
    }

    fun search(query: String, columns: Array<String>): Cursor {
        val builder = SQLiteQueryBuilder()
        builder.projectionMap = buildColumnMap()
        return getSigns(query, columns, builder, RESULTS_LIMIT)
    }

    fun getRandomExamples(): ArrayList<Example> {
        var examples = ArrayList<Example>()
        val builder = SQLiteQueryBuilder()
        builder.tables = "examples JOIN signs ON examples.signid == signs.id JOIN examples_signs ON examples_signs.exampleid == examples.rowid"

        val columns = mExampleColumns
        val cursor = builder.query(mOpenHelper.database, columns, null, null,
                "examples.rowid", "COUNT(*) > 1 AND LENGTH(examples.desc) < 80",
                "RANDOM() LIMIT 2") ?: return examples

        while (cursor.moveToNext()) {
            examples.add(makeExample(cursor))
        }

        cursor.close()
        return examples
    }

    fun getSigns(query: String, limit: String? = RESULTS_LIMIT): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val cursor = getSigns(query, mSignColumns, SQLiteQueryBuilder(), limit)

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    private fun getMask(topic: Long): Long {
        var remaining = topic
        var mask = 0L

        while (remaining > 0) {
            remaining = remaining shr 8
            mask = (mask shl 8) or 0xff
        }

        return mask
    }

    fun getTopics(query: String): List<Topic> {
        val literal = Pattern.quote(query)
        val pattern = Pattern.compile(".*?\\b$literal.*", Pattern.UNICODE_CASE or Pattern.CASE_INSENSITIVE)
        return Topics.topics.filter {
            pattern.matcher(it.name).matches()
        }
    }

    fun getSubTopics(topicId: Long): List<Topic> {
        val mask = getMask(topicId)
        val levelMask = ((mask shl 8) or 0xff).inv()

        return Topics.topics.filter {
            it.id != topicId && ((it.id and mask) == topicId) && ((it.id and levelMask) == 0L)
        }
    }

    fun getParentTopic(topicId: Long): List<Topic> {
        val mask = getMask(topicId) shr 8
        val levelMask = mask.inv()

        if (mask == 0L) {
            return ArrayList()
        }

        return Topics.topics.filter {
            it.id != topicId && ((it.id and mask) == (topicId and mask)) && ((it.id and levelMask) == 0L)
        }
    }

    fun getExamples(query: String, limit: String? = RESULTS_LIMIT): ArrayList<Example> {
        val examples = ArrayList<Example>()
        val builder = SQLiteQueryBuilder()
        val fixedQuery = query.trim().toLowerCase()
        val selection = "sentences_examples.rowid IN (SELECT docid FROM sentences WHERE sentences.content MATCH ?)"
        val selectionArgs = arrayOf("$fixedQuery*")
        val groupBy = "examples.rowid"
        val sortOrder = "examples.desc"

        builder.tables = "examples JOIN sentences_examples ON sentences_examples.exampleid == examples.rowid JOIN signs on signs.id == examples.signid"

        val cursor = builder.query(mOpenHelper.database, mExampleColumns, selection, selectionArgs,
                groupBy, null, sortOrder, limit) ?: return examples

        while (cursor.moveToNext()) {
            examples.add(makeExample(cursor))
        }

        cursor.close()
        return examples
    }

    fun clearHistory() {
        mOpenHelper.database?.delete("history", null, null)
    }

    fun removeAllBookmarks() {
        getDatabase()?.apply {
            delete("bookmarks", null, null)
            delete("folders", null, null)
        }
    }

    fun getExampleSigns(keyword: String): ArrayList<Sign> {
        val signs = ArrayList<Sign>()
        val selection = "examples_signs.exampleid IN (SELECT examples.rowid FROM examples WHERE examples.video LIKE ?)"
        val like = "%$keyword%"
        val selectionArgs = arrayOf(like)

        val builder = SQLiteQueryBuilder()
        builder.tables = "signs JOIN examples_signs ON signs.id == examples_signs.signid"

        val cursor: Cursor = builder.query(mOpenHelper.database, mSignColumns, selection, selectionArgs,
                null, null, "sv") ?: return signs

        while (cursor.moveToNext()) {
            signs.add(makeSign(cursor))
        }

        cursor.close()
        return signs
    }

    fun getTopicSigns(topic: Long): ArrayList<Sign> {
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
        builder.projectionMap = buildColumnMap()

        return builder.query(mOpenHelper.database, columns, null, null,
                null, null, "sv")
    }

    fun close() {
        mOpenHelper.database?.let { it.close() }
    }

    fun getDatabase() = mOpenHelper.database

    private class SignDatabaseOpenHelper(context: Context) : ShippedSQLiteOpenHelper(context, DATABASE_NAME, null, DatabaseVersion.version)

    companion object {
        private const val RESULTS_LIMIT = "100"
        private const val DATABASE_NAME = "signs.jet"

        @Volatile
        private var INSTANCE: SignDatabase? = null

        fun getInstance(context: Context): SignDatabase =
                INSTANCE ?: synchronized(this) {
                    INSTANCE ?: SignDatabase(context).also { INSTANCE = it }
                }

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
