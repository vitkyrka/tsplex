package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable
import java.util.*

class Sign internal constructor(internal val id: Int,
                                internal val word: String,
                                internal val video: String,
                                internal val description: String,
                                internal val slug: String,
                                internal val images: Int,
                                internal val topic1: Int,
                                internal val topic2: Int) : Parcelable {
    internal val examples: ArrayList<Example> = ArrayList()

    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readString(),
            parcel.readInt(),
            parcel.readInt(),
            parcel.readInt()) {
        parcel.readList(examples, Example::class.java.classLoader)
    }

    internal fun getImageUrls(): Array<String> {
        return Array(images, {
            val number = "%05d".format(id)
            "http://teckensprakslexikon.su.se/photos/%s/%s-%s-photo-%d.jpg".format((number.substring(0..1)),
                    slug, number, it + 1)
        })
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(word)
        parcel.writeString(video)
        parcel.writeString(description)
        parcel.writeString(slug)
        parcel.writeInt(topic1)
        parcel.writeInt(topic2)
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
