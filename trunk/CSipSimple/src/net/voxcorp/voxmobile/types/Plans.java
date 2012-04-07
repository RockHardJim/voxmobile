package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class Plans extends JsonBase {

	public Plan[] plans;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(plans, 0);
	}

	public Plans() {
		super();
	}

	public Plans(Parcel source) {
		super();
		success = source.readInt() == DBBoolean.TRUE ? true : false;
		httpStatus = source.readInt();
		error = source.readString();
		plans = (Plan[]) source.readParcelableArray(Plan.class.getClassLoader());
	}

	public static final Parcelable.Creator<Plans> CREATOR = new Parcelable.Creator<Plans>() {
		@Override
		public Plans createFromParcel(Parcel in) {
			return new Plans(in);
		}

		@Override
		public Plans[] newArray(int size) {
			return new Plans[size];
		}
	};
}
