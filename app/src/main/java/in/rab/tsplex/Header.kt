package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable

class Header constructor (internal val mText: String) : Item() {
    constructor(parcel: Parcel) : this(parcel.readString()!!)

    override fun toString(): String {
        return mText
    }

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        parcel.writeString(mText)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Header> {
        override fun createFromParcel(parcel: Parcel): Header {
            return Header(parcel)
        }

        override fun newArray(size: Int): Array<Header?> {
            return arrayOfNulls(size)
        }
    }
}