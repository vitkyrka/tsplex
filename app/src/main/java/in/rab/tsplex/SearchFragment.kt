package `in`.rab.tsplex

import android.os.Bundle

class SearchFragment : SignListFragment() {
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            query = args.getString(ARG_QUERY)
        }
    }

    override fun getSigns(): ArrayList<Sign> {
        val act = activity ?: return ArrayList<Sign>()
        val db = SignDatabase(act)

        if (query == null)
            return ArrayList<Sign>()

        if (query!!.startsWith("topic:")) {
            val topic = query!!.substring(6)
            val topicid = topic.toIntOrNull() ?: return arrayListOf<Sign>()

            return db.getTopicSigns(topicid)
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