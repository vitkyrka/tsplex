package `in`.rab.tsplex

class Attribute constructor(
        val name: String,
        val tagId: Int,
        val states: Array<AttributeState>,
        val defaultStateName: String? = null
) {
}