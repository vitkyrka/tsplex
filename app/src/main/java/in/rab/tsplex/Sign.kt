package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class Sign internal constructor(internal val id: Int,
                                internal val word: String,
                                internal val video: String,
                                internal val description: String,
                                internal val transcription: String,
                                internal val comment: String,
                                private val slug: String,
                                private val images: Int,
                                internal val topic1: Int,
                                internal val topic2: Int,
                                internal val examplesCount: Int) : Parcelable {
    internal val examples: ArrayList<Example> = ArrayList()

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readString()!!,
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()) {
        parcel.readList(examples, Example::class.java.classLoader)
    }

    internal fun getImageUrls(): Array<String> {
        return Array(images) {
            val number = "%05d".format(id)
            "https://teckensprakslexikon.su.se/photos/%s/%s-%s-photo-%d.jpg".format((number.substring(0..1)),
                    slug, number, it + 1)
        }
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(word)
        parcel.writeString(video)
        parcel.writeString(description)
        parcel.writeString(transcription)
        parcel.writeString(comment)
        parcel.writeString(slug)
        parcel.writeInt(images)
        parcel.writeInt(topic1)
        parcel.writeInt(topic2)
        parcel.writeInt(examplesCount)
        parcel.writeList(examples)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Sign> {
        override fun createFromParcel(parcel: Parcel): Sign {
            return Sign(parcel)
        }

        override fun newArray(size: Int): Array<Sign?> {
            return arrayOfNulls(size)
        }
    }
}
