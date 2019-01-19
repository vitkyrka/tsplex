package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.app.AppCompatActivity
import android.support.v7.widget.Toolbar
import android.view.Menu

class SearchActivity : AppCompatActivity(), ItemListFragment.OnListFragmentInteractionListener {
    private var mOrdboken: Ordboken? = null
    private var mQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_search)

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
            val query = intent.getStringExtra(SearchManager.QUERY)
            val fragment: Fragment? = SearchFragment.newInstance(query)

            val title = intent.getStringExtra(Intent.EXTRA_TITLE) ?: query
            supportActionBar?.title = title

            mQuery = query
            if (fragment != null) {
                supportFragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
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

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Example) {
    }
}
