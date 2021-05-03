package `in`.rab.tsplex


class Explanation internal constructor(internal val video: String,
                                       private val description: String) : Item() {
    override fun toString(): String {
        return description
    }
}