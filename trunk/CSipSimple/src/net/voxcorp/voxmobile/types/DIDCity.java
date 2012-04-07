package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class DIDCity implements Parcelable {
	
	public String state_id;
	public String city_id;
	public String description;
	public Integer did_count;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(state_id);
		dest.writeString(city_id);
		dest.writeString(description);
		dest.writeInt(did_count);

	}

	public DIDCity() {
		super();
		state_id = "";
		city_id = "";
		description = "";
		did_count = 0;
	}

	public DIDCity(Parcel source) {
		super();
		state_id = source.readString();
		city_id = source.readString();
		description = source.readString();
		did_count = source.readInt();
	}
	
    public static final Parcelable.Creator<DIDCity> CREATOR = new Parcelable.Creator<DIDCity>() {
        @Override
        public DIDCity createFromParcel(Parcel in) {
            return new DIDCity(in);
        }

        @Override
        public DIDCity[] newArray(int size) {
            return new DIDCity[size];
        }
    };

    @Override
    public String toString() {
    	return this.description;
    }
}
