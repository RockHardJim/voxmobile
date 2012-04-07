package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class RateCountries extends JsonBase implements Parcelable {

	public int version;
	public RateCountry[] countries;

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
		dest.writeParcelableArray(countries, 0);
	}

	public RateCountries() {
		super();
		version = 0;
	}

	public RateCountries(Parcel source) {
		super();
		success = source.readInt() == DBBoolean.TRUE ? true : false;
		httpStatus = source.readInt();
		error = source.readString();
		version = source.readInt();
		countries = (RateCountry[]) source.readParcelableArray(RateCountry.class.getClassLoader());
	}

	public static final Parcelable.Creator<RateCountries> CREATOR = new Parcelable.Creator<RateCountries>() {
		@Override
		public RateCountries createFromParcel(Parcel in) {
			return new RateCountries(in);
		}

		@Override
		public RateCountries[] newArray(int size) {
			return new RateCountries[size];
		}
	};
}
