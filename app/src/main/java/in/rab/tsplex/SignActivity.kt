package `in`.rab.tsplex

import `in`.rab.tsplex.OrdbokenContract.FavoritesEntry
import `in`.rab.tsplex.OrdbokenContract.HistoryEntry
import android.app.SearchManager
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import com.google.android.material.tabs.TabLayout
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentPagerAdapter
import androidx.viewpager.widget.ViewPager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import java.util.*
import kotlin.collections.ArrayList


class SignActivity : RoutingAppCompactActivity(), ItemListFragment.OnListFragmentInteractionListener {
    private var mOrdboken: Ordboken? = null
    private var mStarred: Boolean = false
    private var mGotStarred: Boolean = false
    private var mSign: Sign? = null
    private var mExampleUrl: String? = null
    private var mSynonyms: ArrayList<Item>? = null
    private var mPosition = 0
    private var mViewPager: androidx.viewpager.widget.ViewPager? = null
    private var mTabLayout: TabLayout? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mOrdboken = Ordboken.getInstance(this)

        setContentView(R.layout.activity_sign)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mStarred = false

        title = ""

        val pager = findViewById<androidx.viewpager.widget.ViewPager>(R.id.viewPager)
        pager.addOnPageChangeListener(
                object : androidx.viewpager.widget.ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        var fragment = pager.adapter?.instantiateItem(pager, mPosition) as FragmentVisibilityNotifier
                        fragment.onHide()

                        mPosition = position
                        fragment = pager.adapter?.instantiateItem(pager, position) as FragmentVisibilityNotifier
                        fragment.onShow()
                    }
                }
        )

        mViewPager = pager
        mTabLayout = findViewById(R.id.tabs)

        handleIntent(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        handleIntent(intent)
    }

    private fun failLoad(signId: String) {
        // The slash at the end ensures that it doesn't match our app links filter
        val browserIntent = Intent(Intent.ACTION_VIEW, Uri.parse("https://teckensprakslexikon.su.se/ord/$signId/"))
        startActivity(browserIntent)
        finish()
    }

    private fun handleIntent(intent: Intent) {
        var signId = intent.getStringExtra("url")

        if (intent.action == Intent.ACTION_VIEW && signId == null) {
            signId = intent.data?.lastPathSegment
        }

        if (signId == null) {
            finish()
        }

        mExampleUrl = intent.getStringExtra("exampleUrl")

        SignLoadTask().execute(signId)
    }

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Example) {
    }

    private inner class TabPagerAdapter(fm: androidx.fragment.app.FragmentManager) : androidx.fragment.app.FragmentPagerAdapter(fm) {
        override fun getPageTitle(position: Int): CharSequence {
            var adjpos = position

            if (adjpos > 0) {
                if (mSynonyms!!.size == 0) {
                    adjpos += 1
                }
            }

            return when (adjpos) {
                0 -> getText(R.string.sign)
                1 -> getText(R.string.relations)
                else -> ""
            }
        }

        override fun getItem(pos: Int): androidx.fragment.app.Fragment? {
            var adjpos = pos

            if (adjpos > 0) {
                if (mSynonyms!!.size == 0) {
                    adjpos += 1
                }
            }

            return when (adjpos) {
                0 -> SignFragment.newInstance(mSign!!, mExampleUrl)
                1 -> ArrayListFragment.newInstance(mSynonyms!!)
                else -> null
            }
        }

        override fun getCount(): Int {
            var count = 1

            if (mSynonyms!!.size > 0) {
                count += 1
            }

            return count
        }
    }

    private inner class SignLoadTask : AsyncTask<String, Void, Sign?>() {
        var mSignId: String = ""

        override fun doInBackground(vararg params: String?): Sign? {
            mSignId = params[0]!!

            val id = try {
                Integer.parseInt(mSignId)
            } catch (e: NumberFormatException) {
                return null
            }

            val database = SignDatabase(this@SignActivity)
            val sign = database.getSign(id) ?: return null

            mSign = sign
            val synonyms = database.getSynonyms(sign.id)
            val homonoyms = database.getHomonyms(sign.id)

            val combined = ArrayList<Item>()

            if (synonyms.isNotEmpty()) {
                combined.add(Header(getString(R.string.other_signs)))
                combined.addAll(synonyms)
            }

            if (homonoyms.isNotEmpty()) {
                combined.add(Header(getString(R.string.other_meanings)))
                combined.addAll(homonoyms)
            }

            mSynonyms = combined

            return sign
        }

        override fun onPostExecute(result: Sign?) {
            super.onPostExecute(result)

            if (result == null) {
                failLoad(mSignId)
                return
            }

            supportActionBar?.title = mSign?.word

            val adapter = TabPagerAdapter(supportFragmentManager)
            mViewPager?.adapter = adapter
            if (adapter.count == 1) {
                mViewPager?.currentItem = 0
                mTabLayout?.visibility = GONE
            } else {
                mTabLayout?.setupWithViewPager(mViewPager)
            }

            HistorySaveTask().execute()
            StarUpdateTask().execute()
        }
    }

    private inner class HistorySaveTask : AsyncTask<Void, Void, Void>() {
        override fun doInBackground(vararg params: Void): Void? {
            val db = SignDatabase(this@SignActivity).getDatabase() ?: return null
            val values = ContentValues()

            values.put(HistoryEntry.COLUMN_NAME_ID, mSign!!.id)
            values.put(HistoryEntry.COLUMN_NAME_DATE, Date().time)

            db.insert(HistoryEntry.TABLE_NAME, "null", values)

            db.delete(HistoryEntry.TABLE_NAME,
                    String.format("%s IN (SELECT %s FROM %s ORDER BY %s DESC LIMIT -1 OFFSET 50)",
                            HistoryEntry.COLUMN_NAME_ID,
                            HistoryEntry.COLUMN_NAME_ID,
                            HistoryEntry.TABLE_NAME,
                            HistoryEntry.COLUMN_NAME_DATE), null)

            return null
        }
    }

    private abstract inner class StarTask : AsyncTask<Void, Void, Boolean>() {
        protected val db: SQLiteDatabase?
            get() {
                return SignDatabase(this@SignActivity).getDatabase()
            }

        protected fun isStarred(db: SQLiteDatabase?): Boolean {
            val cursor = db!!.query(FavoritesEntry.TABLE_NAME, null,
                    FavoritesEntry.COLUMN_NAME_ID + "=?",
                    arrayOf(mSign!!.id.toString()), null, null, null, "1")
            val count = cursor.count

            cursor.close()
            return count > 0
        }

        override fun onPostExecute(starred: Boolean?) {
            mStarred = starred!!
            mGotStarred = true
            // updateStar();
            invalidateOptionsMenu()
        }
    }

    private inner class StarUpdateTask : StarTask() {
        override fun doInBackground(vararg params: Void): Boolean? {
            val db = db

            return isStarred(db)
        }
    }

    private inner class StarToggleTask : StarTask() {
        override fun doInBackground(vararg params: Void): Boolean? {
            val db = db
            val starred = isStarred(db)

            if (starred) {
                db!!.delete(FavoritesEntry.TABLE_NAME,
                        FavoritesEntry.COLUMN_NAME_ID + "=?",
                        arrayOf(mSign!!.id.toString()))
            } else {
                val values = ContentValues()

                values.put(FavoritesEntry.COLUMN_NAME_ID, mSign!!.id)
                values.put(FavoritesEntry.COLUMN_NAME_DATE, Date().time)

                db!!.insert(FavoritesEntry.TABLE_NAME, "null", values)
            }

            return !starred
        }
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        val star = menu.findItem(R.id.menu_star)

        if (mStarred) {
            star.setIcon(R.drawable.ic_bookmark_white_24dp)
            star.isChecked = true
            star.title = getString(R.string.remove_bookmark)
        } else {
            star.setIcon(R.drawable.ic_bookmark_border_white_24dp)
            star.isChecked = false
            star.title = getString(R.string.add_bookmark)
        }

        return super.onPrepareOptionsMenu(menu)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOrdboken!!.initSearchView(this, menu, null, false)
        menuInflater.inflate(R.menu.word, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        if (item.itemId == R.id.openInBrowser) {
            mSign?.let {
                val url = "https://teckensprakslexikon.su.se/ord/%05d/".format(it.id)
                startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
            }
            return true
        }

        if (item.itemId == R.id.menu_star) {
            StarToggleTask().execute()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
