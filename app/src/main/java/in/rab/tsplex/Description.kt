package `in`.rab.tsplex

import android.os.Parcel
import android.os.Parcelable

class Description constructor (internal val mSign: Sign) : Item() {
    constructor(parcel: Parcel) : this(Sign.createFromParcel(parcel))

    override fun writeToParcel(parcel: Parcel, flags: Int) {
        mSign.writeToParcel(parcel, flags)
    }

    override fun describeContents(): Int {
        return 0
    }

    companion object CREATOR : Parcelable.Creator<Description> {
        override fun createFromParcel(parcel: Parcel): Description {
            return Description(parcel)
        }

        override fun newArray(size: Int): Array<Description?> {
            return arrayOfNulls(size)
        }
    }
}