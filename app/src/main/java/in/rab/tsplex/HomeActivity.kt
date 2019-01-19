package `in`.rab.tsplex

import android.content.res.Configuration
import android.os.Bundle
import android.support.design.widget.NavigationView
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : RoutingAppCompactActivity(), NavigationView.OnNavigationItemSelectedListener {
    private var mOrdboken: Ordboken? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null
    private var mActionBar: android.support.v7.app.ActionBar? = null
    private var mTitle: String? = null

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.putString("title", mTitle)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        setTheme(R.style.AppTheme)
        super.onCreate(savedInstanceState)

        mOrdboken = Ordboken.getInstance(this)

        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        mDrawerToggle = ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open,
                R.string.drawer_close)
        drawer_layout.addDrawerListener(mDrawerToggle!!)

        mActionBar = supportActionBar ?: return
        mActionBar!!.setDisplayHomeAsUpEnabled(true)
        mActionBar!!.setHomeButtonEnabled(true)

        navigation.setNavigationItemSelectedListener(this)

        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString("title")
        }

        if (mTitle == null) {
            onNavigationItemSelected(navigation.menu.findItem(R.id.navigation_home))
        } else {
            mActionBar!!.setTitle(mTitle)
        }
    }

    private fun setAndSaveTitle(res: Int) {
        mTitle = getString(res)
        mActionBar!!.setTitle(res)
    }

    override fun onNavigationItemSelected(it: MenuItem): Boolean {
        it.isChecked = true
        drawer_layout.closeDrawers()

        when (it.itemId) {
            R.id.navigation_home -> {
                val fragment: Fragment? = HomeFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.app_name)
                return true
            }
            R.id.navigation_history -> {
                val fragment: Fragment? = HistoryFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_history)
                return true
            }
            R.id.navigation_favorites -> {
                val fragment: Fragment? = FavoritesFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_favorites)
                return true
            }
            R.id.navigation_topics -> {
                val fragment: Fragment? = TopicListFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_topics)
                return true
            }
            else -> {
                return false
            }
        }
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)

        mDrawerToggle?.syncState()
    }

    override fun onConfigurationChanged(newConfig: Configuration?) {
        super.onConfigurationChanged(newConfig)

        mDrawerToggle?.syncState()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOrdboken!!.initSearchView(this, menu, null, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mDrawerToggle?.onOptionsItemSelected(item)!!) {
            return true
        }

        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }


        return super.onOptionsItemSelected(item)
    }
}
