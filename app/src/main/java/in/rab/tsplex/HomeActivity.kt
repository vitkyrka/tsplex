package `in`.rab.tsplex

import Topics
import android.app.SearchManager
import android.content.Intent
import android.content.res.Configuration
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.ActionBarDrawerToggle
import android.support.v7.app.AppCompatActivity
import android.view.Menu
import android.view.MenuItem
import kotlinx.android.synthetic.main.activity_home.*

class HomeActivity : AppCompatActivity(), SignListFragment.OnListFragmentInteractionListener, TopicListFragment.OnTopicClickListener {
    private var mOrdboken: Ordboken? = null
    private var mDrawerToggle: ActionBarDrawerToggle? = null

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    override fun onTopicClick(topic: Int) {
        val intent = Intent(this, SearchActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, Topics.names[topic])
        intent.putExtra(SearchManager.QUERY, "topic:" + topic.toString())
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        mOrdboken = Ordboken.getInstance(this)

        setContentView(R.layout.activity_home)
        setSupportActionBar(toolbar)

        mDrawerToggle = ActionBarDrawerToggle(this, drawer_layout, R.string.drawer_open,
                R.string.drawer_close)
        drawer_layout.addDrawerListener(mDrawerToggle!!)

        val actionBar = supportActionBar ?: return
        actionBar.setDisplayHomeAsUpEnabled(true)
        actionBar.setHomeButtonEnabled(true)

        navigation.setNavigationItemSelectedListener {
            it.isChecked = true
            drawer_layout.closeDrawers()

            when (it.itemId) {
                R.id.navigation_history -> {
                    val fragment: Fragment? = HistoryFragment.newInstance()
                    supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                    actionBar.setTitle(R.string.title_history)
                    return@setNavigationItemSelectedListener true
                }
                R.id.navigation_favorites -> {
                    val fragment: Fragment? = FavoritesFragment.newInstance()
                    supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                    actionBar.setTitle(R.string.title_favorites)
                    return@setNavigationItemSelectedListener true
                }
                R.id.navigation_topics -> {
                    val fragment: Fragment? = TopicListFragment.newInstance()
                    supportFragmentManager.beginTransaction().replace(R.id.content, fragment!!).commit()
                    actionBar.setTitle(R.string.title_topics)
                    return@setNavigationItemSelectedListener true
                }
                else -> {
                    return@setNavigationItemSelectedListener false
                }
            }
        }

        val fragment: Fragment? = HistoryFragment.newInstance()
        if (fragment != null) {
            supportFragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
        }
        actionBar.setTitle(R.string.title_history)
        navigation.menu.findItem(R.id.navigation_history).isChecked = true
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

        return super.onOptionsItemSelected(item)
    }
}
