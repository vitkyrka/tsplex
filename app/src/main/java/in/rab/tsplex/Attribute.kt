package `in`.rab.tsplex

class Attribute constructor(
        val name: String,
        val group: String,
        val tagId: Int,
        val states: Array<AttributeState>,
        val defaultStateName: String? = null
) {
    val dynamic: Boolean
        get() = defaultStateName == null
}