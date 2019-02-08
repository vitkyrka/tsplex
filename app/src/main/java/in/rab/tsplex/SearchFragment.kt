package `in`.rab.tsplex

import android.os.Bundle
import java.util.regex.Pattern

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

            val signs = db.getTopicSigns(topicid)
            val topics = db.getSubTopics(topicid)
            val parent = db.getParentTopic(topicid)
            val combined = ArrayList<Item>()

            if (parent.isNotEmpty()) {
                combined.add(Header(getString(R.string.parent_topic)))
                combined.addAll(parent)
            }

            if (topics.isNotEmpty()) {
                combined.add(Header(getString(R.string.subtopics) + " (${topics.size})"))
                combined.addAll(topics)
            }

            if (signs.isNotEmpty()) {
                combined.add(Header(getString(R.string.signs) + " (${signs.size})"))
                combined.addAll(signs)
            }

            return combined
        } else if (query.startsWith("ex:")) {
            val ex = query.substring(3)

            return ArrayList(db.getExampleSigns(ex))
        } else {
            val signs = db.getSigns(query)
            val examples = db.getExamples(query)
            val topics = db.getTopics(query)

            val combined = ArrayList<Item>()

            if (topics.isNotEmpty()) {
                combined.add(Header(getString(R.string.topics) + " (${topics.size})"))
                combined.addAll(topics)
            }

            if (signs.isNotEmpty()) {
                combined.add(Header(getString(R.string.signs) + " (${signs.size})"))
                combined.addAll(signs)
            }

            if (examples.isNotEmpty()) {
                combined.add(Header(getString(R.string.examples) + " (${examples.size})"))
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