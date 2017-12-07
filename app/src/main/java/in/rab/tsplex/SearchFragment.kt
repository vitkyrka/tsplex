package `in`.rab.tsplex

import android.os.Bundle

class SearchFragment : SignListFragment() {
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (arguments != null) {
            query = arguments.getString(ARG_QUERY)
        }
    }

    override fun getSigns(): ArrayList<Sign> {
        val db = SignDatabase(activity)

        if (query!!.startsWith("topic:")) {
            val topic = query!!.substring(6)
            return db.getTopicSigns(topic.toInt())
        } else {
            return db.getSigns(query!!)
        }
    }

    companion object {
        private val ARG_QUERY = "query"

        fun newInstance(query: String): SearchFragment {
            val fragment = SearchFragment()
            val args = Bundle()
            args.putString(ARG_QUERY, query)
            fragment.arguments = args
            return fragment
        }
    }
}