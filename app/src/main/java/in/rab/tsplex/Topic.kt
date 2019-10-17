package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable

class Folder constructor (internal val id: Int, internal val name: String) : Item() {
    constructor(parcel: Parcel) : this(
            parcel.readInt(),
            parcel.readString()!!)

    constructor(id: Int) : this(id, Topics.names[id]!!)

    override fun toString(): String {
        return name
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeInt(id)
        parcel.writeString(name)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Topic> {
        override fun createFromParcel(parcel: Parcel): Topic {
            return Topic(parcel)
        }

        override fun newArray(size: Int): Array<Topic?> {
            return arrayOfNulls(size)
        }
    }
}