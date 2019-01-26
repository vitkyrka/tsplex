package `in`.rab.tsplex

import android.app.SearchManager
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.SearchRecentSuggestions
import android.support.v7.widget.Toolbar
import android.text.Editable
import android.text.TextWatcher
import android.view.Menu
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*


class SearchActivity : RoutingAppCompactActivity(), TextWatcher {
    private var mHandler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null
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

        searchView.addTextChangedListener(this)

        searchView.setOnEditorActionListener { v, actionId, event ->
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                mQuery?.let { saveRecent(it) }
            }

            false
        }

        recentList.setOnItemClickListener { parent, view, position, id ->
            (parent as ListView).adapter?.apply {
                val item = this.getItem(position) as String

                searchView?.append(item)
            }
        }

        val intent = intent
        if (Intent.ACTION_SEARCH == intent.action || Intent.ACTION_VIEW == intent.action) {
            onNewIntent(intent)
        } else {
            searchView.requestFocus()
            RecentTask().execute()
        }
    }


    override fun afterTextChanged(s: Editable?) = Unit
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val query = s.toString()

        if (query == mQuery) {
            return
        }

        mRunnable?.let { mHandler.removeCallbacks(it) }

        mQuery = query

        mRunnable = Runnable {
            if (query.isEmpty()) {
                recentList?.visibility = VISIBLE

                supportFragmentManager.findFragmentByTag("foo")?.let {
                    supportFragmentManager.beginTransaction().remove(it).commit()
                }
            } else {
                recentList?.visibility = GONE

                val fragment = supportFragmentManager.findFragmentByTag("foo")
                if (fragment != null) {
                    (fragment as SearchFragment).setQuery(query)
                } else {
                    SearchFragment.newInstance(query).let {
                        supportFragmentManager.beginTransaction().replace(R.id.content, it, "foo").commit()
                    }
                }
            }
        }

        mHandler.postDelayed(mRunnable, 250)
    }


    private fun saveRecent(query: String) {
        val suggestions = object : SearchRecentSuggestions(this@SearchActivity,
                SignRecentSuggestionsProvider.AUTHORITY, SignRecentSuggestionsProvider.MODE) {
            override fun truncateHistory(cr: ContentResolver?, maxEntries: Int) {
                super.truncateHistory(cr, if (maxEntries > 0) {
                    10
                } else {
                    maxEntries
                })
            }
        }
        suggestions.saveRecentQuery(query, null)
    }

    override fun onListFragmentInteraction(item: Sign) {
        mQuery?.let { saveRecent(it) }

        super.onListFragmentInteraction(item)
    }

    override fun onListFragmentInteraction(item: Example) {
        mQuery?.let { saveRecent(it) }

        super.onListFragmentInteraction(item)
    }

    override fun onListFragmentInteraction(item: Topic) {
        mQuery?.let { saveRecent(it) }

        super.onListFragmentInteraction(item)
    }

    private inner class RecentTask : AsyncTask<Void, Void, List<String>>() {
        override fun doInBackground(vararg params: Void): List<String> {
            var cur = contentResolver.query(Uri.parse("content://in.rab.tsplex.SignRecentSuggestionsProvider/search_suggest_query"),
                    arrayOf(SearchManager.SUGGEST_COLUMN_TEXT_1),
                    null, arrayOf(""), null)
            val queries = ArrayList<String>()

            cur?.apply {
                val col = getColumnIndex(SearchManager.SUGGEST_COLUMN_TEXT_1)

                while (moveToNext()) {
                    queries.add(cur.getString(col))
                }
            }

            return queries
        }

        override fun onPostExecute(queries: List<String>) {
            recentList?.adapter = ArrayAdapter<String>(this@SearchActivity,
                    R.layout.item_recent,
                    queries)
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        if (Intent.ACTION_SEARCH == intent.action) {
            val query = intent.getStringExtra(SearchManager.QUERY)

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

        menuInflater.inflate(R.menu.main_search, menu)
        return true
    }
}
