package `in`.rab.tsplex

import android.os.Bundle
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.lang.Exception
import java.util.*
import kotlin.math.min

class HomeFragment : ItemListFragment() {
    private var mRandomExamples: List<Example> = ArrayList()
    private var mRandomFavorites: List<Sign> = ArrayList()
    private var mRandomTime: Date = Date(0)

    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()

        val db = SignDatabase(act)
        val history = db.getSignsByIds("history",
                "history.date DESC")

        val signs = ArrayList<Item>()

        signs.add(Search())

        val now = Date()
        val diff = (now.time - mRandomTime.time) / 1000
        val old = diff < 0 || diff > (60 * 15)

        if (mRandomExamples.size < 2 || old) {
            mRandomExamples = db.getRandomExamples()
            mRandomTime = now
        }

        signs.add(Header(getString(R.string.random_examples)))
        signs.addAll(mRandomExamples)

        signs.add(Header(getString(R.string.recently_seen)))
        signs.addAll(history.subList(0, min(2, history.size)))

        if (mRandomFavorites.size < 2 || old) {
            val favorites = db.getSignsByIds("favorites",
                    "RANDOM() LIMIT 2")
            mRandomFavorites = favorites.subList(0, min(2, favorites.size))
            mRandomTime = now
        }

        signs.add(Header(getString(R.string.random_favorites)))
        signs.addAll(mRandomFavorites)


        return signs
    }

    override fun onRefresh() {
        mRandomTime = Date(0)
        super.onRefresh()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = super.onCreateView(inflater, container, savedInstanceState)

        view?.findViewById<androidx.swiperefreshlayout.widget.SwipeRefreshLayout>(R.id.swipeLayout)?.apply {
            isEnabled = true
        }

        return view
    }

    override fun onResume() {
        activity?.getSharedPreferences(PREFS_NAME, 0)?.apply {
            var randomExamples: List<Example>? = null
            var randomFavorites: List<Sign>? = null
            val gson = Gson()

            try {
                val exampleListType = object : TypeToken<List<Example>>() {}.type
                randomExamples = gson.fromJson<List<Example>>(getString("randomExamples", null),
                        exampleListType)
            } catch (e: Exception) {
                throw(e)
            }

            try {
                val signListType = object : TypeToken<List<Sign>>() {}.type
                randomFavorites = gson.fromJson<List<Sign>>(getString("randomFavorites", null),
                        signListType)
            } catch (e: Exception) {
            }

            mRandomTime = Date(getLong("randomTime", 0))

            randomExamples?.let { mRandomExamples = it }
            randomFavorites?.let { mRandomFavorites = it }
        }

        super.onResume()
    }

    override fun onPause() {
        super.onPause()

        activity?.getSharedPreferences(PREFS_NAME, 0)?.edit()?.apply {
            val gson = Gson()
            putString("randomExamples", gson.toJson(mRandomExamples))
            putString("randomFavorites", gson.toJson(mRandomFavorites))
            putLong("randomTime", mRandomTime.time)
            apply()
        }
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}