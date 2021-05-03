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
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.google.android.material.chip.Chip
import com.google.android.material.chip.ChipGroup

class ReverseSearchActivity : AppCompatActivity() {
    private var mOrdboken: Ordboken? = null
    private var segments: ArrayList<Segment> = arrayListOf()
    private val numImages: Int = 6
    private var imageViews: ArrayList<ImageView> = arrayListOf()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_reverse_search)

        val toolbar = findViewById<Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        val actionBar = supportActionBar
        actionBar?.setDisplayHomeAsUpEnabled(true)
        actionBar?.title = "Teckenväljare"

        mOrdboken = Ordboken.getInstance(this)

        val imagesContainer = findViewById<LinearLayout>(R.id.images)
        val dpToPixels = resources.displayMetrics.density
        val width = (120 * dpToPixels).toInt()
        val height = (90 * dpToPixels).toInt()

        for (i in 0..numImages) {
            val imageView = ImageView(this)

            val params = LinearLayout.LayoutParams(width, height)
            imagesContainer.addView(imageView, params)
            imageViews.add(imageView)
        }

        intent.getStringExtra("tagIds")?.let {
            loadTagsFromString(it)
        }

        findViewById<Button>(R.id.addSegment).setOnClickListener {
            addSegment()
            updateSearchCount()
        }

        findViewById<Button>(R.id.search).setOnClickListener {
            search()
        }
    }

    private fun loadTagsFromString(tagIds: String, filterRedundant: Boolean = true) {
        var tags: List<TagGroup> = arrayListOf()

        if (tagIds.isNotEmpty()) {
            tags = TagGroupConvert.stringToTagGroups(tagIds)
        }

        Log.i("tags", tags.toString())

        if (tags.isEmpty()) {
            addSegment()
        } else {
            tags.forEach { addSegment(it, filterRedundant) }
        }

        updateSearchCount()
    }

    fun addSegment(group: TagGroup = arrayListOf(), filterRedundant: Boolean = false) {
        val segmentContainer = findViewById<ViewGroup>(R.id.segmentContainer)
        val view = layoutInflater.inflate(R.layout.segment, null)

        segmentContainer.addView(view)

        val segment = Segment(view as ViewGroup)
        segment.create(group, filterRedundant)
        segments.add(segment)
    }

    fun removeSegment(segment: Segment, partial: Boolean = false) {
        val segmentContainer = findViewById<ViewGroup>(R.id.segmentContainer)

        segmentContainer.removeView(segment.view)

        if (!partial) {
            segments.remove(segment)
            updateSearchCount()
        }
    }

    inner class Segment constructor(
            val view: ViewGroup
    ) {
        lateinit var container: LinearLayout
        var holderMap = mutableMapOf<String, ViewGroup>()
        val chips = ArrayList<Chip>()

        fun create(tagGroup: TagGroup, filterRedundant: Boolean = false) {
            Log.i("view", view.toString())

            container = view.findViewById(R.id.container)

            view.findViewById<View>(R.id.removeSegment).setOnClickListener {
                removeSegment(this)
            }

            view.findViewById<View>(R.id.more).let {
                it.setOnClickListener {
                    ChooseDynamicAttributeTask().execute(ChooseDynamicAttributeArgs(this, getAllTagIds(),
                            Attributes.attributes.filter { at -> at.dynamic }.map { it.tagId }.toTypedArray()))
                }
            }

            val flatTags = arrayListOf<Int>()

            tagGroup.forEach { list ->
                flatTags.addAll(list.filter { !Attributes.redundantTagIds.contains(it) || !filterRedundant })
            }

            Attributes.attributes.filter { !it.dynamic }.forEach { at ->
                val activeHeadTag = if (flatTags.contains(at.tagId)) arrayListOf(at.tagId) else arrayListOf()
                val activeStateTags = at.states.filter { state -> flatTags.contains(state.tagId) }.map { state -> state.tagId }
                val activeTags = if (activeStateTags.isNotEmpty()) activeStateTags else activeHeadTag

                addChip(at, activeTags, update = false)
            }

            tagGroup.forEach { list ->
                val tags = list.filter { !filterRedundant || !Attributes.redundantTagIds.contains(it) }

                Attributes.attributes.filter { it.dynamic }.forEach attrLoop@{ at ->
                    val activeHeadTag = if (tags.contains(at.tagId)) arrayListOf(at.tagId) else arrayListOf()
                    val activeStateTags = at.states.filter { state -> tags.contains(state.tagId) }.map { state -> state.tagId }
                    val activeTags = if (activeStateTags.isNotEmpty()) activeStateTags else activeHeadTag

                    if (activeTags.isNotEmpty()) {
                        if (filterRedundant && at.states.isNotEmpty() && activeStateTags.isEmpty()) {
                            return@attrLoop
                        }

                        addChip(at, activeTags, update = false)
                    }
                }
            }
        }

        fun addChip(at: Attribute, initialTags: List<Int>, update: Boolean = true) {
            val flex = holderMap.getOrPut(at.group, {
                val f = ChipGroup(this@ReverseSearchActivity).apply {
                    chipSpacingHorizontal = 5
                }

                if (at.group.isNotEmpty()) {
                    val titleView = TextView(f.context).apply {
                        text = at.group
                        val tv = TypedValue()
                        if (context.theme.resolveAttribute(R.attr.colorOnSurface, tv, true)) {
                            setTextColor(tv.data)
                        }
                    }
                    container.addView(titleView)
                    f.setTag(R.id.titleView, titleView)
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
                        ChooseChipStatesTask().execute(ChooseChipStateArgs(this@Segment, this, at, getAllTagIds(exclude = this)))
                    }
                }

                setOnCloseIconClickListener {
                    removeChip(this)
                }
            })
        }

        fun removeChip(chip: Chip, update: Boolean = true, removeFromSegment: Boolean = true) {
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
                val parent = chip.parent as ChipGroup
                parent.removeView(chip)

                if (removeFromSegment) {
                    this.chips.remove(chip)

                    if (at.group.isNotEmpty() && this.chips.none { (it.getTag(R.id.attribute) as Attribute).group == at.group }) {
                        val titleView = parent.getTag(R.id.titleView) as View

                        container.removeView(titleView)
                        container.removeView(parent)
                        holderMap.remove(at.group)
                    }
                }
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

            ChooseNewDynamicAttributeStatesTask().execute(ChooseNewDynamicAttributeStatesArgs(this, at, getAllTagIds()))
        }

        fun chooseDynamicAttribute(counts: HashMap<Int, Int>) {
            val available = Attributes.attributes.filter {
                it.dynamic && counts.containsKey(it.tagId) && counts[it.tagId]!! > 0
            }

            val builder = AlertDialog.Builder(this@ReverseSearchActivity)
            builder.setTitle(R.string.action)
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

        fun getTagIds(exclude: Chip? = null): TagGroup {
            val group = arrayListOf<TagList>()

            chips.forEach chipForEach@{
                if (it == exclude) {
                    return@chipForEach
                }

                val subTags = it.getTag(R.id.tagIds) as ArrayList<Int>

                if (subTags.isEmpty()) {
                    val defTag = it.getTag(R.id.defaultTagId) as Int
                    if (defTag != -1) {
                        group.add(arrayListOf(defTag))
                    }
                } else {
                    group.add(subTags)
                }
            }

            return group
        }
    }

    fun getSegmentIndex(segment: Segment): Int {
        return segments.indexOf(segment)
    }

    private fun getAllTagIds(exclude: Chip? = null): List<TagGroup> {
        return segments.map { it.getTagIds(exclude) }
    }

    private fun resetFilters() {
        segments.forEach { segment ->
            removeSegment(segment, partial = true)
        }

        segments.clear()
        addSegment()
        updateSearchCount()
    }

    private fun getTagIdsString(): String = TagGroupConvert.tagGroupsToString((getAllTagIds()))

    private fun search() {
        val query = "tags:${getTagIdsString()}"
        Log.i("foo", query)

        val intent = Intent(this, SearchListActivity::class.java)
        intent.action = Intent.ACTION_SEARCH
        intent.putExtra(Intent.EXTRA_TITLE, getString(R.string.matching_signs))
        intent.putExtra(SearchManager.QUERY, query)

        startActivity(intent)
    }

    override fun onPause() {
        super.onPause()

        getSharedPreferences("in.rab.tsplex", 0)?.edit()?.apply {
            putString("reverseSearchTagIds", getTagIdsString())
            apply()
        }
    }

    override fun onResume() {
        super.onResume()

        if (segments.isEmpty()) {
            getSharedPreferences("in.rab.tsplex", 0)?.apply {
                loadTagsFromString(getString("reverseSearchTagIds", "")!!, filterRedundant = false)
            }
        }
    }

    fun openSign(sign: Sign) {
        val intent = Intent(this, SignActivity::class.java)
        intent.putExtra("url", sign.id.toString())
        startActivity(intent)
    }

    private inner class SearchCountTask : AsyncTask<List<List<List<Int>>>, Void, Pair<Int, java.util.ArrayList<Sign>>>() {
        override fun doInBackground(vararg params: List<List<List<Int>>>?): Pair<Int, java.util.ArrayList<Sign>> {
            val tagIds = params[0]!!
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val count = db.getSignsCountByTags(tagIds)

            val signs = if (count > 0) {
                db.getSignsByTags(tagIds, limit = numImages.toString())
            } else {
                arrayListOf()
            }

            return Pair(count, signs)
        }

        override fun onPostExecute(res: Pair<Int, java.util.ArrayList<Sign>>) {
            if (isFinishing) return

            val count = res.first
            var text = "$count tecken matchar"
            val glide = Glide.with(this@ReverseSearchActivity)
            val signs = res.second

            imageViews.forEachIndexed { index, imageView ->
                if (index >= signs.size) {
                    imageView.visibility = View.GONE
                    glide.clear(imageView)
                    return@forEachIndexed
                }

                imageView.setOnClickListener {
                    openSign(signs[index])
                }

                glide.load(signs[index].getImageUrls()[0]).into(imageView)
                imageView.visibility = View.VISIBLE
            }

            if (count > 0) {
                text += " (" + res.second.joinToString("; ") { it.word.toUpperCase() }
                if (count > 5) {
                    text += "..."
                }
                text += ")"
            }

            findViewById<TextView>(R.id.info).text = text
        }
    }

    private fun updateSearchCount() {
        SearchCountTask().execute(getAllTagIds())
    }

    private inner class ChooseChipStateArgs constructor(val segment: Segment, val chip: Chip, val at: Attribute, val tagIds: List<List<List<Int>>>) {
        val segmentIndex = getSegmentIndex(segment)
    }

    private class ChooseChipStateResult constructor(val args: ChooseChipStateArgs, val stateCounts: HashMap<Int, Int>)

    private inner class ChooseChipStatesTask : AsyncTask<ChooseChipStateArgs, Void, ChooseChipStateResult>() {
        override fun doInBackground(vararg params: ChooseChipStateArgs): ChooseChipStateResult {
            val tagIds = params[0].tagIds
            val chip = params[0].chip
            val at = params[0].at
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val headTagId = if (at.tagId == -1) arrayOf() else arrayOf(at.tagId)
            val stateTagIds = at.states.map { it.tagId }.toTypedArray()

            return ChooseChipStateResult(params[0], db.getNewTagsSignCounts(tagIds, headTagId + stateTagIds, params[0].segmentIndex))
        }

        override fun onPostExecute(res: ChooseChipStateResult) {
            if (isFinishing) return
            res.args.segment.chooseChipStates(res.args.chip, res.args.at, res.stateCounts)
        }
    }

    private inner class ChooseDynamicAttributeArgs constructor(val segment: Segment, val tagIds: List<List<List<Int>>>, val newTagIds: Array<Int>) {
        val segmentIndex = getSegmentIndex(segment)
    }

    private class ChooseDynamicAttributeResult constructor(val segment: Segment, val signCounts: HashMap<Int, Int>)

    private inner class ChooseDynamicAttributeTask : AsyncTask<ChooseDynamicAttributeArgs, Void, ChooseDynamicAttributeResult>() {
        override fun doInBackground(vararg params: ChooseDynamicAttributeArgs): ChooseDynamicAttributeResult {
            val tagIds = params[0].tagIds
            val newTagIds = params[0].newTagIds
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)

            return ChooseDynamicAttributeResult(params[0].segment, db.getNewTagsSignCounts(tagIds, newTagIds, params[0].segmentIndex))
        }

        override fun onPostExecute(res: ChooseDynamicAttributeResult) {
            if (isFinishing) return
            res.segment.chooseDynamicAttribute(res.signCounts)
        }
    }

    private inner class ChooseNewDynamicAttributeStatesArgs constructor(val segment: Segment, val at: Attribute, val tagIds: List<List<List<Int>>>) {
        val segmentIndex = getSegmentIndex(segment)
    }

    private class ChooseNewDynamicAttributeStatesResult constructor(val args: ChooseNewDynamicAttributeStatesArgs, val tagCounts: HashMap<Int, Int>)

    private inner class ChooseNewDynamicAttributeStatesTask : AsyncTask<ChooseNewDynamicAttributeStatesArgs, Void, ChooseNewDynamicAttributeStatesResult>() {
        override fun doInBackground(vararg params: ChooseNewDynamicAttributeStatesArgs): ChooseNewDynamicAttributeStatesResult {
            val tagIds = params[0].tagIds
            val at = params[0].at
            val db = SignDatabase.getInstance(this@ReverseSearchActivity)
            val headTagId = if (at.tagId == -1) arrayOf() else arrayOf(at.tagId)
            val stateTagIds = at.states.map { it.tagId }.toTypedArray()

            return ChooseNewDynamicAttributeStatesResult(params[0], db.getNewTagsSignCounts(tagIds, headTagId + stateTagIds, params[0].segmentIndex))
        }

        override fun onPostExecute(res: ChooseNewDynamicAttributeStatesResult) {
            if (isFinishing) return
            res.args.segment.chooseNewDynamicAttributeStates(res.args.at, res.tagCounts)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.reverse_search, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem?): Boolean {
        if (mOrdboken!!.onOptionsItemSelected(this, item)) {
            return true
        }

        if (item?.itemId == R.id.resetSearch) {
            resetFilters()
            return true
        }

        return super.onOptionsItemSelected(item)
    }

}
