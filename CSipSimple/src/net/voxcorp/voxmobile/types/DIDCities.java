package net.voxcorp.voxmobile.types;

import net.voxcorp.voxmobile.provider.DBContract.DBBoolean;

import android.os.Parcel;
import android.os.Parcelable;

public class DIDCities extends JsonBase {
	
	public DIDCity[] cities;

	@Override
	public int describeContents() {
		return 0;
	}
	
	@Override
	public void writeToParcel(Parcel dest, int flags) {
		dest.writeInt(success ? DBBoolean.TRUE : DBBoolean.FALSE);
		dest.writeInt(httpStatus);
		dest.writeString(error);
		dest.writeParcelableArray(cities, 0);
	}

	public DIDCities() {
		super();
	}

	public DIDCities(Parcel source) {
		super();
        success = source.readInt() == DBBoolean.TRUE ? true : false;
        httpStatus = source.readInt();
        error = source.readString();
        cities = (DIDCity[]) source.readParcelableArray(DIDCity.class.getClassLoader());
	}
	
    public static final Parcelable.Creator<DIDCities> CREATOR = new Parcelable.Creator<DIDCities>() {
        @Override
        public DIDCities createFromParcel(Parcel in) {
            return new DIDCities(in);
        }

        @Override
        public DIDCities[] newArray(int size) {
            return new DIDCities[size];
        }
    };
}
