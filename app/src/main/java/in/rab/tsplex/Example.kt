package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable

class Example internal constructor(internal val video: String, private val description: String) : Parcelable {
    override fun toString(): String {
        return description
    }

    constructor(parcel: Parcel) : this(
            parcel.readString()!!,
            parcel.readString()!!
    )

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(video)
        parcel.writeString(description)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Example> {
        override fun createFromParcel(parcel: Parcel): Example {
            return Example(parcel)
        }

        override fun newArray(size: Int): Array<Example?> {
            return arrayOfNulls(size)
        }
    }
}