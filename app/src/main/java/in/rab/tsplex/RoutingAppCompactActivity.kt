package `in`.rab.tsplex

import Topics
import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import android.support.v7.app.AppCompatActivity

abstract class RoutingAppCompactActivity() : AppCompatActivity(),
        SignDescriptionFragment.OnTopicClickListener,
        TopicListFragment.OnTopicClickListener,
        ItemListFragment.OnListFragmentInteractionListener {

    override fun onTopicClick(topic: Int) {
        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, Topics.names[topic])
        intent.putExtra(SearchManager.QUERY, "topic:" + topic.toString())
        startActivity(intent)
    }

    override fun onExampleLongClick(example: Example) {
        val q = Uri.parse(example.video).lastPathSegment
        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, example.toString())
        intent.putExtra(SearchManager.QUERY, "ex:" + q)

        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Example) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.signId.toString())
        intent.putExtra("exampleUrl", item.video)
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Search) {
        val intent = Intent(this, SearchActivity::class.java)
        startActivity(intent)
        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
    }
}