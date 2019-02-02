package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.idling.CountingIdlingResource

abstract class RoutingAppCompactActivity : AppCompatActivity(),
        SignDescriptionFragment.OnTopicClickListener,
        TopicListFragment.OnTopicClickListener,
        ItemListFragment.OnListFragmentInteractionListener {
    internal val mVideoFetchResource: CountingIdlingResource = CountingIdlingResource("fetch")
    internal lateinit var mCurrentVideo: String

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

    override fun onExampleSearchClick(example: Example) {
        onExampleLongClick(example)
    }

    override fun onListFragmentInteraction(item: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.id.toString())
        startActivity(intent)
    }

    override fun onVideoFetchStart(video: String) {
        mCurrentVideo = video
        mVideoFetchResource.increment()
    }

    override fun onVideoFetchEnd() {
        if (!mVideoFetchResource.isIdleNow) {
            mVideoFetchResource.decrement()
        }
    }

    override fun onListFragmentInteraction(item: Example) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", item.signId.toString())
        intent.putExtra("exampleUrl", item.video)
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Topic) {
        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, item.toString())
        intent.putExtra(SearchManager.QUERY, "topic:" + item.id.toString())
        startActivity(intent)
    }

    override fun onLoadList(items: List<Item>) {
    }
}