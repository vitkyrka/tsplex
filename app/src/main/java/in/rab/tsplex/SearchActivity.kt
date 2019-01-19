package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.os.Bundle
import android.support.v4.app.Fragment
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import kotlinx.android.synthetic.main.activity_search.*

class SearchActivity : RoutingAppCompactActivity() {
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

        searchView.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                val query = s.toString()

                val fragment: Fragment? = SearchFragment.newInstance(query)
                mQuery = query
                if (fragment != null) {
                    supportFragmentManager.beginTransaction().replace(R.id.content, fragment).commit()
                }
            }

        })

        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action || Intent.ACTION_VIEW == intent.action) {
            onNewIntent(intent)
        } else {
            searchView.requestFocus()
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

        menuInflater.inflate(R.menu.main_search, menu)
        return true
    }
}
