package `in`.rab.tsplex

class Example internal constructor(internal val video: String,
                                   private val description: String,
                                   internal val signId: Int,
                                   internal val signWord: String) : Item() {
    override fun toString(): String {
        return description
    }
}