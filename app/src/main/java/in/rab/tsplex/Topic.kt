package `in`.rab.tsplex

class Topic constructor (internal val id: Int, internal val name: String) : Item() {
    override fun toString(): String {
        return Topics.names[id]!!
    }
}