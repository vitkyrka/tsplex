package `in`.rab.tsplex

import android.content.ContentValues
import android.content.Context
import android.database.DatabaseErrorHandler
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteDatabase.CursorFactory
import android.database.sqlite.SQLiteException
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.channels.Channels

abstract class ShippedSQLiteOpenHelper @JvmOverloads constructor(private val mContext: Context, private val databaseName: String, private val mFactory: CursorFactory?,
                                                                 private val mWantedVersion: Int, private val mErrorHandler: DatabaseErrorHandler? = null) {

    private var mDatabase: SQLiteDatabase? = null
    private var mIsInitializing: Boolean = false
    private var mVersion: Int = 0

    val database: SQLiteDatabase?
        get() = synchronized(this) {
            val db: SQLiteDatabase? = try {
                databaseLocked
            } catch (e: SQLiteException) {
                null
            }

            if (db != null) {
                if (mVersion == mWantedVersion) {
                    return db
                }
            }

            try {
                Log.d("ShippedSQLiteOpenHelper", "Copy database, mVersion ${mVersion} mWantedVersion ${mWantedVersion}")
                copyDatabase(db)
            } catch (e: IOException) {
                throw SQLiteException("Unable to copy database: " + e.message)
            }

            return databaseLocked
        }

    private val databaseLocked: SQLiteDatabase?
        get() {
            if (mDatabase != null) {
                if (!mDatabase!!.isOpen) {
                    mDatabase = null
                } else {
                    return mDatabase
                }
            }

            if (mIsInitializing) {
                throw IllegalStateException("getDatabase called recursively")
            }

            var db = mDatabase
            try {
                mIsInitializing = true
                mVersion = 0

                if (db == null) {
                    val path = mContext.getDatabasePath(databaseName).path
                    db = SQLiteDatabase.openDatabase(path, mFactory, SQLiteDatabase.OPEN_READWRITE,
                            mErrorHandler)
                }

                if (db != null) {
                    mVersion = db.version
                }

                mDatabase = db
                return db
            } finally {
                mIsInitializing = false
                if (db != null && db != mDatabase) {
                    db.close()
                }
            }
        }

    @Throws(IOException::class)
    private fun copyApkDatabase(destPath: String) {
        val afd = mContext.assets.openFd(databaseName)
        val outputFile = File(destPath)
        val inputStream = mContext.assets.open(databaseName)
        val inputChan = Channels.newChannel(inputStream)

        outputFile.parentFile.mkdirs()

        val output = FileOutputStream(outputFile)
        val outputChan = output.channel

        outputChan.transferFrom(inputChan, 0, afd.length)

        inputChan.close()
        outputChan.close()
        afd.close()
    }

    private fun copyUserTable(old: SQLiteDatabase, new: SQLiteDatabase, table: String) {
        val cursor = try {
            old.rawQuery("SELECT id, date FROM $table", null) ?: return
        } catch (e: SQLiteException) {
            return
        }

        while (cursor.moveToNext()) {
            val values = ContentValues()

            values.put("id", cursor.getInt(0))
            values.put("date", cursor.getLong(1))

            new.insertOrThrow(table, null, values)
        }

        cursor.close()
    }

    private fun copyFavoritesToBookmarks(old: SQLiteDatabase, new: SQLiteDatabase) {
        val cursor = try {
            old.rawQuery("SELECT id, date FROM favorites", null) ?: return
        } catch (e: SQLiteException) {
            return
        }

        while (cursor.moveToNext()) {
            val values = ContentValues()

            values.put("id", cursor.getInt(0))
            values.put("date", cursor.getLong(1))
            values.put("folderid", 0)

            new.insertOrThrow("bookmarks", null, values)
        }

        cursor.close()
    }

    private fun copyFolders(old: SQLiteDatabase, new: SQLiteDatabase) {
        val cursor = try {
            old.rawQuery("SELECT id, name, lastused FROM folders", null) ?: return
        } catch (e: SQLiteException) {
            return
        }

        while (cursor.moveToNext()) {
            val values = ContentValues()

            values.put("id", cursor.getInt(0))
            values.put("name", cursor.getString(1))
            values.put("lastused", cursor.getLong(0))

            new.insertOrThrow("folders", null, values)
        }

        cursor.close()
    }

    private fun copyBoomarks(old: SQLiteDatabase, new: SQLiteDatabase) {
        val cursor = try {
            old.rawQuery("SELECT id, date, folderid FROM bookmarks", null) ?: return
        } catch (e: SQLiteException) {
            return
        }

        while (cursor.moveToNext()) {
            val values = ContentValues()

            values.put("id", cursor.getInt(0))
            values.put("date", cursor.getLong(1))
            values.put("folderid", cursor.getInt(0))

            new.insertOrThrow("bookmarks", null, values)
        }

        cursor.close()
    }

    private fun copyUserTables(old: SQLiteDatabase, new: SQLiteDatabase) {
        copyUserTable(old, new, "history")
        copyFavoritesToBookmarks(old, new)
        copyFolders(old, new)
        copyBoomarks(old, new)
    }

    private fun createUserTables(new: SQLiteDatabase) {
        val values = ContentValues()

        // teckenspråk
        values.put("id", 5382)
        values.put("date", 1)

        new.insertOrThrow("history", null, values)

        // lexikon
        values.put("id", 4999)
        values.put("date", 0)

        new.insertOrThrow("history", null, values)
    }

    @Throws(IOException::class)
    private fun copyDatabase(oldDb: SQLiteDatabase?) {
        val tempPath = mContext.getDatabasePath("$databaseName-tmp").path

        copyApkDatabase(tempPath)

        val newDb = SQLiteDatabase.openDatabase(tempPath, mFactory, SQLiteDatabase.OPEN_READWRITE,
                mErrorHandler) ?: return

        if (oldDb == null) {
            createUserTables(newDb)
        } else {
            copyUserTables(oldDb, newDb)
            oldDb.close()
            mDatabase = null
            mVersion = 0
        }

        newDb.close()

        val from = File(tempPath)
        val to = File(mContext.getDatabasePath(databaseName).path)
        from.renameTo(to)
    }
}