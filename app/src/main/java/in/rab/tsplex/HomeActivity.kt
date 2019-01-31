package `in`.rab.tsplex

import android.content.res.Configuration
import android.os.Bundle
import com.google.android.material.navigation.NavigationView
import androidx.fragment.app.Fragment
import androidx.appcompat.app.ActionBarDrawerToggle
import android.view.Menu
import android.view.MenuItem
import com.google.android.material.bottomnavigation.BottomNavigationView
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : RoutingAppCompactActivity(), BottomNavigationView.OnNavigationItemSelectedListener {
    private var mOrdboken: Ordboken? = null
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

        navigation.setOnNavigationItemSelectedListener(this)


        if (savedInstanceState != null) {
            mTitle = savedInstanceState.getString("title")
        }

        if (mTitle == null) {
            onNavigationItemSelected(navigation.menu.findItem(R.id.navigation_home))
        }
    }

    private fun setAndSaveTitle(res: Int) {
        mTitle = getString(res)
    }

    override fun onNavigationItemSelected(it: MenuItem): Boolean {
        it.isChecked = true

        when (it.itemId) {
            R.id.navigation_home -> {
                val fragment: androidx.fragment.app.Fragment? = HomeFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.app_name)
                return true
            }
            R.id.navigation_history -> {
                val fragment: androidx.fragment.app.Fragment? = HistoryFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_history)
                return true
            }
            R.id.navigation_favorites -> {
                val fragment: androidx.fragment.app.Fragment? = FavoritesFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_favorites)
                return true
            }
            R.id.navigation_topics -> {
                val fragment: androidx.fragment.app.Fragment? = TopicListFragment.newInstance()
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                setAndSaveTitle(R.string.title_topics)
                return true
            }
            else -> {
                return false
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        mOrdboken!!.initSearchView(this, menu, null, true)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }


        return super.onOptionsItemSelected(item)
    }
}
