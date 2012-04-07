package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class RateDialCodes extends JsonBase implements Parcelable {

	public RateDialCode[] dial_codes;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(dial_codes, 0);
	}

	public RateDialCodes() {
		super();
	}

	public RateDialCodes(Parcel source) {
		super();
		success = source.readInt() == DBBoolean.TRUE ? true : false;
		httpStatus = source.readInt();
		error = source.readString();
		dial_codes = (RateDialCode[]) source.readParcelableArray(RateDialCode.class.getClassLoader());
	}

	public static final Parcelable.Creator<RateDialCodes> CREATOR = new Parcelable.Creator<RateDialCodes>() {
		@Override
		public RateDialCodes createFromParcel(Parcel in) {
			return new RateDialCodes(in);
		}

		@Override
		public RateDialCodes[] newArray(int size) {
			return new RateDialCodes[size];
		}
	};
}
