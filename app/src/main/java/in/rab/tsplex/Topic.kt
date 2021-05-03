package `in`.rab.tsplex

class Folder constructor(internal val id: Int, internal val name: String) : Item() {
    override fun toString(): String {
        return name
    }
}