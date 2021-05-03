package `in`.rab.tsplex

class Topic constructor(internal val id: Long, internal val name: String, internal val extra: String = "") : Item() {
    override fun toString(): String {
        return Topics.names[id]!!
    }
}