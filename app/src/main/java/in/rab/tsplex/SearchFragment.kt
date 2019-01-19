package `in`.rab.tsplex

import android.os.Bundle

class SearchFragment : ItemListFragment() {
    private var query: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            query = args.getString(ARG_QUERY)
        }
    }

    override fun getSigns(): List<Item> {
        val act = activity ?: return ArrayList()
        val db = SignDatabase(act)

        if (query == null)
            return ArrayList()

        if (query!!.startsWith("topic:")) {
            val topic = query!!.substring(6)
            val topicid = topic.toIntOrNull() ?: return arrayListOf()

            return ArrayList(db.getTopicSigns(topicid))
        } else if (query!!.startsWith("ex:")) {
            val ex = query!!.substring(3)

            return ArrayList(db.getExampleSigns(ex))
        } else {
            val signs = db.getSigns(query!!)
            val examples = db.getExamples(query!!)

            if (examples.size == 0) {
                return ArrayList<Item>(signs)
            }

            return ArrayList<Item>(signs) + arrayListOf(Header(getString(R.string.examples))) + ArrayList<Item>(examples)
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