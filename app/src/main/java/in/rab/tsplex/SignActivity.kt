package `in`.rab.tsplex

import Topics
import `in`.rab.tsplex.OrdbokenContract.FavoritesEntry
import `in`.rab.tsplex.OrdbokenContract.HistoryEntry
import android.app.SearchManager
import android.content.ContentValues
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.os.AsyncTask
import android.os.Bundle
import android.support.design.widget.TabLayout
import android.support.v4.app.FragmentManager
import android.support.v4.app.FragmentPagerAdapter
import android.support.v4.view.ViewPager
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.widget.Toast
import java.util.*

class SignActivity : AppCompatActivity(), SignDescriptionFragment.OnTopicClickListener, SignListFragment.OnListFragmentInteractionListener {
    private var mOrdboken: Ordboken? = null
    private var mStarred: Boolean = false
    private var mGotStarred: Boolean = false
    private var mSign: Sign? = null
    private var mSynonyms: ArrayList<Sign>? = null
    private var mHomonyms: ArrayList<Sign>? = null
    private var mPosition = 0
    private var mViewPager: ViewPager? = null
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

        val intent = intent

        title = ""

        val url = intent.getStringExtra("url") ?: return

        val pager = findViewById<ViewPager>(R.id.viewPager)
        pager.addOnPageChangeListener(
                object : ViewPager.SimpleOnPageChangeListener() {
                    override fun onPageSelected(position: Int) {
                        var fragment = pager.adapter.instantiateItem(pager, mPosition) as FragmentVisibilityNotifier
                        fragment.onHide()

                        mPosition = position
                        fragment = pager.adapter.instantiateItem(pager, position) as FragmentVisibilityNotifier
                        fragment.onShow()
                    }
                }
        )

        mViewPager = pager
        mTabLayout = findViewById<TabLayout>(R.id.tabs)

        SignLoadTask().execute(Integer.parseInt(url))
    }

    override fun onTopicClick(topic: Int) {
        val intent = Intent(this, SearchActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, Topics.names[topic])
        intent.putExtra(SearchManager.QUERY, "topic:" + topic.toString())
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    private inner class TabPagerAdapter(fm: FragmentManager) : FragmentPagerAdapter(fm) {
        override fun getPageTitle(position: Int): CharSequence {
            var adjpos = position;

            if (adjpos > 0) {
                if (mSign!!.examples.size == 0) {
                    adjpos += 1
                }
            }

            if (adjpos > 1) {
                if (mSynonyms!!.size == 0) {
                    adjpos += 1
                }
            }

            return when (adjpos) {
                0 -> getText(R.string.sign)
                1 -> getText(R.string.examples)
                2 -> getText(R.string.other_signs)
                3 -> getText(R.string.other_meanings)
                else -> ""
            }
        }

        override fun getItem(pos: Int): android.support.v4.app.Fragment? {
            var adjpos = pos;

            if (adjpos > 0) {
                if (mSign!!.examples.size == 0) {
                    adjpos += 1
                }
            }

            if (adjpos > 1) {
                if (mSynonyms!!.size == 0) {
                    adjpos += 1
                }
            }

            return when (adjpos) {
                0 -> SignDescriptionFragment.newInstance(mSign!!)
                1 -> SignExampleFragment.newInstance(mSign!!)
                2 -> SignFragment.newInstance(mSynonyms!!)
                3 -> SignFragment.newInstance(mHomonyms!!)
                else -> null
            }
        }

        override fun getCount(): Int {
            var count = 1

            if (mSign!!.examples.size > 0) {
                count += 1
            }

            if (mSynonyms!!.size > 0) {
                count += 1
            }

            if (mHomonyms!!.size > 0) {
                count += 1
            }

            return count
        }
    }

    private inner class SignLoadTask : AsyncTask<Int, Void, Sign?>() {
        override fun doInBackground(vararg params: Int?): Sign? {
            val database = SignDatabase(this@SignActivity)
            val sign = database.getSign(params[0]!!) ?: return null

            mSign = sign
            mSynonyms = database.getSynonyms(sign.id)
            mHomonyms = database.getHomonyms(sign.id)

            return sign
        }

        override fun onPostExecute(result: Sign?) {
            super.onPostExecute(result)

            if (result == null) {
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
            val db = SignDatabase(this@SignActivity).getDatabase()
            val values = ContentValues()

            values.put(HistoryEntry.COLUMN_NAME_ID, mSign!!.id)
            values.put(HistoryEntry.COLUMN_NAME_DATE, Date().time)

            db!!.insert(HistoryEntry.TABLE_NAME, "null", values)

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
            val starred = isStarred(db)

            return starred
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
            star.setIcon(R.drawable.ic_favorite_white_24dp)
            star.isChecked = true
            star.title = getString(R.string.remove_bookmark)
        } else {
            star.setIcon(R.drawable.ic_favorite_border_white_24dp)
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

        if (item.itemId == R.id.menu_star) {
            StarToggleTask().execute()
        }

        return super.onOptionsItemSelected(item)
    }
}
