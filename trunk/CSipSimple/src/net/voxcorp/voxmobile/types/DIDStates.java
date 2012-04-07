package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class DIDStates extends JsonBase {
	
	public DIDState[] states;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(states, 0);
	}

	public DIDStates() {
		super();
	}

	public DIDStates(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        states = (DIDState[]) source.readParcelableArray(DIDState.class.getClassLoader());
	}
	
    public static final Parcelable.Creator<DIDStates> CREATOR = new Parcelable.Creator<DIDStates>() {
        @Override
        public DIDStates createFromParcel(Parcel in) {
            return new DIDStates(in);
        }

        @Override
        public DIDStates[] newArray(int size) {
            return new DIDStates[size];
        }
    };
}
