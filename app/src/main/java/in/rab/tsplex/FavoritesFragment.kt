package `in`.rab.tsplex

import android.os.Bundle
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.widget.EditText
import android.widget.LinearLayout
import androidx.appcompat.app.AlertDialog


class FavoritesFragment : ItemListFragment(mCache = false, mEmptyText = R.string.no_bookmarks) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setHasOptionsMenu(true)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.menu_add_folder) {

            val builder = AlertDialog.Builder(context!!)

            val input = EditText(context).apply {
                hint = getString(R.string.name)
            }
            val layout = LinearLayout(context)
            val params = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT)
            params.setMargins(30, 0, 30, 0)
            layout.addView(input, params)

            builder.setTitle(R.string.add_folder)
                    .setView(layout)
                    .setPositiveButton(R.string.ok) { _, _ ->
                        SignDatabase.getInstance(context!!).addBookmarksFolder(input.text.toString())
                        update(false)

                    }
                    .setNegativeButton(R.string.cancel) { dialog, _ ->
                        dialog.cancel()
                    }
                    .show()

            return true
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        super.onCreateOptionsMenu(menu, inflater)
        inflater.inflate(R.menu.bookmarks, menu)
    }

    override fun getSigns(): List<Item> {
        val act = activity ?: return java.util.ArrayList()

        val items: ArrayList<Item> = arrayListOf()

        val db = SignDatabase.getInstance(act)
        val folders = db.getBookmarksFolders()

        if (folders.isNotEmpty()) {
            items.add(Header("Mappar"))
            items.addAll(folders)
        }

        val signs = db.getFolderSigns(0)
        if (signs.isNotEmpty()) {
            items.add(Header(getString(R.string.unorganized_bookmarks)))
            items.addAll(signs)
        }

        return items
    }

    companion object {
        fun newInstance() = FavoritesFragment()
    }
}