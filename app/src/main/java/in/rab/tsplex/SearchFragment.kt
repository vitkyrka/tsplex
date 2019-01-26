package `in`.rab.tsplex

import android.os.Bundle

class SearchFragment : ItemListFragment() {
    private var mQuery: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            mQuery = args.getString(ARG_QUERY)
        }
    }

    internal fun setQuery(query: String) {
        if (query == mQuery) {
            return
        }

        mQuery = query
        update()
    }

    override fun getSigns(): List<Item> {
        val act = activity ?: return ArrayList()
        val query = mQuery ?: return ArrayList()
        val db = SignDatabase(act)

        if (query.startsWith("topic:")) {
            val topic = query.substring(6)
            val topicid = topic.toIntOrNull() ?: return arrayListOf()

            return ArrayList(db.getTopicSigns(topicid))
        } else if (query.startsWith("ex:")) {
            val ex = query.substring(3)

            return ArrayList(db.getExampleSigns(ex))
        } else {
            val signs = db.getSigns(query)
            val examples = db.getExamples(query)
            val topics = ArrayList<Item>(Topics.topics.filter {
                it.name.contains(query, ignoreCase = true)
            })

            val combined = ArrayList<Item>(signs)

            if (topics.size > 0) {
                combined.add(Header(getString(R.string.topics)))
                combined.addAll(topics)
            }

            if (examples.size > 0) {
                combined.add(Header(getString(R.string.examples)))
                combined.addAll(examples)
            }

            return combined
        }
    }

    companion object {
        private const val ARG_QUERY = "query"

        fun newInstance(query: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            fragment.arguments = args
            return fragment
        }
    }
}