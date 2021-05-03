package `in`.rab.tsplex

import android.os.Bundle
import android.view.View
import kotlinx.android.synthetic.main.exoplayer_preview_control.*

class SignFragment : ItemListFragment() {
    private lateinit var mSign: Sign
    private var mSelectedExample = -1
    private var mPosition = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val args = arguments
        if (args != null) {
            val sign: Sign = args.getSerializable(SignFragment.ARG_SIGN)!! as Sign
            mSign = sign
            mSelectedExample = args.getInt(SignFragment.ARG_SELECTED_EXAMPLE)
            mPosition = mSelectedExample + 1

            val items: ArrayList<Item> = arrayListOf()
            items.add(Description(sign))
            items.addAll(sign.explanations)
            items.addAll(sign.examples)

            if (sign.topic1 != 0L && !Topics.names[sign.topic1]!!.startsWith("Okä")) {
                items.add(Topic(sign.topic1, Topics.names[sign.topic1]!!, sign.topic1Extra))

                if (sign.topic2 != 0L) {
                    items.add(Topic(sign.topic2, Topics.names[sign.topic2]!!, sign.topic2Extra))
                }
            }

            mItems = items
            mPreviewPosition = mSelectedExample + 1
        }
    }

    override fun onExampleClick(item: Example, position: Int) {
        onItemPlay(item, position)
    }

    override fun getSigns(): List<Item> {
        return mItems
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        mCloseVisible = View.GONE
        mOpenNewVisible = View.GONE

        exoPlayerClose.visibility = View.GONE
        exoPlayerOpenNew.visibility = View.GONE
    }

    companion object {
        private const val ARG_SIGN = "sign"
        private const val ARG_SELECTED_EXAMPLE = "selectedExample"

        fun newInstance(sign: Sign, exampleUrl: String?): SignFragment {
            val fragment = SignFragment()
            val args = Bundle()

            args.putSerializable(ARG_SIGN, sign)

            if (exampleUrl == null) {
                args.putInt(ARG_SELECTED_EXAMPLE, -1)
            } else {
                for ((i, example) in sign.examples.withIndex()) {
                    if (exampleUrl.endsWith(example.video)) {
                        args.putInt(ARG_SELECTED_EXAMPLE, i)
                    }
                }
            }

            fragment.arguments = args
            return fragment
        }
    }
}