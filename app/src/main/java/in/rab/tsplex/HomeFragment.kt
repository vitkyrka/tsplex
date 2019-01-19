package `in`.rab.tsplex

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import kotlin.math.min

class HomeFragment : ItemListFragment() {

    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()

        val db = SignDatabase(act)
        val history = db.getSignsByIds("history",
                "history.date DESC")
        val favorites = db.getSignsByIds("favorites",
                "RANDOM() LIMIT 2")

        val signs = ArrayList<Item>()

        signs.add(Search())

        signs.add(Header(getString(R.string.random_examples)))
        signs.addAll(db.getRandomExamples())

        signs.add(Header(getString(R.string.recently_seen)))
        signs.addAll(history.subList(0, min(2, history.size)))

        signs.add(Header(getString(R.string.random_favorites)))
        signs.addAll(favorites.subList(0, min(2, favorites.size)))


        return signs
    }

    companion object {
        fun newInstance() = HomeFragment()
    }
}