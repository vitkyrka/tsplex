package `in`.rab.tsplex

import android.app.SearchManager
import android.content.Intent
import android.content.res.ColorStateList
import android.os.AsyncTask
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ReverseSearchActivity : AppCompatActivity() {
    private var mOrdboken: Ordboken? = null
    private var segments: ArrayList<Segment> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reverse_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = "Teckenväljare"

        mOrdboken = Ordboken.getInstance(this)


        val tags = arrayListOf<Int>()

        intent.getIntegerArrayListExtra("tagIds")?.let {
            tags.addAll(it.filter { tagId -> !Attributes.redundantTagIds.contains(tagId) })
        }

        Log.i("tags", tags.toString())

        val segment = Segment(findViewById(R.id.container), tags)

        segment.create()
        segments.add(segment)


        updateSearchCount()

        findViewById<Button>(R.id.search).setOnClickListener {
            search()
        }
    }

    private inner class Segment constructor(
            val container: LinearLayout,
            val tags: ArrayList<Int> = arrayListOf()
    ) {
        var holderMap = mutableMapOf<String, ViewGroup>()
        val chips = ArrayList<Chip>()

        fun create() {
            findViewById<Button>(R.id.more).let {
                it.setOnClickListener {
                    ChooseDynamicAttributeTask().execute(ChooseDynamicAttributeArgs(this, getTagIds(),
                            Attributes.attributes.filter { at -> at.dynamic }.map { it.tagId }.toTypedArray()))
                }
            }

            Attributes.attributes.forEach { at ->
                val activeHeadTag = if (tags.contains(at.tagId)) arrayListOf(at.tagId) else arrayListOf()
                val activeStateTags = at.states.filter { state -> tags.contains(state.tagId) }.map { state -> state.tagId }
                val activeTags = if (activeStateTags.isNotEmpty()) activeStateTags else activeHeadTag

                Log.i("foo", activeTags.toString())
                if (!at.dynamic || activeTags.isNotEmpty()) {
                    Log.i("foo", at.name)
                    addChip(at, activeTags, update = false)
                }
            }
        }

        fun addChip(at: Attribute, initialTags: List<Int>, update: Boolean = true) {
            val flex = holderMap.getOrPut(at.group, {
                val f = ChipGroup(this@ReverseSearchActivity).apply {
                    chipSpacingHorizontal = 5
                }

                if (at.group.isNotEmpty()) {
                    container.addView(TextView(f.context).apply {
                        text = at.group
                        val tv = TypedValue()
                        if (context.theme.resolveAttribute(R.attr.colorOnSurface, tv, true)) {
                            setTextColor(tv.data)
                        }
                    })
                }

                container.addView(f)
                f
            })

            flex.addView(Chip(flex.context).apply {
                setTag(R.id.defaultTagId, at.tagId)
                setTag(R.id.attribute, at)
                // setEnsureMinTouchTargetSize(false)
                chips.add(this)
                refreshChip(this, at, initialTags, update)

                if (at.states.isNotEmpty()) {
                    setOnClickListener {
                        ChooseChipStatesTask().execute(ChooseChipStateArgs(this@Segment, this, at, getTagIds(exclude = this)))
                    }
                }

                setOnCloseIconClickListener {
                    removeChip(this)
                }
            })
        }

        fun removeChip(chip: Chip, update: Boolean = true) {
            val at = chip.getTag(R.id.attribute) as Attribute
            chip.setTag(R.id.tagIds, ArrayList<Int>())

            if (!at.dynamic) {
                chip.text = at.defaultStateName
                chip.isCloseIconVisible = false
                chip.chipBackgroundColor =
                        ColorStateList.valueOf(ContextCompat.getColor(chip.context, android.R.color.transparent))
                chip.chipStrokeWidth = resources.getDimension(R.dimen.chip_stroke_width)
                chip.chipStrokeColor =
                        ColorStateList.valueOf(ContextCompat.getColor(chip.context, android.R.color.darker_gray))
            } else {
                (chip.parent as ViewGroup).removeView(chip)
                this.chips.remove(chip)
            }

            if (update) {
                updateSearchCount()
            }
        }

        fun refreshChip(chip: Chip, at: Attribute, tags: List<Int>, update: Boolean = true) {
            val selectedStates = at.states.filter { state -> tags.contains(state.tagId) }

            chip.chipBackgroundColor =
                    ColorStateList.valueOf(ContextCompat.getColor(chip.context, android.R.color.holo_blue_light))
            chip.chipStrokeWidth = 0f

            if (selectedStates.isEmpty()) {
                if (at.dynamic) {
                    chip.text = at.name
                    chip.isCloseIconVisible = true
                } else {
                    this.removeChip(chip, update)
                }
            } else {
                val stateString = selectedStates.joinToString(", ") { state ->
                    state.name
                }
                chip.text = if (at.dynamic) {
                    at.name + ": " + stateString
                } else {
                    stateString
                }
                chip.isCloseIconVisible = true
            }

            chip.setTag(R.id.tagIds, ArrayList(tags))
            if (update) {
                updateSearchCount()
            }
        }

        fun addDynamicAttribute(at: Attribute) {
            if (at.states.isEmpty()) {
                addChip(at, arrayListOf())
                return
            }

            ChooseNewDynamicAttributeStatesTask().execute(ChooseNewDynamicAttributeStatesArgs(this, at, getTagIds()))
        }

        fun chooseDynamicAttribute(counts: HashMap<Int, Int>) {
            val available = Attributes.attributes.filter {
                it.dynamic && counts.containsKey(it.tagId) && counts[it.tagId]!! > 0
            }

            val builder = AlertDialog.Builder(this@ReverseSearchActivity)
            builder.setTitle("More")
                    .setItems(available.map { "${it.name} (${counts[it.tagId]})" }.toTypedArray()) { _, which ->
                        addDynamicAttribute(available[which])
                    }
                    .show()
        }

        fun chooseChipStates(chip: Chip, at: Attribute, stateCounts: HashMap<Int, Int>) {
            val obj = chip.getTag(R.id.tagIds)
            val tags = if (obj != null) {
                obj as ArrayList<*>
            } else {
                ArrayList<Int>()
            }


            val defaultStateCount = if (stateCounts.containsKey(at.tagId)) {
                stateCounts[at.tagId]
            } else {
                0
            }

            val selectedStates =
                    at.states.filter { state -> tags.contains(state.tagId) }
            val available = at.states.filter {
                (stateCounts.containsKey(it.tagId) && stateCounts[it.tagId]!! > 0) || selectedStates.contains(it)
            }
            val selectedItems =
                    ArrayList(selectedStates.map { state -> state.tagId })
            val builder = AlertDialog.Builder(this@ReverseSearchActivity)
            val dialog = builder
                    .setTitle(if (at.dynamic) "${at.name} (${defaultStateCount})" else {
                        at.name
                    })
                    .setMultiChoiceItems(
                            available.map { state ->
                                val count = if (stateCounts.containsKey(state.tagId)) {
                                    stateCounts[state.tagId]
                                } else {
                                    0
                                }
                                "${state.name} ($count)"
                            }.toTypedArray(),
                            available.map { state -> tags.contains(state.tagId) }
                                    .toBooleanArray()
                    ) { _, which, checked ->
                        if (checked) {
                            selectedItems.add(available[which].tagId)
                        } else if (selectedItems.contains(available[which].tagId)) {
                            selectedItems.remove(available[which].tagId)
                        }
                    }
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        refreshChip(chip, at, selectedItems)
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
                    selectedItems.addAll(available.map { state -> state.tagId })
                }
            }

            dialog.show()
        }

        fun chooseNewDynamicAttributeStates(at: Attribute, stateCounts: HashMap<Int, Int>) {
            val selectedItems = ArrayList<Int>()
            val builder = AlertDialog.Builder(this@ReverseSearchActivity)

            val defaultStateCount = if (stateCounts.containsKey(at.tagId)) {
                stateCounts[at.tagId]
            } else {
                0
            }

            val availableStates = at.states.filter {
                stateCounts.containsKey(it.tagId) && stateCounts[it.tagId]!! > 0
            }

            val dialog = builder.setTitle("${at.name} (${defaultStateCount})")
                    .setMultiChoiceItems(
                            availableStates.map { "${it.name} (${stateCounts[it.tagId]})" }.toTypedArray(),
                            null
                    ) { _, which, checked ->
                        Log.i("foo", "$which $checked")
                        if (checked) {
                            selectedItems.add(availableStates[which].tagId)
                        } else if (selectedItems.contains(availableStates[which].tagId)) {
                            selectedItems.remove(availableStates[which].tagId)
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
                    selectedItems.addAll(availableStates.map { state -> state.tagId })
                }
            }
            dialog.show()
        }
    }

    private fun getTagIds(exclude: Chip? = null): Array<Array<Int>> {
        val tagIds = arrayListOf<Array<Int>>()

        segments.forEach { segment ->
            segment.chips.forEach chipForEach@{
                if (it == exclude) {
                    return@chipForEach
                }

                val subTags = it.getTag(R.id.tagIds) as ArrayList<Int>

                if (subTags.isEmpty()) {
                    val defTag = it.getTag(R.id.defaultTagId) as Int
                    if (defTag != -1) {
                        tagIds.add(arrayOf(defTag))
                    }
                } else {
                    tagIds.add(subTags.toTypedArray())
                }
            }
        }
        return tagIds.toTypedArray()
    }

    private fun resetFilters() {
        segments.forEach { segment ->
            segment.tags.clear()

            segment.chips.forEach {
                segment.removeChip(it, update = false)
            }
        }

        updateSearchCount()
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
                db.getSignsByTags(tagIds, limit = "5")
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

    private class ChooseChipStateArgs constructor(val segment: Segment, val chip: Chip, val at: Attribute, val tagIds: Array<Array<Int>>)
    private class ChooseChipStateResult constructor(val args: ChooseChipStateArgs, val stateCounts: HashMap<Int, Int>)

    private inner class ChooseChipStatesTask : AsyncTask<ChooseChipStateArgs, Void, ChooseChipStateResult>() {
        override fun doInBackground(vararg params: ChooseChipStateArgs): ChooseChipStateResult {
            val tagIds = params[0].tagIds
            val chip = params[0].chip
            val at = params[0].at
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val headTagId = if (at.tagId == -1) arrayOf() else arrayOf(at.tagId)
            val stateTagIds = at.states.map { it.tagId }.toTypedArray()

            return ChooseChipStateResult(params[0], db.getNewTagsSignCounts(tagIds, headTagId + stateTagIds))
        }

        override fun onPostExecute(res: ChooseChipStateResult) {
            res.args.segment.chooseChipStates(res.args.chip, res.args.at, res.stateCounts)
        }
    }

    private class ChooseDynamicAttributeArgs constructor(val segment: Segment, val tagIds: Array<Array<Int>>, val newTagIds: Array<Int>)
    private class ChooseDynamicAttributeResult constructor(val segment: Segment, val signCounts: HashMap<Int, Int>)

    private inner class ChooseDynamicAttributeTask : AsyncTask<ChooseDynamicAttributeArgs, Void, ChooseDynamicAttributeResult>() {
        override fun doInBackground(vararg params: ChooseDynamicAttributeArgs): ChooseDynamicAttributeResult {
            val tagIds = params[0].tagIds
            val newTagIds = params[0].newTagIds
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)

            return ChooseDynamicAttributeResult(params[0].segment, db.getNewTagsSignCounts(tagIds, newTagIds))
        }

        override fun onPostExecute(res: ChooseDynamicAttributeResult) {
            res.segment.chooseDynamicAttribute(res.signCounts)
        }
    }

    private class ChooseNewDynamicAttributeStatesArgs constructor(val segment: Segment, val at: Attribute, val tagIds: Array<Array<Int>>)
    private class ChooseNewDynamicAttributeStatesResult constructor(val args: ChooseNewDynamicAttributeStatesArgs, val tagCounts: HashMap<Int, Int>)

    private inner class ChooseNewDynamicAttributeStatesTask : AsyncTask<ChooseNewDynamicAttributeStatesArgs, Void, ChooseNewDynamicAttributeStatesResult>() {
        override fun doInBackground(vararg params: ChooseNewDynamicAttributeStatesArgs): ChooseNewDynamicAttributeStatesResult {
            val tagIds = params[0].tagIds
            val at = params[0].at
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val headTagId = if (at.tagId == -1) arrayOf() else arrayOf(at.tagId)
            val stateTagIds = at.states.map { it.tagId }.toTypedArray()

            return ChooseNewDynamicAttributeStatesResult(params[0], db.getNewTagsSignCounts(tagIds, headTagId + stateTagIds))
        }

        override fun onPostExecute(res: ChooseNewDynamicAttributeStatesResult) {
            res.args.segment.chooseNewDynamicAttributeStates(res.args.at, res.tagCounts)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (item?.itemId == R.id.reverseSearch) {
            resetFilters()
            return true
        }

        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
