package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class RateDialCode implements Parcelable {
	
	public int country_id;
	public String city;
	public String rate;
	public String dial_code;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(country_id);
		dest.writeString(city);
		dest.writeString(rate);
		dest.writeString(dial_code);
	}

	public RateDialCode() {
		super();
		country_id = 0;
		city = "";
		rate = "";
		dial_code = "";
	}

	public RateDialCode(Parcel source) {
		super();
		country_id = source.readInt();
		city = source.readString();
		rate = source.readString();
		dial_code = source.readString();
	}
	
    public static final Parcelable.Creator<RateDialCode> CREATOR = new Parcelable.Creator<RateDialCode>() {
        @Override
        public RateDialCode createFromParcel(Parcel in) {
            return new RateDialCode(in);
        }

        @Override
        public RateDialCode[] newArray(int size) {
            return new RateDialCode[size];
        }
    };
}
