package `in`.rab.tsplex

class Sign internal constructor(internal val id: Int,
                                internal val word: String,
                                internal val video: String,
                                internal val description: String,
                                internal val transcription: String,
                                internal val comment: String,
                                private val slug: String,
                                private val images: Int,
                                internal val topic1: Long,
                                internal val topic2: Long,
                                internal val topic1Extra: String,
                                internal val topic2Extra: String,
                                internal val examplesCount: Int,
                                internal val occurence: Int) : Item() {
    internal val examples: ArrayList<Example> = ArrayList()
    internal val explanations: ArrayList<Explanation> = ArrayList()

    override fun toString(): String {
        return "$word ($id)"
    }

    internal fun getImageUrls(): Array<String> {
        return Array(images) {
            val number = "%05d".format(id)
            "https://teckensprakslexikon.su.se/photos/%s/%s-%s-photo-%d.jpg".format((number.substring(0..1)),
                    slug, number, it + 1)
        }
    }
}
