package `in`.rab.tsplex

typealias TagList = List<Int>
typealias TagGroup = List<TagList>

object TagGroupConvert {
    fun tagGroupsToString(tagGroups: List<TagGroup>): String {
        return tagGroups.joinToString("/") { group ->
            group.joinToString(";") { list ->
                list.joinToString(",") { it.toString() }
            }
        }
    }

    fun stringToTagGroups(string: String) : List<TagGroup> {
        return string.split("/").map { group ->
            if (group.isEmpty()) {
                arrayListOf()
            } else {
                group.split(";").map { list ->
                    list.split(",").map { Integer.valueOf(it) }
                }
            }
        }
    }
}