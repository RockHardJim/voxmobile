package net.voxcorp.voxmobile.types;

import android.os.Parcel;
import android.os.Parcelable;

public class DIDState implements Parcelable {
	
	public String state_id;
	public String description;
	public Integer did_count;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeString(state_id);
		dest.writeString(description);
		dest.writeInt(did_count);
	}

	public DIDState() {
		super();
		state_id = "";
		description = "";
		did_count = 0;
	}

	public DIDState(Parcel source) {
		super();
		state_id = source.readString();
		description = source.readString();
		did_count = source.readInt();
	}
	
    public static final Parcelable.Creator<DIDState> CREATOR = new Parcelable.Creator<DIDState>() {
        @Override
        public DIDState createFromParcel(Parcel in) {
            return new DIDState(in);
        }

        @Override
        public DIDState[] newArray(int size) {
            return new DIDState[size];
        }
    };

    @Override
    public String toString() {
    	return this.description;
    }
}
