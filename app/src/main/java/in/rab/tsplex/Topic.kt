package `in`.rab.tsplex

class Topic constructor (internal val mName: String) : Item() {
    override fun toString(): String {
        return mName
    }
}