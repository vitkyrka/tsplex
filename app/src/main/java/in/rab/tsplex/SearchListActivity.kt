package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.widget.Toolbar

class SearchListActivity : RoutingAppCompactActivity() {
    private var mOrdboken: Ordboken? = null
    private var mQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search_list)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mOrdboken = Ordboken.getInstance(this)

        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action || Intent.ACTION_VIEW == intent.action) {
            onNewIntent(intent)
        } else {
            finish()
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY) ?: ""

            val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: query
            supportActionBar?.title = title

            mQuery = query

            val fragment = supportFragmentManager.findFragmentByTag("foo")
            if (fragment != null) {
                (fragment as SearchFragment).setQuery(query)
            } else {
                SearchFragment.newInstance(query).let {
                    supportFragmentManager.beginTransaction().replace(R.id.content, it, "foo").commit()
                }
            }
        } else if (Intent.ACTION_VIEW == intent.action) {
            val url = intent.dataString
            val word = intent.getStringExtra(SearchManager.EXTRA_DATA_KEY)
            Ordboken.startWordActivity(this, word, url)
            finish()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        val query = if (mQuery != null && mQuery!!.startsWith("topic:")) {
            null
        } else {
            mQuery
        }

        mOrdboken!!.initSearchView(this, menu, query, false)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
