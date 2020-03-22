package `in`.rab.tsplex

import android.app.AlertDialog
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout


class SignActivity : RoutingAppCompactActivity(), ItemListFragment.OnListFragmentInteractionListener {
    private var mOrdboken: Ordboken? = null
    private var mStarred: Boolean = false
    private var mSign: Sign? = null
    private var mExampleUrl: String? = null
    private var mSynonyms: ArrayList<Item>? = null
    private var mPosition = 0
    private var mViewPager: androidx.viewpager.widget.ViewPager? = null
    private var mTabLayout: TabLayout? = null
    private var mFolders = ArrayList<Folder>()

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

    override fun onListFragmentInteraction(item: Folder) = Unit

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

        override fun getItem(pos: Int): Fragment {
            var adjpos = pos

            if (adjpos > 0) {
                if (mSynonyms!!.size == 0) {
                    adjpos += 1
                }
            }

            return when (adjpos) {
                0 -> SignFragment.newInstance(mSign!!, mExampleUrl)
                else -> ArrayListFragment.newInstance(mSynonyms!!)
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

    private inner class SignLoadTask : AsyncTask<String, Void, Triple<Sign?, Boolean, ArrayList<Folder>>>() {
        var mSignId: String = ""

        override fun doInBackground(vararg params: String?): Triple<Sign?, Boolean, ArrayList<Folder>> {
            var folders = ArrayList<Folder>()
            var starred = false

            mSignId = params[0]!!

            val id = try {
                Integer.parseInt(mSignId)
            } catch (e: NumberFormatException) {
                return Triple(null, starred, folders)
            }

            val database = SignDatabase.getInstance(this@SignActivity)
            val sign = database.getSign(id) ?: return Triple(null, starred, folders)

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

            database.addToHistory(sign.id)
            starred = database.isFavorite(sign.id)
            folders = database.getBookmarksFolders()

            return Triple(sign, starred, folders)
        }

        override fun onPostExecute(result: Triple<Sign?, Boolean, ArrayList<Folder>>) {
            super.onPostExecute(result)

            if (result.first == null) {
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

            mFolders = result.third
            mStarred = result.second
            invalidateOptionsMenu()
        }
    }

    private inner class StarToggleTask : AsyncTask<Int, Void, Boolean>() {
        override fun doInBackground(vararg params: Int?): Boolean? {
            val folderId = params[0]!!

            mSign?.let {
                val db = SignDatabase.getInstance(this@SignActivity)
                val starred = db.isFavorite(it.id)

                if (starred) {
                    db.removeFromFavorites(it.id)
                } else {
                    db.addToFavorites(it.id, folderId)
                }

                return !starred
            }

            return null
        }

        override fun onPostExecute(starred: Boolean?) {
            if (starred == null) {
                return
            }

            mStarred = starred
            invalidateOptionsMenu()
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
            if (!mStarred) {
                if (mFolders.isNotEmpty()) {
                    val builder = AlertDialog.Builder(this)
                    builder.setTitle(R.string.add_bookmark)
                            .setItems(mFolders.map { it.name }.toTypedArray()
                            ) { _, which ->
                                StarToggleTask().execute(mFolders[which].id)
                            }
                    builder.show()
                    return true
                }
            }

            StarToggleTask().execute(0)
            return true
        }

        if (item.itemId == R.id.settings) {
            startActivity(Intent(this, SettingsActivity::class.java))
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
