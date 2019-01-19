package `in`.rab.tsplex

class Header constructor (internal val mText: String) : Item() {
    override fun toString(): String {
        return mText
    }
}