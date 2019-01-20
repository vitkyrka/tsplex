package `in`.rab.tsplex

import android.content.Context
import android.os.Bundle
import android.support.v4.app.ListFragment
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ListView
import kotlinx.android.synthetic.main.fragment_signexample.*

class TopicListFragment : FragmentVisibilityNotifier, ListFragment() {
    private var listener: OnTopicClickListener? = null
    private val topics = Topics.topics

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_topic_list, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val adapter = ArrayAdapter(activity!!,
                android.R.layout.simple_list_item_1, topics)
        listView.adapter = adapter
    }

    override fun onListItemClick(l: ListView?, v: View?, position: Int, id: Long) {
        val items = Topics.names.filter { it.value == listView.adapter.getItem(position) }

        if (items.isNotEmpty()) {
            listener?.onTopicClick(items.keys.toIntArray()[0])
        }
    }

    interface OnTopicClickListener {
        fun onTopicClick(topic: Int)
    }

    override fun onAttach(context: Context?) {
        super.onAttach(context)

        if (context is OnTopicClickListener) {
            listener = context
        }
    }

    override fun onDetach() {
        super.onDetach()
        listener = null
    }

    companion object {
        fun newInstance() = TopicListFragment()
    }
}
