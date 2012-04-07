package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class VersionCheck extends JsonBase {
	
	public boolean supported;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeInt(supported ? DBBoolean.TRUE : DBBoolean.FALSE);
	}

	public VersionCheck() {
		super();
		supported = false;
	}

	public VersionCheck(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        supported = source.readInt() == DBBoolean.TRUE ? true : false;
	}
	
    public static final Parcelable.Creator<VersionCheck> CREATOR = new Parcelable.Creator<VersionCheck>() {
        @Override
        public VersionCheck createFromParcel(Parcel in) {
            return new VersionCheck(in);
        }

        @Override
        public VersionCheck[] newArray(int size) {
            return new VersionCheck[size];
        }
    };
}
