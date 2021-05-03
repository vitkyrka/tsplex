package `in`.rab.tsplex

import android.app.SearchManager
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.SearchRecentSuggestions
import android.text.*
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.test.espresso.idling.CountingIdlingResource
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*


class SearchActivity : RoutingAppCompactActivity(), TextWatcher {
    private var mHandler = Handler(Looper.getMainLooper())
    private var mRunnable: Runnable? = null
    private var mOrdboken: Ordboken? = null
    private var mQuery: String? = null
    internal var mAutoSearch = true
    internal val mIdleResource: CountingIdlingResource = CountingIdlingResource("search")

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
                mQuery?.let {
                    saveRecent(it)
                    cancelDelayedSearch()
                    performSearch(it)
                }
            }

            false
        }

        layoutInflater.inflate(R.layout.item_searchtips, null).let {
            val searchHelp = it.findViewById<TextView>(R.id.searchHelp)
            initHelp(searchHelp, getString(R.string.search_help))
            recentList.addFooterView(it)
        }

        recentList.setOnItemClickListener { parent, view, position, id ->
            (parent as ListView).adapter?.apply {
                val item = this.getItem(position) as String

                setSearch(item)
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

    private fun setSearch(string: String) {
        searchView?.apply {
            text.clear()
            append(string)

            cancelDelayedSearch()
            performSearch(string)
        }

    }

    private fun fromHtml(html: String): Spanned {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            Html.fromHtml(html, Html.FROM_HTML_MODE_LEGACY)
        } else {
            Html.fromHtml(html)
        }
    }

    private fun initHelp(textView: TextView, str: String) {
        val html = fromHtml(str)
        val builder = SpannableStringBuilder(html)

        for (span in builder.getSpans(0, html.length, URLSpan::class.java)) {
            builder.setSpan(object : URLSpan(span.url.toString()) {
                override fun onClick(widget: View) {
                    setSearch(url.replace("_", " "))
                }

            }, builder.getSpanStart(span), builder.getSpanEnd(span), builder.getSpanFlags(span))
            builder.removeSpan(span)
        }

        textView.movementMethod = LinkMovementMethod.getInstance()
        textView.text = builder
    }

    override fun onLoadList(items: List<Item>) {
        if (items.isEmpty()) {
            emptyQueryInfo.visibility = VISIBLE
            noResults.text = fromHtml(getString(R.string.no_results, mQuery,
                    Uri.parse("https://teckensprakslexikon.su.se/sok")
                            .buildUpon()
                            .appendQueryParameter("q", mQuery)
                            .toString()))
            noResults.movementMethod = LinkMovementMethod.getInstance()
            noResults.visibility = VISIBLE
        } else {
            emptyQueryInfo.visibility = GONE
            noResults.visibility = GONE
        }
    }

    override fun afterTextChanged(s: Editable?) = Unit
    override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

    private fun performSearch(query: String) {
        if (query.isEmpty()) {
            emptyQueryInfo?.visibility = VISIBLE
            noResults.visibility = GONE

            supportFragmentManager.findFragmentByTag("foo")?.let {
                supportFragmentManager.beginTransaction().remove(it).commitAllowingStateLoss()
            }
        } else {
            val fragment = supportFragmentManager.findFragmentByTag("foo")
            if (fragment != null) {
                (fragment as SearchFragment).setQuery(query)
            } else {
                SearchFragment.newInstance(query).let {
                    supportFragmentManager.beginTransaction().replace(R.id.content, it, "foo").commitAllowingStateLoss()
                }
            }
        }
    }

    private fun cancelDelayedSearch() {
        mRunnable?.let {
            mHandler.removeCallbacks(it)

            if (!mIdleResource.isIdleNow) {
                mIdleResource.decrement()
            }
        }
    }

    override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
        val query = s.toString().trim()

        if (query == mQuery) {
            return
        }

        cancelDelayedSearch()

        mQuery = query

        if (!mAutoSearch) {
            return
        }

        mRunnable = Runnable {
            performSearch(query)
            mIdleResource.decrement()
        }

        mIdleResource.increment()
        mHandler.postDelayed(mRunnable, if (query.isEmpty() || query.length > 1) {
            300
        } else {
            1000
        })
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

    override fun onExampleSearchClick(example: Example) {
        mQuery?.let { saveRecent(it) }

        super.onExampleSearchClick(example)
    }

    override fun onItemPlay(item: Item) {
        mQuery?.let { saveRecent(it) }

        super.onItemPlay(item)
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
        menuInflater.inflate(R.menu.main_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        if (item?.itemId == R.id.resetSearch) {
            searchView.text.clear()
            RecentTask().execute()
            return true
        }

        return super.onOptionsItemSelected(item)
    }
}
