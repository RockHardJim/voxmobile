package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class AccountSummary extends JsonBase {

	public String summary;

	@Override
	public int describeContents() {
		return 0;
	}

	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeString(summary);
	}

	public AccountSummary() {
		super();
	}

	public AccountSummary(Parcel source) {
		super();
		success = source.readInt() == DBBoolean.TRUE ? true : false;
		httpStatus = source.readInt();
		error = source.readString();
		summary = source.readString();
	}

	public static final Parcelable.Creator<AccountSummary> CREATOR = new Parcelable.Creator<AccountSummary>() {
		@Override
		public AccountSummary createFromParcel(Parcel in) {
			return new AccountSummary(in);
		}

		@Override
		public AccountSummary[] newArray(int size) {
			return new AccountSummary[size];
		}
	};
}
