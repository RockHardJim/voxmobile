package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class RatesVersionCheck extends JsonBase {
	
	public int version;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeInt(version);
	}

	public RatesVersionCheck() {
		super();
		version = 0;
	}

	public RatesVersionCheck(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        version = source.readInt();
	}
	
    public static final Parcelable.Creator<RatesVersionCheck> CREATOR = new Parcelable.Creator<RatesVersionCheck>() {
        @Override
        public RatesVersionCheck createFromParcel(Parcel in) {
            return new RatesVersionCheck(in);
        }

        @Override
        public RatesVersionCheck[] newArray(int size) {
            return new RatesVersionCheck[size];
        }
    };
}
