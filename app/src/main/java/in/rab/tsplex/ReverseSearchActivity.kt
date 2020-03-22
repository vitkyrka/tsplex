package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.widget.Button
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.flexbox.FlexboxLayout
import com.google.android.material.chip.Chip


class ReverseSearchActivity : AppCompatActivity() {
    private var mOrdboken: Ordboken? = null
    private lateinit var container: FlexboxLayout
    val tags: ArrayList<Int> = arrayListOf()
    val chips = ArrayList<Chip>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reverse_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)

        mOrdboken = Ordboken.getInstance(this)
        container = findViewById(R.id.container)

        findViewById<Button>(R.id.more).let {
            it.setOnClickListener {
                val dynamicAttributes = Attributes.attributes.filter { at -> at.defaultStateName == null }
                val builder = AlertDialog.Builder(this)
                builder.setTitle("More")
                        .setItems(dynamicAttributes.map { at -> at.name }
                                .toTypedArray()) { _, which ->
                            addDynamicAttribute(dynamicAttributes[which])
                        }
                        .show()
            }
        }

        Attributes.attributes.forEach { at ->
            val activeStates = at.states.filter { state -> tags.contains(state.tagId) }
            if (at.defaultStateName != null || activeStates.isNotEmpty()) {
                addChip(at, activeStates.map { state -> state.tagId })
            }
        }

        findViewById<Button>(R.id.search).setOnClickListener {
            search()
        }
    }

    private fun getTagIds() : Array<Array<Int>> {
        val tagIds = arrayListOf<Array<Int>>()

        chips.forEach {
            val subTags = it.getTag(R.id.tagIds) as ArrayList<Int>

            if (subTags.isEmpty()) {
                tagIds.add(arrayOf(it.getTag(R.id.defaultTagId) as Int))
            } else {
                tagIds.add(subTags.toTypedArray())
            }
        }
        return tagIds.toTypedArray()
    }

    private fun search() {
        val tagIds = getTagIds()
        val query = "tags:" + tagIds.joinToString(";") { it.joinToString(",") { tagId -> tagId.toString() } }
        Log.i("foo", query)

        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, "foo")
        intent.putExtra(SearchManager.QUERY, query)

        startActivity(intent)
    }

    private inner class SearchCountTask : AsyncTask<Array<Array<Int>>, Void, Pair<Int, java.util.ArrayList<Sign>>>() {
        override fun doInBackground(vararg params: Array<Array<Int>>?): Pair<Int, java.util.ArrayList<Sign>> {
            val tagIds = params[0]!!
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val count = db.getSignsCountByTags(tagIds)

            val signs = if (count > 0) {
                db.getSignsByTags(tagIds, limit="5")
            } else {
                arrayListOf()
            }

            return Pair(count, signs)
        }

        override fun onPostExecute(res: Pair<Int, java.util.ArrayList<Sign>>) {
            var text = "${res.first} tecken matchar"

            if (res.first > 0) {
                text += " (" + res.second.map { it.word.toUpperCase() }.joinToString("; ") + "...)"
            }

            findViewById<TextView>(R.id.info).text = text
        }
    }

    private fun updateSearchCount() {
        SearchCountTask().execute(getTagIds())
    }

    private fun addChip(at: Attribute, initialTags: List<Int>) {
        container.addView(Chip(this).apply {
            setTag(R.id.defaultTagId, at.tagId)
            chips.add(this)
            refreshChip(this, at, initialTags)

            if (at.states.isNotEmpty()) {
                setOnClickListener {
                    val obj = it.getTag(R.id.tagIds)
                    val tags = if (obj != null) {
                        obj as ArrayList<*>
                    } else {
                        ArrayList<Int>()
                    }
                    val selectedStates =
                            at.states.filter { state -> tags.contains(state.tagId) }
                    val selectedItems =
                            ArrayList(selectedStates.map { state -> state.tagId })
                    val builder = AlertDialog.Builder(context)
                    val dialog = builder.setTitle(at.name)
                            .setMultiChoiceItems(
                                    at.states.map { state -> state.name }.toTypedArray(),
                                    at.states.map { state -> tags.contains(state.tagId) }
                                            .toBooleanArray()
                            ) { _, which, checked ->
                                if (checked) {
                                    selectedItems.add(at.states[which].tagId)
                                } else if (selectedItems.contains(at.states[which].tagId)) {
                                    selectedItems.remove(at.states[which].tagId)
                                }
                            }
                            .setPositiveButton(android.R.string.ok) { _, _ ->
                                refreshChip(this, at, selectedItems)
                            }
                            .setNeutralButton("Kryssa alla", null)
                            .setNegativeButton(android.R.string.cancel) { _, _ ->
                            }
                            .create()

                    dialog.setOnShowListener {
                        dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                            for (i in 0..dialog.listView.adapter.count) {
                                dialog.listView.setItemChecked(i, true)
                            }

                            // OnMultiChoiceClickListener is not called when we do setItemChecked
                            selectedItems.clear()
                            selectedItems.addAll(at.states.map { state -> state.tagId })
                        }
                    }

                    dialog.show()
                }
            }

            setOnCloseIconClickListener {
                removeChip(this, at)
            }
        })
    }

    private fun removeChip(chip: Chip, at: Attribute) {
        chip.setTag(R.id.tagIds, ArrayList<Int>())

        if (at.defaultStateName != null) {
            chip.text = "${at.name}: ${at.defaultStateName}"
            chip.isCloseIconVisible = false
            chip.chipBackgroundColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.transparent))
            chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
            chip.chipStrokeColor =
                    ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.darker_gray))
        } else {
            container.removeView(chip)
            chips.remove(chip)
        }

        updateSearchCount()
    }

    private fun refreshChip(chip: Chip, at: Attribute, tags: List<Int>) {
        val selectedStates = at.states.filter { state -> tags.contains(state.tagId) }

        chip.chipBackgroundColor =
                ColorStateList.valueOf(ContextCompat.getColor(this, android.R.color.holo_blue_light))
        chip.chipStrokeWidth = 0f

        if (selectedStates.isEmpty()) {
            if (at.defaultStateName == null) {
                chip.text = at.name
                chip.isCloseIconVisible = true
            } else {
                removeChip(chip, at)
            }
        } else {
            chip.text = at.name + ": " + selectedStates.joinToString(", ") { state ->
                state.name
            }
            chip.isCloseIconVisible = true
        }

        chip.setTag(R.id.tagIds, ArrayList(tags))
        updateSearchCount()
    }

    private fun addDynamicAttribute(at: Attribute) {
        if (at.states.isEmpty()) {
            addChip(at, arrayListOf())
            return
        }

        val selectedItems = ArrayList<Int>()
        val builder = AlertDialog.Builder(this)
        val dialog = builder.setTitle(at.name)
                .setMultiChoiceItems(
                        at.states.map { state -> state.name }.toTypedArray(),
                        null
                ) { _, which, checked ->
                    Log.i("foo", "$which $checked")
                    if (checked) {
                        selectedItems.add(at.states[which].tagId)
                    } else if (selectedItems.contains(at.states[which].tagId)) {
                        selectedItems.remove(at.states[which].tagId)
                    }
                }
                .setPositiveButton(android.R.string.ok) { _, _ ->
                    addChip(at, selectedItems)
                }
                .setNeutralButton("Kryssa alla", null)
                .setNegativeButton(android.R.string.cancel) { _, _ ->
                }
                .create()

        dialog.setOnShowListener {
            dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener {
                for (i in 0..dialog.listView.adapter.count) {
                    dialog.listView.setItemChecked(i, true)
                }

                // OnMultiChoiceClickListener is not called when we do setItemChecked
                selectedItems.clear()
                selectedItems.addAll(at.states.map { state -> state.tagId })
            }
        }
        dialog.show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
