package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class TrialDialCode implements Parcelable {
	
	public int country_id;
	public String dial_code;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(country_id);
		dest.writeString(dial_code);
	}

	public TrialDialCode() {
		super();
		country_id = 0;
		dial_code = "";
	}

	public TrialDialCode(Parcel source) {
		super();
		country_id = source.readInt();
		dial_code = source.readString();
	}
	
    public static final Parcelable.Creator<TrialDialCode> CREATOR = new Parcelable.Creator<TrialDialCode>() {
        @Override
        public TrialDialCode createFromParcel(Parcel in) {
            return new TrialDialCode(in);
        }

        @Override
        public TrialDialCode[] newArray(int size) {
            return new TrialDialCode[size];
        }
    };
}
