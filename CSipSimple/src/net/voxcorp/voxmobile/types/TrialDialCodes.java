package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class TrialDialCodes extends JsonBase implements Parcelable {

	public TrialDialCode[] allowed;
	public TrialDialCode[] blocked;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(allowed, 0);
		dest.writeParcelableArray(blocked, 0);
	}

	public TrialDialCodes() {
		super();
	}

	public TrialDialCodes(Parcel source) {
		super();
		success = source.readInt() == DBBoolean.TRUE ? true : false;
		httpStatus = source.readInt();
		error = source.readString();
		allowed = (TrialDialCode[]) source.readParcelableArray(TrialDialCode.class.getClassLoader());
		blocked = (TrialDialCode[]) source.readParcelableArray(TrialDialCode.class.getClassLoader());
	}

	public static final Parcelable.Creator<TrialDialCodes> CREATOR = new Parcelable.Creator<TrialDialCodes>() {
		@Override
		public TrialDialCodes createFromParcel(Parcel in) {
			return new TrialDialCodes(in);
		}

		@Override
		public TrialDialCodes[] newArray(int size) {
			return new TrialDialCodes[size];
		}
	};
}
