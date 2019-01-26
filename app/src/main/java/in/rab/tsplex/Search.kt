package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable

class Search() : Item() {
    constructor(parcel: Parcel) : this()

    override fun writeToParcel(parcel: Parcel?, flags: Int) {
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Search> {
        override fun createFromParcel(parcel: Parcel): Search {
            return Search(parcel)
        }

        override fun newArray(size: Int): Array<Search?> {
            return arrayOfNulls(size)
        }
    }
}