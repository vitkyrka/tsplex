package `in`.rab.tsplex

import android.app.Activity
import android.app.SearchManager
import android.app.TaskStackBuilder
import android.content.ContentResolver
import android.content.Intent
import android.net.Uri
import android.os.*
import android.provider.SearchRecentSuggestions
import androidx.appcompat.widget.Toolbar
import android.text.Editable
import android.text.Html
import android.text.Spanned
import android.text.TextWatcher
import android.view.Menu
import android.view.MenuItem
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.inputmethod.EditorInfo
import android.widget.ArrayAdapter
import android.widget.ListView
import androidx.core.app.NavUtils
import kotlinx.android.synthetic.main.activity_search.*
import java.util.*
import android.text.method.LinkMovementMethod
import android.text.style.URLSpan
import android.text.SpannableStringBuilder
import android.widget.TextView
import android.text.style.ClickableSpan
import android.util.Log
import android.view.View
import androidx.test.espresso.IdlingResource
import androidx.test.espresso.idling.CountingIdlingResource


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

        initHelp(searchHelp, getString(R.string.search_help))

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
                    setSearch(url)
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
                supportFragmentManager.beginTransaction().remove(it).commit()
            }
        } else {
            emptyQueryInfo?.visibility = GONE

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

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.clearSearchBox) {
            searchView.text.clear()
            RecentTask().execute()
            return true
        } else if (item?.itemId == R.id.clearHistory) {
            SearchRecentSuggestions(this@SearchActivity,
                    SignRecentSuggestionsProvider.AUTHORITY, SignRecentSuggestionsProvider.MODE)
                    .clearHistory()
            RecentTask().execute()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
