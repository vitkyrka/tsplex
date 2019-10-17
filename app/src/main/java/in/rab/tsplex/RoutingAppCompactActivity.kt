package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import androidx.test.espresso.idling.CountingIdlingResource

abstract class RoutingAppCompactActivity : AppCompatActivity(),
        ItemListFragment.OnListFragmentInteractionListener {
    internal val mVideoFetchResource: CountingIdlingResource = CountingIdlingResource("fetch")
    internal lateinit var mCurrentVideo: String

    override fun onExampleSearchClick(example: Example) {
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

    override fun onVideoFetchStart(video: String) {
        mCurrentVideo = video
        mVideoFetchResource.increment()
    }

    override fun onVideoFetchEnd() {
        if (!mVideoFetchResource.isIdleNow) {
            mVideoFetchResource.decrement()
        }
    }

    override fun onItemPlay(item: Item) {
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
        intent.putExtra(Intent.EXTRA_TITLE, item.name)
        intent.putExtra(SearchManager.QUERY, "topic:" + item.id.toString())
        startActivity(intent)
    }

    override fun onListFragmentInteraction(item: Folder) {
        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, item.name)
        intent.putExtra(SearchManager.QUERY, "folder:" + item.id.toString())
        startActivity(intent)
    }

    override fun onItemLongClick(item: Folder): Boolean = false

    override fun onLoadList(items: List<Item>) {
    }
}