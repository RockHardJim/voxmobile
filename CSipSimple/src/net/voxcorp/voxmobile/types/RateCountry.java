package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class RateCountry implements Parcelable {
	
	public int country_id;
	public String group;
	public String country;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(country_id);
		dest.writeString(group);
		dest.writeString(country);
	}

	public RateCountry() {
		super();
		country_id = 0;
		group = "";
		country = "";
	}

	public RateCountry(Parcel source) {
		super();
		country_id = source.readInt();
		group = source.readString();
		country = source.readString();
	}
	
    public static final Parcelable.Creator<RateCountry> CREATOR = new Parcelable.Creator<RateCountry>() {
        @Override
        public RateCountry createFromParcel(Parcel in) {
            return new RateCountry(in);
        }

        @Override
        public RateCountry[] newArray(int size) {
            return new RateCountry[size];
        }
    };
}
